package com.camerax

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSIONS_CODE = 101
    private val REQUIRED_PERMISSIONS =
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private val excecutor = Executors.newSingleThreadExecutor()
    private lateinit var cameraTextureView:TextureView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraTextureView = findViewById(R.id.cameraTextureView)
        requestPermissions()

        if (isCameraPermissionGranted()) startCamera()

        cameraTextureView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            updateTransform()
        }
    }

    private fun startCamera(){
        val metrics = DisplayMetrics().also { cameraTextureView.display?.getRealMetrics(it) }

        val aspectRatio = Rational(metrics.widthPixels,metrics.heightPixels)
        val rotation = cameraTextureView.display?.rotation
        val resolution = Size(metrics.widthPixels,metrics.heightPixels)

        val previewConfig = PreviewConfig.Builder().apply {
            //setTargetResolution(resolution)
            setTargetAspectRatio(AspectRatio.RATIO_16_9)
            rotation?.let { setTargetRotation(rotation) }

        }.build()
        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener {
            val parent = cameraTextureView.parent as ViewGroup
            parent.removeView(cameraTextureView)
            parent.addView(cameraTextureView,0)

            cameraTextureView.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
        }.build()

        val imageCapture = ImageCapture(imageCaptureConfig)
        captureImg?.setOnClickListener {
            val file = File(externalMediaDirs.first(),"LS_${System.currentTimeMillis()}.jpg")
            imageCapture.takePicture(file,excecutor,object:ImageCapture.OnImageSavedListener{
                override fun onImageSaved(file: File) {
                    cameraTextureView.post {
                        Toast.makeText(this@MainActivity,"Image Captured",Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(
                    imageCaptureError: ImageCapture.ImageCaptureError,
                    message: String,
                    cause: Throwable?
                ) {
                    cameraTextureView.post {
                        Toast.makeText(this@MainActivity,"Image Capture failed",Toast.LENGTH_SHORT).show()
                    }
                }

            })
        }

        CameraX.bindToLifecycle(this,preview,imageCapture)
    }

    private fun updateTransform(){
        val matrix = Matrix()

        val centerX = cameraTextureView.width/2f
        val centerY = cameraTextureView.height/2f

        val rotationDegrees = when(cameraTextureView.display?.rotation){
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }

        matrix.postRotate(-rotationDegrees.toFloat(),centerX,centerY)

        cameraTextureView.setTransform(matrix)
    }

    private fun isCameraPermissionGranted(): Boolean = ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun isStoragePermissionGranted():Boolean = ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions(){
        if (!isCameraPermissionGranted() || !isStoragePermissionGranted()){
            ActivityCompat.requestPermissions(this,REQUIRED_PERMISSIONS,REQUEST_PERMISSIONS_CODE)
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (isCameraPermissionGranted()){
                cameraTextureView.post { startCamera() }
            }
        }
    }
}
