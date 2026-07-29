package com.adroid.guru2_swuperdefense

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.adroid.guru2_swuperdefense.data.repository.BoardRepository
import com.google.android.material.card.MaterialCardView

class ScrappedPostsFragment : Fragment() {
    private val repository = BoardRepository.instance

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_scrapped_posts, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        loadScraps(view)
    }

    private fun loadScraps(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.scrappedPostContainer)
        val empty = view.findViewById<TextView>(R.id.tvScrapEmpty)
        empty.text = "스크랩한 게시글을 불러오는 중입니다."
        empty.visibility = View.VISIBLE

        repository.getScrappedPosts()
            .addOnSuccessListener { remotePosts ->
                if (!isAdded) return@addOnSuccessListener
                container.removeAllViews()
                val posts = remotePosts.map { BoardFragment.toUiPost(it) }
                empty.text = "아직 스크랩한 게시글이 없습니다."
                empty.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
                posts.forEach { post ->
                    post.isScrapped = true
                    BoardFragment.cachePost(post)
                    container.addView(buildCard(post))
                }
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                empty.text = "스크랩 목록을 불러오지 못했습니다."
                Toast.makeText(
                    requireContext(),
                    "게시판 연결을 확인해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun buildCard(post: BoardFragment.Post): MaterialCardView {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            radius = dp(14).toFloat()
            setCardBackgroundColor(colorOf(R.color.card_charcoal))
            strokeColor = colorOf(R.color.card_border)
            strokeWidth = dp(1)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, PostDetailFragment.newInstance(post.id))
                    .addToBackStack(null)
                    .commit()
            }
        }
        val column = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }
        column.addView(TextView(requireContext()).apply {
            text = "★ ${post.category}"
            setTextColor(colorOf(R.color.orange_primary))
            textSize = 12f
        })
        column.addView(TextView(requireContext()).apply {
            text = post.title
            setTextColor(colorOf(R.color.text_primary))
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, 0)
        })
        column.addView(TextView(requireContext()).apply {
            text = post.body
            maxLines = 2
            setTextColor(colorOf(R.color.text_secondary))
            textSize = 13f
            setPadding(0, dp(6), 0, 0)
        })
        column.addView(TextView(requireContext()).apply {
            text = post.metaText()
            gravity = Gravity.END
            setTextColor(colorOf(R.color.text_secondary))
            textSize = 12f
            setPadding(0, dp(10), 0, 0)
        })
        card.addView(column)
        return card
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun colorOf(colorRes: Int): Int =
        ContextCompat.getColor(requireContext(), colorRes)
}
