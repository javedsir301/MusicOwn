package com.example.musicown

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import java.util.concurrent.TimeUnit

class PlayMusicActivity : AppCompatActivity() {

    private lateinit var songImg: ImageView
    private lateinit var songName: TextView
    private lateinit var btnNext: Button
    private lateinit var btnPrevious: Button
    private lateinit var btnPlayPause: Button
    private lateinit var songSeekbar: SeekBar
    private lateinit var startTimeTextView: TextView
    private lateinit var endTimeTextView: TextView
    private lateinit var toolbar: Toolbar

    private var myMediaPlayer: MediaPlayer? = null
    private var position: Int = 0
    private lateinit var songUris: Array<String>
    private val handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_music)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Feel the Song"

        initializeViews()
        initializeMediaPlayer()
        setupListeners()
    }

    private fun initializeViews() {
        songImg = findViewById(R.id.songImg)
        songName = findViewById(R.id.songName)
        btnNext = findViewById(R.id.nextBtn)
        btnPrevious = findViewById(R.id.previousBtn)
        btnPlayPause = findViewById(R.id.pauseBtn)
        songSeekbar = findViewById(R.id.seekBar)
        startTimeTextView = findViewById(R.id.startTime)
        endTimeTextView = findViewById(R.id.endTime)
    }

    @SuppressLint("SetTextI18n")
    private fun initializeMediaPlayer() {
        intent.extras?.let { bundle ->
            songUris = bundle.getStringArray("songs") ?: arrayOf()
            if (songUris.isEmpty()) {
                songName.text = "No songs available"
                return
            }

            position = bundle.getInt("pos", 0)
            playSongAtPosition(position)
        }
    }

    private fun setupListeners() {
        btnPlayPause.setOnClickListener {
            myMediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    btnPlayPause.setBackgroundResource(R.drawable.play_btn_img)
                } else {
                    it.start()
                    btnPlayPause.setBackgroundResource(R.drawable.pause_btn_img)
                }
            }
        }

        btnNext.setOnClickListener {
            position = (position + 1) % songUris.size
            playSongAtPosition(position)
        }

        btnPrevious.setOnClickListener {
            position = if (position > 0) position - 1 else songUris.size - 1
            playSongAtPosition(position)
        }

        songSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    startTimeTextView.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    myMediaPlayer?.seekTo(it.progress)
                    isUserSeeking = false
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun playSongAtPosition(newPosition: Int) {
        try {
            myMediaPlayer?.release()
            val uri = Uri.parse(songUris[newPosition])
            myMediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                prepare()
                setOnCompletionListener {
                    btnNext.performClick()
                }
                start()
                songSeekbar.max = duration
                endTimeTextView.text = formatTime(duration.toLong())
                updateSongInfo(uri)
                if (!isUserSeeking) songSeekbar.progress = currentPosition
                btnPlayPause.setBackgroundResource(R.drawable.pause_btn_img)
            }
            startSeekBarUpdate()
        } catch (e: Exception) {
            Log.e("PlayMusicActivity", "Error playing song", e)
            songName.text = "Error playing song"
        }
    }

    private fun updateSongInfo(uri: Uri) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, uri)

        val songTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            ?: uri.lastPathSegment
            ?: "Unknown Song"
        songName.text = songTitle
        songName.isSelected = true

        retriever.embeddedPicture?.let { albumArt ->
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(albumArt, 0, albumArt.size)
            songImg.setImageBitmap(bitmap)
        } ?: run {
            songImg.setImageResource(R.drawable.musical_notes)
        }

        retriever.release()
    }

    private fun startSeekBarUpdate() {
        handler.post(object : Runnable {
            override fun run() {
                myMediaPlayer?.let {
                    if (!isUserSeeking) {
                        songSeekbar.progress = it.currentPosition
                        startTimeTextView.text = formatTime(it.currentPosition.toLong())
                    }
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(milliseconds: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        myMediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
    }
}