package com.adroid.guru2_swuperdefense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

/**
 * 게시판 글쓰기/수정 화면. 한 화면을 두 모드로 재사용한다.
 * - 새 글 작성: [newInstance]로 진입 → editPostId 없음 → 저장 시 [BoardFragment.addPost] 호출
 * - 기존 글 수정: [newInstanceForEdit]로 진입 → editPostId 있음 → 화면 진입 시 기존 제목/내용/
 *   카테고리를 입력창에 미리 채워두고, 저장 시 [BoardFragment.updatePost] 호출
 *
 * 진입 경로: [BoardFragment]의 "+" 버튼(새 글) / [PostDetailFragment]의 "수정" 링크(본인 글만)
 */
class WritePostFragment : Fragment() {

    private var editPostId: Int? = null
    private var selectedCategory: String = "피싱/스미싱"
    private lateinit var categoryChips: List<MaterialButton>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_write_post,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postId = arguments?.getInt(ARG_EDIT_POST_ID, -1) ?: -1
        editPostId = if (postId != -1) postId else null
        val editingPost = editPostId?.let { BoardFragment.getPostById(it) }

        val chipPhishing = view.findViewById<MaterialButton>(R.id.chipCatPhishing)
        val chipAccount = view.findViewById<MaterialButton>(R.id.chipCatAccount)
        val chipMoney = view.findViewById<MaterialButton>(R.id.chipCatMoney)
        val chipVoice = view.findViewById<MaterialButton>(R.id.chipCatVoice)
        val chipEtc = view.findViewById<MaterialButton>(R.id.chipCatEtc)

        categoryChips = listOf(chipPhishing, chipAccount, chipMoney, chipVoice, chipEtc)

        val categoryOf = mapOf(
            chipPhishing to "피싱/스미싱",
            chipAccount to "계정 도용",
            chipMoney to "금전 사기",
            chipVoice to "보이스피싱",
            chipEtc to "기타"
        )

        categoryChips.forEach { chip ->
            chip.setOnClickListener {
                selectedCategory = categoryOf[chip] ?: "피싱/스미싱"
                updateChipStyles(chip)
            }
        }

        val etTitle = view.findViewById<EditText>(R.id.etPostTitle)
        val etBody = view.findViewById<EditText>(R.id.etPostBody)
        val tvError = view.findViewById<TextView>(R.id.tvWriteError)
        val tvHeader = view.findViewById<TextView>(R.id.tvWriteHeader)
        val btnSubmit = view.findViewById<MaterialButton>(R.id.btnSubmitPost)

        // ==== 수정 시작: 수정 모드일 때 기존 글 내용을 입력창에 미리 채워둠 ====
        if (editingPost != null) {
            tvHeader.text = "글 수정"
            btnSubmit.text = "수정하기"
            etTitle.setText(editingPost.title)
            etBody.setText(editingPost.body)
            selectedCategory = editingPost.category
            val matchedChip = categoryOf.entries.find { it.value == editingPost.category }?.key
            if (matchedChip != null) updateChipStyles(matchedChip)
        }
        // ==== 수정 끝 ====

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSubmit.setOnClickListener {
            val title = etTitle.text.toString()
            val body = etBody.text.toString()

            if (title.isBlank() || body.isBlank()) {
                tvError.text = "제목과 내용을 모두 입력해주세요."
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            // TODO: 백엔드 연동 지점 - BoardDao.insertPost()/updatePost()로 교체. 지금은 companion object 메모리 목록만 갱신.
            val currentEditId = editPostId
            if (currentEditId != null) {
                BoardFragment.updatePost(currentEditId, title, body, selectedCategory)
                Toast.makeText(requireContext(), "게시글을 수정했습니다.", Toast.LENGTH_SHORT).show()
            } else {
                // id는 addPost()에서 실제 값으로 다시 부여하므로 여기선 임시값(0)만 넣음
                BoardFragment.addPost(
                    BoardFragment.Post(
                        id = 0,
                        tag = selectedCategory,
                        tagColor = BoardFragment.categoryTagColor(selectedCategory),
                        title = title,
                        body = body,
                        viewCount = 0,
                        commentCount = 0,
                        timeAgo = "방금 전",
                        category = selectedCategory,
                        authorInitial = "나",
                        authorColor = colorOf(R.color.orange_primary),
                        isNew = true,
                        isMine = true
                    )
                )
                Toast.makeText(requireContext(), "게시글이 등록되었습니다.", Toast.LENGTH_SHORT).show()
            }

            parentFragmentManager.popBackStack()
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

    private fun colorOf(colorRes: Int): Int = ContextCompat.getColor(requireContext(), colorRes)

    companion object {
        private const val ARG_EDIT_POST_ID = "edit_post_id"

        /** 새 글 작성 */
        fun newInstance(): WritePostFragment = WritePostFragment()

        /** 기존 글 수정 */
        fun newInstanceForEdit(postId: Int): WritePostFragment {
            return WritePostFragment().apply {
                arguments = Bundle().apply { putInt(ARG_EDIT_POST_ID, postId) }
            }
        }
    }
}
