package com.adroid.guru2_swuperdefense

import android.content.Context
import com.adroid.guru2_swuperdefense.data.local.AppDatabase
import com.adroid.guru2_swuperdefense.data.local.entity.SmishingCheckEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** 스미싱 문구 판별 로직. 위험 신호를 규칙으로 채점하고, 문구 속 URL은 KISA 피싱사이트 데이터셋과 대조한다. */
object SmishingAnalyzer {

    data class RiskFactor(
        val title: String,
        val description: String,
        val level: String // "높음" | "중간"
    )

    data class AnalysisResult(
        val score: Int,
        val riskFactors: List<RiskFactor>,
        val matchedPublicDataUrls: List<String> = emptyList(),
        val checkedPublicDataRecords: Int = 0
    )

    private val urgencyKeywords = listOf("긴급", "즉시", "오늘까지", "정지", "마지막", "지금 바로", "당장")
    private val moneyKeywords = listOf("입금", "계좌", "결제", "환불", "당첨", "수수료", "인증번호", "개인정보")
    private val appInstallKeywords = listOf("앱 설치", "어플 설치", "apk", "보안 앱", "원격제어")
    private val institutionKeywords = listOf("경찰", "검찰", "금융감독원", "은행", "카드사", "택배", "국세청", "정부")
    private val officialPrefixes = listOf("1588", "1544", "1600", "112", "118")

    suspend fun analyze(
        context: Context,
        message: String,
        sender: String
    ): AnalysisResult = withContext(Dispatchers.Default) {
        val dataset = KisaPhishingUrlDataset.load(context.applicationContext)
        analyze(
            message = message,
            sender = sender,
            knownPhishingUrls = dataset.normalizedUrls,
            checkedRecordCount = dataset.recordCount
        )
    }

    /**
     * 같은 입력과 데이터셋이면 항상 같은 결과를 내는 순수 채점 함수.
     * URL은 단순 포함 여부와 KISA 데이터셋 정확 일치를 구분해 점수를 부여한다.
     */
    fun analyze(
        message: String,
        sender: String,
        knownPhishingUrls: Set<String> = emptySet(),
        checkedRecordCount: Int = 0
    ): AnalysisResult {
        val riskFactors = mutableListOf<RiskFactor>()
        var score = 10
        val extractedUrls = KisaPhishingUrlDataset.extractUrls(message)
        val matchedPublicDataUrls = extractedUrls.filter { rawUrl ->
            KisaPhishingUrlDataset.normalize(rawUrl)?.let(knownPhishingUrls::contains) == true
        }

        if (matchedPublicDataUrls.isNotEmpty()) {
            riskFactors += RiskFactor(
                "KISA 피싱 URL 일치",
                "한국인터넷진흥원 공공데이터의 피싱사이트 URL과 일치합니다. 링크를 열지 마세요.",
                "높음"
            )
            score += 65
        } else if (extractedUrls.isNotEmpty()) {
            riskFactors += RiskFactor(
                "URL 포함",
                "공공데이터 목록과 일치하지 않아도 새로 생성된 피싱 링크일 수 있으니 주의하세요.",
                "중간"
            )
            score += 35
        }

        val normalizedSender = sender.filter(Char::isDigit)
        val claimsToBeInstitution = institutionKeywords.any(message::contains)
        if (
            normalizedSender.isNotBlank() &&
            claimsToBeInstitution &&
            officialPrefixes.none(normalizedSender::startsWith)
        ) {
            riskFactors += RiskFactor(
                "발신자 정보 불일치",
                "공공기관·금융기관을 언급하지만 대표번호 형태와 일치하지 않습니다.",
                "중간"
            )
            score += 20
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
            score += 20
        }

        if (appInstallKeywords.any { message.contains(it, ignoreCase = true) }) {
            riskFactors += RiskFactor(
                "앱 설치 유도",
                "출처가 불분명한 앱이나 원격제어 프로그램 설치를 유도합니다.",
                "높음"
            )
            score += 25
        }

        return AnalysisResult(
            score = score.coerceAtMost(100),
            riskFactors = riskFactors,
            matchedPublicDataUrls = matchedPublicDataUrls,
            checkedPublicDataRecords = checkedRecordCount
        )
    }

    fun riskLevelLabel(score: Int): String = when {
        score >= 70 -> "높은 위험"
        score >= 40 -> "주의 필요"
        else -> "낮은 위험"
    }

    // 최근 활동에서 과거 검사 결과를 다시 열 수 있도록 Room에 검사 이력을 보관한다.
    data class CheckRecord(
        val id: Int,
        val message: String,
        val sender: String,
        val timestamp: Long
    )

    /** [SmishingCheckFragment]에서 "분석하기" 클릭 시 호출. 이력에 저장하고 결과 화면에 넘길 id를 반환한다. */
    suspend fun saveCheck(
        context: Context,
        message: String,
        sender: String,
        analysisResult: AnalysisResult? = null
    ): Int {
        val result = analysisResult ?: analyze(context, message, sender)
        return AppDatabase.getInstance(context).smishingCheckDao().insert(
            SmishingCheckEntity(
                message = message,
                sender = sender,
                score = result.score,
                riskLevel = riskLevelLabel(result.score)
            )
        ).toInt()
    }

    suspend fun getCheckById(context: Context, id: Int): CheckRecord? =
        AppDatabase.getInstance(context).smishingCheckDao().getById(id)?.toRecord()

    fun observeLatest(context: Context): Flow<CheckRecord?> =
        AppDatabase.getInstance(context).smishingCheckDao().observeLatest()
            .map { it?.toRecord() }

    private fun SmishingCheckEntity.toRecord(): CheckRecord =
        CheckRecord(
            id = id,
            message = message,
            sender = sender,
            timestamp = createdAt
        )
}
