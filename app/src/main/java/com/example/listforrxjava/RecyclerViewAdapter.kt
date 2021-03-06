package com.example.listforrxjava

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.subjects.PublishSubject
import java.io.Serializable

class RecyclerViewAdapter(
    private val values: ArrayList<Joke>,
    private val textbackcolor: Int,
    private val selectcolor : Int
) : Serializable, RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> ()
{
    val CONTENT_TYPE : Int = 0
    val PROGRESSTYPE : Int = 1
    val loaderval : Joke = Joke(-1, "loader")
    private val selectindexes : ArrayList<Int> = ArrayList()
    private val unselectindexes : ArrayList<Int> = ArrayList()


    public fun getValues() : ArrayList<Joke> {return values}

    override fun getItemViewType(position : Int) : Int
    {
        return if (values[position].getId() != loaderval.getId()) {CONTENT_TYPE}
        else {PROGRESSTYPE}
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var itemView : View
        Log.v("loadobserv", "ccreate")
        if (viewType == CONTENT_TYPE)
        {
            itemView = LayoutInflater.from(parent.context).inflate(
                R.layout.recyclerview_item, parent, false
            )
        }
        else
        {
            itemView = LayoutInflater.from(parent.context).inflate(
                R.layout.down_loader_progressbar, parent, false
            )
        }
        return ViewHolder(itemView)
    }


    override fun getItemCount(): Int {return values.size}


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == 0)
        {
            val lp = holder.textview?.layoutParams as ViewGroup.MarginLayoutParams
            holder.textview?.layoutParams = lp
        }
        if (values[position] != loaderval)
        {
            holder.textview?.text =  values[position].getJokeText()
            holder.textview?.setTag(values[position].getId())
            Log.v("eree", " selectindexes$position")
        }
        Log.v("selectbind", " selectindexes.setid")
        // ?????? ??????????????????
        if (values[position] != loaderval && selectindexes.size > 0)
        {
            val id = values[position].getId()
            val index = selectindexes.indexOf(id)
            var isunselect = true
            if (unselectindexes.indexOf(id) < 0) isunselect = false
            Log.v("selectbind", "select>index" + index.toString())
            Log.v("selectbind", "select>pos" + position.toString())
            Log.v("selectbind", "select>id" + holder.textview?.tag!!.toString())
            if (!isunselect && index >= 0)
            {
                // ????????????????
                holder.textview?.setBackgroundColor(selectcolor)
            }
            else
            {
                //?????????? ??????????????????
                holder.textview?.setBackgroundColor(textbackcolor)
                if (unselectindexes.size > 0) unselectindexes.remove(id)
                if (index >= 0) selectindexes.remove(id)
            }
            Log.v("eree", " selectindexes.size. se")
        }
        else
        {
            //????????????????
            holder.textview?.setBackgroundColor(textbackcolor)
            Log.v("selectbind", " selectindexes.size. text")
        }
    }


    public fun SelectItem(id : Int)
    {
        Log.v("selectt", "funselect")
        selectindexes.add(id)
        val index = getIndexOfId(id)
        if (index == -1) return
        Log.v("selectt", index.toString())
        Log.v("selectt", Thread.currentThread().name)
        notifyItemChanged(index)
    }

    public fun UnselectItem(id : Int)
    {
        Log.v("selectt", "unselect")
        unselectindexes.add(id)
        val index = getIndexOfId(id)
        if (index == -1) return
        Log.v("selectt", index.toString())
        Log.v("selectt", Thread.currentThread().name)
        notifyItemChanged(index)
    }

    public fun getIndexOfId(id: Int) : Int
    {
        for (i in 0 until itemCount)
        {
            if (values[i].getId() == id)
            {
                return i
            }
        }
        return -1
    }

    public fun setAllItem()
    {
        selectindexes.clear()
        unselectindexes.clear()
        notifyItemRangeRemoved(0, itemCount)
    }



    public fun removeItem(index : Int)
    {
        Log.v("deleteitema", values[index].getId().toString())
        selectindexes.clear()
        notifyItemRemoved(index)
    }

    public fun addLoader()
    {
        values.add(loaderval)
        notifyItemInserted(itemCount);
    }

    public fun removeLoader()
    {
        Log.v("lload", "remove")
        var b = false
        while (!b)
        {
            for (i in 0 until itemCount)
            {
                if (values[i].getId() == loaderval.getId())
                {
                    values.removeAt(i)
                    notifyItemRemoved(i)
                    break
                }
            }
            b = true
        }
    }

    @Transient public val itemClickStream: PublishSubject<View> = PublishSubject.create()

    public inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        var textview : TextView? = null
        init {
            textview = itemView.findViewById(R.id.textview)
            itemView.setOnLongClickListener {
                Log.v("selectt", "click" + Thread.currentThread().name)
                Log.v("onclickl", "onclicklong")
                itemClickStream.onNext(it)
                true
            }
        }
    }
}