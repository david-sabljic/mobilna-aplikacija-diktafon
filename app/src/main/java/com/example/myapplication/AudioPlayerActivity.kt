package com.example.myapplication

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.media.PlaybackParams
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import java.text.DecimalFormat
import java.text.NumberFormat

@Suppress("OVERRIDE_DEPRECATION")
class AudioPlayerActivity : AppCompatActivity() {

    private lateinit var mediaPlayer : MediaPlayer
    private lateinit var btnPlay : ImageButton
    private lateinit var btnBackward : ImageButton
    private lateinit var btnForward : ImageButton
    private lateinit var speedChip: Chip
    private lateinit var seekBar: SeekBar
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvFileName: TextView
    private lateinit var tvTrackProgress: TextView
    private lateinit var tvTrackDuration: TextView

    private lateinit var runnable: Runnable
    private lateinit var handler: Handler
    private var delay = 1000L
    private var jumpValue = 1000
    private var playSpeed = 1.0f

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_player)

        val filePath = intent.getStringExtra("filePath")
        val fileName = intent.getStringExtra("fileName")
        btnPlay = findViewById(R.id.playBtn)
        btnBackward = findViewById(R.id.backwardBtn)
        btnForward = findViewById(R.id.fowardBtn)
        speedChip = findViewById(R.id.chip)
        seekBar = findViewById(R.id.seekBar)
        toolbar = findViewById(R.id.toolbar)
        tvFileName = findViewById(R.id.tvFileName)
        tvTrackProgress = findViewById(R.id.tvTrackProgress)
        tvTrackDuration = findViewById(R.id.tvTrackPDuration)


        tvFileName.text = fileName
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        mediaPlayer = MediaPlayer()
        mediaPlayer.apply {
            setDataSource(filePath)
            prepare()
        }

        tvTrackDuration.text = dateFormat(mediaPlayer.duration)

        btnPlay.setOnClickListener{
            playPausePlayer()
        }

        btnForward.setOnClickListener{
            mediaPlayer.seekTo(mediaPlayer.currentPosition + jumpValue)
            seekBar.progress += jumpValue
        }

        btnBackward.setOnClickListener{
            mediaPlayer.seekTo(mediaPlayer.currentPosition - jumpValue)
            seekBar.progress -= jumpValue
        }

        speedChip.setOnClickListener {
            if(playSpeed != 2.0f){
                playSpeed += 0.5f
            }
            else{
                playSpeed = 0.5f
            }

            mediaPlayer.playbackParams = PlaybackParams().setSpeed(playSpeed)
            speedChip.text = "x$playSpeed"
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if(p2){
                    mediaPlayer.seekTo(p1)
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?){}
            override fun onStopTrackingTouch(p0: SeekBar?) {}

        })

        seekBar.max = mediaPlayer.duration

        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            seekBar.progress = mediaPlayer.currentPosition
            tvTrackProgress.text = dateFormat(mediaPlayer.currentPosition)
            handler.postDelayed(runnable,delay)
        }

        mediaPlayer.setOnCompletionListener {
            btnPlay.background = ResourcesCompat.getDrawable(resources,R.drawable.ic_play_circle,theme)
        }

        playPausePlayer()
    }

    private fun playPausePlayer(){
        if(!mediaPlayer.isPlaying){
            mediaPlayer.start()
            btnPlay.background = ResourcesCompat.getDrawable(resources,R.drawable.ic_pause_circle,theme)
            handler.postDelayed(runnable,delay)
        }
        else{
            mediaPlayer.pause()
            btnPlay.background = ResourcesCompat.getDrawable(resources,R.drawable.ic_play_circle,theme)
            handler.removeCallbacks(runnable)
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        mediaPlayer.stop()
        mediaPlayer.release()
        handler.removeCallbacks(runnable)
    }

    private fun dateFormat(duration: Int): String{
        val d = duration/1000
        val s = d%60
        val m = (d/60 % 60)
        val h = ((d - m*60)/360)

        val f: NumberFormat = DecimalFormat("00")
        var str = "$m:${f.format(s)}"
        if (h>0){
            str = "$h:$str"
        }
        return str
    }
}