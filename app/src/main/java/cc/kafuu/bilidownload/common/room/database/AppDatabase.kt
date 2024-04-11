package cc.kafuu.bilidownload.common.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cc.kafuu.bilidownload.common.room.dao.BiliVideoDao
import cc.kafuu.bilidownload.common.room.dao.DownloadTaskDao
import cc.kafuu.bilidownload.common.room.dao.ResourceDao
import cc.kafuu.bilidownload.common.room.entity.BiliVideoMainEntity
import cc.kafuu.bilidownload.common.room.entity.BiliVideoPartEntity
import cc.kafuu.bilidownload.common.room.entity.DownloadTaskEntity
import cc.kafuu.bilidownload.common.room.entity.ResourceEntity

@Database(
    entities =
    [
        BiliVideoMainEntity::class,
        BiliVideoPartEntity::class,
        DownloadTaskEntity::class,
        ResourceEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app.db" // 数据库名称
                ).build()
                INSTANCE = instance
                // 返回实例
                instance
            }
        }
    }

    abstract fun downloadTaskDao(): DownloadTaskDao
    abstract fun resourceDao(): ResourceDao
    abstract fun biliVideoDao(): BiliVideoDao
}