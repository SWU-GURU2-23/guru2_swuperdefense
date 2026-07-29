package com.adroid.guru2_swuperdefense

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.adroid.guru2_swuperdefense.data.remote.model.BoardPostDto
import com.adroid.guru2_swuperdefense.data.repository.BoardRepository
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.util.concurrent.TimeUnit

/** 게시판 목록 화면. Firestore의 [BoardRepository] 실시간 목록을 화면 모델로 변환해 표시한다. */
class BoardFragment : Fragment() {

    data class Comment(
        val author: String,
        val body: String,
        val timeAgo: String
    )

    // 실제 작성자 UID는 공개 게시글 문서에 저장하지 않고, 저장소가 확인한 소유 여부만 유지한다.
    data class Post(
        val documentId: String,
        val id: Int,
        val authorKey: String,
        val authorName: String,
        val isAnonymous: Boolean,
        val tag: String,
        val tagColor: Int,
        val title: String,
        val body: String,
        var viewCount: Int,
        var commentCount: Int,
        val timeAgo: String,
        val category: String,
        val authorInitial: String,
        val authorColor: Int,
        var isNew: Boolean = false,
        var likeCount: Int = 0,
        val comments: MutableList<Comment> = mutableListOf(),
        val isMine: Boolean,
        var hasLiked: Boolean = false,
        var isScrapped: Boolean = false,
        val isPinnedNotice: Boolean = false
    ) {
        /** 게시판 목록 카드에 표시할 "조회 n   댓글 n   · 시간" 형태 문자열 */
        fun metaText(): String {
            if (isPinnedNotice) return ""
            val base = "조회 ${"%,d".format(viewCount)}     댓글 $commentCount"
            return if (timeAgo.isBlank()) base else "$base     · $timeAgo"
        }
    }

    companion object {
        private const val PINNED_NOTICE_ID = Int.MIN_VALUE
        private val pinnedNotice = Post(
            documentId = "__pinned_notice__",
            id = PINNED_NOTICE_ID,
            authorKey = "",
            authorName = "SWUPERDEPENSE",
            isAnonymous = false,
            tag = "공지",
            tagColor = 0xFFFF7A00.toInt(),
            title = "[공지] 의심스러운 전화·문자 번호 공유 부탁드립니다",
            body = "최근 다양한 수법의 스미싱과 보이스피싱이 증가하고 있습니다.",
            viewCount = 0,
            commentCount = 0,
            timeAgo = "",
            category = "공지",
            authorInitial = "📌",
            authorColor = 0xFFFF7A00.toInt(),
            isMine = false,
            isPinnedNotice = true
        )
        private val posts = mutableListOf<Post>()

        fun getPostById(id: Int): Post? =
            if (id == PINNED_NOTICE_ID) pinnedNotice else posts.find { it.id == id }

        fun cachePost(post: Post) {
            posts.removeAll { it.id == post.id }
            posts.add(post)
        }

        private fun replacePosts(newPosts: List<Post>) {
            posts.clear()
            posts.addAll(newPosts)
        }

        fun categoryTagColor(category: String): Int = when (category) {
            "피싱/스미싱" -> 0xFFFF7A00.toInt()
            "계정 도용" -> 0xFF58C36A.toInt()
            "금전 사기" -> 0xFFD97CF5.toInt()
            "보이스피싱" -> 0xFF9B7CF5.toInt()
            else -> 0xFF5B9DFF.toInt()
        }

        fun toUiPost(remote: BoardPostDto): Post {
            val authorName =
                if (remote.isAnonymous) "익명" else remote.authorDisplayName.ifBlank { "사용자" }
            return Post(
                documentId = remote.documentId,
                id = remote.localId,
                authorKey = remote.documentId,
                authorName = authorName,
                isAnonymous = remote.isAnonymous,
                tag = remote.category,
                tagColor = categoryTagColor(remote.category),
                title = remote.title,
                body = remote.body,
                viewCount = remote.viewCount.toInt(),
                commentCount = remote.commentCount.toInt(),
                timeAgo = timeAgo(remote.createdAt?.toDate()?.time),
                category = remote.category,
                authorInitial = if (remote.isAnonymous) "익" else authorName.take(1),
                authorColor = if (remote.isAnonymous) {
                    0xFF6B6B6B.toInt()
                } else {
                    avatarColor(remote.documentId)
                },
                isNew = true,
                likeCount = remote.likeCount.toInt(),
                isMine = remote.isMine
            )
        }

        private fun avatarColor(authorKey: String): Int {
            val colors = intArrayOf(
                0xFFFF7A00.toInt(),
                0xFF5B9DFF.toInt(),
                0xFF58C36A.toInt(),
                0xFF9B7CF5.toInt(),
                0xFFD97CF5.toInt()
            )
            return colors[(authorKey.hashCode() and Int.MAX_VALUE) % colors.size]
        }

        private fun timeAgo(timestamp: Long?): String {
            if (timestamp == null) return "방금 전"
            val minutes = TimeUnit.MILLISECONDS.toMinutes(
                (System.currentTimeMillis() - timestamp).coerceAtLeast(0)
            )
            return when {
                minutes < 1 -> "방금 전"
                minutes < 60 -> "${minutes}분 전"
                minutes < 24 * 60 -> "${minutes / 60}시간 전"
                else -> "${minutes / (24 * 60)}일 전"
            }
        }
    }
    private var selectedCategory: String = "전체"
    private var searchKeyword: String = ""
    private var postListener: ListenerRegistration? = null

    private lateinit var postContainer: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var categoryChips: List<MaterialButton>
    private val repository = BoardRepository.instance

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_board,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postContainer = view.findViewById(R.id.postContainer)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)

        val chipAll = view.findViewById<MaterialButton>(R.id.chipAll)
        val chipPhishing = view.findViewById<MaterialButton>(R.id.chipPhishing)
        val chipAccount = view.findViewById<MaterialButton>(R.id.chipAccount)
        val chipMoney = view.findViewById<MaterialButton>(R.id.chipMoney)
        val chipVoicePhishing = view.findViewById<MaterialButton>(R.id.chipVoicePhishing)
        val chipEtc = view.findViewById<MaterialButton>(R.id.chipEtc)

        categoryChips = listOf(chipAll, chipPhishing, chipAccount, chipMoney, chipVoicePhishing, chipEtc)

        val categoryOf = mapOf(
            chipAll to "전체",
            chipPhishing to "피싱/스미싱",
            chipAccount to "계정 도용",
            chipMoney to "금전 사기",
            chipVoicePhishing to "보이스피싱",
            chipEtc to "기타"
        )

        categoryChips.forEach { chip ->
            chip.setOnClickListener {
                selectedCategory = categoryOf[chip] ?: "전체"
                updateChipStyles(chip)
                renderList()
            }
        }

        view.findViewById<EditText>(R.id.etSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchKeyword = s?.toString().orEmpty()
                renderList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        view.findViewById<View>(R.id.btnWrite).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, WritePostFragment())
                .addToBackStack(null)
                .commit()
        }

        // 네트워크 상태와 관계없이 앱 이용 공지는 항상 먼저 보여준다.
        renderList()
        observePosts()
    }

    private fun observePosts() {
        postListener?.remove()
        postListener = repository.observePosts(
            onChanged = { remotePosts ->
                if (!isAdded) return@observePosts
                replacePosts(remotePosts.map(::toUiPost))
                renderList()
                loadUserStates()
            },
            onError = {
                if (!isAdded) return@observePosts
                renderList()
                Toast.makeText(requireContext(), "게시판 연결을 확인해주세요.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadUserStates() {
        posts.forEach { post ->
            repository.getUserPostState(post.documentId)
                .addOnSuccessListener { state ->
                    post.hasLiked = state.hasLiked
                    post.isScrapped = state.isScrapped
                    post.isNew = !state.hasRead
                    if (isAdded && view != null) renderList()
                }
        }
    }

    private fun updateChipStyles(selected: MaterialButton) {
        categoryChips.forEach { chip ->
            val isSelected = chip == selected
            chip.setTextColor(colorOf(if (isSelected) R.color.orange_primary else R.color.text_secondary))
            chip.strokeColor = android.content.res.ColorStateList.valueOf(
                colorOf(if (isSelected) R.color.orange_primary else R.color.card_border)
            )
            chip.setBackgroundColor(if (isSelected) 0xFF24170D.toInt() else android.graphics.Color.TRANSPARENT)
        }
    }

    /** 공지는 필터와 무관하게 항상 노출하고, 나머지는 카테고리/검색어로 필터링해 다시 그린다. */
    private fun renderList() {
        postContainer.removeAllViews()

        val filtered = listOf(pinnedNotice) + posts.filter { post ->
            val matchesCategory = selectedCategory == "전체" ||
                post.category == selectedCategory
            val matchesSearch = searchKeyword.isBlank() ||
                post.title.contains(searchKeyword, ignoreCase = true) ||
                post.body.contains(searchKeyword, ignoreCase = true)
            matchesCategory && matchesSearch
        }

        tvEmptyState.text = "조건에 맞는 게시글이 없습니다."
        tvEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        filtered.forEach { post ->
            postContainer.addView(buildPostCard(post))
        }
    }

    /** 게시글 카드를 코드로 조립한다. 클릭 시 조회수를 1 증가시키고 상세 화면으로 이동한다. */
    private fun buildPostCard(post: Post): MaterialCardView {
        val context = requireContext()

        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            radius = dp(14).toFloat()
            setCardBackgroundColor(colorOf(R.color.card_charcoal))
            strokeColor = colorOf(R.color.card_border)
            strokeWidth = dp(1)
            cardElevation = dp(2).toFloat()
            isClickable = true
            isFocusable = true
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val avatar = TextView(context).apply {
            text = post.authorInitial
            setTextColor(colorOf(R.color.text_primary))
            textSize = 15f
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                topMargin = dp(18)
                marginStart = dp(18)
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(post.authorColor)
            }
        }

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            setPadding(dp(12), dp(18), dp(18), dp(18))
        }

        if (post.category != "공지") {
            val tagRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            tagRow.addView(TextView(context).apply {
                text = post.tag
                setTextColor(post.tagColor)
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dp(10), dp(5), dp(10), dp(5))
                setBackgroundResource(R.drawable.bg_category_chip)
            })
            column.addView(tagRow)
        }

        val titleRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        titleRow.addView(TextView(context).apply {
            text = post.title
            setTextColor(colorOf(R.color.text_primary))
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        if (post.isNew) {
            titleRow.addView(TextView(context).apply {
                text = "N"
                setTextColor(colorOf(R.color.text_primary))
                textSize = 11f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(18), dp(18))
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(colorOf(R.color.orange_primary))
                }
            })
        }
        titleRow.setPadding(0, if (post.category != "공지") dp(7) else 0, 0, 0)
        column.addView(titleRow)

        column.addView(TextView(context).apply {
            text = post.body
            setTextColor(colorOf(R.color.text_secondary))
            textSize = 14f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, dp(9), 0, 0)
        })

        if (!post.isPinnedNotice) {
            column.addView(TextView(context).apply {
                text = post.metaText()
                setTextColor(colorOf(R.color.text_secondary))
                textSize = 13f
                setPadding(0, dp(12), 0, 0)
            })
        }

        row.addView(avatar)
        row.addView(column)
        card.addView(row)

        card.setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, PostDetailFragment.newInstance(post.id))
                .addToBackStack(null)
                .commit()
        }

        return card
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun colorOf(colorRes: Int): Int = ContextCompat.getColor(requireContext(), colorRes)

    override fun onDestroyView() {
        postListener?.remove()
        postListener = null
        super.onDestroyView()
    }
}
