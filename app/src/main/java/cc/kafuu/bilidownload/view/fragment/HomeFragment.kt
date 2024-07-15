package cc.kafuu.bilidownload.view.fragment

import cc.kafuu.bilidownload.BR
import cc.kafuu.bilidownload.R
import cc.kafuu.bilidownload.common.CommonLibs
import cc.kafuu.bilidownload.common.adapter.FragmentAdapter
import cc.kafuu.bilidownload.common.core.CoreFragment
import cc.kafuu.bilidownload.common.core.CoreFragmentBuilder
import cc.kafuu.bilidownload.common.room.entity.DownloadTaskEntity
import cc.kafuu.bilidownload.databinding.FragmentHomeBinding
import cc.kafuu.bilidownload.viewmodel.fragment.HomeViewModel
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment : CoreFragment<FragmentHomeBinding, HomeViewModel>(
    HomeViewModel::class.java,
    R.layout.fragment_home,
    BR.viewModel
) {
    companion object {
        @JvmStatic
        fun getBuilder() = CoreFragmentBuilder(HomeFragment::class)
    }

    override fun initViews() {
        val fragments = getFragments()
        mViewDataBinding.vp2Home.apply {
            adapter = FragmentAdapter(childFragmentManager, lifecycle, fragments.map { it.second })
        }
        TabLayoutMediator(
            mViewDataBinding.tlPageSelector,
            mViewDataBinding.vp2Home
        ) { tab, position -> tab.text = fragments[position].first }.attach()
    }

    private fun getFragments() = listOf(
        CommonLibs.getString(R.string.home_tab_text_all) to HistoryFragment.getBuilder(
            DownloadTaskEntity.STATUS_PREPARE,
            DownloadTaskEntity.STATUS_DOWNLOADING,
            DownloadTaskEntity.STATUS_DOWNLOAD_FAILED,
            DownloadTaskEntity.STATUS_SYNTHESIS,
            DownloadTaskEntity.STATUS_SYNTHESIS_FAILED,
            DownloadTaskEntity.STATUS_COMPLETED
        ),
        CommonLibs.getString(R.string.home_tab_text_completed) to HistoryFragment.getBuilder(
            DownloadTaskEntity.STATUS_COMPLETED
        ),
        CommonLibs.getString(R.string.home_tab_text_running) to HistoryFragment.getBuilder(
            DownloadTaskEntity.STATUS_PREPARE,
            DownloadTaskEntity.STATUS_DOWNLOADING,
            DownloadTaskEntity.STATUS_SYNTHESIS,
        ),
        CommonLibs.getString(R.string.home_tab_text_failed) to HistoryFragment.getBuilder(
            DownloadTaskEntity.STATUS_DOWNLOAD_FAILED,
            DownloadTaskEntity.STATUS_SYNTHESIS_FAILED,
        ),
    )
}