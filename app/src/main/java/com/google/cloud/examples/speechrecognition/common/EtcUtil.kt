package com.google.cloud.examples.speechrecognition.common

import android.content.Context
import android.util.Log
import android.widget.Toast

object EtcUtil {
    fun makeLog(text: String) = Log.d("qwe123", text)

    fun makeToast(context: Context, text: String) =
        Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT).show()
}