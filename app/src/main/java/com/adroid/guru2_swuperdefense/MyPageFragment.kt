package com.adroid.guru2_swuperdefense

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

/** 마이페이지 메뉴 화면: 계정 / 스크랩한 게시글 / 버전 / 로그아웃. */
class MyPageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_mypage,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUserId = AppSession.currentUserId(requireContext())
        view.findViewById<TextView>(R.id.tvProfileId).text =
            currentUserId?.takeIf { it.isNotBlank() } ?: "로그인 정보 없음"

        view.findViewById<TextView>(R.id.tvVersionValue).text = appVersionName()

        view.findViewById<View>(R.id.rowAccount).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, AccountFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.rowScrappedPosts).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, ScrappedPostsFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.rowVersion).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.app_name))
                .setMessage("현재 버전: ${appVersionName()}")
                .setPositiveButton("확인", null)
                .show()
        }

        view.findViewById<View>(R.id.rowLogout).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("로그아웃")
                .setMessage("현재 계정에서 로그아웃하시겠습니까?")
                .setPositiveButton("로그아웃") { _, _ ->
                    AppSession.logOut(requireContext())
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private fun appVersionName(): String {
        return try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }
}
