package cc.kafuu.bilidownload.viewmodel.fragment

import androidx.lifecycle.viewModelScope
import cc.kafuu.bilidownload.common.model.LoadingStatus
import cc.kafuu.bilidownload.common.model.action.popmessage.ToastMessageAction
import cc.kafuu.bilidownload.common.model.bili.BiliFavoriteModel
import cc.kafuu.bilidownload.common.network.IServerCallback
import cc.kafuu.bilidownload.common.network.manager.NetworkManager
import cc.kafuu.bilidownload.common.network.model.BiliFavoriteData
import cc.kafuu.bilidownload.common.network.model.BiliFavoriteListData
import cc.kafuu.bilidownload.viewmodel.common.BiliRVViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteListViewModel : BiliRVViewModel() {
    private val mBiliAccountRepository = NetworkManager.biliAccountRepository

    // 用户mid
    private var mMid: Long = 0

    fun initData(mid: Long) {
        mMid = mid
        onRefreshData()
    }

    override fun onRefreshData(
        onSucceeded: (() -> Unit)?,
        onFailed: (() -> Unit)?,
    ) {
        setLoadingStatus(LoadingStatus.loadingStatus(false))
        val callback = object : IServerCallback<BiliFavoriteListData> {
            override fun onSuccess(
                httpCode: Int,
                code: Int,
                message: String,
                data: BiliFavoriteListData
            ) {
                viewModelScope.launch {
                    updateList(withContext(Dispatchers.IO) {
                        onFavoritesLoaded(data.list).toMutableList()
                    })
                    onSucceeded?.invoke()
                }
            }

            override fun onFailure(httpCode: Int, code: Int, message: String) {
                onFailed?.invoke()
                popMessage(ToastMessageAction(message))
            }
        }
        mBiliAccountRepository.requestUserFavorites(mid = mMid, type = 2, callback)
    }

    private suspend fun onFavoritesLoaded(
        list: List<BiliFavoriteData>
    ) = coroutineScope {
        val deferredList = list.map {
            async {
                val cover = mBiliAccountRepository.syncRequestFavoriteCover(it.id)
                BiliFavoriteModel.create(it, cover)
            }
        }
        deferredList.awaitAll()
    }
}