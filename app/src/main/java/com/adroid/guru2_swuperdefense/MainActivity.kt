package com.adroid.guru2_swuperdefense

import android.graphics.Typeface
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

class MainActivity : AppCompatActivity() {

    private lateinit var navHome: TextView
    private lateinit var navBoard: TextView
    private lateinit var navEvidence: TextView
    private lateinit var navMyPage: TextView

    private lateinit var navigationItems: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
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

        navigationItems = listOf(
            navHome,
            navBoard,
            navEvidence,
            navMyPage
        )

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

    private fun selectNavigation(
        selectedItem: TextView
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
            item.setTextColor(normalColor)
            item.setTypeface(null, Typeface.NORMAL)
        }

        selectedItem.setTextColor(selectedColor)
        selectedItem.setTypeface(null, Typeface.BOLD)
    }
}