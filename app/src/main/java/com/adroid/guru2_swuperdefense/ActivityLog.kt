package com.adroid.guru2_swuperdefense

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 앱 전역 사용자 활동(스미싱 점검/증거 저장/게시글 작성) 기록.
 * Fragment가 아니라 순수 데이터+유틸 object라서, 어떤 화면에서든 `ActivityLog.log(...)`만
 * 호출하면 기록되고, [HomeFragment]의 "최근 활동"과 [ActivityLogListFragment]의 "전체 보기"가
 * 이 객체 하나를 공용으로 조회한다. 카드 UI([buildCard])와 클릭 시 이동 로직([navigateTo])도
 * 여기 모아둬서 두 화면이 완전히 동일하게 동작한다.
 *
 * TODO: 백엔드 연동 지점 - ActivityLogDao로 교체. 지금은 앱 실행 중에만 유지되는 메모리 로그.
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

    private var nextId = 0
    private val entries = mutableListOf<Entry>()

    // ==== 추가: 데모/디자인 확인용 하드코딩 샘플 3개를 초기값으로 유지.
    // 실제 활동이 쌓이면 시간순으로 자연스럽게 아래로 밀려남. 아직은 삭제하지 않음.
    init {
        val now = System.currentTimeMillis()
        entries.add(Entry(nextId++, "🛡", "스미싱 URL 검사", "위험 URL 차단됨", now, Type.NONE, -1))
        entries.add(Entry(nextId++, "📁", "증거 파일 저장", "증거 2건 저장 완료", now - 23 * 60 * 1000L, Type.NONE, -1))
        entries.add(Entry(nextId++, "🏛", "금융기관 신고 완료", "신한은행", now - 60 * 60 * 1000L, Type.NONE, -1))
    }

    /**
     * 새 활동 1건 기록. 목록 맨 앞(최신순)에 추가된다.
     * 호출 지점: [BoardFragment.addPost], [EvidenceFragment.addEvidence], [SmishingCheckFragment]의 분석 버튼.
     * @param refId 해당 기능의 상세화면으로 이동할 때 쓸 id (Post.id / Evidence.id / CheckRecord.id)
     */
    fun log(icon: String, title: String, description: String, type: Type, refId: Int) {
        entries.add(0, Entry(nextId++, icon, title, description, System.currentTimeMillis(), type, refId))
    }

    /** 홈 화면용: 최근 N개만 */
    fun recent(limit: Int): List<Entry> = entries.take(limit)

    /** 전체보기 화면용 */
    fun all(): List<Entry> = entries.toList()

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
