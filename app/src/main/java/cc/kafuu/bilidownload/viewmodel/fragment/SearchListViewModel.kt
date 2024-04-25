package cc.kafuu.bilidownload.viewmodel.fragment

import android.util.Log
import cc.kafuu.bilidownload.R
import cc.kafuu.bilidownload.common.network.IServerCallback
import cc.kafuu.bilidownload.common.network.manager.NetworkManager
import cc.kafuu.bilidownload.common.network.model.BiliSearchData
import cc.kafuu.bilidownload.common.utils.CommonLibs
import cc.kafuu.bilidownload.model.BiliVideo
import cc.kafuu.bilidownload.model.LoadingStatus
import com.bumptech.glide.load.resource.bitmap.CenterCrop

class SearchListViewModel : RVViewModel() {
    val centerCrop = CenterCrop()

    companion object {
        private const val TAG = "SearchListViewModel"
    }

    fun doSearch(keyword: String) {
        Log.d(TAG, "doSearch: $keyword")
        if (loadingStatusMessageMutableLiveData.value == LOADING_STATUS_LOADING) {
            Log.d(TAG, "doSearch: Search execution")
            return
        }
        loadingStatusMessageMutableLiveData.value = LOADING_STATUS_LOADING
        NetworkManager.biliSearchRepository.search(
            keyword,
            object : IServerCallback<BiliSearchData> {
                override fun onSuccess(
                    httpCode: Int,
                    code: Int,
                    message: String,
                    data: BiliSearchData
                ) {
                    onSearchRespond(data)
                }

                override fun onFailure(httpCode: Int, code: Int, message: String) {
                    Log.d(TAG, "onFailure: httpCode($httpCode), code($code), message($message)")
                    loadingStatusMessageMutableLiveData.postValue(
                        LoadingStatus(
                            true,
                            CommonLibs.getDrawable(R.drawable.ic_error),
                            message
                        )
                    )
                }
            })
    }

    private fun onSearchRespond(data: BiliSearchData) {
        Log.d(TAG, "onSearchRespond: $data")
        val searchData: MutableList<Any> = mutableListOf()
        data.result.forEach { biliSearchResult ->
            val result = biliSearchResult.data.filter { it.type == "video" }.map {
                BiliVideo(
                    it.bvid,
                    it.title,
                    it.description,
                    "https:${it.pic}"
                )
            }
            searchData.addAll(result)
        }
        loadingStatusMessageMutableLiveData.postValue(
            if (searchData.isEmpty()) {
                LOADING_STATUS_EMPTY
            } else {
                LOADING_STATUS_DONE
            }
        )
        listMutableLiveData.postValue(searchData)
    }
}