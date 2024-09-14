package com.example.musicown

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible

class MainActivity : AppCompatActivity() {

    private lateinit var myListViewForSongs: ListView
    private lateinit var statusMsg: TextView
    private lateinit var fileCheck: TextView
    private lateinit var givePermission: Button
    private lateinit var toolbar: Toolbar
    private val PERMISSION_REQUEST_CODE = 100

    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        myListViewForSongs = findViewById(R.id.mySongListView)
        statusMsg = findViewById(R.id.statusTxt)
        fileCheck = findViewById(R.id.fileStatus)
        givePermission = findViewById(R.id.giveAccess)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "MusicOwn"
        requestPermission()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            displaySongs()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
        }
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                statusMsg.isVisible = false
                givePermission.isVisible = false
                displaySongs()
            } else {
                statusMsg.isVisible = true
                givePermission.isVisible = true
                statusMsg.text = "Please grant access to audio files."
                givePermission.setOnClickListener { requestPermission() }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun displaySongs() {
        val songList = getSongList()
        if (songList.isEmpty()) {
            fileCheck.isVisible = true
            fileCheck.text = "No audio files found"
        } else {
            fileCheck.isVisible = false
            val items = songList.map { it.title }.toTypedArray()
            val myAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val textView = view.findViewById<TextView>(android.R.id.text1)
                    textView.setTextColor(Color.BLACK)
                    return view
                }
            }
            myListViewForSongs.adapter = myAdapter
            myListViewForSongs.setOnItemClickListener { _, _, position, _ ->
                val selectedSong = songList[position]
                val intent = Intent(this, PlayMusicActivity::class.java).apply {
                    putExtra("songs", songList.map { it.uri.toString() }.toTypedArray())
                    putExtra("songname", selectedSong.title)
                    putExtra("pos", position)
                }
                startActivity(intent)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getSongList(): List<Song> {
        val songList = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

        contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val contentUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                val uri = contentUri.buildUpon().appendPath(id.toString()).build()

                songList.add(Song(id, name, uri))
            }
        }
        return songList
    }

    data class Song(val id: Long, val title: String, val uri: android.net.Uri)
}