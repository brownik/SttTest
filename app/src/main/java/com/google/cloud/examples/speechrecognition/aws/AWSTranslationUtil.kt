package com.google.cloud.examples.speechrecognition.aws

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.translate.AmazonTranslateAsyncClient
import com.amazonaws.services.translate.model.TranslateTextRequest
import com.amazonaws.services.translate.model.TranslateTextResult
import java.lang.Exception


object AWSTranslationUtil {

    private val awsCredentials = object : AWSCredentials {
        override fun getAWSAccessKeyId(): String = "AKIAR26O4CMBPKY3HRVH"

        override fun getAWSSecretKey(): String = "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"
    }

    private val client = AmazonTranslateAsyncClient(awsCredentials)

    fun textTranslation(reTranslate: Boolean, text: String?, callback: (String?) -> Unit) {

        val request = TranslateTextRequest()
            .withText(text)
            .withSourceLanguageCode(if (reTranslate) "en" else "ko")
            .withTargetLanguageCode(if (reTranslate) "ko" else "en")

        client.translateTextAsync(
            request,
            object : AsyncHandler<TranslateTextRequest, TranslateTextResult> {
                override fun onError(exception: Exception?) {
                    callback.invoke(exception?.message)
                }

                override fun onSuccess(
                    request: TranslateTextRequest?,
                    result: TranslateTextResult?
                ) {
                    callback.invoke(result?.translatedText)
                }
            }
        )
    }
}