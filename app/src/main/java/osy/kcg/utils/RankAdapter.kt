package osy.kcg.utils

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.PorterDuff
import android.graphics.Shader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import osy.kcg.mykotlin.R


class RankAdapter() : BaseAdapter(){
    val TAG = "RankAdapter"
    var mContext: Context? = null
    var mLayoutInflater: LayoutInflater? = null
    var sample: ArrayList<String>? = null

    constructor(context: Context?, data: ArrayList<String>?) : this() {
        mContext = context
        sample = data
        mLayoutInflater =LayoutInflater.from(mContext)

    }

    override fun getCount(): Int {
        return sample!!.size
    }

    override fun getItem(i: Int): String? {
        return sample!![i]
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    internal class ViewHolder {
        var value1: TextView? = null
        var value1Support : TextView? = null
        var value2: TextView? = null
        var value3: TextView? = null

    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup?): View? {
        var convertView = convertView
        val holder: ViewHolder
        if (convertView == null) {
            holder = ViewHolder()
            convertView = mLayoutInflater!!.inflate(R.layout.rank_list_item, viewGroup, false)
            holder.value1 = convertView.findViewById(R.id.rank_list1)
            holder.value1Support = convertView.findViewById(R.id.rank_list1_support)
            holder.value2 = convertView.findViewById(R.id.rank_list2)
            holder.value3 = convertView.findViewById(R.id.rank_list3)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }
        var t = sample!![i].split(",")

        holder.value1Support!!.text = "${i+1}"
        holder.value2!!.text = t[0]
        holder.value3!!.text = "${t[1]}건"


        when (i % 3) {
            0 -> {
                gradientColor(holder.value1!!, getColor(R.color.list_color_1), getColor(R.color.list_color_1a))
                holder.value1!!.setBackgroundColor(mContext!!.resources.getColor(R.color.list_color_1))
            }
            1 -> {
                gradientColor(holder.value1!!, getColor(R.color.list_color_2), getColor(R.color.list_color_2a))
                holder.value1!!.setBackgroundColor(mContext!!.resources.getColor(R.color.list_color_2))
            }
            2 -> {
                gradientColor(holder.value1!!, getColor(R.color.list_color_3), getColor(R.color.list_color_3a))
                holder.value1!!.setBackgroundColor(mContext!!.resources.getColor(R.color.list_color_3))
            }
            else -> {}
        }
        return convertView
    }

    private fun gradientColor(tv: TextView, color1: Int, color2: Int) {

        val shader: Shader = LinearGradient(
            0.toFloat(), 0.toFloat(), 0.toFloat(), tv.textSize,
            color1, color2, Shader.TileMode.CLAMP
        )
        tv.paint.shader = shader
    }

    private fun getColor(id: Int): Int {
        return mContext!!.resources.getColor(id)
    }

}