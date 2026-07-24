package com.adroid.guru2_swuperdefense

/**
 * 스미싱 문구 간이 판별 로직.
 * TODO: 지금은 키워드 기반 규칙으로 동작함. 백엔드 담당자가 서버 API나 공공데이터
 *       (한국인터넷진흥원 스미싱 URL 목록 등) 연동으로 고도화할 수 있는 자리.
 */
object SmishingAnalyzer {

    data class RiskFactor(
        val title: String,
        val description: String,
        val level: String // "높음" | "중간"
    )

    data class AnalysisResult(
        val score: Int,
        val riskFactors: List<RiskFactor>
    )

    private val urlPattern = Regex("(https?://|www\\.|[a-zA-Z0-9-]+\\.(com|kr|net)(/|\\s|$))")
    private val urgencyKeywords = listOf("긴급", "즉시", "오늘까지", "정지", "마지막", "지금 바로", "당장")
    private val moneyKeywords = listOf("입금", "계좌", "결제", "환불", "당첨", "수수료", "인증번호", "개인정보")
    private val officialPrefixes = listOf("1588", "1544", "1600", "112", "118")

    /**
     * 문구/발신번호를 규칙 기반으로 채점한다. 기본 점수 20에서 시작해서
     * 위험 요소가 발견될 때마다 가중치를 더하고, 최종 점수는 [riskLevelLabel]로 등급화한다.
     * 같은 입력이면 항상 같은 결과를 내는 순수 함수라서, [SmishingResultFragment]가
     * [CheckRecord](message, sender)만 저장해두고 화면을 그릴 때마다 다시 호출해도 결과가 동일하다.
     */
    fun analyze(message: String, sender: String): AnalysisResult {
        val riskFactors = mutableListOf<RiskFactor>()
        var score = 20

        if (urlPattern.containsMatchIn(message)) {
            riskFactors += RiskFactor(
                "악성 URL 포함",
                "메시지 내 링크가 악성 사이트로 연결될 가능성이 높습니다.",
                "높음"
            )
            score += 30
        }

        if (sender.isNotBlank() && officialPrefixes.none { sender.startsWith(it) }) {
            riskFactors += RiskFactor(
                "발신자 정보 불일치",
                "발신 번호가 공식 기관과 일치하지 않습니다.",
                "높음"
            )
            score += 25
        }

        if (urgencyKeywords.any { message.contains(it) }) {
            riskFactors += RiskFactor(
                "긴급성 강조 사용",
                "긴급한 조치를 유도하는 문구가 포함되어 있습니다.",
                "중간"
            )
            score += 20
        }

        if (moneyKeywords.any { message.contains(it) }) {
            riskFactors += RiskFactor(
                "금전·개인정보 요구",
                "금전 또는 개인정보 입력을 유도하는 표현이 포함되어 있습니다.",
                "중간"
            )
            score += 15
        }

        return AnalysisResult(score.coerceAtMost(100), riskFactors)
    }

    fun riskLevelLabel(score: Int): String = when {
        score >= 70 -> "높은 위험"
        score >= 40 -> "주의 필요"
        else -> "낮은 위험"
    }

    // ==== 추가: 최근활동에서 과거 검사 결과로 다시 들어갈 수 있도록 검사 이력 보관 ====
    // TODO: 백엔드 연동 지점 - SmishingCheckDao로 교체. 지금은 앱 실행 중에만 유지되는 메모리 이력.
    data class CheckRecord(
        val id: Int,
        val message: String,
        val sender: String,
        val timestamp: Long
    )

    private var nextCheckId = 0
    private val checkHistory = mutableListOf<CheckRecord>()

    /** [SmishingCheckFragment]에서 "분석하기" 클릭 시 호출. 이력에 저장하고 결과 화면에 넘길 id를 반환한다. */
    fun saveCheck(message: String, sender: String): Int {
        val id = nextCheckId++
        checkHistory.add(0, CheckRecord(id, message, sender, System.currentTimeMillis()))
        return id
    }

    fun getCheckById(id: Int): CheckRecord? = checkHistory.find { it.id == id }
}
