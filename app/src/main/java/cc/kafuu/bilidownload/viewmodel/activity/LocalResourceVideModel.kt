package cc.kafuu.bilidownload.viewmodel.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.MutableLiveData
import cc.kafuu.bilidownload.R
import cc.kafuu.bilidownload.common.CommonLibs
import cc.kafuu.bilidownload.common.core.CoreViewModel
import cc.kafuu.bilidownload.common.ext.liveData
import cc.kafuu.bilidownload.common.model.IAsyncCallback
import cc.kafuu.bilidownload.common.model.LoadingStatus
import cc.kafuu.bilidownload.common.model.LocalMediaDetail
import cc.kafuu.bilidownload.common.model.action.popmessage.ToastMessageAction
import cc.kafuu.bilidownload.common.model.av.AVFormat
import cc.kafuu.bilidownload.common.room.dto.DownloadTaskWithVideoDetails
import cc.kafuu.bilidownload.common.room.entity.DownloadResourceEntity
import cc.kafuu.bilidownload.common.room.repository.DownloadRepository
import cc.kafuu.bilidownload.common.utils.FFMpegUtils
import cc.kafuu.bilidownload.common.utils.FileUtils
import cc.kafuu.bilidownload.common.utils.TimeUtils
import cc.kafuu.bilidownload.view.dialog.ConvertDialog
import kotlinx.coroutines.runBlocking
import java.io.File

class LocalResourceVideModel : CoreViewModel() {
    companion object {
        private const val TAG = "LocalResourceVideModel"
    }

    // 此页面加载状态，loading状态将显示加载动画（默认开启）
    private val mLoadingStatusLiveData = MutableLiveData(LoadingStatus.loadingStatus())
    val loadingStatusLiveData = mLoadingStatusLiveData.liveData()

    // 此资源隶属的任务详情
    private val mTaskDetailLiveData = MutableLiveData<DownloadTaskWithVideoDetails>()
    val taskDetailLiveData = mTaskDetailLiveData.liveData()

    // 下载的资源实体
    private val mResourceLiveData = MutableLiveData<DownloadResourceEntity>()
    val resourceLiveData = mResourceLiveData.liveData()

    // 此资源文件信息
    private val mLocalMediaDetailLiveData = MutableLiveData<LocalMediaDetail>()
    val localMediaDetailLiveData = mLocalMediaDetailLiveData.liveData()

    // 是否正在导出
    private val mIsExportingLiveData = MutableLiveData(false)
    val isExportingLiveData = mIsExportingLiveData.liveData()

    // 是否正在转换
    private val mIsCoveringLiveData = MutableLiveData(false)
    val isCoveringLiveData = mIsCoveringLiveData.liveData()

    // 转换格式线程
    private var mConvertThread: Thread? = null

    fun updateResourceEntity(resource: DownloadResourceEntity) {
        mResourceLiveData.value = resource
        doLoadResourceDetails(resource)
    }

    fun updateTaskDetails(details: DownloadTaskWithVideoDetails) {
        mTaskDetailLiveData.value = details
    }

    /**
     * @brief 通过FFMpeg加载此资源的详细详细，包括音视频封装格式与流编码
     */
    private fun doLoadResourceDetails(resource: DownloadResourceEntity) {
        object : IAsyncCallback<LocalMediaDetail, Exception> {
            override fun onSuccess(data: LocalMediaDetail) {
                Log.d(TAG, "onSuccess: $data")
                mLocalMediaDetailLiveData.postValue(data)
            }

            override fun onFailure(exception: Exception) {
                exception.printStackTrace()
                mLoadingStatusLiveData.postValue(
                    LoadingStatus.errorStatus(message = exception.message ?: "Unknown exception")
                )
            }
        }.also { FFMpegUtils.getMediaInfo(resource.file, it) }
    }

    /**
     * @brief 校验数据是否已经全部加载完成，若已全部加载完成则设置加载状态为完成状态
     */
    fun checkLoaded() {
        if (
            mTaskDetailLiveData.value != null &&
            mResourceLiveData.value != null &&
            mLocalMediaDetailLiveData.value != null
        ) {
            mLoadingStatusLiveData.postValue(LoadingStatus.doneStatus())
        }
    }

    /**
     * @brief 尝试询问用户如何分析此资源
     */
    fun tryShareResource(context: Context) {
        val taskDetail = mTaskDetailLiveData.value ?: return
        val resource = mResourceLiveData.value ?: return
        FileUtils.tryShareFile(context, taskDetail.title, File(resource.file), resource.mimeType)
    }

    /**
     * @brief 尝试询问用户要将此资源导出到何处
     */
    fun tryExportResource(createDocumentLauncher: ActivityResultLauncher<Intent>) {
        if (mIsExportingLiveData.value == true) return
        val resource = mResourceLiveData.value ?: return
        val file = File(resource.file)
        FileUtils.tryExportFile(file, resource.mimeType, createDocumentLauncher)
    }

    /**
     * @brief 尝试询问用户转换此资源为何种格式
     */
    fun tryCovertResource() {
        val details = mLocalMediaDetailLiveData.value ?: return
        val resource = resourceLiveData.value ?: return
        val format = details.getAVFormatOrNull() ?: return
        val audioCodec = details.getAudioAVCodecOrNull()
        val videoCodec = details.getVideoAVCodecOrNull()
        popDialog(
            ConvertDialog.buildDialog(resource.name, format, audioCodec, videoCodec),
            success = {
                if (mConvertThread != null) return@popDialog
                mIsCoveringLiveData.value = true
                mConvertThread = Thread {
                    if (doCovertResource(it as ConvertDialog.Companion.Result)) {
                        return@Thread
                    }
                    popMessage(
                        ToastMessageAction(CommonLibs.getString(R.string.convert_resource_failed_message))
                    )
                }
                mConvertThread?.start()
            }
        )
    }

    /**
     * @brief 导出资源
     */
    fun exportResource(uri: Uri) {
        val taskDetail = mTaskDetailLiveData.value ?: return
        val resource = mResourceLiveData.value ?: return
        val sourceFile = File(resource.file)
        mIsExportingLiveData.postValue(true)
        if (!FileUtils.writeFileToUri(CommonLibs.requireContext(), uri, sourceFile)) {
            popMessage(
                ToastMessageAction(CommonLibs.getString(R.string.export_resource_failed_message))
            )
        } else {
            popMessage(
                ToastMessageAction(
                    CommonLibs.getString(
                        R.string.export_resource_success_message,
                        taskDetail.title
                    ),
                )
            )
        }
        mIsExportingLiveData.postValue(false)
    }

    /**
     * @brief 删除此资源
     */
    suspend fun doDeleteResource() {
        val resource = mResourceLiveData.value ?: return
        val file = File(resource.file)
        if (file.exists() && !file.delete()) {
            popMessage(
                ToastMessageAction(CommonLibs.getString(R.string.delete_resource_failed_message))
            )
            return
        }
        DownloadRepository.deleteResourceById(resource.id)
        finishActivity()
    }

    /**
     * @brief 尝试转换音视频封装格式或音视频流编码
     */
    private fun doCovertResource(result: ConvertDialog.Companion.Result): Boolean {
        val details = mLocalMediaDetailLiveData.value ?: return false
        val resource = resourceLiveData.value ?: return false

        val sourceAudioCodec = details.getAudioAVCodecOrNull()
        val sourceVideoCodec = details.getVideoAVCodecOrNull()

        val targetName =
            "convert-${resource.taskId}-${System.currentTimeMillis()}.${result.format.suffix}"
        val covertCacheFile = File(CommonLibs.requireConvertTemporaryDir(), targetName)

        val isSuccess = FFMpegUtils.convertMedia(
            resource.file, covertCacheFile.absolutePath,
            sourceVideoCodec?.let { it to (result.videoCodec ?: return false) },
            sourceAudioCodec?.let { it to (result.audioCodec ?: return false) }
        )

        if (isSuccess) {
            onCovertFinish(targetName, covertCacheFile, result.format)
        }

        covertCacheFile.delete()

        mIsCoveringLiveData.postValue(false)
        mConvertThread = null

        return isSuccess
    }


    /**
     * @brief 转换执行完成
     */
    private fun onCovertFinish(
        targetName: String,
        covertCacheFile: File,
        targetFormat: AVFormat,
    ) {
        val resource = mResourceLiveData.value ?: return
        val targetFile = File(CommonLibs.requireResourcesDir(), targetName)
        // 将临时文件移动到资源目录
        if (!covertCacheFile.renameTo(targetFile)) {
            popMessage(
                ToastMessageAction(CommonLibs.getString(R.string.convert_resource_failed_message))
            )
            return
        }
        // 登记资源
        runBlocking {
            DownloadRepository.registerResource(
                resource.taskId,
                "Convert-${TimeUtils.formatTimestamp(System.currentTimeMillis())}",
                resource.type,
                targetFile.absoluteFile,
                targetFormat.mimeType
            )
        }
        popMessage(
            ToastMessageAction(CommonLibs.getString(R.string.convert_resource_success_message))
        )
    }

    /**
     * @brief 捕获activity退出事件，判断当前是否有任务正在执行，如果有则阻止退出
     */
    override fun finishActivity(activityResult: ActivityResult?) {
        if (mIsExportingLiveData.value == true || mIsCoveringLiveData.value == true) {
            popMessage(
                ToastMessageAction(CommonLibs.getString(R.string.resource_have_mission_progress))
            )
            return
        }
        super.finishActivity(activityResult)
    }

}