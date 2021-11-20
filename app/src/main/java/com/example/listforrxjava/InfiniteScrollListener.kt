package com.example.listforrxjava

import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class InfiniteScrollListener (val linearLayoutManager: LinearLayoutManager,
        val listener: OnLoadMoreListener?) : RecyclerView.OnScrollListener()
{
    //класс для перехвата скролла
    private val VISIBLE_THRESHOLD = 2
    var loading = false

    public override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int)
    {
        super.onScrolled(recyclerView, dx, dy)
        if (dx == 0 && dy == 0) return
        val totalItemCount = linearLayoutManager.itemCount
        val lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition() + VISIBLE_THRESHOLD
        Log.v("swipeee", lastVisibleItem.toString())
        Log.v("swipeee", loading.toString())
        Log.v("swipeee", totalItemCount.toString())
        if (!loading && totalItemCount <= lastVisibleItem && totalItemCount != 0)
        {
            //последний элемент - догружаем
            Log.v("swipeee", "lllllload")
            listener?.onLoadMore()
        }
    }

    public interface OnLoadMoreListener {fun onLoadMore()}
}