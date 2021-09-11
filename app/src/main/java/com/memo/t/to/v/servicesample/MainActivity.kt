package com.memo.t.to.v.servicesample

import android.Manifest
import android.content.ComponentName
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {

    //リクエストの結果
    private val code = 512
    private val permissionCode = 810
    lateinit var projectionManager: MediaProjectionManager

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        private var bound = false
        private lateinit var testService: TestService

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as TestService.LocalBinder
            testService = binder.getService()
            bound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }

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
        intent.putExtra("title", getFileName())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun getFileName(): String {
        return "録画テスト_${System.currentTimeMillis()}.mp4"
    }
}