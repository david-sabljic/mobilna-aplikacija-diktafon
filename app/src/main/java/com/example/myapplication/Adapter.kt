package com.example.myapplication

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date

class Adapter(private var records:ArrayList<AudioRecord>): RecyclerView.Adapter<Adapter.ViewHolder>() {

    lateinit var listener: OnItemClickListener
    private var editMode = false

    fun isEditMode():Boolean{
        return editMode
    }

    fun setEditMode(mode:Boolean){
        if(editMode!= mode){
            editMode = mode
            //notifyDataSetChanged()
        }
    }

    inner class ViewHolder(itemView: View):RecyclerView.ViewHolder(itemView), View.OnClickListener,View.OnLongClickListener{
        var tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        var tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        var checkbox: CheckBox = itemView.findViewById(R.id.checkbox)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(p0: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION){
                listener.onItemClickListener(position)
            }
        }

        override fun onLongClick(p0: View?): Boolean {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION){
                listener.onItemLongClickListener(position)
            }
            return true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.itemview_layout,parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return records.size
    }

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position != RecyclerView.NO_POSITION){
            val record = records[position]
            val sdf = SimpleDateFormat("dd/MM/yyyy")
            val date = Date(record.timeStamp)
            val strDate = sdf.format(date)

            holder.tvFileName.text = record.fileName
            holder.tvMeta.text = "${record.duration} $strDate"

            if (editMode){
                holder.checkbox.visibility = View.VISIBLE
                holder.checkbox.isChecked = record.isChecked
            }
            else{
                holder.checkbox.visibility = View.GONE
                holder.checkbox.isChecked = false
            }
        }
    }

    fun setListner(listen:OnItemClickListener){
        listener = listen
    }
}