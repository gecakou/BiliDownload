package cc.kafuu.bilidownload.viewmodel.fragment

import android.util.Log
import cc.kafuu.bilidownload.common.constant.SearchType
import cc.kafuu.bilidownload.common.model.LoadingStatus
import cc.kafuu.bilidownload.common.model.bili.BiliMediaModel
import cc.kafuu.bilidownload.common.model.bili.BiliVideoModel
import cc.kafuu.bilidownload.common.network.IServerCallback
import cc.kafuu.bilidownload.common.network.manager.NetworkManager
import cc.kafuu.bilidownload.common.network.model.BiliSearchData
import cc.kafuu.bilidownload.common.network.model.BiliSearchMediaResultData
import cc.kafuu.bilidownload.common.network.model.BiliSearchVideoResultData
import cc.kafuu.bilidownload.common.network.model.BiliSeasonData
import cc.kafuu.bilidownload.common.network.model.BiliVideoData
import cc.kafuu.bilidownload.common.utils.BvConvertUtils
import cc.kafuu.bilidownload.common.utils.NetworkUtils
import cc.kafuu.bilidownload.common.utils.TimeUtils
import cc.kafuu.bilidownload.view.activity.VideoDetailsActivity
import java.util.Locale
import java.util.regex.Pattern

class SearchListViewModel : RVViewModel() {
    var keyword: String? = null

    @SearchType
    var searchType: Int = SearchType.VIDEO

    private var mNextPage = 1

    companion object {
        private const val TAG = "SearchListViewModel"
        private val mBiliSearchRepository = NetworkManager.biliSearchRepository
        private val mBiliVideoRepository = NetworkManager.biliVideoRepository
    }

    fun doSearch(
        loadingStatus: LoadingStatus,
        loadMore: Boolean,
        forceSearch: Boolean = false,
        onSucceeded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        if (keyword == null || mLoadingStatusMessageMutableLiveData.value?.statusCode == LoadingStatus.CODE_LOADING) {
            if (keyword != null) Log.d(TAG, "doSearch: Search execution")
            return
        }
        val keyword = keyword ?: return

        // 非强制搜索的情况，尝试解析搜索内容
        if (!forceSearch) {
            // 搜索的内容是否包含链接，如果包含则解析链接
            if (!loadMore && NetworkUtils.containsUrl(keyword)) {
                doAnalysisUrl(keyword)
                return
            }
            // 尝试直接解析搜索内容中是否有视频id
            parseAddress(keyword)?.let {
                tryAnalysisId(it)
            }
        }

        if (!loadMore) mNextPage = 1
        mLoadingStatusMessageMutableLiveData.value = loadingStatus

        when (searchType) {
            SearchType.VIDEO -> mBiliSearchRepository.requestSearchVideo(
                keyword, mNextPage,
                createSearchCallback(onSucceeded, onFailed, loadMore)
            )

            SearchType.MEDIA_BANGUMI -> mBiliSearchRepository.requestSearchMediaBangumi(
                keyword, mNextPage,
                createSearchCallback(onSucceeded, onFailed, loadMore)
            )

            SearchType.MEDIA_FT -> mBiliSearchRepository.requestSearchMediaFt(
                keyword, mNextPage,
                createSearchCallback(onSucceeded, onFailed, loadMore)
            )
        }
    }

    /**
     * 强制搜索
     */
    private fun forceSearch() {
        doSearch(LoadingStatus.loadingStatus(), loadMore = false, forceSearch = true)
    }

    private fun doAnalysisUrl(text: String) {
        // 如果是分享链接
        if (text.contains("https://b23.tv/")) {
            object : IServerCallback<String> {
                override fun onSuccess(httpCode: Int, code: Int, message: String, data: String) {
                    doAnalysisUrl(data)
                }

                override fun onFailure(httpCode: Int, code: Int, message: String) {
                    Log.e(TAG, "onFailure: $message")
                    forceSearch()
                }
            }.also {
                val matcher = Pattern.compile("https://b23.tv/.*").matcher(text)
                if (!matcher.find()) {
                    forceSearch()
                    return
                }
                Log.d(TAG, "doAnalysisUrl: $text")
                NetworkUtils.redirection(matcher.group(), it)
            }
            return
        }
        // 尝试从链接中提取BVID或AV、EP、SS
        val id = parseAddress(text) ?: run {
            forceSearch()
            return
        }
        tryAnalysisId(id)
    }

    private fun tryAnalysisId(id: String) = try {
        doAnalysisId(id)
    } catch (e: Exception) {
        e.printStackTrace()
        forceSearch()
    }

    /**
     * 根据ID调用其相对应的请求
     */
    private fun doAnalysisId(id: String) {
        if (id.length <= 2) {
            forceSearch()
            return
        }

        when (id.substring(0, 2).uppercase(Locale.ROOT)) {
            "BV" -> mBiliVideoRepository.requestVideoDetail(
                id,
                createVideoDetailCallback()
            )

            "AV" -> mBiliVideoRepository.requestVideoDetail(
                BvConvertUtils.av2bv(id.substring(2)),
                createVideoDetailCallback()
            )

            "SS" -> mBiliVideoRepository.requestSeasonDetailBySeasonId(
                id.substring(2).toLong(),
                createSeasonDetailCallback()
            )

            "EP" -> mBiliVideoRepository.requestSeasonDetailByEpId(
                id.substring(2).toLong(),
                createSeasonDetailCallback()
            )

            else -> forceSearch()
        }
    }

    /**
     * 获取视频详情回调
     */
    private fun createVideoDetailCallback() = object : IServerCallback<BiliVideoData> {
        override fun onSuccess(httpCode: Int, code: Int, message: String, data: BiliVideoData) {
            BiliVideoModel(
                author = data.owner.name,
                bvid = data.bvid,
                title = data.title,
                description = data.desc,
                cover = data.pic,
                pubDate = data.pubDate,
                duration = TimeUtils.formatDuration(data.duration.toDouble())
            ).also {
                enterDetails(it)
            }
            mLoadingStatusMessageMutableLiveData.postValue(LoadingStatus.doneStatus())
        }

        override fun onFailure(httpCode: Int, code: Int, message: String) {
            Log.e(TAG, "onFailure: $message")
            forceSearch()
        }
    }

    /**
     * 获取电视剧详情回调
     */
    private fun createSeasonDetailCallback() = object : IServerCallback<BiliSeasonData> {
        override fun onSuccess(httpCode: Int, code: Int, message: String, data: BiliSeasonData) {
            BiliMediaModel(
                mediaId = data.mediaId,
                seasonId = data.seasonId,
                title = data.title,
                cover = data.cover,
                mediaType = data.type,
                description = data.evaluate,
                pubDate = data.episodes.firstOrNull()?.pubTime ?: 0
            ).also {
                enterDetails(it)
            }
            mLoadingStatusMessageMutableLiveData.postValue(LoadingStatus.doneStatus())
        }

        override fun onFailure(httpCode: Int, code: Int, message: String) {
            Log.e(TAG, "onFailure: $message")
            forceSearch()
        }
    }

    private fun <T> createSearchCallback(
        onSucceeded: (() -> Unit)?,
        onFailed: (() -> Unit)?,
        loadMore: Boolean
    ) = object : IServerCallback<BiliSearchData<T>> {
        override fun onSuccess(
            httpCode: Int,
            code: Int,
            message: String,
            data: BiliSearchData<T>
        ) {
            onSucceeded?.invoke()
            onLoadingCompleted(data, loadMore)
        }

        override fun onFailure(httpCode: Int, code: Int, message: String) {
            onFailed?.invoke()
            LoadingStatus.errorStatus(visibility = !loadMore, message = message).let {
                mLoadingStatusMessageMutableLiveData.postValue(it)
            }
        }
    }

    private fun onLoadingCompleted(data: BiliSearchData<*>, loadMore: Boolean) {
        Log.d(TAG, "onLoadingCompleted: $data")
        val searchData: MutableList<Any> = if (loadMore) {
            listMutableLiveData.value ?: mutableListOf()
        } else {
            mutableListOf()
        }
        searchData.addAll(data.result.orEmpty().mapNotNull { result ->
            when (result) {
                is BiliSearchVideoResultData -> if (result.type == "video") disposeResult(result) else null
                is BiliSearchMediaResultData -> disposeResult(result)
                else -> throw IllegalStateException("Unknown result from $result")
            }
        })
        updateList(searchData)
        mNextPage++
    }

    /**
     * 将BiliSearchVideoResultData解析为BiliVideoModel
     */
    private fun disposeResult(element: BiliSearchVideoResultData) = BiliVideoModel(
        author = element.author,
        bvid = element.bvid,
        title = element.title,
        description = element.description,
        cover = "https:${element.pic}",
        pubDate = element.pubDate,
        duration = element.duration
    )

    /**
     * 将BiliSearchMediaResultData解析为BiliMediaModel
     */
    private fun disposeResult(element: BiliSearchMediaResultData) = BiliMediaModel(
        mediaId = element.mediaId,
        seasonId = element.seasonId,
        title = element.title,
        cover = element.cover,
        mediaType = element.mediaType,
        description = element.desc,
        pubDate = element.pubTime
    )

    fun enterDetails(element: BiliVideoModel) {
        startActivity(VideoDetailsActivity::class.java, VideoDetailsActivity.buildIntent(element))
    }

    fun enterDetails(element: BiliMediaModel) {
        startActivity(VideoDetailsActivity::class.java, VideoDetailsActivity.buildIntent(element))
    }

    /**
     * 根据url地址视频的BV号或AV号
     * @param address 要解析的视频地址
     */
    private fun parseAddress(address: String): String? {
        val matcher = Pattern.compile("(BV.{10})|((av|ep|ss|AV|EP|SS)\\d*)").matcher(address)
        return if (!matcher.find()) null else matcher.group()
    }
}