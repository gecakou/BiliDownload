package cc.kafuu.bilidownload.common.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cc.kafuu.bilidownload.common.data.entity.DownloadTaskEntity

@Dao
interface DownloadTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(downloadTask: DownloadTaskEntity): Long

    @Update
    suspend fun update(downloadTask: DownloadTaskEntity)

    @Delete
    suspend fun delete(downloadTask: DownloadTaskEntity)

    @Query("SELECT * FROM DownloadTaskEntity")
    suspend fun getAllDownloadTask(): List<DownloadTaskEntity>

    @Query("SELECT * FROM DownloadTaskEntity WHERE status IN (:statuses) ORDER BY id DESC LIMIT :limit")
    suspend fun getLatestDownloadTasks(limit: Long, vararg statuses: Int): List<DownloadTaskEntity>

    @Query("SELECT * FROM DownloadTaskEntity WHERE id < :lastId AND status IN (:statuses) ORDER BY id DESC LIMIT :limit")
    suspend fun getDownloadTasksPagedAfter(limit: Long, lastId: Long, vararg statuses: Int): List<DownloadTaskEntity>

    @Query("SELECT * FROM DownloadTaskEntity WHERE id = :id")
    suspend fun getDownloadTaskById(id: Long): DownloadTaskEntity?

    @Query("SELECT * FROM DownloadTaskEntity WHERE downloadTaskId = :downloadTaskId")
    suspend fun getDownloadTaskByDownloadTaskId(downloadTaskId: Long): DownloadTaskEntity?

    @Query("DELETE FROM DownloadTaskEntity")
    suspend fun deleteAll()
}