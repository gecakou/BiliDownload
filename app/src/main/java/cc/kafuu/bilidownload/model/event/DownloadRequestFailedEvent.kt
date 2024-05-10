package cc.kafuu.bilidownload.model.event

import cc.kafuu.bilidownload.common.room.entity.DownloadTaskEntity

class DownloadRequestFailedEvent(
    entity: DownloadTaskEntity,
    val httpCode: Int,
    val code: Int,
    val message: String
): DownloadTaskEvent(entity)