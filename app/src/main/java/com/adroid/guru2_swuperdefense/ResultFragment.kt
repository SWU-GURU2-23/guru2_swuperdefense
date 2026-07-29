package com.adroid.guru2_swuperdefense

// ============================================================================
// 수정 안내: 위험도 라벨 판정 기준을 점수 0~6 → 0~100(⚠즉시긴급 오버라이드 포함)으로,
// 안내 문구를 "피해 상황 확인 질문&주의사항 리스트.md"의 긴급/주의/확인 필요 3단계
// 행동요령·주의사항·참고 공식자료로 교체함. "가장 먼저 해야 할 행동"/"주의사항"은
// 항목별 리스트(List<String>)로 관리해 ResultDetailFragment에서 카드 단위로 보여주고,
// 화면에 노출하는 점수는 홈 화면 SECURITY SCORE와 동일한 값(100-위험도점수)을 쓰도록
// 통일함. 세부 지점은 "==== 수정 시작/끝 ====" 주석으로 표시함.
// ============================================================================

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.adroid.guru2_swuperdefense.data.repository.DiagnosisHistoryRepository
import kotlinx.coroutines.launch

class ResultFragment : Fragment() {

    private lateinit var incidentType: String
    private var riskScore: Int = 0
    private var hasCriticalFlag: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_result,
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

        riskScore = arguments?.getInt(ARG_RISK_SCORE) ?: 0
        hasCriticalFlag = arguments?.getBoolean(ARG_HAS_CRITICAL_FLAG) ?: false

        if (savedInstanceState == null) {
            viewLifecycleOwner.lifecycleScope.launch {
                DiagnosisHistoryRepository.getInstance(requireContext()).save(
                    incidentType = incidentType,
                    riskScore = riskScore,
                    hasCriticalFlag = hasCriticalFlag
                )
            }
        }
        ChecklistProgressStore.setActiveIncident(requireContext(), incidentType)

        val riskLevel = DiagnosisSummaryStore.riskLevelLabel(riskScore, hasCriticalFlag)
        val immediateActionSteps = getImmediateActionSteps(incidentType, riskLevel)
        val precautionBullets = getPrecautionBullets(incidentType)
        val references = getReferences(incidentType)

        view.findViewById<TextView>(R.id.tvResultType).text =
            incidentType

        view.findViewById<TextView>(R.id.tvRiskLevel).text =
            riskLevel

        // ==== 수정: "예"라고 답할수록 점수가 쌓이는 원래 방식대로 위험도 원점수를 그대로 "점수/100"
        // 형식으로 표시 (100-위험도점수로 뒤집어서 보여주던 방식을 되돌림). 홈 화면도 같은 값을
        // 쓰도록 HomeFragment에서 동일하게 diagnosis.riskScore를 그대로 사용함 ====
        view.findViewById<TextView>(R.id.tvRiskScore).text =
            "$riskScore/100"
        // ==== 수정 끝 ====

        // ==== 수정 시작: 참고 공식자료 링크 표시 (자동 링크로 눌러서 바로 열람 가능) ====
        view.findViewById<TextView>(R.id.tvReferences).text =
            references
        // ==== 수정 끝 ====

        // 이전 질문 화면으로 이동
        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // ==== 수정: "가장 먼저 해야 할 행동"/"주의사항" 본문이 길어 이 화면에 통째로 넣는 대신
        // 항목별 카드 리스트로 보여주는 별도 상세 화면(ResultDetailFragment)으로 이동하는
        // 버튼 2개로 분리 (기존: "맞춤 대응 가이드 보기" 버튼 1개 + 이 화면에 긴 문단을 그대로 표시) ====
        view.findViewById<View>(R.id.btnOpenImmediateAction).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    ResultDetailFragment.newInstance(
                        title = "가장 먼저 해야 할 행동",
                        items = immediateActionSteps,
                        numbered = true
                    )
                )
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.btnOpenPrecaution).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    ResultDetailFragment.newInstance(
                        title = "주의사항",
                        items = precautionBullets,
                        numbered = false
                    )
                )
                .addToBackStack(null)
                .commit()
        }
        // ==== 수정 끝 ====

        // 모든 이전 화면을 닫고 홈으로 이동
        view.findViewById<View>(R.id.btnReturnHome).setOnClickListener {
            parentFragmentManager.popBackStack(
                null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        }
    }

    // ==== 수정 시작: 긴급도별(긴급/주의/확인 필요) 행동요령을 항목별 리스트로 관리 - MD 문서
    // "가장 먼저 해야 할 행동"을 단계별로 쪼갬 (기존: 한 문단짜리 긴 문자열) ====
    private fun getImmediateActionSteps(type: String, riskLevel: String): List<String> {
        val stepsByLevel = immediateActionMap[type] ?: defaultImmediateActions()
        return stepsByLevel[riskLevel] ?: stepsByLevel.values.first()
    }

    private val immediateActionMap: Map<String, Map<String, List<String>>> = mapOf(
        "문자·메신저 피싱" to mapOf(
            "긴급" to listOf(
                "악성앱에 감염되면 그 상태로 어디에 전화하더라도 사기범에게만 연결될 수 있습니다. 초기화하기 전까지 휴대전화 전원을 끄거나 '비행기 모드'로 전환하고, 다른 사람의 전화기로 신고하세요.",
                "금융회사 콜센터·영업점에 연락해 본인 명의 전 계좌 지급정지를 요청하세요.",
                "금융감독원 사이트에서 개인정보 노출사실을 등록하세요.",
                "통신사 고객센터에서 모바일 결제 내역을 확인하세요.",
                "피해가 확인되면 스미싱 문자를 캡처해 소액결제확인서 발급을 요청한 뒤, 관할 경찰서 사이버수사대(또는 민원실)에 신고해 사건사고 사실 확인서를 받으세요."
            ),
            "주의" to listOf(
                "문자 수신 화면 상단의 '스팸으로 신고' 버튼으로 즉시 신고하세요.",
                "전기통신금융사기 통합신고대응센터(counterscam112.go.kr) '간편제보하기'로도 신고할 수 있습니다.",
                "앱을 설치했다면 모바일 백신으로 검사 후 삭제하세요. 악성앱 발견 시에는 반드시 비행기 모드로 전환한 뒤 112에 신고하세요.",
                "필요하면 휴대폰 서비스센터를 방문하세요.",
                "스마트폰 '설정 → 보안 및 개인정보보호 → 보안 위험 자동 차단'을 켜두세요.",
                "번호가 도용돼 지인에게 스미싱이 재발송될 수 있으니, 통신사 부가서비스에서 '번호도용문자차단서비스'를 무료로 신청하세요."
            ),
            "확인 필요" to listOf(
                "링크는 누르지 말고 문자를 삭제하세요.",
                "KISA(국번없이 118)에 신고 상담하세요.",
                "보호나라 카카오톡 채널의 '스미싱·피싱 확인서비스'로 의심 URL의 악성 여부를 확인할 수 있습니다."
            )
        ),

        "보이스피싱·금전 피해" to mapOf(
            "긴급" to listOf(
                "입금·송금한 금융회사 콜센터에 즉시 전화해 피해신고 및 본인 명의 전 계좌 지급정지를 요청하세요. (경찰 112, 금감원 1332에서도 연결 가능)",
                "개인정보가 유출됐거나 악성앱 설치가 의심되면 휴대전화 전원을 끄거나 비행기모드로 전환한 뒤 삭제·초기화하세요.",
                "금감원 개인정보 노출자 사고예방시스템(pd.fss.or.kr)에 노출사실을 등록하세요.",
                "금융결제원 계좌정보통합관리서비스(payinfo.or.kr)에서 본인 계좌를 일괄 지급정지하세요.",
                "명의도용방지서비스(msafer.or.kr)에서 본인 명의로 개통된 휴대전화가 있는지 조회하세요.",
                "경찰서(사이버수사대)에서 발급받은 사건사고사실확인원을 지참해, 지급정지를 신청한 영업점에 신청일로부터 3영업일 이내 서면으로 피해구제신청을 접수하세요."
            ),
            "주의" to listOf(
                "즉시 전화를 끊고 추가 연락에 응하지 마세요.",
                "상대방이 알려준 번호로 재확인하지 말고, 해당 기관의 공식 대표번호로 직접 확인하세요.",
                "자녀·지인을 사칭한 전화는 영상통화 등 다른 수단으로 본인에게 직접 재확인하세요."
            ),
            "확인 필요" to listOf(
                "통화 내용을 메모해두세요.",
                "118 또는 1332에 문의해 실제 사기 여부를 확인하세요."
            )
        ),

        "딥페이크·불법 촬영물" to mapOf(
            "긴급" to listOf(
                "상대방 요구에 무대응·무접촉으로 일관하세요. 질문하거나 영상물을 추가로 주지 마세요.",
                "협박 내용·피해영상물을 캡처해 증거로 확보하세요.",
                "디지털성범죄피해자지원센터(☎02-735-8994, 365일 24시간 상담)로 즉시 전화하거나 온라인 게시판 상담(d4u.stop.or.kr)을 접수하세요.",
                "경찰 신고를 원하면 112 또는 사이버범죄 신고시스템(ECRM, ecrm.police.go.kr)으로 신고하세요. 디지털성범죄피해자지원센터를 통하면 수사 동행·채증 지원까지 함께 받을 수 있습니다.",
                "불법촬영 기기를 발견했거나 현장을 목격했다면 문자로 112에 신고해 신변안전부터 확보하세요."
            ),
            "주의" to listOf(
                "유포처·유포 URL, 대화 캡처 등 증거를 먼저 확보하세요.",
                "디지털성범죄피해자지원센터에 상담을 요청하세요. 영상물 원본이 없어도 URL만으로 원본을 확보할 수 있습니다.",
                "신고 여부와 관계없이 디지털성범죄피해자지원센터에서 삭제지원·유포 모니터링(접수일로부터 3년, 연장 가능)을 신청할 수 있습니다.",
                "이미 경찰에 신고했다면 제출한 영상물이 시스템 연동으로 디지털성범죄피해자지원센터에 연계되므로 별도 제출은 필요 없습니다."
            ),
            "확인 필요" to listOf(
                "증거를 확보하지 못했더라도 육하원칙에 따른 진술만으로 신고·수사가 가능합니다.",
                "혐의 입증에 실패한 것이 곧 무고죄는 아니니 지나치게 걱정하지 마세요.",
                "디지털성범죄피해자지원센터(02-735-8994) 또는 d4u.stop.or.kr 온라인 상담으로 먼저 문의해보세요."
            )
        ),

        "계정 해킹·도용" to mapOf(
            "긴급" to listOf(
                "가능하다면 즉시 비밀번호를 변경하고 모든 기기에서 로그아웃하세요.",
                "로그인이 안 된다면 계정 복구 절차를 진행하세요.",
                "도용 결제가 있다면 결제사에 이의 신청하세요."
            ),
            "주의" to listOf(
                "즉시 비밀번호를 변경하세요.",
                "2단계 인증을 설정하세요.",
                "등록된 복구 이메일·전화번호가 본인 것인지 확인하세요."
            ),
            "확인 필요" to listOf(
                "동일 비밀번호를 쓰는 다른 사이트가 있다면 전부 변경하세요.",
                "2단계 인증을 켜두세요."
            )
        ),

        "온라인 거래 사기" to mapOf(
            "긴급" to listOf(
                "이체확인증, 대화 내역(판매글·채팅), 게시물 캡처 등 증거를 확보하세요.",
                "경찰청 사이버범죄 신고시스템(ECRM)에 신고하거나 가까운 경찰서 사이버수사팀을 방문하세요.",
                "입금 은행 콜센터·영업점에 연락해 사기계좌 지급정지를 요청하세요. 시간이 지날수록 인출 가능성이 높아지므로 최대한 빨리 진행하세요.",
                "오픈마켓·플랫폼의 정식 결제(카드·안전결제)로 거래했다면 해당 플랫폼 고객센터에 결제 취소·환불도 함께 요청하세요."
            ),
            "주의" to listOf(
                "추가 입금 요구에는 절대 응하지 마세요.",
                "경찰청 사이버캅(전화번호·계좌번호 사기 피해 신고 이력을 조회할 수 있는 사이트)에서 상대방 계좌번호·전화번호를 조회해 최근 3개월간 3회 이상 신고된 이력이 있는지 확인하세요.",
                "단순 교환·환불 등 소비자 분쟁이라면 공정거래위원회 '행복드림 열린소비자포털' 또는 국번없이 1372(소비자상담센터), 한국소비자원에 증빙서류와 함께 피해구제를 신청하세요."
            ),
            "확인 필요" to listOf(
                "거래 전 판매자의 사업장 주소·전화번호 등 사업자 정보를 확인하세요.",
                "통신판매신고 여부를 공정위 사이트에서 확인하세요.",
                "고객 게시판·후기를 미리 확인하세요.",
                "가능하면 직거래보다 안전결제(에스크로)·신용카드 결제를 이용하세요."
            )
        )
    )

    private fun defaultImmediateActions(): Map<String, List<String>> = mapOf(
        "긴급" to listOf(
            "관련 연락과 추가 행동을 중단하세요.",
            "관련 대화와 결제 내역을 보관하세요.",
            "112 또는 118로 신고하세요."
        ),
        "주의" to listOf(
            "관련 대화와 결제 내역을 보관하세요.",
            "공식 기관을 통해 상황을 확인하세요."
        ),
        "확인 필요" to listOf(
            "의심되는 정황을 기록해두세요.",
            "공식 기관 상담을 받아보세요."
        )
    )
    // ==== 수정 끝 ====

    // ==== 수정 시작: 카테고리별 주의사항을 항목별 리스트로 관리 - MD 문서 "주의사항" 불릿 리스트 반영 ====
    private fun getPrecautionBullets(type: String): List<String> =
        precautionMap[type] ?: defaultPrecautions()

    private val precautionMap: Map<String, List<String>> = mapOf(
        "문자·메신저 피싱" to listOf(
            "문자 속 링크는 클릭한 것만으로는 감염되지 않지만, 클릭 후 앱을 설치했다면 반드시 점검이 필요합니다.",
            "정부기관·금융회사는 전화나 문자로 원격제어 앱 설치를 요구하지 않습니다(정상 스토어에 등록된 앱이어도 마찬가지).",
            "인증번호·OTP·보안카드 번호는 절대 타인에게 알려주지 마세요. 유출된 금융거래정보는 즉시 폐기하세요.",
            "스크린샷 등 증거는 삭제하지 말고 보관하세요.",
            "감염됐던 스마트폰으로 금융서비스를 이용했다면 공인인증서·보안카드는 폐기 후 재발급하세요.",
            "악성앱이 주소록을 조회해 지인에게 같은 스미싱을 재발송할 수 있으니, 피해 사실을 주변에도 알려 2차 피해를 막으세요.",
            "최근에는 카드사, 텔레그램·애플·왓츠앱 등 메신저·계정 인증, 이커머스 해킹 피해보상·환불 안내를 사칭하는 문구가 특히 많이 확인되고 있습니다."
        ),

        "보이스피싱·금전 피해" to listOf(
            "전화로 계좌번호·카드번호·인터넷뱅킹 정보를 묻거나 사이트 입력을 요구하면 절대 응대하지 마세요.",
            "현금지급기(ATM)로 유인하면 100% 보이스피싱입니다.",
            "자녀납치 빙자 보이스피싱에 미리 대비하고, 실제로는 자녀에게 직접 연락해 확인하세요.",
            "개인·금융거래정보를 상대방이 미리 알고 접근하더라도 내용의 진위를 반드시 재확인하세요.",
            "유출된 금융거래정보는 즉시 폐기·변경하세요.",
            "예금통장·현금(체크)카드는 어떤 이유로도 타인에게 양도하지 마세요.",
            "발신 전화번호는 조작이 가능하므로 번호만으로 신뢰하지 마세요.",
            "접속한 금융회사 홈페이지 주소가 정확한지 확인하세요.",
            "\"안전계좌\"라는 개념 자체가 존재하지 않습니다 — 이 말을 들으면 사기입니다."
        ),

        "딥페이크·불법 촬영물" to listOf(
            "촬영에 동의했더라도 유포에는 동의한 것이 아닙니다. 동의 하에 촬영한 영상물이 동의 없이 유포되면 이 역시 명백한 범죄입니다(성폭력처벌법 제14조).",
            "피해 영상물·복제물을 소지·구입·저장·시청한 사람도 처벌 대상입니다.",
            "협박이나 강요가 있을 경우 어떤 조치를 취하기에 앞서 먼저 디지털성범죄피해자지원센터로 연락하세요.",
            "아동·청소년이 연루된 경우 경찰 신고를 하지 않아도, 부모님에게 알리지 않아도 디지털성범죄피해자지원센터의 상담·지원을 받을 수 있습니다.",
            "모든 상담·접수 내용은 비밀이 보장되며, 모든 지원은 피해자 동의 후에만 진행됩니다.",
            "지인으로부터 유포 사실을 알게 된 경우, 피해자 본인에게 알리는 것은 신중하게 접근해야 합니다.",
            "원본 대화·파일은 증거 확보 전까지 삭제하지 말고, 유포된 게시물 URL과 캡처를 함께 보관하세요."
        ),

        "계정 해킹·도용" to listOf(
            "같은 비밀번호를 여러 사이트에서 재사용하지 마세요.",
            "복구 이메일·전화번호는 주기적으로 본인 것인지 확인하세요.",
            "계정 도용을 이용한 지인 대상 사기가 있는지 주변에 알리세요.",
            "2단계 인증(OTP, 생체인증)을 반드시 설정하세요."
        ),

        "온라인 거래 사기" to listOf(
            "\"초특가·한정상품\" 등 시세보다 지나치게 싼 상품은 의심하세요.",
            "판매자의 사업장 주소·전화번호 등 사업자 정보, 고객 게시판·불만 글 여부를 확인하세요.",
            "대형 오픈마켓에 입점한 판매자라도 개별 판매자의 거래 이력·평가는 별도로 검증해야 합니다.",
            "SNS 거래 시 통신판매신고를 한 사업자인지, 청약철회가 가능한지 공정거래위원회 홈페이지에서 확인하세요.",
            "현금거래를 유도하는 판매자와는 거래하지 말고, 분쟁 시 결제 취소가 가능한 신용카드 거래를 이용하세요.",
            "해외직구는 Scamadviser.com 등에서 사이트 신뢰도를 확인하고, 한국소비자원의 국제거래 소비자포털을 활용하세요.",
            "상품을 받은 걸 확인한 뒤에만 대금이 판매자에게 지급되는 안전결제(에스크로)를 거부하는 판매자와는 거래하지 마세요.",
            "계좌번호·전화번호는 거래 전 미리 조회하고, 대화 내역과 이체 확인증은 반드시 보관하세요."
        )
    )

    private fun defaultPrecautions(): List<String> = listOf(
        "증거를 삭제하지 마세요.",
        "공식 기관을 통해 확인하세요."
    )
    // ==== 수정 끝 ====

    // ==== 수정 시작: 카테고리별 참고 공식자료 - MD 문서 "참고 공식자료" 반영 ====
    private fun getReferences(type: String): String {
        val links = referenceMap[type] ?: defaultReferences()
        return links.joinToString(separator = "\n")
    }

    // "문자·메신저 피싱" 참고자료에서 전기통신금융사기 통합신고대응센터 항목은 MD 문서에서
    // 제외되어 코드에서도 함께 제거함 (다른 항목과 URL 성격이 달라 팀 판단으로 뺌)
    private val referenceMap: Map<String, List<String>> = mapOf(
        "문자·메신저 피싱" to listOf(
            "KISA 한국인터넷진흥원 https://www.kisa.or.kr/1020601",
            "KISA 보호나라 보안공지 https://www.boho.or.kr/kr/bbs/view.do?bbsId=B0000133&menuNo=205020&pageIndex=1&nttId=71612"
        ),

        "보이스피싱·금전 피해" to listOf(
            "금융감독원 보이스피싱지킴이 - 피해 시 대처방법 https://www.fss.or.kr/fss/main/contents.do?menuNo=200365",
            "금융감독원 보이스피싱지킴이 - 예방요령 https://www.fss.or.kr/fss/main/contents.do?menuNo=200364",
            "금융사기 신고·피해구제 기관 안내 https://www.kfcpf.or.kr/front/protect/reportingAgency.do"
        ),

        "딥페이크·불법 촬영물" to listOf(
            "디지털성범죄피해자지원센터 https://d4u.stop.or.kr/ (☎02-735-8994, 365일 24시간)",
            "디지털성범죄피해자지원센터 - 불법촬영 관련 대응사례 FAQ https://d4u.stop.or.kr/ko/faq/cases/illegal-filming",
            "사이버범죄 신고시스템(ECRM) https://ecrm.police.go.kr",
            "여성긴급전화1366 ☎1366"
        ),

        "계정 해킹·도용" to listOf(
            "개인정보보호위원회 https://www.pipc.go.kr/np/default/page.do?mCode=D030040000",
            "KISA 개인정보침해 신고센터 https://privacy.kisa.or.kr/main.do",
            "상담 전화 ☎118"
        ),

        "온라인 거래 사기" to listOf(
            "경찰청 사이버수사국 - 사이버 사기(쇼핑몰 사기) 예방 https://cyberbureau.police.go.kr/prevention/prevention2_2.jsp?mid=020302",
            "사이버범죄 신고시스템(ECRM) https://ecrm.police.go.kr/minwon/main",
            "공정거래위원회 소비자상담센터 국번없이 1372"
        )
    )

    private fun defaultReferences(): List<String> = listOf(
        "경찰청 사이버안전지킴이 https://cyberbureau.police.go.kr"
    )
    // ==== 수정 끝 ====

    companion object {
        private const val ARG_INCIDENT_TYPE = "incident_type"
        private const val ARG_RISK_SCORE = "risk_score"
        private const val ARG_HAS_CRITICAL_FLAG = "has_critical_flag"

        fun newInstance(
            incidentType: String,
            riskScore: Int,
            hasCriticalFlag: Boolean = false
        ): ResultFragment {
            return ResultFragment().apply {
                arguments = Bundle().apply {
                    putString(
                        ARG_INCIDENT_TYPE,
                        incidentType
                    )

                    putInt(
                        ARG_RISK_SCORE,
                        riskScore
                    )

                    putBoolean(
                        ARG_HAS_CRITICAL_FLAG,
                        hasCriticalFlag
                    )
                }
            }
        }
    }
}
