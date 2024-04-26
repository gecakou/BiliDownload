package cc.kafuu.bilidownload.viewmodel.fragment

import android.util.Log
import cc.kafuu.bilidownload.common.network.IServerCallback
import cc.kafuu.bilidownload.common.network.manager.NetworkManager
import cc.kafuu.bilidownload.common.network.model.BiliSearchData
import cc.kafuu.bilidownload.common.network.model.BiliSearchResultData
import cc.kafuu.bilidownload.common.network.repository.BiliSearchRepository
import cc.kafuu.bilidownload.model.BiliVideo
import cc.kafuu.bilidownload.model.LoadingStatus
import com.bumptech.glide.load.resource.bitmap.CenterCrop

class SearchListViewModel : RVViewModel() {
    val centerCrop = CenterCrop()
    var keyword: String? = null

    private var mNextPage = 1

    companion object {
        private const val TAG = "SearchListViewModel"
        private val mBiliSearchRepository = NetworkManager.biliSearchRepository
    }


    fun doSearch(
        loadingStatus: LoadingStatus,
        loadMore: Boolean,
        callback: IServerCallback<BiliSearchData>? = null
    ) {
        if (keyword == null || loadingStatusMessageMutableLiveData.value?.statusCode == LoadingStatus.CODE_LOADING) {
            if (keyword != null) Log.d(TAG, "doSearch: Search execution")
            return
        }
        if (!loadMore) {
            mNextPage = 1
        }
        loadingStatusMessageMutableLiveData.value = loadingStatus
        val searchCallback = object : IServerCallback<BiliSearchData> {
            override fun onSuccess(
                httpCode: Int,
                code: Int,
                message: String,
                data: BiliSearchData
            ) {
                callback?.onSuccess(httpCode, code, message, data)
                onLoadingCompleted(data, loadMore)
            }

            override fun onFailure(httpCode: Int, code: Int, message: String) {
                callback?.onFailure(httpCode, code, message)
                loadingStatusMessageMutableLiveData.postValue(
                    if (loadMore) {
                        LoadingStatus.errorStatus(visibility = false, message = message)
                    } else {
                        LoadingStatus.errorStatus(message = message)
                    }
                )
            }
        }
        mBiliSearchRepository.search(
            BiliSearchRepository.SEARCH_TYPE_VIDEO,
            keyword!!,
            mNextPage,
            searchCallback
        )
    }

    private fun onLoadingCompleted(data: BiliSearchData, loadMore: Boolean) {
        Log.d(TAG, "onLoadingCompleted: $data")
        val searchData: MutableList<Any> = if (loadMore) {
            listMutableLiveData.value ?: mutableListOf()
        } else {
            mutableListOf()
        }

        searchData.addAll(getResultVideos(data.result))
        loadingStatusMessageMutableLiveData.postValue(
            if (searchData.isEmpty()) {
                LoadingStatus.emptyStatus()
            } else {
                LoadingStatus.doneStatus()
            }
        )
        mNextPage++
        listMutableLiveData.postValue(searchData)
    }

    private fun getResultVideos(biliSearchResultDataList: List<BiliSearchResultData>): List<BiliVideo> {
        return biliSearchResultDataList.filter { it.type == "video" }.map {
            BiliVideo(
                author = it.author,
                authorId = it.mid,
                bvid = it.bvid,
                title = it.title,
                description = it.description,
                pic = "https:${it.pic}",
                pubDate = it.pubdate,
                sendDate = it.senddate,
                duration = it.duration
            )
        }
    }
}