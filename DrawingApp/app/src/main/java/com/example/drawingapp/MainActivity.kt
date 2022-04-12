package com.example.drawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.Image
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null
    private var colorLayout: LinearLayout? = null
    private var currPaint: ImageButton ?= null
    private var  customProgressDialog : Dialog ?= null
    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageBackground = findViewById<ImageView>(R.id.ivBackground)
                imageBackground.setImageURI(result.data?.data)
            }
        }


    private val galleryPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    val pickIntent = Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )
                    openGalleryLauncher.launch(pickIntent)
                } else {
                    Toast.makeText(
                        this, "Permission denied for Gallery", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById<DrawingView>(R.id.drawingView)
        colorLayout = findViewById(R.id.ColorLayout)
        currPaint = colorLayout!![1] as ImageButton
        currPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.palet_selected)
        )
        drawingView?.setSizeForBrush(20.toFloat())
        val ib_brush = findViewById<ImageButton>(R.id.ib_brush)
        ib_brush.setOnClickListener {
            showBrushChooserDialog()
        }
        val ib_gallery = findViewById<ImageButton>(R.id.ib_gallery)
        ib_gallery.setOnClickListener {
            requestStoragePermission()
        }
        val ib_undo = findViewById<ImageButton>(R.id.ib_undo)
        ib_undo.setOnClickListener {
            drawingView?.undoPath()
        }
        val ib_save = findViewById<ImageButton>(R.id.ib_save)
        ib_save.setOnClickListener {
            if (isReadingStorageAllowed()) {
                showProgressDialog()
                lifecycleScope.launch {
                    val flDrawingView: FrameLayout = findViewById(R.id.frameLayout)
                    saveBitMapFile(getBitmapFromView(flDrawingView))
                }
            }
        }
        val ib_share = findViewById<ImageButton>(R.id.ib_share)
        ib_share.setOnClickListener {
            if (isReadingStorageAllowed()) {
                showProgressDialog()
                lifecycleScope.launch {
                    val flDrawingView: FrameLayout = findViewById(R.id.frameLayout)
                    sendBitMapFile(getBitmapFromView(flDrawingView))
                }
            }
        }
    }

    private fun showBrushChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        val smallButton = brushDialog.findViewById<ImageButton>(R.id.small_brush)
        val mediumButton = brushDialog.findViewById<ImageButton>(R.id.medium_brush)
        val largeButton = brushDialog.findViewById<ImageButton>(R.id.large_brush)
        smallButton.setOnClickListener {
            changeBrushSize(10.toFloat())
            brushDialog.dismiss()
        }
        mediumButton.setOnClickListener {
            changeBrushSize(20.toFloat())
            brushDialog.dismiss()
        }
        largeButton.setOnClickListener {
            changeBrushSize(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    private fun changeBrushSize(size: Float) {
        drawingView?.setSizeForBrush(size)
    }

    private fun isReadingStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            showRationaleDialog(
                "Drawing APP",
                "Needs Access Of Your External Storage to set Background Image"
            )
        } else {
            galleryPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun showRationaleDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun getBitmapFromView(view: View): Bitmap {

        //Define a bitmap with the same size as the view.
        // CreateBitmap : Returns a mutable bitmap with the specified width and height
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        //Get the view's background
        val bgDrawable = view.background
        if (bgDrawable != null) {
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas)
        } else {
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE)
        }
        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }

    private fun getBitMapfromView(view: View): Bitmap {
        val returnMap = Bitmap.createBitmap(view.height, view.width, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnMap)
        val bgBackground = view.background
        if (bgBackground != null) {
            bgBackground.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        return returnMap
    }

    fun changeBrushColor(view: View) {
        if (view !== currPaint) {
            val btn = view as ImageButton
            drawingView?.setColorForBrush(btn.tag.toString())
            currPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.palet_normal)
            )
            currPaint = btn
            currPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.palet_selected)
            )
        }
    }

    private suspend fun sendBitMapFile(bitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (bitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f = File(
                        externalCacheDir?.absoluteFile.toString()
                                + File.separator + "Drawing APP_" + System.currentTimeMillis() / 1000 + ".png"
                    )
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result = f.absolutePath
                    runOnUiThread {
                        cancelProgressDialog()
                        shareImage(result)
                        if (result.isEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong while saving the file", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private suspend fun saveBitMapFile(bitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (bitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f = File(
                        externalCacheDir?.absoluteFile.toString()
                                + File.separator + "Drawing APP_" + System.currentTimeMillis() / 1000 + ".png"
                    )
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result = f.absolutePath
                    runOnUiThread {
                        cancelProgressDialog()
                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File Saved Successfully at:  $result", Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong while saving the file", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }
    private fun showProgressDialog(){
        customProgressDialog = Dialog(this)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }
    private fun cancelProgressDialog(){
        if(customProgressDialog!=null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }
    private fun shareImage(result : String){
        MediaScannerConnection.scanFile(this, arrayOf(result),null){
            path,uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent,"Share"))

        }
    }
}

