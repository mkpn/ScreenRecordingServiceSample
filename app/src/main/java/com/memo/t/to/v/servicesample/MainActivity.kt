package com.memo.t.to.v.servicesample

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {

    //リクエストの結果
    private val permissionCode = 810
    lateinit var projectionManager: MediaProjectionManager

    //マイクの権限があるので画面録画リクエスト
    //ダイアログを出す
    // グローバル変数じゃないとクラッシュするらしい
    private val startForResult =
        registerForActivityResult(StartActivityForResult()) {
            if (it?.resultCode == Activity.RESULT_OK && it.data != null) {
                handleActivityResult(it.data!!)
            }
        }

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
            if (!isPermissionsGranted()) {
                requestRecordPermissions()
            } else {
                startForResult.launch(projectionManager.createScreenCaptureIntent())
            }
        }

        val stopButton = findViewById<View>(R.id.stop_button)
        stopButton.setOnClickListener {
            val intent = Intent(application, TestService::class.java)
            unbindService(connection)
            stopService(intent)
        }
    }

    private fun isPermissionsGranted(): Boolean {
        Log.d(
            "デバッグ",
            "権限 " + (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED)
        )
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordPermissions() {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
        )
        requestPermissions(permissions, permissionCode)
    }

    private fun handleActivityResult(data: Intent) {
        val intent = Intent(application, TestService::class.java)
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
        } else {
            startService(intent)
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun getFileName(): String {
        return "録画テスト_${System.currentTimeMillis()}.mp4"
    }
}