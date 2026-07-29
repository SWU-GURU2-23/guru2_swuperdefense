package com.adroid.guru2_swuperdefense

import android.app.AlertDialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.adroid.guru2_swuperdefense.data.remote.model.BoardCommentDto
import com.adroid.guru2_swuperdefense.data.repository.AuthRepository
import com.adroid.guru2_swuperdefense.data.repository.BoardRepository
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.material.button.MaterialButton
import java.util.concurrent.TimeUnit

/** 게시글 상세 화면: 본문 + 공감/스크랩 + 댓글. 본인 글일 때만 "수정"/"삭제" 링크가 보인다. */
class PostDetailFragment : Fragment() {

    private var postId: Int = -1

    private lateinit var commentContainer: LinearLayout
    private lateinit var tvCommentHeader: TextView
    private var commentListener: ListenerRegistration? = null
    private val repository = BoardRepository.instance
    private val authRepository = AuthRepository.instance

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_post_detail,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postId = arguments?.getInt(ARG_POST_ID) ?: -1
        val post = BoardFragment.getPostById(postId)

        if (post == null) {
            repository.getPostByLocalId(postId)
                .addOnSuccessListener { remote ->
                    if (!isAdded || viewLifecycleOwner.lifecycle.currentState ==
                        androidx.lifecycle.Lifecycle.State.DESTROYED
                    ) {
                        return@addOnSuccessListener
                    }
                    if (remote == null) {
                        Toast.makeText(
                            requireContext(),
                            "게시글이 삭제되었거나 존재하지 않습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                        parentFragmentManager.popBackStack()
                        return@addOnSuccessListener
                    }
                    val loadedPost = BoardFragment.toUiPost(remote)
                    BoardFragment.cachePost(loadedPost)
                    repository.getUserPostState(loadedPost.documentId)
                        .addOnSuccessListener { state ->
                            loadedPost.hasLiked = state.hasLiked
                            loadedPost.isScrapped = state.isScrapped
                            loadedPost.isNew = !state.hasRead
                            this@PostDetailFragment.view?.let {
                                renderPost(it, loadedPost)
                            }
                        }
                        .addOnFailureListener {
                            this@PostDetailFragment.view?.let {
                                renderPost(it, loadedPost)
                            }
                        }
                }
                .addOnFailureListener {
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "게시글을 불러오지 못했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                        parentFragmentManager.popBackStack()
                    }
                }
            return
        }
        renderPost(view, post)
    }

    private fun renderPost(view: View, post: BoardFragment.Post) {
        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        if (!post.isPinnedNotice) {
            post.viewCount++
            post.isNew = false
            repository.incrementViewCount(post.documentId)
            repository.markRead(post.documentId)
        }

        if (post.isMine && !post.isPinnedNotice) {
            view.findViewById<TextView>(R.id.btnEditPost).apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    parentFragmentManager
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, WritePostFragment.newInstanceForEdit(post.id))
                        .addToBackStack(null)
                        .commit()
                }
            }
            configureDeleteButton(view, post, isAdminDelete = false)
        } else if (!post.isPinnedNotice) {
            authRepository.isCurrentUserAdmin()
                .addOnSuccessListener { isAdmin ->
                    if (isAdmin && isAdded) {
                        configureDeleteButton(view, post, isAdminDelete = true)
                    }
                }
        }

        view.findViewById<TextView>(R.id.tvAuthorAvatar).apply {
            text = post.authorInitial
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(post.authorColor)
            }
        }

        view.findViewById<TextView>(R.id.tvAuthorName).text = post.authorName
        view.findViewById<TextView>(R.id.tvPostTime).text =
            if (post.isPinnedNotice) "상단 고정 공지" else post.timeAgo.ifBlank { "방금 전" }

        val tvTag = view.findViewById<TextView>(R.id.tvPostTag)
        if (post.category == "공지") {
            tvTag.visibility = View.GONE
        } else {
            tvTag.text = post.tag
            tvTag.setTextColor(post.tagColor)
        }

        view.findViewById<TextView>(R.id.tvPostTitle).text = post.title
        view.findViewById<TextView>(R.id.tvPostBody).text = post.body
        view.findViewById<TextView>(R.id.tvPostMeta).apply {
            text = post.metaText()
            visibility = if (post.isPinnedNotice) View.GONE else View.VISIBLE
        }

        val btnLike = view.findViewById<MaterialButton>(R.id.btnLike)
        val btnScrap = view.findViewById<MaterialButton>(R.id.btnScrap)
        if (post.isPinnedNotice) {
            btnLike.visibility = View.GONE
            btnScrap.visibility = View.GONE
            view.findViewById<View>(R.id.tvCommentHeader).visibility = View.GONE
            view.findViewById<View>(R.id.commentContainer).visibility = View.GONE
            view.findViewById<View>(R.id.commentInputRow).visibility = View.GONE
            return
        }

        fun refreshLikeButton() {
            btnLike.text = "👍 공감 ${post.likeCount}"
            val color = colorOf(if (post.hasLiked) R.color.orange_primary else R.color.text_secondary)
            btnLike.setTextColor(color)
            btnLike.strokeColor = android.content.res.ColorStateList.valueOf(color)
        }

        fun refreshScrapButton() {
            val color = colorOf(if (post.isScrapped) R.color.orange_primary else R.color.text_secondary)
            btnScrap.text = if (post.isScrapped) "★ 스크랩" else "☆ 스크랩"
            btnScrap.setTextColor(color)
            btnScrap.strokeColor = android.content.res.ColorStateList.valueOf(color)
        }

        refreshLikeButton()
        refreshScrapButton()
        repository.getUserPostState(post.documentId)
            .addOnSuccessListener { state ->
                if (!isAdded) return@addOnSuccessListener
                post.hasLiked = state.hasLiked
                post.isScrapped = state.isScrapped
                post.isNew = !state.hasRead
                refreshLikeButton()
                refreshScrapButton()
            }

        btnLike.setOnClickListener {
            btnLike.isEnabled = false
            repository.toggleLike(post.documentId)
                .addOnSuccessListener { liked ->
                    post.likeCount = (post.likeCount + if (liked) 1 else -1).coerceAtLeast(0)
                    post.hasLiked = liked
                    btnLike.isEnabled = true
                    refreshLikeButton()
                }
                .addOnFailureListener {
                    btnLike.isEnabled = true
                    Toast.makeText(requireContext(), "공감 상태를 저장하지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
        }

        btnScrap.setOnClickListener {
            val newState = !post.isScrapped
            btnScrap.isEnabled = false
            repository.setScrapped(post.documentId, newState)
                .addOnSuccessListener {
                    post.isScrapped = newState
                    btnScrap.isEnabled = true
                    refreshScrapButton()
                    val message = if (newState) "스크랩했습니다." else "스크랩을 취소했습니다."
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    btnScrap.isEnabled = true
                    Toast.makeText(requireContext(), "스크랩 상태를 저장하지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
        }

        commentContainer = view.findViewById(R.id.commentContainer)
        tvCommentHeader = view.findViewById(R.id.tvCommentHeader)
        observeComments(post)

        val etComment = view.findViewById<EditText>(R.id.etComment)
        val cbAnonymous = view.findViewById<CheckBox>(R.id.cbAnonymousComment)
        val btnSubmitComment = view.findViewById<MaterialButton>(R.id.btnSubmitComment)
        btnSubmitComment.setOnClickListener {
            val body = etComment.text.toString().trim()
            if (body.isBlank()) {
                Toast.makeText(requireContext(), "댓글 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSubmitComment.isEnabled = false
            repository.addComment(post.documentId, body, cbAnonymous.isChecked)
                .addOnSuccessListener {
                    etComment.setText("")
                    cbAnonymous.isChecked = false
                    btnSubmitComment.isEnabled = true
                }
                .addOnFailureListener {
                    btnSubmitComment.isEnabled = true
                    Toast.makeText(requireContext(), "댓글을 등록하지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun configureDeleteButton(
        view: View,
        post: BoardFragment.Post,
        isAdminDelete: Boolean
    ) {
        view.findViewById<TextView>(R.id.btnDeletePost).apply {
            visibility = View.VISIBLE
            text = if (isAdminDelete) "관리자 삭제" else "삭제"
            setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle(if (isAdminDelete) "관리자 권한으로 삭제" else "게시글 삭제")
                    .setMessage("정말로 이 게시글을 삭제하시겠습니까?")
                    .setPositiveButton("삭제") { _, _ ->
                        commentListener?.remove()
                        commentListener = null
                        repository.deletePost(post.documentId)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    requireContext(),
                                    "게시글을 삭제했습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                parentFragmentManager.popBackStack()
                            }
                            .addOnFailureListener {
                                if (isAdded && this@PostDetailFragment.view != null) {
                                    observeComments(post)
                                }
                                Toast.makeText(
                                    requireContext(),
                                    "삭제 권한 또는 네트워크 연결을 확인해주세요.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        }
    }

    private fun observeComments(post: BoardFragment.Post) {
        commentListener?.remove()
        commentListener = repository.observeComments(
            postDocumentId = post.documentId,
            onChanged = { remoteComments ->
                post.comments.clear()
                post.comments.addAll(remoteComments.map(::toUiComment))
                post.commentCount = remoteComments.size
                if (isAdded && view != null) renderComments(post)
            },
            onError = {
                if (isAdded) {
                    Toast.makeText(requireContext(), "댓글을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun toUiComment(comment: BoardCommentDto): BoardFragment.Comment {
        val author = if (comment.isAnonymous) {
            comment.anonymousNumber?.let { "익명 $it" } ?: "익명"
        } else {
            comment.authorDisplayName.ifBlank { "사용자" }
        }
        return BoardFragment.Comment(
            author = author,
            body = comment.body,
            timeAgo = timeAgo(comment.createdAt?.toDate()?.time)
        )
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

    private fun renderComments(post: BoardFragment.Post) {
        tvCommentHeader.text = "댓글 (${post.commentCount})"
        commentContainer.removeAllViews()

        if (post.comments.isEmpty()) {
            commentContainer.addView(TextView(requireContext()).apply {
                text = "아직 댓글이 없습니다. 첫 댓글을 남겨보세요."
                setTextColor(colorOf(R.color.text_secondary))
                textSize = 13f
            })
            return
        }

        post.comments.forEach { comment ->
            commentContainer.addView(buildCommentRow(comment))
        }
    }

    private fun buildCommentRow(comment: BoardFragment.Comment): LinearLayout {
        val context = requireContext()

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
        }

        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headerRow.addView(TextView(context).apply {
            text = comment.author
            setTextColor(colorOf(R.color.text_primary))
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        headerRow.addView(TextView(context).apply {
            text = "   ${comment.timeAgo}"
            setTextColor(colorOf(R.color.text_secondary))
            textSize = 12f
        })

        row.addView(headerRow)
        row.addView(TextView(context).apply {
            text = comment.body
            setTextColor(colorOf(R.color.text_primary))
            textSize = 14f
            setPadding(0, dp(4), 0, 0)
        })

        return row
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun colorOf(colorRes: Int): Int = ContextCompat.getColor(requireContext(), colorRes)

    override fun onDestroyView() {
        commentListener?.remove()
        commentListener = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_POST_ID = "post_id"

        fun newInstance(postId: Int): PostDetailFragment {
            return PostDetailFragment().apply {
                arguments = Bundle().apply { putInt(ARG_POST_ID, postId) }
            }
        }
    }
}
