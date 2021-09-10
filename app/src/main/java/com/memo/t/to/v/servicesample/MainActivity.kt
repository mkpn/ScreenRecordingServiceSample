package com.memo.t.to.v.servicesample

import android.Manifest
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import java.io.File


class MainActivity : AppCompatActivity() {

    //リクエストの結果
    val code = 512
    val permissionCode = 810
    lateinit var projectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val recButton = findViewById<View>(R.id.rec_button)
        recButton.setOnClickListener {
            //その前にマイクへアクセスしていいか尋ねる
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), permissionCode)
            } else {
                //マイクの権限があるので画面録画リクエスト
                //ダイアログを出す
                startActivityForResult(projectionManager.createScreenCaptureIntent(), code)
            }

        }

        val stopButton = findViewById<View>(R.id.stop_button)
        stopButton.setOnClickListener {
            val intent = Intent(application, TestService::class.java)
            stopService(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val intent = Intent(application, TestService::class.java)
        Log.d("デバッグ", "何なの")
        intent.putExtra("code", -1) //必要なのは結果。startActivityForResultのrequestCodeではない。
        intent.putExtra("data", data)
        //画面の大きさも一緒に入れる
        val metrics = resources.displayMetrics;
        intent.putExtra("height", metrics.heightPixels)
        intent.putExtra("width", metrics.widthPixels)
        intent.putExtra("dpi", metrics.densityDpi)
        startForegroundService(intent)
    }
}