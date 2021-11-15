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

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView = findViewById(R.id.recyclerview)
        Log.v("cylce", "ser")
        Log.v("cycle", "main")
        val manager : LinearLayoutManager = LinearLayoutManager(this)
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
        longClickSubscribe  = recyclerViewAdapter.itemClickStream.subscribe { v-> SelectItem(v)}
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swiperefreshlayout)
        swipeRefreshLayout.setOnRefreshListener {
            Log.v("swipee", ::server.isInitialized.toString())
            selectlist.clear()
            Log.v("elsse", Thread.currentThread().name)
            LoadObservable(false)
        }
    }

    override fun onStart() {
        super.onStart()
        Thread {
            Thread.sleep(200)
            while (swiperefreshlayout.isRefreshing) swiperefreshlayout.isRefreshing = false
        }.start()
    }

    override fun onDestroy() {
        if (::longClickSubscribe.isInitialized) longClickSubscribe.dispose()
        if (::errorDeleteSubscribe.isInitialized) errorDeleteSubscribe.dispose()
        super.onDestroy()
    }

    fun setCacheFile()
    {
        val s : File? = File(cacheDir.path).walk(FileWalkDirection.BOTTOM_UP)
            .sortedBy{ it.isDirectory }.elementAt(0)
        if (s != null)
        {
            cachefile = s
        }
        else
        {
            cachefile = File.createTempFile("saveInstancestatecache",
                null, applicationContext.cacheDir)
        }
        File(cacheDir.path).walk(FileWalkDirection.BOTTOM_UP)
            .sortedBy{ it.isDirectory }.forEach {
                if (!it.equals(cachefile)) it.delete()
            }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {

        Log.v("filecc", "adapter")
        savedInstanceState.putSerializable("adapter", recyclerViewAdapter)
        Log.v("filecc", "server")
        savedInstanceState.putSerializable("server", server)
        Log.v("filecc", "select")
        savedInstanceState.putIntegerArrayList("selectlist", selectlist)
        try
        {
            //cache
            ObjectOutputStream(FileOutputStream(cachefile)).use { oos ->
                oos.writeObject(contentArrList)
                Log.v("filecc", "cache")
            }
        }
        catch (ex: IOException)
        {
            Log.v("CreateDataFile", ex.message.toString())
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
            server = Server(filesDir, portioncount)
            try
            {
                swiperefreshlayout.isRefreshing = true
                ObjectInputStream(FileInputStream(cachefile)).use { ois ->
                    contentArrList = ois.readObject() as ArrayList<Joke>
                    Log.v("filemapopen", "recycl " + contentArrList.toString())
                }
                LoadObservable(false)
            }
            catch (ex: IOException)
            {
                Log.v("filemapopen", "cache" + ex.message.toString())
            }
        }
        else
        {
            // с чистого листв
            swiperefreshlayout.isRefreshing = true
            server = Server(filesDir, portioncount)
            Log.v("elsse", "elsee")
            Log.v("elsse", Thread.currentThread().name)
            LoadObservable(false)
        }
    }

    fun SelectItem(v: View)
    {
        Log.v("selectcol", "selectItem")
        val selectcolor = resources.getColor(R.color.select)
        val basecolor = resources.getColor(R.color.textback)
        val tag = v.tag!! as Int //view.tag == joke.id
        Log.v("tagg", tag.toString())
        Log.v("selectcol", v.textview.text.toString())
        Log.v("selectt", Thread.currentThread().name)
        when (tag)
        {
            in selectlist ->
            {
                recyclerViewAdapter.UnselectItem(tag)
                Log.v("delete", tag.toString())
                Log.v("delete", recyclerView!!.indexOfChild(v).toString())
                selectlist.remove(tag)
            }
            !in selectlist ->
            {
                recyclerViewAdapter.SelectItem(tag)
                Log.v("delete", tag.toString())
                selectlist.add(tag)
            }
        }
        if (selectlist.size == 0)
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
                    break;
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
                Log.v("setalll", "observable" + newcontentarrlist.size.toString())
                observer = LoadALLObserver()
            }
            Log.v("setalll", "observable")
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
                recyclerViewAdapter.setAllItem()
                contentArrList.clear()
                for (j in newcontentarrlist)
                {
                    contentArrList.add(j)
                }
                Log.v("swipee", contentArrList.size.toString())
                Log.v("swipee", "cos")
                swiperefreshlayout.isRefreshing = false
                Toast.makeText(applicationContext, "Data uploaded", Toast.LENGTH_SHORT).show()
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
                infiniteScrollListener.loading = false
                Log.v("lloadmore", "A"  + contentArrList.size.toString())
                recyclerViewAdapter.removeLoader()
                for (i in 0 until portioncount)
                {
                    contentArrList.add(newcontentarrlist[i])
                }
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

}