package cc.kafuu.bilidownload.view.activity

import android.view.inputmethod.InputMethodManager
import cc.kafuu.bilidownload.BR
import cc.kafuu.bilidownload.R
import cc.kafuu.bilidownload.common.core.CoreActivity
import cc.kafuu.bilidownload.databinding.ActivitySearchBinding
import cc.kafuu.bilidownload.model.SearchType
import cc.kafuu.bilidownload.view.fragment.SearchListFragment
import cc.kafuu.bilidownload.viewmodel.activity.SearchViewModel


class SearchActivity : CoreActivity<ActivitySearchBinding, SearchViewModel>(
    SearchViewModel::class.java,
    R.layout.activity_search,
    BR.viewModel
) {

    override fun initViews() {
        setImmersionStatusBar()
        initListener()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        mViewDataBinding.etSearchContent.apply {
            requestFocus()
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                this,
                InputMethodManager.SHOW_IMPLICIT
            )
        }
    }

    private fun initListener() {
        mViewModel.searchRequestLiveData.observe(this) {
            mViewDataBinding.fvFragment.getFragment<SearchListFragment>().doSearch(it, getSearchType())
        }
    }

    private fun getSearchType() = when(mViewDataBinding.spSearchType.selectedItemPosition) {
        0 -> SearchType.VIDEO
        1 -> SearchType.MEDIA_BANGUMI
        2 -> SearchType.MEDIA_FT
        else -> throw IllegalArgumentException("Unknown search type: $mViewDataBinding")
    }
}