package cc.kafuu.bilidownload.view.activity

import android.content.Intent
import cc.kafuu.bilidownload.BR
import cc.kafuu.bilidownload.R
import cc.kafuu.bilidownload.common.CommonLibs
import cc.kafuu.bilidownload.common.adapter.FragmentAdapter
import cc.kafuu.bilidownload.common.core.CoreActivity
import cc.kafuu.bilidownload.common.core.CoreFragmentBuilder
import cc.kafuu.bilidownload.common.manager.AccountManager
import cc.kafuu.bilidownload.databinding.ActivityPersonalDetailsBinding
import cc.kafuu.bilidownload.view.fragment.WatchHistoryFragment
import cc.kafuu.bilidownload.viewmodel.activity.PersonalDetailsViewModel
import com.google.android.material.tabs.TabLayoutMediator

class PersonalDetailsActivity :
    CoreActivity<ActivityPersonalDetailsBinding, PersonalDetailsViewModel>(
        PersonalDetailsViewModel::class.java,
        R.layout.activity_personal_details,
        BR.viewModel
    ) {
    companion object {
        private const val KEY_MID = "mid"

        fun buildIntent(mid: Long) = Intent().apply {
            putExtra(KEY_MID, mid)
        }
    }

    override fun initViews() {
        val mid = intent.getLongExtra(KEY_MID, 0)
        mViewDataBinding.initViews(mid)
        mViewModel.init(mid)
    }

    private fun ActivityPersonalDetailsBinding.initViews(mid: Long) {
        val fragments = getFragmentBuilders(mid == AccountManager.accountLiveData.value?.mid)
        vp2Personal.apply {
            adapter =
                FragmentAdapter(supportFragmentManager, lifecycle, fragments.map { it.second })
        }
        TabLayoutMediator(tlPageSelector, vp2Personal) { tab, position ->
            tab.text = CommonLibs.getString(fragments[position].first)
        }.attach()
    }

    private fun PersonalDetailsViewModel.init(mid: Long) {
        initData(mid)
    }

    private fun getFragmentBuilders(isMyself: Boolean): List<Pair<Int, CoreFragmentBuilder<*>>> {
        val fragmentBuilders = mutableListOf<Pair<Int, CoreFragmentBuilder<*>>>()
        if (isMyself) {
            fragmentBuilders.add(R.string.personal_tab_history to WatchHistoryFragment.builder())
        }
        return fragmentBuilders
    }

}