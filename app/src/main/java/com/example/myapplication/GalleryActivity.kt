package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.myapplication.databinding.ActivityGalleryBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GalleryActivity : AppCompatActivity(),OnItemClickListener {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var records: ArrayList<AudioRecord>
    private lateinit var mAdapter: Adapter
    private lateinit var db: AppDatabase
    private lateinit var newRecyclerView: RecyclerView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var editBar: View
    private lateinit var btnClose: ImageButton
    private lateinit var btnSelectAll: ImageButton
    private lateinit var bottomSheet: LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var btnDelete: ImageButton
    private lateinit var tvDelete: TextView

    private var allChecked = false

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        records = ArrayList()

        toolbar = findViewById(R.id.toolbar)
        editBar = findViewById(R.id.editBar)
        btnClose = findViewById(R.id.btnClose)
        btnSelectAll = findViewById(R.id.btnSelectAll)
        bottomSheet = findViewById(R.id.bottomSheetGallery)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        btnDelete = findViewById(R.id.btnDelete)
        tvDelete = findViewById(R.id.tvDelete)



        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()

        mAdapter = Adapter(records)
        mAdapter.setListner(this)

        getData()
        mAdapter.setListner(this)

        newRecyclerView = findViewById(R.id.recyclerView)
        newRecyclerView.layoutManager =LinearLayoutManager(this)
        newRecyclerView.setHasFixedSize(false)
        newRecyclerView.adapter = mAdapter

        btnClose.setOnClickListener {
            records.map { it.isChecked = false }
            mAdapter.notifyDataSetChanged()
            leaveEditMode()
        }

        btnSelectAll.setOnClickListener {
            allChecked = !allChecked
            records.map { it.isChecked = allChecked }
            mAdapter.notifyDataSetChanged()
            if (allChecked){
                enableDelete()
            }
            else{
                disableDelete()
            }
        }

        btnDelete.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Delete record?")
            val nbRecords = records.count { it.isChecked }
            builder.setMessage("Are you sure you want to delete $nbRecords records?")
            builder.setPositiveButton("delete") { _, _ ->
                val toDelete = records.filter { it.isChecked }.toTypedArray()
                GlobalScope.launch {
                    db.audioRecordDao().delete(toDelete)
                    runOnUiThread{
                        records.removeAll(toDelete.toSet())
                        mAdapter.notifyDataSetChanged()
                        leaveEditMode()
                    }
                }
            }
            builder.setNegativeButton("cancel") { _, _ ->}

            val dialog = builder.create()
            dialog.show()

        }
    }

    private fun leaveEditMode(){
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        editBar.visibility = View.GONE
        records.map { it.isChecked = false }
        mAdapter.setEditMode(false)
    }

    private fun disableDelete(){
        btnDelete.isClickable = false
        btnDelete.backgroundTintList = ResourcesCompat.getColorStateList(resources,R.color.grayDarkDisabled,theme)
        tvDelete.setTextColor(ResourcesCompat.getColorStateList(resources,R.color.grayDarkDisabled,theme))
    }


    private fun enableDelete(){
        btnDelete.isClickable = true
        btnDelete.backgroundTintList = ResourcesCompat.getColorStateList(resources,R.color.grayDark,theme)
        tvDelete.setTextColor(ResourcesCompat.getColorStateList(resources,R.color.grayDark,theme))
    }


    @OptIn(DelicateCoroutinesApi::class)
    private fun getData(){
        GlobalScope.launch {
            records.clear()
            val queryResult = db.audioRecordDao().getAll()
            records.addAll(queryResult)
            mAdapter = Adapter(records)
        }
    }

    override fun onItemClickListener(position: Int) {
        val audioRecord = records[position]
        val intent = Intent(this,AudioPlayerActivity::class.java)

        if (mAdapter.isEditMode()){
            records[position].isChecked = !records[position].isChecked
            mAdapter.notifyItemChanged(position)
            when(records.count{it.isChecked}){
                0->{
                    disableDelete()
                }
                1->{
                    enableDelete()
                }
                else->{
                    enableDelete()
                }
            }
        }
        else {
            intent.putExtra("filePath", audioRecord.filePath)
            intent.putExtra("fileName", audioRecord.fileName)
            startActivity(intent)
        }
    }

    override fun onItemLongClickListener(position: Int) {
        mAdapter.setEditMode(true)
        records[position].isChecked = !records[position].isChecked
        runOnUiThread {
            mAdapter.notifyItemChanged(position)
        }
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        if (mAdapter.isEditMode() && editBar.visibility == View.GONE){
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setDisplayShowHomeEnabled(false)

            editBar.visibility = View.VISIBLE
            enableDelete()
        }
    }
}