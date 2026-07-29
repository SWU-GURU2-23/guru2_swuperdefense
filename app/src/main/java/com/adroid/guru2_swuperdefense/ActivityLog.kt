package com.adroid.guru2_swuperdefense

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.adroid.guru2_swuperdefense.data.local.AppDatabase
import com.adroid.guru2_swuperdefense.data.local.entity.ActivityLogEntity
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 앱 전역 사용자 활동(스미싱 점검/증거 저장/게시글 작성) 기록.
 * [HomeFragment]와 [ActivityLogListFragment]가 이 object 하나를 공용으로 조회·렌더링한다.
 */
object ActivityLog {

    // NONE: 실제 기능과 연결되지 않은 데모용 샘플 항목 (클릭해도 이동 안 함)
    enum class Type { SMISHING_CHECK, EVIDENCE, BOARD_POST, NONE }

    data class Entry(
        val id: Int,
        val icon: String,
        val title: String,
        val description: String,
        val timestamp: Long,
        val type: Type,
        val refId: Int
    )

    /** @param refId 해당 기능의 상세화면으로 이동할 때 쓸 id (Post.id / Evidence.id / CheckRecord.id) */
    suspend fun log(
        context: Context,
        icon: String,
        title: String,
        description: String,
        type: Type,
        refId: Int
    ) {
        AppDatabase.getInstance(context).activityLogDao().insert(
            ActivityLogEntity(
                icon = icon,
                title = title,
                description = description,
                type = type.name,
                referenceId = refId
            )
        )
    }

    /** 홈 화면용: 최근 N개를 실시간 관찰 */
    fun observeRecent(context: Context, limit: Int): Flow<List<Entry>> =
        AppDatabase.getInstance(context).activityLogDao().observeRecent(limit)
            .map { entities -> entities.map(::toEntry) }

    /** 전체보기 화면용 */
    fun observeAll(context: Context): Flow<List<Entry>> =
        AppDatabase.getInstance(context).activityLogDao().observeAll()
            .map { entities -> entities.map(::toEntry) }

    private fun toEntry(entity: ActivityLogEntity): Entry =
        Entry(
            id = entity.id,
            icon = entity.icon,
            title = entity.title,
            description = entity.description,
            timestamp = entity.timestamp,
            type = runCatching { Type.valueOf(entity.type) }.getOrDefault(Type.NONE),
            refId = entity.referenceId
        )

    fun timeAgo(timestamp: Long): String {
        val minutes = (System.currentTimeMillis() - timestamp) / (60 * 1000)
        return when {
            minutes < 1 -> "방금 전"
            minutes < 60 -> "${minutes}분 전"
            minutes < 24 * 60 -> "${minutes / 60}시간 전"
            else -> SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(timestamp))
        }
    }

    /** 활동 항목 클릭 시 관련 기능/화면으로 이동. 데모용 샘플 항목은 이동 없이 안내만 표시 */
    fun navigateTo(context: Context, fragmentManager: FragmentManager, containerId: Int, entry: Entry) {
        if (entry.type == Type.NONE) {
            Toast.makeText(context, "샘플 데이터입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val fragment = when (entry.type) {
            Type.EVIDENCE -> EvidenceDetailFragment.newInstance(entry.refId)
            Type.BOARD_POST -> PostDetailFragment.newInstance(entry.refId)
            Type.SMISHING_CHECK -> SmishingResultFragment.newInstance(entry.refId)
            Type.NONE -> return
        }
        fragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commit()
    }

    /** 활동 카드 한 개를 만드는 공용 뷰 빌더 (홈 화면/전체보기 화면 공용) */
    fun buildCard(context: Context, entry: Entry): MaterialCardView {
        fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()
        fun color(res: Int) = ContextCompat.getColor(context, res)

        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(9) }
            radius = dp(14).toFloat()
            setCardBackgroundColor(color(R.color.card_charcoal))
            strokeColor = color(R.color.card_border)
            strokeWidth = dp(1)
            cardElevation = 0f
            isClickable = true
            isFocusable = true
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val iconBadge = TextView(context).apply {
            text = entry.icon
            textSize = 18f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            setBackgroundResource(R.drawable.bg_icon_circle)
        }

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { marginStart = dp(14) }
        }

        column.addView(TextView(context).apply {
            text = "${entry.title}     ${timeAgo(entry.timestamp)}"
            setTextColor(color(R.color.text_primary))
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
        })
        column.addView(TextView(context).apply {
            text = entry.description
            setTextColor(color(R.color.text_secondary))
            textSize = 13f
            setPadding(0, dp(6), 0, 0)
        })

        row.addView(iconBadge)
        row.addView(column)
        card.addView(row)
        return card
    }
}
