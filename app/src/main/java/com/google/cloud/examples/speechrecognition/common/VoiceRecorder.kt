package com.google.cloud.examples.speechrecognition.common

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.IOException

internal class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    val recordingFilePath: String by lazy {
        "${context.externalCacheDir?.absolutePath}/recording.mp4"
    }

    fun recorderStart() {
        recorder = (if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else MediaRecorder()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(recordingFilePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)

            try {
                prepare()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        recorder?.start()
    }

    fun recorderStop() {
        recorder?.stop()
        recorder?.release()
        recorder = null
    }
}