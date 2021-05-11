package com.m2sys.secugenconnector

import android.content.Context
import android.util.Log
import com.cloudapper.datamodel.FingerData
import com.cloudapper.datamodel.FingerDataTypes
import java.io.*

/**
 * Created by arman on 12/1/2017.
 */
class FileManager private constructor(private val mContext: Context) {
    private var appFingerprintDirectory: File? = null
    private val appFingerPrintDirectory: File
        get() {
            if (appFingerprintDirectory == null) {
                appFingerprintDirectory = File(mContext.filesDir, "/CloudApper/FingerPrint")
                appFingerprintDirectory!!.mkdirs()
            }

            return appFingerprintDirectory!!
        }

    fun saveFingerPrintFromFingerData(context: Context, fingerDatum: FingerData?): String {
        if (fingerDatum == null) return ""
        val destinationFilename: String = if (fingerDatum.dataType == FingerDataTypes.WSQ) File(
            appFingerPrintDirectory, System.currentTimeMillis().toString() + ".wsq"
        ).absolutePath else File(
            appFingerPrintDirectory, System.currentTimeMillis().toString() + ".jpg"
        ).absolutePath
        var bis: InputStream? = null
        var bos: BufferedOutputStream? = null
        try {
            if(fingerDatum.fileUri == null) { return "" }
            bis = context.contentResolver.openInputStream(fingerDatum.fileUri!!)
            bos = BufferedOutputStream(FileOutputStream(destinationFilename, false))
            val buf = ByteArray(1024)
            if(bis == null) { return "" }
            if (bis.read(buf) != -1) {
                do {
                    bos.write(buf, 0, buf.size)
                } while (bis.read(buf) != -1)
            }
            Log.d("__AppFileManager__", "File read complete")
            return destinationFilename
        } catch (e: Exception) {
            Log.d("AppFileManager", "Problem occurred. Due to : " + e.message)
        } finally {
            try {
                bis?.close()
                bos?.close()
            } catch (ignored: IOException) {
            }
        }
        return ""
    }

    companion object {
        private var ourInstance: FileManager? = null
        @Synchronized
        fun getInstance(context: Context): FileManager {
            if (ourInstance == null) {
                ourInstance = FileManager(context.applicationContext)
            }
            return ourInstance!!
        }
    }
}