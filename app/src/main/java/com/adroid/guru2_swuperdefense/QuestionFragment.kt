package com.adroid.guru2_swuperdefense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class QuestionFragment : Fragment() {

    private data class DiagnosisQuestion(
        val text: String,
        val points: Int,
        val isCritical: Boolean = false,
        val isReferenceOnly: Boolean = false
    )

    private enum class Answer { YES, UNSURE, NO }

    private lateinit var incidentType: String

    private var currentQuestionIndex = 0
    private var riskScore = 0
    private var hasCriticalFlag = false

    private lateinit var tvIncidentType: TextView
    private lateinit var tvQuestionCount: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var tvDescription: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnYes: MaterialButton
    private lateinit var btnUnsure: MaterialButton
    private lateinit var btnNo: MaterialButton

    // 질문 문구는 자동 줄바꿈이 단어 중간에서 끊기지 않도록 어절 단위로 \n을 직접 지정한다.
    private val questionMap = mapOf(
        "문자·메신저 피싱" to listOf(
            DiagnosisQuestion("문자·메신저로 받은 링크를\n눌러본 적이 있나요?", points = 10),
            DiagnosisQuestion("링크를 누른 뒤 앱을 설치하거나\n업데이트했나요?", points = 15),
            DiagnosisQuestion("그 앱/사이트에 개인정보\n(이름, 주민번호, 계좌번호 등)를\n입력했나요?", points = 20),
            DiagnosisQuestion("인증번호(OTP)나 보안카드 번호를\n입력하거나 전달했나요?", points = 30, isCritical = true),
            DiagnosisQuestion("발신번호가 실제 기관 공식 번호와\n다르거나 확인되지 않았나요?", points = 10),
            DiagnosisQuestion("\"긴급\", \"24시간 내\", \"계정 정지\" 등\n긴급성을 강조하는 문구가 있었나요?", points = 5),
            DiagnosisQuestion("설치 후 스마트폰이 평소와 다르게\n느려지거나 모르는 앱 권한 요청이\n뜨나요?", points = 10),
            DiagnosisQuestion("이후 본인 명의로 소액결제·유심 변경\n알림을 받은 적이 있나요?", points = 35, isCritical = true)
        ),

        "보이스피싱·금전 피해" to listOf(
            DiagnosisQuestion("검찰·경찰·금융감독원 등을 사칭한\n전화를 받았나요?", points = 15),
            DiagnosisQuestion("\"계좌가 범죄에 연루됐다\",\n\"안전계좌로 이체하라\"는 말을 들었나요?", points = 15),
            DiagnosisQuestion("실제로 계좌 이체나 현금 인출을\n진행했나요?", points = 40, isCritical = true),
            DiagnosisQuestion("통화 중 원격제어 앱(팀뷰어 등)\n설치를 요구받고 설치했나요?", points = 35, isCritical = true),
            DiagnosisQuestion("대출을 빙자하며 수수료·보증금\n선입금을 요구받았나요?", points = 15),
            DiagnosisQuestion("자녀·지인을 사칭하며 급하게\n돈을 요구받았나요?", points = 15),
            DiagnosisQuestion("통화 중 은행 앱에 접속해\n정보를 입력했나요?", points = 20),
            DiagnosisQuestion("상대방이 알려준 번호로 재전화해서\n신원을 \"확인\"했나요?", points = 10)
        ),

        "딥페이크·불법 촬영물" to listOf(
            DiagnosisQuestion("본인 얼굴·신체가 합성/편집된\n영상·이미지 유포 사실을 확인했나요?", points = 20),
            DiagnosisQuestion("상대방이 해당 자료를 빌미로\n금전이나 다른 요구를 했나요? (협박)", points = 40, isCritical = true),
            DiagnosisQuestion("유포 경로(SNS, 메신저, 사이트)를\n특정할 수 있나요?", points = 10),
            DiagnosisQuestion("원본 자료(대화 캡처, 파일)를\n보관하고 있나요?", points = 0, isReferenceOnly = true),
            DiagnosisQuestion("상대방의 신원(계정, 연락처)을\n알고 있나요?", points = 0, isReferenceOnly = true),
            DiagnosisQuestion("이미 특정 커뮤니티·사이트에\n게시된 것을 확인했나요?", points = 20),
            DiagnosisQuestion("미성년자가 연루된 사안인가요?", points = 40, isCritical = true)
        ),

        "계정 해킹·도용" to listOf(
            DiagnosisQuestion("요청하지 않은 비밀번호 변경 알림을\n받았나요?", points = 15),
            DiagnosisQuestion("모르는 기기·위치에서의 로그인\n알림을 받았나요?", points = 15),
            DiagnosisQuestion("계정으로 본인이 하지 않은\n게시물·메시지·결제가 발생했나요?", points = 35, isCritical = true),
            DiagnosisQuestion("로그인이 안 되거나 강제로\n로그아웃됐나요?", points = 20),
            DiagnosisQuestion("등록된 이메일·전화번호가\n동의 없이 변경됐나요?", points = 40, isCritical = true),
            DiagnosisQuestion("본인이 로그인하지 않았는데\n2단계 인증(OTP) 요청이 왔나요?", points = 15),
            DiagnosisQuestion("최근 출처가 불분명한 사이트에\n같은 아이디·비밀번호를\n입력한 적이 있나요?", points = 10)
        ),

        "온라인 거래 사기" to listOf(
            DiagnosisQuestion("시세보다 현저히 저렴한 가격에\n거래를 진행했나요?", points = 10),
            DiagnosisQuestion("판매자가 직거래 대신\n선입금만 요구했나요?", points = 15),
            DiagnosisQuestion("상품을 받은 걸 확인한 뒤에만\n대금이 지급되는 안전결제(에스크로)를\n거부하거나 회피했나요?", points = 15),
            DiagnosisQuestion("입금 후 판매자와\n연락이 두절됐나요?", points = 40, isCritical = true),
            DiagnosisQuestion("판매자 계좌번호·연락처를 사기 피해\n조회 사이트(경찰청 사이버캅 등)에서\n확인해봤나요?", points = 0, isReferenceOnly = true),
            DiagnosisQuestion("상품을 받았는데 사진과\n다르거나 아예 오지 않았나요?", points = 35, isCritical = true),
            DiagnosisQuestion("판매자가 외부 링크로 유도해\n결제를 진행시켰나요?", points = 15)
        )
    )

    private lateinit var questions: List<DiagnosisQuestion>

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
                DiagnosisQuestion("의심스러운 연락이나\n메시지를 받았나요?", points = 20),
                DiagnosisQuestion("개인정보를 상대방에게\n전달했나요?", points = 30),
                DiagnosisQuestion("금전적인 피해가 발생했나요?", points = 40, isCritical = true)
            )

        currentQuestionIndex = savedInstanceState?.getInt(STATE_QUESTION_INDEX) ?: 0
        riskScore = savedInstanceState?.getInt(STATE_RISK_SCORE) ?: 0
        hasCriticalFlag = savedInstanceState?.getBoolean(STATE_CRITICAL_FLAG) ?: false

        tvIncidentType = view.findViewById(R.id.tvIncidentType)
        tvQuestionCount = view.findViewById(R.id.tvQuestionCount)
        tvQuestion = view.findViewById(R.id.tvQuestion)
        tvDescription = view.findViewById(R.id.tvQuestionDescription)
        progressBar = view.findViewById(R.id.questionProgress)
        btnYes = view.findViewById(R.id.btnYes)
        btnUnsure = view.findViewById(R.id.btnUnsure)
        btnNo = view.findViewById(R.id.btnNo)

        tvIncidentType.text = incidentType
        progressBar.max = questions.size

        showCurrentQuestion()

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnYes.setOnClickListener {
            moveToNextQuestion(Answer.YES)
        }

        btnUnsure.setOnClickListener {
            moveToNextQuestion(Answer.UNSURE)
        }

        btnNo.setOnClickListener {
            moveToNextQuestion(Answer.NO)
        }
    }

    private fun showCurrentQuestion() {
        tvQuestionCount.text =
            "질문 ${currentQuestionIndex + 1} / ${questions.size}"

        val question = questions[currentQuestionIndex]
        tvQuestion.text = question.text

        tvDescription.text = if (question.isReferenceOnly) {
            "이 질문은 위험도 점수에는 반영되지 않고,\n대응 방법 안내에만 참고돼요."
        } else {
            "실제로 겪은 상황을 기준으로 선택해주세요.\n확실하지 않다면\n'잘 모르겠어요'를 선택해도 괜찮아요."
        }

        progressBar.progress = currentQuestionIndex + 1
    }

    private fun moveToNextQuestion(answer: Answer) {
        val currentQuestion = questions[currentQuestionIndex]

        if (!currentQuestion.isReferenceOnly) {
            riskScore += when (answer) {
                Answer.YES -> currentQuestion.points
                Answer.UNSURE -> currentQuestion.points / 2
                Answer.NO -> 0
            }

            // "잘 모르겠어요"는 확정된 피해가 아니므로 즉시긴급 판정에는 반영하지 않음
            if (answer == Answer.YES && currentQuestion.isCritical) {
                hasCriticalFlag = true
            }
        }

        currentQuestionIndex++

        if (currentQuestionIndex < questions.size) {
            showCurrentQuestion()
        } else {
            showCompletion()
        }
    }

    private fun showCompletion() {
        val finalScore = riskScore.coerceIn(0, 100)

        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                ResultFragment.newInstance(
                    incidentType = incidentType,
                    riskScore = finalScore,
                    hasCriticalFlag = hasCriticalFlag
                )
            )
            .addToBackStack(null)
            .commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_QUESTION_INDEX, currentQuestionIndex)
        outState.putInt(STATE_RISK_SCORE, riskScore)
        outState.putBoolean(STATE_CRITICAL_FLAG, hasCriticalFlag)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val ARG_INCIDENT_TYPE = "incident_type"
        private const val STATE_QUESTION_INDEX = "question_index"
        private const val STATE_RISK_SCORE = "risk_score"
        private const val STATE_CRITICAL_FLAG = "has_critical_flag"

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
