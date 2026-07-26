package com.adroid.guru2_swuperdefense

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmishingAnalyzerTest {

    @Test
    fun ordinaryMessageWithoutRiskSignals_isLowRisk() {
        val result = SmishingAnalyzer.analyze(
            message = "오늘 저녁 7시에 도서관 앞에서 만나요.",
            sender = "01012345678"
        )

        assertEquals("낮은 위험", SmishingAnalyzer.riskLevelLabel(result.score))
        assertTrue(result.riskFactors.isEmpty())
    }

    @Test
    fun linkAndUrgentPaymentRequest_isHighRisk() {
        val result = SmishingAnalyzer.analyze(
            message = "긴급! 결제가 정지됩니다. 지금 바로 https://unsafe.example.com 에서 인증번호를 입력하세요.",
            sender = "01099999999"
        )

        assertEquals("높은 위험", SmishingAnalyzer.riskLevelLabel(result.score))
        assertTrue(result.riskFactors.any { it.title == "악성 URL 포함" })
        assertTrue(result.riskFactors.any { it.title == "긴급성 강조 사용" })
        assertTrue(result.riskFactors.any { it.title == "금전·개인정보 요구" })
    }

    @Test
    fun institutionClaimFromUnknownNumber_addsMismatchFactor() {
        val result = SmishingAnalyzer.analyze(
            message = "금융감독원 안내입니다.",
            sender = "01012345678"
        )

        assertTrue(result.riskFactors.any { it.title == "발신자 정보 불일치" })
    }

    @Test
    fun personalSenderWithoutInstitutionClaim_doesNotAddMismatchFactor() {
        val result = SmishingAnalyzer.analyze(
            message = "회의 자료 확인 부탁드립니다.",
            sender = "01012345678"
        )

        assertTrue(result.riskFactors.none { it.title == "발신자 정보 불일치" })
    }
}
