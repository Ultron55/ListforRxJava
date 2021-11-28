package com.example.listforrxjava

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.recyclerview_item.view.*
import java.io.*


class MainActivity : InfiniteScrollListener.OnLoadMoreListener, AppCompatActivity ()  {

    var recyclerView : RecyclerView? = null
    //private val PORTIONCOUNT = 20 // количество контента в порции
    val viewModelServerOperations : ViewModelServerOperations by viewModels()
    lateinit var LoadMoreSubscribe : Disposable
    lateinit var infiniteScrollListener : InfiniteScrollListener //для ловли прокрутки
    lateinit var recyclerViewAdapter: RecyclerViewAdapter
    private var contentArrList : ArrayList<Joke> = ArrayList() //контент
    var selectlist : ArrayList<Int> = ArrayList() //индексы выделенных
    lateinit var longClickSubscribe : Disposable
    lateinit var errorDeleteSubscribe : Disposable
    lateinit var cachefile : File
    var issavedinstancestate = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.v("OnCreate", "start")
//        MobileAds.initialize(this) {}
        issavedinstancestate = savedInstanceState != null
        recyclerView = findViewById(R.id.recyclerview)
        val manager = LinearLayoutManager(this)
        swiperefreshlayout.isRefreshing = viewModelServerOperations.isRefreshing
        infiniteScrollListener = InfiniteScrollListener(manager, this)
        infiniteScrollListener.loading = viewModelServerOperations.isloadingmore
        recyclerView!!.layoutManager = manager
        recyclerView!!.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
        val selectcolor = ContextCompat.getColor(applicationContext, R.color.select)
        val textbackcolor = ContextCompat.getColor(applicationContext, R.color.textback)
        setCacheFile()
        var b = false
        val n = viewModelServerOperations.isContentEmplty()
        Log.v("viewmodel", "n$n")
        if (n == 0)
        {
            viewModelServerOperations.ReloadContent()
            val s =
                viewModelServerOperations.ReloadDone.subscribe { newcontentarrlist ->
                    b = true
                    contentArrList = viewModelServerOperations.getContentArrList()
                }
            while (!b)
            {
                Thread.sleep(20)
                Log.v("ViewModelget", b.toString())
            }
            s.dispose()
            Log.v("ViewModelget", "no")
        }
        else
        {
            Log.v("ViewModelget", "yes")
            contentArrList = viewModelServerOperations.getContentArrList()
        }
        recyclerViewAdapter = RecyclerViewAdapter(contentArrList, textbackcolor, selectcolor)
        recyclerView!!.adapter = recyclerViewAdapter
        var l = contentArrList.indexOf(recyclerViewAdapter.loaderval)
        Log.v("loadobservnloader", l.toString())
        recyclerView!!.addOnScrollListener(infiniteScrollListener)
        if (issavedinstancestate)
        {
            if (selectlist.isNotEmpty())
            {
                for (tag in selectlist)
                {
                    recyclerViewAdapter.SelectItem(tag)
                }
                floatingdeletebtn.show()
            }
        }
        longClickSubscribe  = recyclerViewAdapter.itemClickStream.subscribe { v-> SelectItem(v)}
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swiperefreshlayout)
        swipeRefreshLayout.setOnRefreshListener {SwipeReload()}
        LoadMore()
        l = recyclerViewAdapter.getValues().indexOf(recyclerViewAdapter.loaderval)
        Log.v("loadobservnloaderr", l.toString())
    }

    fun SwipeReload()
    {
        viewModelServerOperations.isRefreshing = true
        selectlist.clear()
        Log.v("swipee", Thread.currentThread().name)
        viewModelServerOperations.ReloadContent()
        var b = false
        val s = viewModelServerOperations.ReloadDone.observeOn(AndroidSchedulers.mainThread())
            .subscribe({ newcontentarrlist ->
                if (newcontentarrlist.isEmpty())
                {
                    Toast.makeText(
                        applicationContext,
                        "Data don't uploaded", Toast.LENGTH_SHORT
                    ).show()
                }
                else
                {
                    recyclerView!!.scrollToPosition(0)
                    recyclerViewAdapter.setAllItem()
                    contentArrList.clear()
                    contentArrList.addAll(newcontentarrlist)
                }
                b = true
                viewModelServerOperations.isRefreshing = false
                swiperefreshlayout.isRefreshing = false
            })
            {throwable -> Log.v("reloaddone", throwable.message.toString())}
        Thread {
            while (!b) Thread.sleep(20)
            s.dispose()
        }.start()
    }

    override fun onLoadMore() {
        if (viewModelServerOperations.isRefreshing)
        {
            infiniteScrollListener.loading = false
            viewModelServerOperations.isloadingmore = false
            Log.v("loadobservn", "nooload")
            return
        }
        infiniteScrollListener.loading = true
        Log.v("lloadmore", viewModelServerOperations.isContentEmplty().toString())
        Log.v("lloadmore", contentArrList.toString())
        Log.v("lloadmore", "onload")
        recyclerView!!.post {recyclerViewAdapter.addLoader()}
        viewModelServerOperations.LoadMoreContent()
    }

    fun LoadMore()
    {
        LoadMoreSubscribe = viewModelServerOperations.LoadMoreDone.observeOn(AndroidSchedulers.mainThread())
            .subscribe({ newcontentarrlist ->
                if (!viewModelServerOperations.isloadingmore) return@subscribe
                if (newcontentarrlist.isEmpty())
                {
                    Toast.makeText(
                        applicationContext,
                        "Data don't uploaded", Toast.LENGTH_SHORT
                    ).show()
                }
                else
                {
                    contentArrList.addAll(newcontentarrlist)
                }
                recyclerViewAdapter.removeLoader()
                Log.v("loadobserv", "recyclecoutn${recyclerViewAdapter.itemCount}")
                Log.v("loadobserv", Thread.currentThread().name)
                infiniteScrollListener.loading = false
                viewModelServerOperations.isloadingmore = false
            })
            {throwable -> Log.v("reloaddone", throwable.message.toString())}
        Log.v("lloadmore", "onload2")
    }

    override fun onStart() {
        super.onStart()
        if (issavedinstancestate)
        {
            Thread {
                Thread.sleep(200)
                while (swiperefreshlayout.isRefreshing) swiperefreshlayout.isRefreshing = false
            }.start()
        }
    }

    override fun onDestroy() {
        if (::longClickSubscribe.isInitialized) longClickSubscribe.dispose()
        if (::errorDeleteSubscribe.isInitialized) errorDeleteSubscribe.dispose()
        if (::LoadMoreSubscribe.isInitialized) LoadMoreSubscribe.dispose()
        super.onDestroy()
    }

    fun setCacheFile()
    {
        val s = File(cacheDir.path).listFiles()
        if (s!!.isNotEmpty())
        {
            Log.v("filem", "cacheexist")
            cachefile = s[0]
        }
        else
        {
            Log.v("filem", "s.cachenull")
            cachefile = File.createTempFile(
                "cachejokesnewcontentarrlist",
                null, applicationContext.cacheDir
            )
        }
    }

    fun SelectItem(v: View)
    {
        val tag = v.tag!! as Int //view.tag == joke.id
        Log.v("tagg", tag.toString())
        Log.v("selectt", Thread.currentThread().name)
        when (tag)
        {
            in selectlist -> {
                recyclerViewAdapter.UnselectItem(tag)
                Log.v("setselist", "del $tag")
                Log.v("setselist", "del " + recyclerView!!.indexOfChild(v).toString())
                selectlist.remove(tag)
            }
            !in selectlist -> {
                recyclerViewAdapter.SelectItem(tag)
                Log.v("setselist", "add $tag")
                selectlist.add(tag)
            }
        }
        if (selectlist.isEmpty())
        {
            findViewById<FloatingActionButton>(R.id.floatingdeletebtn).hide()
        }
        else
        {
            findViewById<FloatingActionButton>(R.id.floatingdeletebtn).show()
        }
    }

    public fun Deleteitem(v: View)
    {
        // Для удаления
        Log.v("onclickl", "delete")
        var index : Int = 0
        val backupcontentArrList : ArrayList<Joke> = ArrayList() //бэкап
        Log.v("deleteitema", "BS" + backupcontentArrList.size.toString())
        for (i in 0 until selectlist.size)
        {
            Log.v("deleteitema", i.toString())
            Log.v("deleteitema", selectlist[i].toString())
            // поиск элемента по id
            for (j in 0 until contentArrList.size)
            {
                if (contentArrList[j].getId() == selectlist[i])
                {
                    index = j
                    break
                }
            }
            backupcontentArrList.add(contentArrList[index])
            recyclerViewAdapter.removeItem(index)
            contentArrList.removeAt(index)
        }
        Log.v("deleteitema", "BS" + backupcontentArrList.size.toString())
        selectlist.clear()
        floatingdeletebtn.hide()
        //server.DeleteContent(backupcontentArrList) //удаление с сервера
        backupcontentArrList.clear() // освобождаю память
        /*errorDeleteSubscribe = server.errorDelete.subscribe {
                arr ->
            for (k in 0 until arr.size)
            {
                //восстанавливает элементы если при удалении на сервере произошла ошибка
                contentArrList.add(arr[k])
                Log.v("deleteitema", "ca.size" + contentArrList.size.toString())
            }
            errorDeleteSubscribe.dispose()
        }*/
        // не слишком ли мало осталось
        val manager = LinearLayoutManager(this)
        if (manager.findLastVisibleItemPosition() == manager.itemCount - 1)
        {
            infiniteScrollListener.loading = true
            onLoadMore()
        }
    }

}