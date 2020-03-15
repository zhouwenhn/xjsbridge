package com.karmars.xjsbridge

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.LayoutInflater
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.FileChooserParams
import android.webkit.WebView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.karmars.xjsbridge.R
import com.karmars.xjsbridge.utils.XImageUtils
import com.karmars.xjsbridge.wedget.XWebView
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Logger
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.find

/**
 *Created by zhouwen on 2020-02-28.
 */
class MainActivity : Activity(),  EasyPermissions.PermissionCallbacks{

    private val sLogger = Logger.getLogger("MainActivity")
    private val REQUEST_SELECT_FILE_CODE = 100
    private val REQUEST_FILE_CHOOSER_CODE = 101
    private val REQUEST_FILE_CAMERA_CODE = 102
    val IMAGE_COMPRESS_SIZE_DEFAULT = 400
    val COMPRESS_MIN_HEIGHT = 600
    val COMPRESS_MIN_WIDTH = 500

    private var mUploadMsg: ValueCallback<Uri?>? = null
    private var mUploadMsgs: ValueCallback<Array<Uri>>? = null
    private var mFileFromCamera: File? = null
    private var selectPicDialog: BottomSheetDialog? = null
    private var  mWv: XWebView? = null

    private lateinit var mUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mWv = find<XWebView>(R.id.mainWV)
        initChooser()
        checkPermissions()
        mUrl = "file:///android_asset/index.html"
        mWv?.loadUrl(mUrl)

    }

    override fun onResume() {
        super.onResume()
    }

    private fun initChooser() {
        mWv?.setOpenFileChooserCallBack(object : XWebView.OpenFileChooserCallBack {
            override fun openFileChooserCallBack(
                uploadMsg: ValueCallback<Uri?>?,
                acceptType: String?
            ) {
                mUploadMsg = uploadMsg
                showSelectPictureView(0, null)
            }

            override fun showFileChooserCallBack(
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ) {
                sLogger.severe("showFileChooserCallBack#filePathCallback: ${filePathCallback == null}")
                if (mUploadMsgs != null) {
                    mUploadMsgs?.onReceiveValue(null)
                }
                mUploadMsgs = filePathCallback
                sLogger.severe("showFileChooserCallBack#mUploadMsgs: ${mUploadMsgs == null}")
                showSelectPictureView(1, fileChooserParams)
            }
        })

        mWv?.setCreateWindowCallBack(object : XWebView.CreateWindowCallBack {
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ) {

            }
        })
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun showSelectPictureView(
        tag: Int,
        fileChooserParams: FileChooserParams?
    ) {
        selectPicDialog = BottomSheetDialog(this, R.style.Dialog_NoTitle)
        selectPicDialog?.setCanceledOnTouchOutside(false)
        val view =
            LayoutInflater.from(this).inflate(R.layout.select_view, null)
        val album = view.findViewById<TextView>(R.id.tv_select_album)
        val camera = view.findViewById<TextView>(R.id.tv_select_camera)
        val cancel = view.findViewById<TextView>(R.id.tv_select_cancel)
        album.setOnClickListener {
            if (tag == 0) {
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "*/*"
                startActivityForResult(
                    Intent.createChooser(i, "File Browser"),
                    REQUEST_FILE_CHOOSER_CODE
                )
            } else {
                try {
                    val intent = fileChooserParams?.createIntent()
                    startActivityForResult(intent, REQUEST_SELECT_FILE_CODE)
                } catch (e: ActivityNotFoundException) {
                    sLogger.severe("showSelectPictrueDialog#ActivityNotFoundException")
                    mUploadMsgs = null
                }
            }
            selectPicDialog?.dismiss()
        }
        camera.setOnClickListener {
            takeCameraPhoto()
            selectPicDialog?.dismiss()
        }
        cancel.setOnClickListener {
            selectPicDialog?.dismiss()
            onReceiveValue()
        }
        selectPicDialog?.setContentView(view)
        selectPicDialog?.show()
    }

    fun onReceiveValue() {
        if (mUploadMsgs != null) {
            mUploadMsgs?.onReceiveValue(null)
            mUploadMsgs = null
        }
        if (mUploadMsg != null) {
            mUploadMsg?.onReceiveValue(null)
            mUploadMsg = null
        }
    }

    fun takeCameraPhoto() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this, "设备无摄像头", Toast.LENGTH_SHORT).show()
            return
        }
        if (!EasyPermissions.hasPermissions(
                this,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            EasyPermissions.requestPermissions(
                this,
                "need use storage",
                200,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            processCamera()
        }
    }

    private fun processCamera() {
        mFileFromCamera = getFileFromCamera()
        if (mFileFromCamera != null) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val imgUrl: Uri
            imgUrl = if (applicationInfo.targetSdkVersion > Build.VERSION_CODES.M) {
                val authority = "com.xa.xsas.jsbridge.UploadFileProvider"
                FileProvider.getUriForFile(this, authority, mFileFromCamera!!)
            } else {
                Uri.fromFile(mFileFromCamera)
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUrl)
            startActivityForResult(intent, REQUEST_FILE_CAMERA_CODE)
        } else {
            sLogger.severe("takeCameraPhoto#mFileFromCamera = null")
        }
    }

    override fun onBackPressed() {
        sLogger.info("BizActivity#onBackPressed>mUrl:${mWv?.getUrl()} > mUrl:${mUrl}")
        super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (KeyEvent.KEYCODE_BACK == keyCode && mWv?.canGoBack()!!) {
            sLogger.info("BizActivity#onKeyDown>mUrl:${mWv?.getUrl()} > mUrl:${mUrl}")
            if (mWv?.getUrl().equals(mUrl)) {
                finish()
            } else {
                mWv?.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private fun getFileFromCamera(): File? {
        var imageFile: File? = null
        val storagePath: String
        val storageDir: File
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        try {
            storagePath =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    .absolutePath
            storageDir = File(storagePath)
            storageDir.mkdirs()
            imageFile = File.createTempFile(timeStamp, ".jpg", storageDir)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return imageFile
    }

    fun checkPermissions(){
        if (!EasyPermissions.hasPermissions(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            EasyPermissions.requestPermissions(
                this,
                "need use permissions",
                200,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(this, "授权失败", Toast.LENGTH_LONG).show()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
//        processCamera()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        sLogger.info("onActivityResult#requestCode:${requestCode}#resultCode:${resultCode}")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppCompatActivity.RESULT_CANCELED) {
            if (mUploadMsg != null) {
                mUploadMsg?.onReceiveValue(null)
                mUploadMsg = null
                return
            }
            if (mUploadMsgs != null) {
                mUploadMsgs?.onReceiveValue(null)
                mUploadMsgs = null
                return
            }
        }
        when (requestCode) {
            REQUEST_SELECT_FILE_CODE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                sLogger.info(
                    "onActivityResult#mUploadMsgs is null:${mUploadMsgs == null} #parseResult= ${FileChooserParams.parseResult(
                        resultCode,
                        data
                    )}"
                )

                if (mUploadMsgs == null) {
                    return
                }

                sLogger.info(
                    "onActivityResult#FileChooserParams #parseResult= ${FileChooserParams.parseResult(
                        resultCode,
                        data
                    )}"
                )
                mUploadMsgs?.onReceiveValue(
                    FileChooserParams.parseResult(
                        resultCode,
                        data
                    )
                )
                mUploadMsgs = null
            }
            REQUEST_FILE_CHOOSER_CODE -> {
                if (mUploadMsg == null) {
                    return
                }
                val result =
                    if (data == null || resultCode != AppCompatActivity.RESULT_OK) null else data.data

                sLogger.info("onActivityResult#result= ${result}")
                mUploadMsg?.onReceiveValue(result)
                mUploadMsg = null
            }
            REQUEST_FILE_CAMERA_CODE -> takePictureFromCamera()
            else -> {

            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun takePictureFromCamera() {
        if (mFileFromCamera != null && mFileFromCamera!!.exists()) {
            val filePath: String = mFileFromCamera!!.getAbsolutePath()

            val bitmap =
                XImageUtils.decodeSampledBitmapFromFile(
                    filePath,
                    COMPRESS_MIN_WIDTH,
                    COMPRESS_MIN_HEIGHT
                )

            if (bitmap == null) {
                onReceiveValue()
                return
            }

            val imgFile: File? = XImageUtils.compressImage(
                this,
                filePath,
                COMPRESS_MIN_WIDTH,
                COMPRESS_MIN_HEIGHT,
                IMAGE_COMPRESS_SIZE_DEFAULT
            )
            val localUri = Uri.fromFile(imgFile)
            val localIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, localUri)
            this.sendBroadcast(localIntent)
            val result = Uri.fromFile(imgFile)
            if (mUploadMsg != null) {
                mUploadMsg?.onReceiveValue(Uri.parse(filePath))
                mUploadMsg = null
            }
            if (mUploadMsgs != null) {
                mUploadMsgs?.onReceiveValue(arrayOf(result))
                mUploadMsgs = null
            }
        }
    }
}