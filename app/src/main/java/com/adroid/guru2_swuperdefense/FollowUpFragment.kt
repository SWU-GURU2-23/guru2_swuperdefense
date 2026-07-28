package com.adroid.guru2_swuperdefense

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

class FollowUpFragment : Fragment() {

    // 서비스 하나
    private data class ServiceItem(
        val icon: String,
        val title: String,
        val description: String,
        val url: String
    )

    // 카테고리 하나
    private data class ServiceCategory(
        val title: String,
        @ColorRes val colorRes: Int,
        val services: List<ServiceItem>
    )

    private val categories = listOf(

        // 1. 개인정보 유출
        ServiceCategory(
            title = "개인정보 유출",
            colorRes = R.color.category_privacy,
            services = listOf(
                ServiceItem(
                    icon = "🔍",
                    title = "털린 내 정보 찾기",
                    description = "유출된 내 정보를 확인해요.",
                    url = "https://kidc.eprivacy.go.kr/"
                ),

                ServiceItem(
                    icon = "👤",
                    title = "e프라이버시 클린서비스",
                    description = "안 쓰는 사이트를 탈퇴해요.",
                    url = "https://www.privacy.go.kr/front/contents/cntntsView.do?contsNo=192"
                )
            )
        ),

        // 2. 명의 도용
        ServiceCategory(
            title = "명의 도용",
            colorRes = R.color.category_identity,
            services = listOf(
                ServiceItem(
                    icon = "📱",
                    title = "M-Safer 명의도용방지서비스",
                    description = "내 명의 사용 여부를 조회하고, 신규 개통을 차단해요.",
                    url = "https://www.msafer.or.kr/"
                ),

                ServiceItem(
                    icon = "💳",
                    title = "금융거래 안심차단 서비스",
                    description = "여신거래 및 비대면 계좌개설을 차단해요.",
                    url = "https://www.payinfo.or.kr/main/main.do"
                )
            )
        ),

        // 3. 게시물·개인정보 삭제
        ServiceCategory(
            title = "게시물·개인정보 삭제",
            colorRes = R.color.category_delete,
            services = listOf(
                ServiceItem(
                    icon = "🗑",
                    title = "지우개 서비스",
                    description = "개인정보가 포함된 게시글을 삭제해요.",
                    url = "https://www.privacy.go.kr/delete.do"
                )
            )
        ),

        // 4. 사이버범죄 신고
        ServiceCategory(
            title = "사이버범죄 신고",
            colorRes = R.color.category_cyber,
            services = listOf(
                ServiceItem(
                    icon = "🚨",
                    title = "경찰청 사이버범죄 신고시스템(ECRM)",
                    description = "사이버사기, 해킹, 불법콘텐츠 등을 신고해요.",
                    url = "https://ecrm.police.go.kr/minwon/main"
                )
            )
        ),

        // 5. 금융 피해
        ServiceCategory(
            title = "금융 피해",
            colorRes = R.color.category_finance,
            services = listOf(
                ServiceItem(
                    icon = "🏦",
                    title = "금융감독원",
                    description = "금융사기 피해 상담 및 대응 정보를 확인해요.",
                    url = "https://www.fss.or.kr/"
                )
            )
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.fragment_follow_up,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로가기
        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 서비스가 들어갈 영역
        val serviceContainer =
            view.findViewById<LinearLayout>(R.id.serviceContainer)

        // 카테고리 생성
        categories.forEach { category ->
            addCategory(
                container = serviceContainer,
                category = category
            )
        }
    }

    /**
     * 개인정보 유출 / 명의 도용 같은
     * 카테고리 한 묶음을 화면에 추가
     */
    private fun addCategory(
        container: LinearLayout,
        category: ServiceCategory
    ) {

        val categoryColor =
            ContextCompat.getColor(
                requireContext(),
                category.colorRes
            )

        // 카테고리 제목
        val categoryTitle = TextView(requireContext()).apply {

            text = "⬟  ${category.title}"

            setTextColor(categoryColor)

            textSize = 19f

            setTypeface(
                typeface,
                Typeface.BOLD
            )
        }

        val categoryTitleParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                topMargin = dp(24)
                bottomMargin = dp(10)
            }

        container.addView(
            categoryTitle,
            categoryTitleParams
        )

        // 카테고리 안 서비스들을 묶는 카드
        val categoryCard =
            MaterialCardView(requireContext()).apply {

                radius = dp(16).toFloat()

                cardElevation = 0f

                setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.card_charcoal
                    )
                )

                strokeWidth = dp(1)

                strokeColor =
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.card_border
                    )
            }

        val rowsContainer =
            LinearLayout(requireContext()).apply {

                orientation = LinearLayout.VERTICAL
            }

        category.services.forEachIndexed { index, service ->

            // 서비스 한 줄
            rowsContainer.addView(
                createServiceRow(
                    service = service,
                    categoryColor = categoryColor
                )
            )

            // 마지막 서비스가 아니면 구분선 추가
            if (index < category.services.lastIndex) {

                val divider =
                    View(requireContext()).apply {

                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.card_border
                            )
                        )
                    }

                val dividerParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                    ).apply {

                        marginStart = dp(68)
                        marginEnd = dp(16)
                    }

                rowsContainer.addView(
                    divider,
                    dividerParams
                )
            }
        }

        categoryCard.addView(rowsContainer)

        container.addView(
            categoryCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    /**
     * 털린 내 정보 찾기 등의
     * 서비스 한 줄 생성
     */
    private fun createServiceRow(
        service: ServiceItem,
        categoryColor: Int
    ): View {

        val row =
            LinearLayout(requireContext()).apply {

                orientation = LinearLayout.HORIZONTAL

                gravity = android.view.Gravity.CENTER_VERTICAL

                setPadding(
                    dp(16),
                    dp(16),
                    dp(16),
                    dp(16)
                )

                isClickable = true
                isFocusable = true

                setOnClickListener {
                    openWebsite(service.url)
                }
            }

        // 아이콘 배경
        val iconBackground =
            GradientDrawable().apply {

                shape = GradientDrawable.OVAL

                setColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.surface_light_dark
                    )
                )

                setStroke(
                    dp(1),
                    categoryColor
                )
            }

        // 아이콘
        val iconView =
            TextView(requireContext()).apply {

                text = service.icon

                textSize = 20f

                gravity =
                    android.view.Gravity.CENTER

                background = iconBackground
            }

        row.addView(
            iconView,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        // 제목 + 설명
        val textArea =
            LinearLayout(requireContext()).apply {

                orientation = LinearLayout.VERTICAL
            }

        val textAreaParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {

                marginStart = dp(14)
            }

        // 제목
        val title =
            TextView(requireContext()).apply {

                text = service.title

                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_primary
                    )
                )

                textSize = 16f

                setTypeface(
                    typeface,
                    Typeface.BOLD
                )
            }

        textArea.addView(title)

        // 설명
        val description =
            TextView(requireContext()).apply {

                text = service.description

                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_secondary
                    )
                )

                textSize = 13f
            }

        val descriptionParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                topMargin = dp(5)
            }

        textArea.addView(
            description,
            descriptionParams
        )

        row.addView(
            textArea,
            textAreaParams
        )

        // 오른쪽 화살표
        val arrow =
            TextView(requireContext()).apply {

                text = "›"

                textSize = 28f

                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_secondary
                    )
                )

                gravity =
                    android.view.Gravity.CENTER
            }

        val arrowParams =
            LinearLayout.LayoutParams(
                dp(28),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        row.addView(
            arrow,
            arrowParams
        )

        return row
    }

    /**
     * 공식 웹사이트 열기
     */
    private fun openWebsite(
        url: String
    ) {

        try {

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )

            startActivity(intent)

        } catch (e: ActivityNotFoundException) {

            Toast.makeText(
                requireContext(),
                "웹사이트를 열 수 없습니다.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * dp → px
     */
    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }
}