package com.adroid.guru2_swuperdefense

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

/**
 * "상황 확인 결과" 화면의 "가장 먼저 해야 할 행동"/"주의사항" 본문이 길어 한 화면에
 * 넣지 않고 각각 별도 화면으로 분리하기 위한 공용 상세보기 화면.
 *
 * 문단을 한 덩어리 텍스트로 붙여넣으면 가독성이 떨어지므로, 항목별로 카드 1개씩
 * 나눠서 보여준다 ([numbered]가 true면 순서대로 번호를, false면 불릿(•)을 붙임).
 */
class ResultDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_result_detail,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString(ARG_TITLE).orEmpty()
        val items = arguments?.getStringArrayList(ARG_ITEMS) ?: arrayListOf()
        val numbered = arguments?.getBoolean(ARG_NUMBERED) ?: true

        view.findViewById<TextView>(R.id.tvDetailTitle).text = title

        val container = view.findViewById<LinearLayout>(R.id.containerDetailItems)
        items.forEachIndexed { index, itemText ->
            container.addView(buildItemCard(requireContext(), index, itemText, numbered))
        }

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun buildItemCard(
        context: Context,
        index: Int,
        itemText: String,
        numbered: Boolean
    ): View {
        val density = context.resources.displayMetrics.density

        val card = MaterialCardView(context).apply {
            val cardParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            cardParams.bottomMargin = (10 * density).toInt()
            layoutParams = cardParams
            radius = 14 * density
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_charcoal))
            strokeColor = ContextCompat.getColor(context, R.color.card_border)
            strokeWidth = (1 * density).toInt()
            cardElevation = 0f
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val padding = (14 * density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        val badge = TextView(context).apply {
            val size = (26 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
            background = ContextCompat.getDrawable(context, R.drawable.bg_icon_circle)
            gravity = Gravity.CENTER
            text = if (numbered) (index + 1).toString() else "•"
            setTextColor(ContextCompat.getColor(context, R.color.orange_primary))
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        }

        val body = TextView(context).apply {
            val bodyParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            bodyParams.marginStart = (12 * density).toInt()
            layoutParams = bodyParams
            text = itemText
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setLineSpacing(4 * density, 1f)
        }

        row.addView(badge)
        row.addView(body)
        card.addView(row)
        return card
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_ITEMS = "items"
        private const val ARG_NUMBERED = "numbered"

        fun newInstance(
            title: String,
            items: List<String>,
            numbered: Boolean = true
        ): ResultDetailFragment {
            return ResultDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putStringArrayList(ARG_ITEMS, ArrayList(items))
                    putBoolean(ARG_NUMBERED, numbered)
                }
            }
        }
    }
}
