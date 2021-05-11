package com.m2sys.secugenconnector

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.cloudapper.datamodel.*
import com.gemalto.wsq.WSQDecoder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private var shouldCaptureMultiple: Boolean = false
    lateinit var fileManager: FileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fileManager = FileManager.getInstance(this)

        scanFingerprint.setOnClickListener {
            shouldCaptureMultiple = false
            openScannerForSingleCapture()
        }
    }

    private fun openScannerForSingleCapture() {
        val intent = Intent("com.cloudapper.secugenplugin.finger_grab")
        intent.putExtra("application_id", "com.m2sys.secugenconnector")
//when you need to capture multiple fingers or want to select which finger to scan use ID_CAPTURE // else use ID_IDENTIFY
        if (shouldCaptureMultiple) {
            intent.putExtra(EXTRA_ACTION_ID, ID_CAPTURE)
        } else {
            intent.putExtra(EXTRA_ACTION_ID, ID_IDENTIFY)
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, 124)
        } else {
            val dialog = AlertDialog.Builder(this).setTitle("Warning!!")
                .setMessage("Currently you don't have Secugen Add-On Installed in your device. To continue, you need to install the add-on first.")
                .setPositiveButton("Continue") { dialog, _ ->
                    dialog.dismiss()
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.cloudapper.secugenplugin")
                        )
                    )
                }.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }.create()
            dialog.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 124 && resultCode == Activity.RESULT_OK && data?.hasExtra("FINGER_DATA") == true) {
            val fingerData: ArrayList<FingerData>? = data.getParcelableArrayListExtra("FINGER_DATA")
            if (fingerData != null) {
                val token = Binder.clearCallingIdentity()
                try {

                    CoroutineScope(Job() + Dispatchers.Main).launch {
                        var finger: FingerData? = null
                        withContext(Dispatchers.IO) {

                            for (fingerImage in fingerData) {
                                val filePath = fileManager.saveFingerPrintFromFingerData(
                                    this@MainActivity,
                                    fingerImage
                                )
                                fingerImage.filePath = filePath

                                if (!fingerImage.filePath.isNullOrBlank()) {
                                    finger = fingerImage
                                    break
                                }
                            }
                        }

                        val bitmap = withContext(Dispatchers.IO) {
                            finger?.let { fingerData ->
                                fingerData.filePath?.let {
                                    if (fingerData.dataType == FingerDataTypes.WSQ) {
                                        WSQDecoder.decode(it)
                                    } else {
                                        BitmapFactory.decodeFile(it)
                                    }
                                }
                            }
                        }

                        bitmap?.let {
                            scannedImageView.setImageBitmap(it)
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(token)
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }
}