package cc.kafuu.bilidownload.view.fragment

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.kafuu.bilidownload.common.adapter.HistoryRVAdapter
import cc.kafuu.bilidownload.common.core.CoreFragmentBuilder
import cc.kafuu.bilidownload.common.ext.getSerializableByClass
import cc.kafuu.bilidownload.common.model.TaskStatus
import cc.kafuu.bilidownload.viewmodel.fragment.HistoryViewModel
import com.arialyy.annotations.DownloadGroup
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.task.DownloadGroupTask

class HistoryFragment : RVFragment<HistoryViewModel>(HistoryViewModel::class.java) {
    companion object {
        private const val KEY_STATES = "states"

        class Builder(private val states: List<TaskStatus>) :
            CoreFragmentBuilder<HistoryFragment>() {
            override fun onMallocFragment() = HistoryFragment()
            override fun onPreparationArguments() {
                putArgument(KEY_STATES, states.toTypedArray())
            }
        }

        @JvmStatic
        fun builder(vararg states: TaskStatus) = Builder(states.toList())
    }

    private lateinit var mStates: Array<TaskStatus>
    private val mAdapter: HistoryRVAdapter by lazy {
        HistoryRVAdapter(
            mViewModel, requireContext()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mStates = arguments?.getSerializableByClass<Array<TaskStatus>>(
            KEY_STATES
        ) ?: arrayOf()
        Aria.download(this).register()
    }

    override fun onDestroy() {
        super.onDestroy()
        Aria.download(this).unRegister()
    }

    override fun initViews() {
        super.initViews()
        initViewMode()
        initSmartRefreshLayout()
    }

    private fun initViewMode() {
        mViewModel.initData(mStates.toList())
        mViewModel.latestDownloadTaskLiveData.observe(this) {
            mViewModel.updateList(it.toMutableList())
        }
    }

    private fun initSmartRefreshLayout() {
        setEnableRefresh(false)
        setEnableLoadMore(false)
    }

    override fun getRVAdapter() = mAdapter

    override fun getRVLayoutManager(): RecyclerView.LayoutManager = LinearLayoutManager(context)

    /**
     * 监听下载任务执行进度，并绑定此任务是否存在于列表，若是存在则通知adapter更新*/
    @DownloadGroup.onTaskRunning
    fun handleTaskRunning(task: DownloadGroupTask) {
        // 查找
        val changeIndex = mViewModel.latestDownloadTaskLiveData.value?.indexOfFirst {
            it.downloadTask.groupId == task.entity.id
        }
        // 不存在在当前列表中
        if (changeIndex == null || changeIndex == -1) {
            return
        }
        // 通知更新
        mAdapter.notifyItemChanged(changeIndex)
    }

}