package com.example.listforrxjava

import android.util.Log
import io.reactivex.subjects.PublishSubject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.collections.ArrayList
import kotlin.random.Random


class Server(dir: File, var countportioncontent : Int) : Serializable
{
    //вместо сервера с возможностью удалением данных
    private val filecontentdata : File = File(dir, "filecontentdata.bin")//бэкап сервера
    private var contentArrList : ArrayList<Joke> = ArrayList()
    private val countforload : Int = 200 //сколько загружается из настоящего сервера
    private var iscomplete : Boolean = false
    private var starindexnetcontent : Int = 0
    private var actualid : Int = 0
    val messagenullcount : String = "Data null. You remove all content or Internet disconnected"
    @Transient public val ContentStream: PublishSubject<ArrayList<Joke>> = PublishSubject.create()

    //Загрузка в мой сервер из настоящего
    public fun LoadNew(count: Int)
    {
        countportioncontent = count
        starindexnetcontent = 0
        Log.v("loaders", "LoadNew")
        Thread{
            CreateDataFile()
            starindexnetcontent += count
        }.start()
    }

    //загрузка следующей порции из этого сервера
    public fun getNextContentArrayList(count : Int) : ArrayList<Joke>
    {
        countportioncontent = count
        Log.v("loaders", "getcontent")
        Log.v("lloadmore", starindexnetcontent.toString())
        val arrayList : ArrayList<Joke> = ArrayList()
        var endindex = count + starindexnetcontent
        if (endindex > getCountContent()) endindex = getCountContent()
        for (i in starindexnetcontent until endindex)
        {
            Log.v("lloadmore", "start" + starindexnetcontent.toString())
            Log.v("lloadmore", "i" + i.toString())
            Log.v("lloadmore", "count" + getCountContent().toString())
            arrayList.add(contentArrList[i])
        }
        starindexnetcontent += count
        Thread.sleep(2000) //будто грузит
        return arrayList
    }

    fun getCountContent() : Int {return contentArrList.size}

    //загрузка из настоящего сервера
    @Throws(IOException::class)
    private fun getContentFromTrueServer(path: String)
    {
        var reader : BufferedReader? = null
        var stream : InputStream? = null
        var connection : HttpURLConnection? = null
        try
        {
            val url = URL(path)
            contentArrList.clear()
            connection = url.openConnection() as HttpURLConnection
            connection.setRequestMethod("GET")
            connection.setReadTimeout(10000)
            connection.connect()
            stream = connection.getInputStream()
            reader = BufferedReader(InputStreamReader(stream))
            var startindex : Int
            var endindex : Int
            val substr1 = "\"joke\":"
            val substr2 = "\", \"categories\""
            var line: StringBuffer? = StringBuffer(reader.readLine())
            var i : Int = 0
            if (true)
            {
                Log.v("cycle", line.toString())
                while (true)
                {
                    startindex = line!!.indexOf(substr1) + substr1.length
                    if (startindex - substr1.length == -1)
                    {
                        Log.v("cycle", "break")
                        break;
                    }
                    endindex = line!!.indexOf(substr2)
                    var resultstring : String = line!!.substring(startindex, endindex)
                    while (resultstring.indexOf("\\") != -1)
                    {
                        Log.v("cycle", resultstring)
                        resultstring = resultstring.replace("\\", "")
                    }
                    contentArrList.add(Joke(actualid++, resultstring))
                    Log.v("swipee", "load i " + contentArrList.size.toString())

                    line!!.delete(0, endindex + substr2.length)
                    i++
                }
            }
        }
        finally
        {
            Log.v("conee", "finally")
            if (contentArrList.size == 0)
            {
                Log.v("conee", "0s")
                contentArrList.add(Joke(-1, messagenullcount))
            }
            Log.v("conee", getCountContent().toString())
            if (reader != null) {reader.close()}
            if (stream != null) {stream.close()}
            if (connection != null) {connection.disconnect()}
            Log.v("conee", getCountContent().toString())
            returnContentList()
            Log.v("conee", "returnconetnt")
        }
    }

    private fun returnContentList()
    {
        val arr : ArrayList<Joke> = ArrayList()
        var endindex = countportioncontent
        Log.v("lload", "cona size" + contentArrList.size.toString())
        if (endindex > getCountContent()) endindex = getCountContent()
        for (j in starindexnetcontent until endindex)
        {
            arr.add(contentArrList[j])
        }
        Log.v("lload", contentArrList[contentArrList.size - 1].getJokeText())
        Log.v("lload", arr[arr.size - 1].getJokeText())
        ContentStream.onNext(arr) // раздача контента
    }

    private fun CreateDataFile()
    {
        try {
            getContentFromTrueServer("http://api.icndb.com/jokes/random/${countforload.toString()}?escape=javascript")
        }
        catch (e: IOException)
        {
            Log.v("coneer", e.message.toString())
        }
    }


//      решил убрать, но оставил вдруг пригодятся
//    private fun saveDataFile()
//    {
//        try
//        {
//            ObjectOutputStream(FileOutputStream(filecontentdata)).use { oos ->
//                for (i in 0 until contentArrList.size)
//                {
//                    oos.writeObject(contentArrList[i])
//                    Log.v("countsave", i.toString())
//                }
//            }
//        }
//        catch (ex: IOException)
//        {
//            Log.v("CreateDataFile", "sa " + ex.message.toString())
//        }
//    }

//    private fun openDataFile()
//    {
//        try
//        {
//            ObjectInputStream(FileInputStream(filecontentdata)).use { ois ->
//                contentArrList.clear()
//                Log.v("fileo", "o")
//                var o = ois.readObject()
//                if (o == null) Log.v("fileo", "onull")
////                while (true)
////                {
////                    val o  = .toString()
////                    if (o == "null")
////                    {
////                        Log.v("cycle", "nuul")
////                        break;
////                    }
////                    else
////                    {
////                        contentArrList.add(o)
////                        Log.v("cycle", "open")
////                    }
////                }
//            }
//        }
//        catch (ex: IOException)
//        {
//            Log.v("CreateDataFile", "o " + ex.message.toString())
//        }
//        finally
//        {
//            Log.v("cycle", "openh")
//            Log.v("cycle", iscomplete.toString())
//            Log.v("count", getCountContent().toString())
//            if (getCountContent() > 0 && contentArrList[0].getId() != -1)
//            {
////                while (!::observableComplete.isInitialized)
////                {
////                    Thread.sleep(5)
////                    Log.v("cycle", iscomplete.toString())
////                }
////                observableComplete.subscribe(ObserverComplete())
//                returnContentList()
//            }
//            else
//            {
//                CreateDataFile()
//            }
//        }
//    }

    @Transient public val errorDelete: PublishSubject<ArrayList<Joke>> = PublishSubject.create()

    public fun DeleteContent(arr: ArrayList<Joke>)
    {
        Log.v("deleteitema", "BS" + arr.size.toString())
        if (Random.nextInt(0, 1000) == 555) //редкая ошибка на сервере
        {
            errorDelete.onNext(arr)
            return
        }
        contentArrList.removeAll(arr)
    }

}