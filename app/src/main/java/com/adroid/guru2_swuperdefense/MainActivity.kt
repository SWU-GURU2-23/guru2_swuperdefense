package com.adroid.guru2_swuperdefense

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

class MainActivity : AppCompatActivity() {

    // ==== 수정 시작: 디자인 캡처 반영 - 하단 네비 항목이 TextView 단독 → 아이콘(ImageView)+라벨(TextView) 구조로 바뀌어 타입 변경 ====
    private data class NavItem(val container: View, val icon: ImageView, val label: TextView)

    private lateinit var navHome: View
    private lateinit var navBoard: View
    private lateinit var navEvidence: View
    private lateinit var navMyPage: View

    private lateinit var navigationItems: List<NavItem>
    // ==== 수정 끝 ====

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->

            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        navHome = findViewById(R.id.navHome)
        navBoard = findViewById(R.id.navBoard)
        navEvidence = findViewById(R.id.navEvidence)
        navMyPage = findViewById(R.id.navMyPage)

        // ==== 수정 시작: 아이콘/라벨 참조 추가 ====
        navigationItems = listOf(
            NavItem(navHome, findViewById(R.id.navHomeIcon), findViewById(R.id.navHomeLabel)),
            NavItem(navBoard, findViewById(R.id.navBoardIcon), findViewById(R.id.navBoardLabel)),
            NavItem(navEvidence, findViewById(R.id.navEvidenceIcon), findViewById(R.id.navEvidenceLabel)),
            NavItem(navMyPage, findViewById(R.id.navMyPageIcon), findViewById(R.id.navMyPageLabel))
        )
        // ==== 수정 끝 ====

        navHome.setOnClickListener {
            openRootFragment(HomeFragment())
            selectNavigation(navHome)
        }

        navBoard.setOnClickListener {
            openRootFragment(BoardFragment())
            selectNavigation(navBoard)
        }

        navEvidence.setOnClickListener {
            openRootFragment(EvidenceFragment())
            selectNavigation(navEvidence)
        }

        navMyPage.setOnClickListener {
            openRootFragment(MyPageFragment())
            selectNavigation(navMyPage)
        }

        if (savedInstanceState == null) {
            openRootFragment(HomeFragment())
            selectNavigation(navHome)
        }
    }

    // ==== 여기부터 추가: 홈 화면 미니카드에서 하단 탭을 직접 전환하기 위한 함수 ====
    /**
     * 홈 화면의 미니카드처럼 다른 화면에서 하단 탭을 직접 전환해야 할 때 사용.
     */
    fun navigateToTab(tabId: Int) {
        when (tabId) {
            R.id.navBoard -> {
                openRootFragment(BoardFragment())
                selectNavigation(navBoard)
            }
            R.id.navEvidence -> {
                openRootFragment(EvidenceFragment())
                selectNavigation(navEvidence)
            }
            R.id.navMyPage -> {
                openRootFragment(MyPageFragment())
                selectNavigation(navMyPage)
            }
            else -> {
                openRootFragment(HomeFragment())
                selectNavigation(navHome)
            }
        }
    }
    // ==== 추가 끝 ====

    private fun openRootFragment(fragment: Fragment) {
        supportFragmentManager.popBackStack(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                fragment
            )
            .commit()
    }

    // ==== 수정 시작: selectNavigation - TextView 색상만 바꾸던 것 → 아이콘 tint까지 같이 바꾸도록 수정 ====
    private fun selectNavigation(
        selectedItem: View
    ) {
        val normalColor = ContextCompat.getColor(
            this,
            R.color.text_secondary
        )

        val selectedColor = ContextCompat.getColor(
            this,
            R.color.orange_primary
        )

        navigationItems.forEach { item ->
            val isSelected = item.container == selectedItem
            val color = if (isSelected) selectedColor else normalColor
            item.icon.setColorFilter(color)
            item.label.setTextColor(color)
            item.label.setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
        }
    }
    // ==== 수정 끝 ====
}
