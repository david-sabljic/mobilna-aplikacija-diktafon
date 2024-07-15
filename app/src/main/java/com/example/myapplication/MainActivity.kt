package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.Date


const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(),Timer.OnTimerTickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var amplitudes: ArrayList<Float>
    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false

    private lateinit var recorder: MediaRecorder
    private var dirPath = ""
    private var fileName = ""
    private var isRecording = false
    private var isPaused = false
    private lateinit var timer: Timer

    private lateinit var db : AppDatabase
    private var duration = ""

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()

        permissionGranted = ActivityCompat.checkSelfPermission(this,permissions[0]) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted){
            ActivityCompat.requestPermissions(this,permissions, REQUEST_CODE)
        }

        binding.btnRecord.setOnClickListener {
            when{
                isPaused -> resumeRecording()
                isRecording -> pauseRecording()
                else -> startRecording()
            }
        }

        binding.btnList.setOnClickListener{
            startActivity(Intent(this,GalleryActivity::class.java))
        }

        binding.btnDone.setOnClickListener {
            stopRecording()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.bottomSheetBg.visibility = View.VISIBLE
            binding.fileNameInput.setText(fileName)
        }

        binding.btnCancel.setOnClickListener {
            File("$dirPath$fileName.mp3").delete()
            dismiss()
        }

        binding.btnOk.setOnClickListener {
            save()
            Toast.makeText(this,"Record saved!",Toast.LENGTH_SHORT).show()
            dismiss()
        }

        binding.bottomSheetBg.setOnClickListener {
            File("$dirPath$fileName.mp3").delete()
            dismiss()
        }

        binding.btnDelate.setOnClickListener {
            stopRecording()
            Toast.makeText(this,"Record deleted!",Toast.LENGTH_SHORT).show()
            File("$dirPath$fileName.mp3").delete()
        }

        timer = Timer(this)
        binding.btnDelate.isClickable = false

    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun save(){
        val newFileName = binding.fileNameInput.text.toString()
        if(fileName!=newFileName){
            val newFile = File("$dirPath$newFileName.mp3")
            File("$dirPath$fileName.mp3").renameTo(newFile)
        }

        val filePath = "$dirPath$newFileName.mp3"
        val timeStamp = Date().time
        val ampsPath = "$dirPath$newFileName"

        try{
            val fos = FileOutputStream(ampsPath)
            val out = ObjectOutputStream(fos)
            out.writeObject(amplitudes)
            fos.close()
            out.close()
        }catch (_:IOException){}

        val record = AudioRecord(newFileName,filePath,timeStamp,duration,ampsPath)

        GlobalScope.launch {
            db.audioRecordDao().insert(record)
        }
    }

    private fun dismiss(){
        binding.bottomSheetBg.visibility = View.GONE
        hideKeyboard(binding.fileNameInput)

        Handler(Looper.getMainLooper()).postDelayed({
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        },100)
    }

    private fun hideKeyboard(view:View){
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken,0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE){
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun resumeRecording(){
        recorder.resume()
        isPaused = false
        timer.start()
        binding.btnRecord.setImageResource(R.drawable.ic_pause)
    }

    private fun pauseRecording(){
        recorder.pause()
        isPaused = true
        timer.pause()
        binding.btnRecord.setImageResource(R.drawable.ic_mic)
    }

    @SuppressLint("SetTextI18n")
    private fun stopRecording(){
        timer.stop()
        recorder.apply {
            stop()
            release()
        }
        isPaused = false
        isRecording = false
        binding.btnList.visibility = View.VISIBLE
        binding.btnDone.visibility = View.GONE
        binding.btnDelate.isClickable = false
        binding.btnDelate.setImageResource(R.drawable.ic_delate_disabled)
        binding.btnRecord.setImageResource(R.drawable.ic_mic)
        binding.tvTimer.text = "00:00.00"
        amplitudes = binding.waveFormView.clear()
    }

    @SuppressLint("SimpleDateFormat")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun startRecording(){
        if(!permissionGranted){
            ActivityCompat.requestPermissions(this,permissions, REQUEST_CODE)
            return
        }

        dirPath = "${externalCacheDir?.absolutePath}/"
        val simpleDateFormat = SimpleDateFormat("yyyy.MM.DD_hh.mm.ss")
        val date = simpleDateFormat.format(Date())
        fileName = "audio_record_$date"


        recorder = MediaRecorder()
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$fileName.mp3")

            try {
                prepare()
            }catch  (_:IOException){}

            start()
        }
        isRecording = true
        isPaused = false
        timer.start()
        binding.btnRecord.setImageResource(R.drawable.ic_pause)
        binding.btnDelate.isClickable = true
        binding.btnDelate.setImageResource(R.drawable.ic_delate)
        binding.btnList.visibility = View.GONE
        binding.btnDone.visibility = View.VISIBLE
    }

    override fun onTimerTick(duration: String) {
        binding.waveFormView.addAmplitude(recorder.maxAmplitude.toFloat()+50)
        this.duration = duration.dropLast(3)
        binding.tvTimer.text = duration

    }
}