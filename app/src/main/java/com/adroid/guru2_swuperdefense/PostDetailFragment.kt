package com.adroid.guru2_swuperdefense

import android.app.AlertDialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

/**
 * 게시글 상세 화면: 전체 본문 + 공감/스크랩 + 댓글 목록/작성.
 *
 * [BoardFragment.getPostById]로 [postId]에 해당하는 [BoardFragment.Post]를 조회해서 그리며,
 * 조회한 post 객체를 직접 들고 있다가 공감 수·댓글을 그 자리에서 mutate한다
 * (Post의 viewCount/commentCount/likeCount/comments는 var 또는 mutableList이므로
 * 별도 update 함수 호출 없이 바로 반영되고, BoardFragment 목록에도 같은 객체 참조라 그대로 보임).
 *
 * 본인 글([BoardFragment.Post.isMine] true)일 때만 상단에 "수정"/"삭제" 링크가 보인다.
 */
class PostDetailFragment : Fragment() {

    private var postId: Int = -1

    private lateinit var commentContainer: LinearLayout
    private lateinit var tvCommentHeader: TextView

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
            parentFragmentManager.popBackStack()
            return
        }
        post.isNew = false

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // ==== 수정 시작: 본인 글일 때만 수정/삭제 링크 노출 ====
        if (post.isMine) {
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
            view.findViewById<TextView>(R.id.btnDeletePost).apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("게시글 삭제")
                        .setMessage("정말로 삭제하시겠습니까?")
                        .setPositiveButton("예") { _, _ ->
                            // TODO: 백엔드 연동 지점 - BoardDao.deletePost()로 교체
                            BoardFragment.deletePost(post.id)
                            Toast.makeText(requireContext(), "게시글을 삭제했습니다.", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                        .setNegativeButton("아니오", null)
                        .show()
                }
            }
        }
        // ==== 수정 끝 ====

        view.findViewById<TextView>(R.id.tvAuthorAvatar).apply {
            text = post.authorInitial
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(post.authorColor)
            }
        }
        view.findViewById<TextView>(R.id.tvAuthorName).text = post.authorInitial
        view.findViewById<TextView>(R.id.tvPostTime).text = post.timeAgo.ifBlank { "방금 전" }

        val tvTag = view.findViewById<TextView>(R.id.tvPostTag)
        if (post.category == "공지") {
            tvTag.visibility = View.GONE
        } else {
            tvTag.text = post.tag
            tvTag.setTextColor(post.tagColor)
        }

        view.findViewById<TextView>(R.id.tvPostTitle).text = post.title
        view.findViewById<TextView>(R.id.tvPostBody).text = post.body
        view.findViewById<TextView>(R.id.tvPostMeta).text = post.metaText()

        val btnLike = view.findViewById<MaterialButton>(R.id.btnLike)
        val btnScrap = view.findViewById<MaterialButton>(R.id.btnScrap)

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

        // TODO: 백엔드 연동 지점 - 사용자별 공감/스크랩 상태를 DB에 저장해서 앱 재실행 후에도 유지되도록
        btnLike.setOnClickListener {
            if (post.hasLiked) post.likeCount-- else post.likeCount++
            post.hasLiked = !post.hasLiked
            refreshLikeButton()
        }

        btnScrap.setOnClickListener {
            post.isScrapped = !post.isScrapped
            refreshScrapButton()
            val message = if (post.isScrapped) "스크랩했습니다." else "스크랩을 취소했습니다."
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        commentContainer = view.findViewById(R.id.commentContainer)
        tvCommentHeader = view.findViewById(R.id.tvCommentHeader)
        renderComments(post)

        val etComment = view.findViewById<EditText>(R.id.etComment)
        view.findViewById<View>(R.id.btnSubmitComment).setOnClickListener {
            val body = etComment.text.toString()
            if (body.isBlank()) {
                Toast.makeText(requireContext(), "댓글 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: 백엔드 연동 지점 - CommentDao.insertComment()로 교체. 지금은 메모리 목록에만 추가.
            post.comments.add(BoardFragment.Comment(author = "나", body = body, timeAgo = "방금 전"))
            post.commentCount++
            etComment.setText("")
            renderComments(post)
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

    companion object {
        private const val ARG_POST_ID = "post_id"

        fun newInstance(postId: Int): PostDetailFragment {
            return PostDetailFragment().apply {
                arguments = Bundle().apply { putInt(ARG_POST_ID, postId) }
            }
        }
    }
}
