package com.adroid.guru2_swuperdefense

// ============================================================================
// 수정 안내: 원래 이 파일은 fragment_board.xml만 그대로 inflate하는 빈 Fragment였음
// (로직 없음). 게시판 검색/카테고리 필터/글 목록 렌더링 로직을 아래에 전부 추가함.
// 세부 추가 지점은 "==== 수정 시작/끝 ====" 주석으로 표시되어 있음.
// ============================================================================

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * 게시판 목록 화면.
 *
 * 책임: 게시글 목록 표시, 검색어/카테고리 필터링, 글쓰기 화면 진입.
 * 이 클래스가 게시판 도메인의 데이터 모델([Post], [Comment])과 저장소([Companion])를
 * 함께 들고 있어서, 다른 화면([WritePostFragment], [PostDetailFragment])은
 * 전부 이 클래스의 companion object 함수(addPost/getPostById/updatePost/deletePost)를
 * 통해서만 데이터에 접근한다 (직접 리스트를 만지지 않음).
 *
 * 화면 흐름:
 *   BoardFragment --(+ 버튼)--> WritePostFragment (새 글)
 *   BoardFragment --(글 클릭)--> PostDetailFragment
 */
class BoardFragment : Fragment() {

    // ==== 수정 시작: 디자인 캡처(0723_141548365_06) 반영 - 작성자 이니셜/색상, 신규글 배지 필드 추가 ====
    // ==== 추가 수정: 게시글 상세화면 구현을 위해 meta(String 한 덩어리) → viewCount/commentCount/timeAgo로 분리,
    //     id(상세화면 조회용), comments(댓글 목록) 필드 추가 ====
    data class Comment(
        val author: String,
        val body: String,
        val timeAgo: String
    )

    // ==== 추가 수정: 수정/삭제 권한 판단을 위해 isMine 필드 추가.
    //     실제 로그인 연동 전이라, 지금은 이 앱에서 직접 작성한 글만 true로 표시함.
    //     TODO: 백엔드 연동 지점 - currentUserId == post.authorId 비교로 교체
    data class Post(
        val id: Int,
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
        val isMine: Boolean = false,
        var hasLiked: Boolean = false,
        var isScrapped: Boolean = false
    ) {
        /** 게시판 목록 카드에 표시할 "조회 n   댓글 n   · 시간" 형태 문자열 */
        fun metaText(): String {
            val base = "조회 ${"%,d".format(viewCount)}     댓글 $commentCount"
            return if (timeAgo.isBlank()) base else "$base     · $timeAgo"
        }
    }
    // ==== 수정 끝 ====

    // ==== 수정 시작: 글쓰기 기능 추가를 위해 posts를 인스턴스 속성 → companion object의 mutableList로 변경.
    // Fragment는 화면 전환마다 새로 생성되므로, 작성한 글이 유지되려면 companion object(앱 프로세스 생존 동안 유지)에 둬야 함.
    companion object {
        private var nextId = 0

        // TODO: 백엔드 연동 지점 - BoardDao.getAllPosts()/insertPost()로 교체. 지금은 메모리 내 샘플 데이터.
        private val posts = mutableListOf(
            Post(nextId++, "공지", 0xFFFF7A00.toInt(), "[공지] 의심스러운 전화·문자 번호 공유 부탁드립니다",
                "최근 다양한 수법의 스미싱과 보이스피싱이 증가하고 있습니다.", 3421, 42, "", "공지",
                "📌", 0xFFFF7A00.toInt()),
            Post(nextId++, "스미싱", 0xFFFF7A00.toInt(), "이런 검찰 번호로 전화가 왔어요",
                "010-1234-5678 이 번호로 검찰이라고 하면서 연락이 왔습니다.", 134, 1, "10분 전", "피싱/스미싱",
                "김", 0xFFFF7A00.toInt(), isNew = true,
                comments = mutableListOf(Comment("박", "저도 같은 번호로 왔어요! 조심하세요.", "6분 전"))),
            Post(nextId++, "피싱", 0xFF5B9DFF.toInt(), "저도 같은 번호로 왔어요!",
                "검찰청 사칭이었어요. 모두 조심하세요!", 58, 3, "20분 전", "피싱/스미싱",
                "박", 0xFF5B9DFF.toInt()),
            Post(nextId++, "계정 도용", 0xFF58C36A.toInt(), "인스타그램 계정이 해킹당했어요",
                "비밀번호가 변경됐다는 메일을 받았는데 로그인이 안 됩니다.", 87, 9, "2시간 전", "계정 도용",
                "정", 0xFF58C36A.toInt()),
            Post(nextId++, "금전 사기", 0xFFD97CF5.toInt(), "중고거래 사기를 당했어요",
                "입금 후 판매자와 연락이 끊겼습니다. 신고 절차가 궁금해요.", 41, 5, "5시간 전", "금전 사기",
                "최", 0xFFD97CF5.toInt()),
            Post(nextId++, "보이스피싱", 0xFFFF7A00.toInt(), "저도 당했습니다.. ㅠㅠ",
                "'경찰입니다' 하고 개인정보를 요구해서 일부 알려줬어요.", 192, 15, "3시간 전", "보이스피싱",
                "이", 0xFF9B7CF5.toInt())
        )

        /** WritePostFragment에서 새 글 등록 시 호출. 목록 맨 위에 추가됨. */
        fun addPost(post: Post) {
            val saved = post.copy(id = nextId++)
            posts.add(0, saved)
            // ==== 추가: 최근활동에 게시글 작성 기록 남기기 ====
            ActivityLog.log(
                icon = "💬",
                title = "게시글 작성",
                description = saved.title,
                type = ActivityLog.Type.BOARD_POST,
                refId = saved.id
            )
        }

        fun getPostById(id: Int): Post? = posts.find { it.id == id }

        /** 본인 글 수정. WritePostFragment 수정 모드에서 호출 */
        fun updatePost(id: Int, title: String, body: String, category: String) {
            val index = posts.indexOfFirst { it.id == id }
            if (index == -1) return
            posts[index] = posts[index].copy(
                title = title,
                body = body,
                category = category,
                tag = category,
                tagColor = categoryTagColor(category)
            )
        }

        /** 본인 글 삭제. PostDetailFragment 삭제 버튼에서 호출 */
        fun deletePost(id: Int) {
            posts.removeAll { it.id == id }
        }

        fun categoryTagColor(category: String): Int = when (category) {
            "피싱/스미싱" -> 0xFFFF7A00.toInt()
            "계정 도용" -> 0xFF58C36A.toInt()
            "금전 사기" -> 0xFFD97CF5.toInt()
            "보이스피싱" -> 0xFF9B7CF5.toInt()
            else -> 0xFF5B9DFF.toInt()
        }
    }
    // ==== 수정 끝 ====

    private var selectedCategory: String = "전체"
    private var searchKeyword: String = ""

    private lateinit var postContainer: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var categoryChips: List<MaterialButton>

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
        // ==== 수정 시작: 디자인 캡처 반영 - "보이스피싱" 칩 참조 추가 ====
        val chipVoicePhishing = view.findViewById<MaterialButton>(R.id.chipVoicePhishing)
        // ==== 수정 끝 ====
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

        // ==== 수정 시작: 글쓰기 화면으로 이동 (기존: Toast 스텁) ====
        view.findViewById<View>(R.id.btnWrite).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, WritePostFragment())
                .addToBackStack(null)
                .commit()
        }
        // ==== 수정 끝 ====

        renderList()
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

    /**
     * 현재 [selectedCategory]/[searchKeyword] 조건으로 [posts]를 필터링해서
     * postContainer를 다시 그린다. 카테고리 칩 클릭·검색어 입력·화면 최초 진입 시 호출됨.
     * 공지("공지" 카테고리)는 어떤 필터를 선택해도 항상 노출된다.
     */
    private fun renderList() {
        postContainer.removeAllViews()

        val filtered = posts.filter { post ->
            val matchesCategory = selectedCategory == "전체" ||
                post.category == "공지" ||
                post.category == selectedCategory
            val matchesSearch = searchKeyword.isBlank() ||
                post.title.contains(searchKeyword, ignoreCase = true) ||
                post.body.contains(searchKeyword, ignoreCase = true)
            matchesCategory && matchesSearch
        }

        tvEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        filtered.forEach { post ->
            postContainer.addView(buildPostCard(post))
        }
    }

    /**
     * 게시글 하나를 카드 View로 조립한다 (XML 없이 코드로 직접 View를 생성하는 방식).
     * 클릭하면 조회수를 1 증가시키고 [PostDetailFragment]로 이동한다.
     */
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

        // ==== 수정 시작: 디자인 캡처 반영 - 좌측 작성자 아바타 원형 + 카테고리 태그 pill 배경 + 신규글 N배지 추가 ====
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

        column.addView(TextView(context).apply {
            text = post.metaText()
            setTextColor(colorOf(R.color.text_secondary))
            textSize = 13f
            setPadding(0, dp(12), 0, 0)
        })

        row.addView(avatar)
        row.addView(column)
        card.addView(row)
        // ==== 수정 끝 ====

        // ==== 수정 시작: 게시글 상세 화면으로 이동 + 조회수 증가 (기존: Toast 스텁) ====
        card.setOnClickListener {
            post.viewCount++
            post.isNew = false
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, PostDetailFragment.newInstance(post.id))
                .addToBackStack(null)
                .commit()
        }
        // ==== 수정 끝 ====

        return card
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun colorOf(colorRes: Int): Int = ContextCompat.getColor(requireContext(), colorRes)
}
