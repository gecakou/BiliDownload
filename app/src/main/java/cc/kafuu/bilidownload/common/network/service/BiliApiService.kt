package cc.kafuu.bilidownload.common.network.service
import cc.kafuu.bilidownload.common.network.model.BiliRespond
import cc.kafuu.bilidownload.common.network.model.BiliStreamData
import cc.kafuu.bilidownload.common.network.model.BiliWbiData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface BiliApiService {

    @GET("x/web-interface/nav")
    fun getWbiInterfaceNav(): Call<BiliRespond<BiliWbiData>>

    @GET("x/player/playurl")
    fun getPlayStream(
        @Query("avid") avid: Long?,
        @Query("bvid") bvid: String?,
        @Query("cid") cid: Long,
        @Query("qn") qn: Int? = null,
        @Query("fnval") fnval: Int? = 1,
        @Query("fnver") fnver: Int? = 0,
        @Query("fourk") fourk: Int? = 1,
        @Query("platform") platform: String? = null,
        @Query("high_quality") highQuality: Int? = null
    ): Call<BiliRespond<BiliStreamData>>
}
