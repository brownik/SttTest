package com.google.cloud.examples.speechrecognition.google

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.examples.speechrecognition.R
import com.google.cloud.examples.speechrecognition.common.EtcUtil
import com.google.cloud.translate.v3.LocationName
import com.google.cloud.translate.v3.TranslateTextRequest
import com.google.cloud.translate.v3.TranslationServiceClient
import com.google.cloud.translate.v3.TranslationServiceSettings

object GoogleTranslateUtil {

    private var mTranslateClient: TranslationServiceClient? = null

    fun initClient(context: Context) {
        mTranslateClient = context.resources.openRawResource(R.raw.credential).use {
            TranslationServiceClient.create(
                TranslationServiceSettings.newBuilder()
                    .setCredentialsProvider { GoogleCredentials.fromStream(it) }
                    .build()
            )
        }
    }

    fun translateText(reTranslation: Boolean, text: String, callback: (String) -> Unit) {
        val parents = LocationName.of("gcp-test-403000", "global")

        // Supported Mime Types: https://cloud.google.com/translate/docs/supported-formats
        val request: TranslateTextRequest = TranslateTextRequest.newBuilder()
            .setParent(parents.toString())
            .setMimeType("text/plain")
            .setTargetLanguageCode(if (reTranslation) "ko-KR" else "en-US")
            .addContents(text)
            .build()

        val result = mTranslateClient?.translateText(request)

        result?.let { response ->
            response.translationsList.forEach {
                EtcUtil.makeLog("Translated text: ${it.translatedText}")
                callback.invoke(it.translatedText)

            }
        }
    }
}