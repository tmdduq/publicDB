package osy.kcg.utils

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import osy.kcg.mykotlin.R


class InputPointToTextAdapter() : BaseAdapter(){
    val TAG = "InputPointToTextAdapter"
    var mContext: Context? = null
    var mLayoutInflater: LayoutInflater? = null
    var ary: ArrayList<MutableList<String>>? = null

    constructor(context: Context?, data: ArrayList<MutableList<String>>) : this() {
        mContext = context
        ary = data
        mLayoutInflater = LayoutInflater.from(mContext)
    }

    override fun getCount(): Int {
        return ary!!.size
    }

    override fun getItem(i: Int): MutableList<String> {
        return ary!![i]
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }
    fun setItem(i:Int, item : MutableList<String>){
        ary!![i] = item
    }

    internal class ViewHolder {
        var value1: EditText? = null
        var value2: EditText? = null
        var value3: Button? = null

    }


    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup?): View? {
        var convertView = convertView
        val holder: ViewHolder
        if (convertView == null) {
            holder = ViewHolder()
            convertView = mLayoutInflater!!.inflate(R.layout.customdialog_inputpoint, viewGroup, false)
            holder.value1 = convertView.findViewById(R.id.inputpoint_lat)
            holder.value2 = convertView.findViewById(R.id.inputpoint_lon)
            holder.value3 = convertView.findViewById(R.id.inputpoint_num)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }
        holder.value1!!.setText(ary!![i][0])
        holder.value2!!.setText(ary!![i][1])

        holder.value3!!.text = "$i"

        if(i==0){
            holder.value3!!.setBackgroundResource(R.color.color_sun_flower)
            holder.value3!!.text = "input"
            holder.value1!!.isEnabled = true
            holder.value2!!.isEnabled = true
        }
        return convertView


    }


}