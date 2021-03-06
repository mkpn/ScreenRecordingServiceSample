package com.memo.t.to.v.servicesample

import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import android.app.NotificationManager
import android.os.Binder
import androidx.core.app.NotificationCompat

open class TestService : Service() {

    // Binder given to clients
    private val binder = LocalBinder()

    private lateinit var newUri: Uri

    //Intentに詰めたデータを受け取る
    var data: Intent? = null
    var code = Activity.RESULT_OK

    //画面録画で使う
    lateinit var mediaRecorder: MediaRecorder
    lateinit var projectionManager: MediaProjectionManager
    lateinit var projection: MediaProjection
    lateinit var virtualDisplay: VirtualDisplay

    //画面の大きさ
    //Pixel 3 XLだとなんかおかしくなる
    var height = 2800
    var width = 1400
    var dpi = 1000

    private lateinit var filePath: File
    private lateinit var values: ContentValues
    private lateinit var fileName: String

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            //データ受け取る
            data = it.getParcelableExtra("data")
            code = it.getIntExtra("code", Activity.RESULT_OK)
            fileName = it.getStringExtra("title") ?: ""

            //画面の大きさ
            height = it.getIntExtra("height", 1000)
            width = it.getIntExtra("width", 1000)
            dpi = it.getIntExtra("dpi", 1000)
        }

        showNotification()
        //録画開始
        startRec()

        return START_NOT_STICKY
    }

    private fun showNotification() {
        //通知を出す。
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        //通知チャンネル
        val channelID = "rec_notify"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //通知チャンネルが存在しないときは登録する
            if (notificationManager.getNotificationChannel(channelID) == null) {
                val channel =
                    NotificationChannel(
                        channelID,
                        "録画サービス起動中通知",
                        NotificationManager.IMPORTANCE_HIGH
                    )
                notificationManager.createNotificationChannel(channel)
            }
            //通知作成
            val notification = Notification.Builder(applicationContext, channelID)
                .setContentText("録画中です。")
                .setContentTitle("画面録画")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()
            startForeground(1, notification)
        } else {
            val notification = NotificationCompat.Builder(applicationContext, channelID)
                .setContentTitle("画面録画")
                .setContentText("録画中です")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            startForeground(1, notification)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    //Service終了と同時に録画終了
    override fun onDestroy() {
        super.onDestroy()
        stopRec()
    }

    //録画開始
    private fun startRec() {
        filePath = getFile()

        values = ContentValues().apply {
            put(MediaStore.Video.Media.TITLE, fileName)
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(
                MediaStore.Video.Media.DATE_ADDED,
                (System.currentTimeMillis() / 1000).toInt()
            )
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Video.Media.IS_PENDING, 1) // 保存中Flag
            val collection =
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            newUri = contentResolver.insert(collection, values)!!
        } else {
            newUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)!!
        }

        val file = contentResolver.openFileDescriptor(newUri, "w")!!

        if (data != null) {
            projectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            //codeはActivity.RESULT_OKとかが入る。
            projection =
                projectionManager.getMediaProjection(code, data!!)

            mediaRecorder = MediaRecorder()
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mediaRecorder.setVideoEncodingBitRate(1080 * 10000) //1080は512ぐらいにしといたほうが小さくできる
            mediaRecorder.setVideoFrameRate(30)
            mediaRecorder.setVideoSize(width, height)
            mediaRecorder.setAudioSamplingRate(44100)
            mediaRecorder.setOutputFile(file.fileDescriptor)
            mediaRecorder.prepare()

            virtualDisplay = projection.createVirtualDisplay(
                "recode",
                width,
                height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.surface,
                null,
                null
            )

            //開始
            mediaRecorder.start()
        }
    }

    //録画止める
    private fun stopRec() {
        mediaRecorder.stop()
        mediaRecorder.release()
        virtualDisplay.release()
        projection.stop()
        addRecordingToMediaLibrary()
    }

    //保存先取得。今回は対象範囲別ストレージに保存する
    private fun getFile(): File {
        //ScopedStorageで作られるサンドボックスへのぱす
        val scopedStoragePath = getExternalFilesDir(null)
        //ファイル作成
        return File("${scopedStoragePath?.path}/${System.currentTimeMillis()}.mp4")
    }

    private fun addRecordingToMediaLibrary() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        //creating content resolver and storing it in the external content uri
        val contentResolver = contentResolver
        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        contentResolver.update(newUri, values, null, null)
        Toast.makeText(this, "Added File $newUri", Toast.LENGTH_LONG).show()
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): TestService = this@TestService
    }
}