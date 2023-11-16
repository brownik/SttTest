package com.google.cloud.examples.speechrecognition.google

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.examples.speechrecognition.R
import com.google.cloud.examples.speechrecognition.common.EtcUtil
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.RecognizeRequest
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechSettings
import java.lang.Exception

object GoogleSTTUtil {

    private var mSpeechClient: SpeechClient? = null

    fun initClient(context: Context) {
        mSpeechClient = context.resources.openRawResource(R.raw.credential).use {
            SpeechClient.create(
                SpeechSettings.newBuilder()
                    .setCredentialsProvider { GoogleCredentials.fromStream(it) }
                    .build()
            )
        }
    }

    fun googleStt(path: String?, callback: (String) -> Unit) {
        try {
            val audio = RecognitionAudio.newBuilder()
                .setUri(path)
                .build()

            val config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.FLAC)
                .setLanguageCode("ko-KR")
//                .setSampleRateHertz(44100)
                .setAudioChannelCount(2)
                .build()

            val request = RecognizeRequest.newBuilder()
                .setConfig(config)
                .setAudio(audio)
                .build()
            val response = mSpeechClient?.recognize(request)
            val result = response?.resultsList
//            EtcUtil.makeLog("result: $response")

            if (!result.isNullOrEmpty()) {
                callback.invoke(result[0].alternativesList[0].transcript)
            }
        } catch (e: Exception) {
            EtcUtil.makeLog("error: ${e.message}")
        }
    }

    fun shutDown() {
        mSpeechClient?.shutdown()
    }
}