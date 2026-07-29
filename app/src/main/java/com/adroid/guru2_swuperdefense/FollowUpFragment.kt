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

    private data class ServiceItem(
        val icon: String,
        val title: String,
        val description: String,
        val url: String
    )

    private data class ServiceCategory(
        val title: String,
        @ColorRes val colorRes: Int,
        val services: List<ServiceItem>
    )

    private val categories = listOf(

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

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val serviceContainer =
            view.findViewById<LinearLayout>(R.id.serviceContainer)

        categories.forEach { category ->
            addCategory(
                container = serviceContainer,
                category = category
            )
        }
    }

    private fun addCategory(
        container: LinearLayout,
        category: ServiceCategory
    ) {

        val categoryColor =
            ContextCompat.getColor(
                requireContext(),
                category.colorRes
            )

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

            rowsContainer.addView(
                createServiceRow(
                    service = service,
                    categoryColor = categoryColor
                )
            )

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

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }
}