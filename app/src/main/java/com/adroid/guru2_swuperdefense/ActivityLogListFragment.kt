package com.adroid.guru2_swuperdefense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

/** 홈 화면 "전체 보기"에서 진입하는 전체 활동 목록 화면 */
class ActivityLogListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_activity_log_list,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val entries = ActivityLog.all()
        val container = view.findViewById<LinearLayout>(R.id.activityListContainer)
        val tvEmpty = view.findViewById<TextView>(R.id.tvActivityListEmpty)

        tvEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE

        entries.forEach { entry ->
            val card = ActivityLog.buildCard(requireContext(), entry)
            card.setOnClickListener {
                ActivityLog.navigateTo(requireContext(), parentFragmentManager, R.id.fragmentContainer, entry)
            }
            container.addView(card)
        }
    }
}
