package com.adroid.guru2_swuperdefense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class QuestionFragment : Fragment() {

    private lateinit var incidentType: String

    private var currentQuestionIndex = 0
    private var riskScore = 0

    private lateinit var tvIncidentType: TextView
    private lateinit var tvQuestionCount: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var tvDescription: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnYes: MaterialButton
    private lateinit var btnNo: MaterialButton

    private val questionMap = mapOf(
        "문자·메신저 피싱" to listOf(
            "문자나 메시지에 포함된 링크를 눌렀나요?",
            "개인정보나 인증번호를 입력했나요?",
            "출처가 불분명한 앱을 설치했나요?"
        ),

        "보이스피싱·금전 피해" to listOf(
            "상대방에게 돈을 송금했나요?",
            "계좌번호나 인증번호를 알려줬나요?",
            "현재도 상대방이 추가 송금을 요구하나요?"
        ),

        "딥페이크·불법 촬영물" to listOf(
            "본인의 사진이나 영상이 무단으로 사용됐나요?",
            "해당 콘텐츠가 온라인에 게시되거나 공유됐나요?",
            "상대방이 협박하거나 금전을 요구하나요?"
        ),

        "계정 해킹·도용" to listOf(
            "본인이 시도하지 않은 로그인 기록이 있나요?",
            "비밀번호나 복구 정보가 변경됐나요?",
            "계정을 이용한 결제나 메시지 전송이 발생했나요?"
        ),

        "온라인 거래 사기" to listOf(
            "상품이나 서비스를 받기 전에 돈을 보냈나요?",
            "송금 후 상대방과 연락이 끊겼나요?",
            "대화 내용과 이체 내역을 보관하고 있나요?"
        )
    )

    private lateinit var questions: List<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_question,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        incidentType = arguments?.getString(ARG_INCIDENT_TYPE)
            ?: "피해 유형 확인"

        questions = questionMap[incidentType]
            ?: listOf(
                "의심스러운 연락이나 메시지를 받았나요?",
                "개인정보를 상대방에게 전달했나요?",
                "금전적인 피해가 발생했나요?"
            )

        tvIncidentType = view.findViewById(R.id.tvIncidentType)
        tvQuestionCount = view.findViewById(R.id.tvQuestionCount)
        tvQuestion = view.findViewById(R.id.tvQuestion)
        tvDescription = view.findViewById(R.id.tvQuestionDescription)
        progressBar = view.findViewById(R.id.questionProgress)
        btnYes = view.findViewById(R.id.btnYes)
        btnNo = view.findViewById(R.id.btnNo)

        tvIncidentType.text = incidentType
        progressBar.max = questions.size

        showCurrentQuestion()

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnYes.setOnClickListener {
            moveToNextQuestion(answeredYes = true)
        }

        btnNo.setOnClickListener {
            moveToNextQuestion(answeredYes = false)
        }
    }

    private fun showCurrentQuestion() {
        tvQuestionCount.text =
            "질문 ${currentQuestionIndex + 1} / ${questions.size}"

        tvQuestion.text = questions[currentQuestionIndex]
        progressBar.progress = currentQuestionIndex + 1
    }

    private fun moveToNextQuestion(answeredYes: Boolean) {
        if (answeredYes) {
            riskScore += 2
        }

        currentQuestionIndex++

        if (currentQuestionIndex < questions.size) {
            showCurrentQuestion()
        } else {
            showCompletion()
        }
    }

    private fun showCompletion() {
        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                ResultFragment.newInstance(
                    incidentType = incidentType,
                    riskScore = riskScore
                )
            )
            .addToBackStack(null)
            .commit()
    }

    companion object {
        private const val ARG_INCIDENT_TYPE = "incident_type"

        fun newInstance(
            incidentType: String
        ): QuestionFragment {

            return QuestionFragment().apply {
                arguments = Bundle().apply {
                    putString(
                        ARG_INCIDENT_TYPE,
                        incidentType
                    )
                }
            }
        }
    }
}