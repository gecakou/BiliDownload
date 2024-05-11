package cc.kafuu.bilidownload.viewmodel.activity

import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import cc.kafuu.bilidownload.common.core.CoreViewModel
import cc.kafuu.bilidownload.common.network.IServerCallback
import cc.kafuu.bilidownload.common.network.manager.NetworkManager
import cc.kafuu.bilidownload.common.network.model.BiliPlayStreamDash
import cc.kafuu.bilidownload.common.network.model.BiliSeasonData
import cc.kafuu.bilidownload.common.network.model.BiliVideoData
import cc.kafuu.bilidownload.common.utils.TimeUtils
import cc.kafuu.bilidownload.model.LoadingStatus
import cc.kafuu.bilidownload.model.bili.BiliMediaDetails
import cc.kafuu.bilidownload.model.bili.BiliResourceDetails
import cc.kafuu.bilidownload.model.bili.BiliVideoDetails
import cc.kafuu.bilidownload.model.bili.BiliVideoPart
import cc.kafuu.bilidownload.model.popmessage.ToastMessage

class VideoDetailsViewModel : CoreViewModel() {
    val loadingStatusLiveData = MutableLiveData(LoadingStatus.waitStatus())
    val biliResourceDetailsLiveData = MutableLiveData<BiliResourceDetails>()
    val biliVideoPageListLiveData = MutableLiveData<List<BiliVideoPart>>()

    val selectedBiliPlayStreamDashLiveData =
        MutableLiveData<Pair<BiliVideoPart, BiliPlayStreamDash>>()

    fun initData(media: BiliMediaDetails) {
        loadingStatusLiveData.value = LoadingStatus.loadingStatus()
        biliResourceDetailsLiveData.value = media

        val callback = object : IServerCallback<BiliSeasonData> {
            override fun onSuccess(
                httpCode: Int,
                code: Int,
                message: String,
                data: BiliSeasonData
            ) {
                biliVideoPageListLiveData.postValue(data.episodes.map {
                    BiliVideoPart(
                        it.bvid,
                        it.cid,
                        "${it.title}: ${it.longTitle}",
                        null
                    )
                })
                loadingStatusLiveData.value = LoadingStatus.doneStatus()
            }

            override fun onFailure(httpCode: Int, code: Int, message: String) {
                loadingStatusLiveData.value = LoadingStatus.errorStatus(
                    message = message
                )
            }
        }
        if (media.seasonId != 0L) {
            NetworkManager.biliVideoRepository.getSeasonDetailBySeasonId(media.seasonId, callback)
        } else {
            NetworkManager.biliVideoRepository.getSeasonDetailByEpId(media.mediaId, callback)
        }
    }

    fun initData(video: BiliVideoDetails) {
        loadingStatusLiveData.value = LoadingStatus.loadingStatus()
        biliResourceDetailsLiveData.value = video
        val callback = object : IServerCallback<BiliVideoData> {
            override fun onSuccess(httpCode: Int, code: Int, message: String, data: BiliVideoData) {
                biliVideoPageListLiveData.postValue(data.pages.map {
                    BiliVideoPart(
                        video.bvid,
                        it.cid,
                        it.part,
                        TimeUtils.formatSecondTime(it.duration)
                    )
                })
                loadingStatusLiveData.value = LoadingStatus.doneStatus()
            }

            override fun onFailure(httpCode: Int, code: Int, message: String) {
                loadingStatusLiveData.value = LoadingStatus.errorStatus(
                    message = message
                )
            }
        }
        NetworkManager.biliVideoRepository.getVideoDetail(video.bvid, callback)
    }

    fun onPartSelected(item: BiliVideoPart) {
        val callback = object : IServerCallback<BiliPlayStreamDash> {
            override fun onSuccess(
                httpCode: Int,
                code: Int,
                message: String,
                data: BiliPlayStreamDash
            ) {
                selectedBiliPlayStreamDashLiveData.postValue(Pair(item, data))
            }

            override fun onFailure(httpCode: Int, code: Int, message: String) {
                popMessage(ToastMessage(message, Toast.LENGTH_SHORT))
            }
        }
        NetworkManager.biliVideoRepository.getPlayStreamDash(item.bvid, item.cid, callback)
    }
}