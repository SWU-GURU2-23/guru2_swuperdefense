package com.adroid.guru2_swuperdefense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.adroid.guru2_swuperdefense.data.repository.BoardRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch

/** 게시판 글쓰기/수정 화면. editPostId 유무로 새 글 작성과 기존 글 수정 두 모드를 한 화면에서 재사용한다. */
class WritePostFragment : Fragment() {

    private var editPostId: Int? = null
    private var selectedCategory: String = "피싱/스미싱"
    private lateinit var categoryChips: List<MaterialButton>
    private val repository = BoardRepository.instance

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
        val cbAnonymousPost = view.findViewById<CheckBox>(R.id.cbAnonymousPost)
        val btnSubmit = view.findViewById<MaterialButton>(R.id.btnSubmitPost)

        if (editingPost != null) {
            tvHeader.text = "글 수정"
            btnSubmit.text = "수정하기"
            etTitle.setText(editingPost.title)
            etBody.setText(editingPost.body)
            cbAnonymousPost.isChecked = editingPost.isAnonymous
            selectedCategory = editingPost.category
            val matchedChip = categoryOf.entries.find { it.value == editingPost.category }?.key
            if (matchedChip != null) updateChipStyles(matchedChip)
        }

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

            val currentEditId = editPostId
            btnSubmit.isEnabled = false
            btnSubmit.text = if (currentEditId != null) "수정 중..." else "등록 중..."

            if (currentEditId != null) {
                val post = BoardFragment.getPostById(currentEditId)
                if (post == null || !post.isMine) {
                    showSaveError(btnSubmit, tvError, "수정할 수 없는 게시글입니다.")
                    return@setOnClickListener
                }
                repository.updatePost(
                    documentId = post.documentId,
                    category = selectedCategory,
                    title = title.trim(),
                    body = body.trim(),
                    isAnonymous = cbAnonymousPost.isChecked
                ).addOnSuccessListener {
                    Toast.makeText(requireContext(), "게시글을 수정했습니다.", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }.addOnFailureListener { error ->
                    showSaveError(btnSubmit, tvError, saveErrorMessage("수정", error))
                }
            } else {
                repository.createPost(
                    category = selectedCategory,
                    title = title.trim(),
                    body = body.trim(),
                    isAnonymous = cbAnonymousPost.isChecked
                ).addOnSuccessListener { document ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        ActivityLog.log(
                            context = requireContext(),
                            icon = "💬",
                            title = "게시글 작성",
                            description = title.trim(),
                            type = ActivityLog.Type.BOARD_POST,
                            refId = document.id.hashCode() and Int.MAX_VALUE
                        )
                        Toast.makeText(
                            requireContext(),
                            "게시글이 등록되었습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                        parentFragmentManager.popBackStack()
                    }
                }.addOnFailureListener { error ->
                    showSaveError(btnSubmit, tvError, saveErrorMessage("등록", error))
                }
            }
        }
    }

    private fun saveErrorMessage(action: String, error: Exception): String {
        val code = (error as? FirebaseFirestoreException)?.code
        return when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "게시글을 ${action}할 권한이 없습니다. 최신 앱인지 확인한 후 다시 로그인해주세요."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "게시글을 ${action}하지 못했습니다. 네트워크 연결을 확인해주세요."
            else ->
                "게시글을 ${action}하지 못했습니다. 잠시 후 다시 시도해주세요."
        }
    }

    private fun showSaveError(button: MaterialButton, errorView: TextView, message: String) {
        button.isEnabled = true
        button.text = if (editPostId != null) "수정하기" else "등록하기"
        errorView.text = message
        errorView.visibility = TextView.VISIBLE
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
