package com.example.demo.use

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.example.demo.R
import com.example.demo.databinding.FragmentMainBinding
import com.example.demo.databinding.ItemTabMainBinding
import com.example.demo.use.mvvm.MainState
import com.example.demo.use.mvvm.MainViewModel
import com.example.demo.use.tab.CircleFragment
import com.example.demo.use.tab.HomeFragment
import com.example.demo.use.tab.MineFragment
import com.kehuafu.base.core.container.base.BaseFragment
import com.kehuafu.base.core.container.widget.toast.showToast
import com.kehuafu.base.core.fragment.widget.TabLayout
import com.kehuafu.base.core.ktx.viewBindings
import java.lang.IndexOutOfBoundsException

class MainFragment : BaseFragment<FragmentMainBinding, MainViewModel, MainState>() {

    companion object {
        private const val TAG = "MainFragment"
    }

    private val tabTexts = arrayOf("首页", "圈子", "我的")
    private val tabDrawable = intArrayOf(
        R.drawable.tab_recommend_selected,
        R.drawable.tab_circle_selected,
        R.drawable.tab_profile_selected,
    )

    private val itemTab by viewBindings<ItemTabMainBinding>()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(
            "MAIN_CURRENT_POSITION",
            viewBinding.tab.getCurrentPosition()
        )
    }

    override fun onViewCreated(savedInstanceState: Bundle?) {
        //获取MainActivity传过来的值
        val arg = arguments?.get("key")
        Log.d(TAG, "onViewCreated: $arg")
        initTestView()
        setTabView(savedInstanceState)
        initViewPager()
    }

    override fun onStateChanged(state: MainState) {
        super.onStateChanged(state)
        itemTab.tvTabName.text = state.token.uid
    }

    private fun initTestView() {
        itemTab.tvTabName.text = "点击试试"
        itemTab.tvTabName.setOnClickListener {
            showToast("哈哈哈哈")
            viewModel.main("uid9999", "token")
        }
    }

    private fun initViewPager() {
        val viewpager = viewBinding.viewpager
        viewpager.offscreenPageLimit = 2
        viewpager.adapter = object :
            FragmentPagerAdapter(childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getCount(): Int {
                return 3
            }

            override fun getItem(position: Int): Fragment {
                return when (position) {
                    0 -> {
                        HomeFragment.newInstance()
                    }
                    1 -> {
                        CircleFragment.newInstance()
                    }
                    2 -> {
                        MineFragment.newInstance()
                    }
                    else -> {
                        throw IndexOutOfBoundsException()
                    }
                }
            }
        }
    }


    private fun setTabView(savedInstanceState: Bundle?) {
        val tab = viewBinding.tab
        tab.setTabMod(TabLayout.MODE.AUTO)
        for (i in 0..2) {
            val layoutInflater = LayoutInflater.from(requireView().context)
            val tabView: View = layoutInflater.inflate(R.layout.item_tab_main, null)
            val imgTab = tabView.findViewById<ImageView>(R.id.iv_tab_icon)
            val txtTab = tabView.findViewById<TextView>(R.id.tv_tab_name)
            imgTab.setImageDrawable(ContextCompat.getDrawable(imgTab.context, tabDrawable[i]))
            imgTab.setColorFilter(ContextCompat.getColor(imgTab.context, R.color.tab_default))
            txtTab.setTextColor(ContextCompat.getColor(txtTab.context, R.color.tab_default))
            txtTab.text = tabTexts[i]
            tab.addTab(tabView)
        }
        tab.setupWithViewPager(viewBinding.viewpager)
        tab.setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tabView: View, position: Int, isRefresh: Boolean) {
                val imgTab = tabView.findViewById<ImageView>(R.id.iv_tab_icon)
                val txtTab = tabView.findViewById<TextView>(R.id.tv_tab_name)
                imgTab.setColorFilter(ContextCompat.getColor(imgTab.context, R.color.tab_selected))
                txtTab.setTextColor(ContextCompat.getColor(txtTab.context, R.color.tab_selected))
            }

            override fun onTabUnselected(tabView: View, position: Int) {
                val imgTab = tabView.findViewById<ImageView>(R.id.iv_tab_icon)
                val txtTab = tabView.findViewById<TextView>(R.id.tv_tab_name)
                imgTab.setColorFilter(ContextCompat.getColor(imgTab.context, R.color.tab_default))
                txtTab.setTextColor(ContextCompat.getColor(txtTab.context, R.color.tab_default))
            }
        })
        tab.selectTab(savedInstanceState?.getInt("MAIN_CURRENT_POSITION") ?: 0)
    }
}