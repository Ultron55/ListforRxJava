package com.example.listforrxjava

import android.util.Log
import androidx.lifecycle.ViewModel
import io.reactivex.subjects.PublishSubject

class ViewModelServerOperations() : ViewModel()
{
    val PORTIONCOUNT : Int = 20
    var server : Server = Server(PORTIONCOUNT)
    private var contentArrList : ArrayList<Joke> = ArrayList() //контент
    var selectlist : ArrayList<Int> = ArrayList() //индексы выделенных
    @Transient public val ReloadDone : PublishSubject<ArrayList<Joke>> =
        PublishSubject.create()
    @Transient public val LoadMoreDone : PublishSubject<ArrayList<Joke>> =
        PublishSubject.create()
    var isloadingmore = false
    var isRefreshing = false
    val loaderval : Joke = Joke(-1, "loader")

    public fun removeLoader()
    {
        Log.v("lload", "remove")
        var b = false
        while (!b)
        {
            for (i in 0 until contentArrList.size)
            {
                if (contentArrList[i].getId() == loaderval.getId())
                {
                    contentArrList.removeAt(i)
                    break
                }
            }
            b = true
        }
    }

    public fun getContentArrList() : ArrayList<Joke>
    {
        return contentArrList
    }

    public fun AddLoader(j : Joke)
    {
        contentArrList.add(j)
    }

    public fun isContentEmplty() : Int
    {
        return contentArrList.size
    }

    public fun ReloadContent()
    {
        Thread {
            while (isloadingmore) Thread.sleep(30)
            val newcontentarrlist : ArrayList<Joke> = ArrayList()
            server.Reload(PORTIONCOUNT)
            var b = false
            val s = server.ContentStream.subscribe { arrlist->
                b = true
                newcontentarrlist.addAll(arrlist)
            }
            while (!b)
            {
                Thread.sleep(20)
            }
            s.dispose()
            if (newcontentarrlist.isEmpty())
            {
                ReloadDone.onNext(ArrayList())
            }
            else
            {
                contentArrList.clear()
                contentArrList.addAll(newcontentarrlist)
                ReloadDone.onNext(newcontentarrlist)
            }
            Log.v("loadobserv", "observable" + newcontentarrlist.size.toString())
        }.start()
    }


    public fun LoadMoreContent()
    {
        isloadingmore = true
        Thread {
            val newcontentarrlist = server.getMoreContentArrayList(PORTIONCOUNT)
            Log.v("loadobservn", "servloaded")
            Log.v("loadobservn", "contmodelbefore" + contentArrList.size.toString())
            Log.v("loadobservn", "0: " +
                    contentArrList[0].getId().toString() + contentArrList[0].getJokeText())
            Log.v("loadobservn", "last: " +
                    contentArrList[contentArrList.lastIndex].getId().toString() +
                    contentArrList[contentArrList.lastIndex].getJokeText())
            if (newcontentarrlist.isEmpty())
            {
                LoadMoreDone.onNext(ArrayList())
                Log.v("loadobservn", "noloadd")
            }
            else
            {
                for (j in newcontentarrlist)
                {
                    contentArrList.add(j)
                }
                LoadMoreDone.onNext(newcontentarrlist)
                Log.v("loadobservn", "loadd")
            }
            Log.v("loadobservn", "contmodel" + contentArrList.size.toString())
            Log.v("loadobservn", "newconmodel" + newcontentarrlist.size.toString())
        }.start()
    }
}
