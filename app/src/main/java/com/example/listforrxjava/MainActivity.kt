package com.example.listforrxjava

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.recyclerview_item.view.*
import java.io.*
import java.nio.file.Files.walk


class MainActivity : InfiniteScrollListener.OnLoadMoreListener, AppCompatActivity ()  {

    var recyclerView : RecyclerView? = null
    private val portioncount = 20 // количество контента в порции
    lateinit var server : Server
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
        issavedinstancestate = savedInstanceState != null
        recyclerView = findViewById(R.id.recyclerview)
        val manager = LinearLayoutManager(this)
        infiniteScrollListener = InfiniteScrollListener(manager, this)
        infiniteScrollListener.loading = false
        recyclerView!!.layoutManager = manager
        recyclerView!!.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
        val selectcolor = ContextCompat.getColor(applicationContext, R.color.select)
        val textbackcolor = ContextCompat.getColor(applicationContext, R.color.textback)
        setCacheFile()
        Restore(savedInstanceState)
        recyclerViewAdapter = RecyclerViewAdapter(contentArrList, textbackcolor, selectcolor)
        recyclerView!!.adapter = recyclerViewAdapter
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
        swipeRefreshLayout.setOnRefreshListener {
            Log.v("swipee", ::server.isInitialized.toString())
            selectlist.clear()
            Log.v("swipee", Thread.currentThread().name)
            LoadObservable(false)
        }
        Thread {swiperefreshlayout.post { swiperefreshlayout.isRefreshing = true }}.start()
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
            cachefile = File.createTempFile("cachejokesarrlist",
                null, applicationContext.cacheDir)
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        Log.v("onsavedata", "adapter")
        savedInstanceState.putSerializable("adapter", recyclerViewAdapter)
        Log.v("onsavedata", "server")
        savedInstanceState.putSerializable("server", server)
        Log.v("onsavedata", "select")
        savedInstanceState.putIntegerArrayList("selectlist", selectlist)
        try
        {
            //cache
            ObjectOutputStream(FileOutputStream(cachefile)).use { oos ->
                Log.v("filecachesave", "cache" + contentArrList.size.toString())
                oos.writeObject(contentArrList)
                Log.v("filecachesave", "cache")
            }
        }
        catch (ex: IOException)
        {
            Log.v("filecachesaveerror", ex.message.toString())
        }
        super.onSaveInstanceState(savedInstanceState)
    }

    fun Restore(savedInstanceState: Bundle?)
    {
        if (savedInstanceState != null)
        {
            //для поворотов экрана
            Log.v("filemapopen", "bundle")
            recyclerViewAdapter = savedInstanceState.getSerializable("adapter") as RecyclerViewAdapter
            selectlist  =  savedInstanceState.getIntegerArrayList("selectlist") as ArrayList<Int>
            server =  savedInstanceState.getSerializable("server") as Server
            contentArrList = recyclerViewAdapter.getValues()

        }
        else if (cachefile.exists() && cachefile.length() > 200)
        {
            //для кэша при открытии
            server = Server(portioncount)
            try
            {
                swiperefreshlayout.isRefreshing = true
                Log.v("filemapopen", "readcache " + contentArrList.toString())
                ObjectInputStream(FileInputStream(cachefile)).use { ois ->
                    contentArrList = ois.readObject() as ArrayList<Joke>
                    Log.v("filemapopen", "contsize " + contentArrList.toString())
                }
                LoadObservable(false)
            }
            catch (ex: IOException)
            {
                Log.v("filemapopenerror", "error " + ex.message.toString())
            }
        }
        else
        {
            // с чистого листв
//            swiperefreshlayout.isRefreshing = true
            server = Server(portioncount)
            Log.v("elsse", "elsee")
            Log.v("elsse", Thread.currentThread().name)
            contentArrList.add(Joke(-1, "CHUCKING JOKES LOADING"))
            LoadObservable(false)
        }
    }

    private fun LoadObservable(isnextcontent : Boolean)
    {
        Thread {
            var newcontentarrlist : ArrayList<Joke> = ArrayList()
            val observer: Observer<ArrayList<Joke>>
            if (isnextcontent)
            {
                newcontentarrlist = server.getNextContentArrayList(portioncount)
                observer = LoadMoreObserver()
            }
            else
            {
                server.LoadNew(portioncount)
                var b : Boolean = false
                val s = server.ContentStream.subscribe { v->
                    b = true
                    newcontentarrlist.clear()
                    for (j in v)
                    {
                        newcontentarrlist.add(j)
                    }
                }
                while (!b)
                {
                    Thread.sleep(20)
                }
                s.dispose()
                observer = LoadALLObserver()
            }
            Log.v("loadobserv", "observable" + newcontentarrlist.size.toString())
            Observable.just(newcontentarrlist)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer)
        }.start()

    }

    private fun LoadALLObserver(): Observer<ArrayList<Joke>> {
        val observer : Observer<ArrayList<Joke>> = object :Observer<ArrayList<Joke>> {
            override fun onNext(newcontentarrlist: ArrayList<Joke>) {
                Log.v("swipee", "observer")
                Log.v("loaders", "loaderall")
                Log.v("swipee", "ns" + newcontentarrlist.size.toString())
                Log.v("swipee", "co" + contentArrList.toString())
                if (newcontentarrlist.isNotEmpty())
                {
                    recyclerViewAdapter.setAllItem()
                    contentArrList.clear()
                    for (j in newcontentarrlist)
                    {
                        contentArrList.add(j)
                    }
                    Log.v("swipee", contentArrList.size.toString())
                    Toast.makeText(applicationContext, "Data uploaded", Toast.LENGTH_SHORT).show()
                }
                else
                {
                    Toast.makeText(applicationContext, "Data don't uploaded", Toast.LENGTH_SHORT).show()
                }
                swiperefreshlayout.isRefreshing = false
            }
            override fun onSubscribe(s: Disposable) {}
            override fun onError(e: Throwable) {Log.v("lloadmore", e.message.toString())}
            override fun onComplete() {}
        }
        return observer
    }

    private fun LoadMoreObserver(): Observer<ArrayList<Joke>> {
        val observer : Observer<ArrayList<Joke>> = object :Observer<ArrayList<Joke>> {
            override fun onNext(newcontentarrlist: ArrayList<Joke>) {
                Log.v("lloadmore", newcontentarrlist.size.toString())
                Log.v("loaders", "loadermore")
                Log.v("lloadmore", "onload4" + Thread.currentThread().name)
                Log.v("lloadmore", "A"  + contentArrList.size.toString())
                recyclerViewAdapter.removeLoader()
                if (newcontentarrlist.isNotEmpty())
                {
                    for (i in 0 until portioncount)
                    {
                        contentArrList.add(newcontentarrlist[i])
                    }
                }
                infiniteScrollListener.loading = false
                Log.v("lloadmore", "A"  + contentArrList.size.toString())
                Log.v("lloadmore", "onload4" + recyclerViewAdapter.itemCount.toString())
            }
            override fun onComplete() {}
            override fun onError(e: Throwable) {Log.v("lloadmore", e.message.toString())}
            override fun onSubscribe(s: Disposable) {}
        }
        return observer
    }

    override fun onLoadMore() {
        Log.v("lloadmore", "onload")
        if (server.getCountContent() == contentArrList.size)
        {
            Log.v("lloadmore", server.getCountContent().toString())
            return
        }
        recyclerViewAdapter.addLoader()
        LoadObservable(true)
        Log.v("lloadmore", "onload2")
    }

    fun SelectItem(v: View)
    {
        val tag = v.tag!! as Int //view.tag == joke.id
        Log.v("tagg", tag.toString())
        Log.v("selectt", Thread.currentThread().name)
        when (tag)
        {
            in selectlist ->
            {
                recyclerViewAdapter.UnselectItem(tag)
                Log.v("setselist", "del $tag")
                Log.v("setselist", "del " + recyclerView!!.indexOfChild(v).toString())
                selectlist.remove(tag)
            }
            !in selectlist ->
            {
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

    public fun Deleteitem(v : View)
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
        server.DeleteContent(backupcontentArrList) //удаление с сервера
        backupcontentArrList.clear() // освобождаю память
        errorDeleteSubscribe = server.errorDelete.subscribe {
                arr ->
            for (k in 0 until arr.size)
            {
                //восстанавливает элементы если при удалении на сервере произошла ошибка
                contentArrList.add(arr[k])
                Log.v("deleteitema", "ca.size" + contentArrList.size.toString())
            }
            errorDeleteSubscribe.dispose()
        }
        // не слишком ли мало осталось
        val manager = LinearLayoutManager(this)
        if (manager.findLastVisibleItemPosition() == manager.itemCount - 1)
        {
            infiniteScrollListener.loading = true
            onLoadMore()
        }
    }

}