package com.adroid.guru2_swuperdefense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class DiagnosisFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_diagnosis,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btnSmishing)
            .setOnClickListener {
                openQuestionScreen("문자·메신저 피싱")
            }

        view.findViewById<View>(R.id.btnVoicePhishing)
            .setOnClickListener {
                openQuestionScreen("보이스피싱·금전 피해")
            }

        view.findViewById<View>(R.id.btnDeepfake)
            .setOnClickListener {
                openQuestionScreen("딥페이크·불법 촬영물")
            }

        view.findViewById<View>(R.id.btnAccount)
            .setOnClickListener {
                openQuestionScreen("계정 해킹·도용")
            }

        view.findViewById<View>(R.id.btnOnlineFraud)
            .setOnClickListener {
                openQuestionScreen("온라인 거래 사기")
            }
    }

    private fun openQuestionScreen(
        incidentType: String
    ) {
        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                QuestionFragment.newInstance(incidentType)
            )
            .addToBackStack(null)
            .commit()
    }
}