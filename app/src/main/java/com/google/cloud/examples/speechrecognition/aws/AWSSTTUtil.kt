package com.google.cloud.examples.speechrecognition.aws

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.cloud.examples.speechrecognition.common.EtcUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.transcribe.TranscribeAsyncClient
import software.amazon.awssdk.services.transcribe.model.GetTranscriptionJobRequest
import software.amazon.awssdk.services.transcribe.model.LanguageCode
import software.amazon.awssdk.services.transcribe.model.Media
import software.amazon.awssdk.services.transcribe.model.StartTranscriptionJobRequest
import software.amazon.awssdk.services.transcribe.model.TranscriptionJobStatus
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader


object AWSSTTUtil {

    @RequiresApi(Build.VERSION_CODES.N)
    fun transcribeAudio(scope: CoroutineScope, uri: String, callback: (String) -> Unit) {
        val transcribeClient: TranscribeAsyncClient = TranscribeAsyncClient.builder()
            .credentialsProvider { AWSUtil.awsCredentials }
            .region(Region.US_EAST_1)
            .build()

        val media: Media = Media.builder().mediaFileUri(uri).build()

        val jobName = "brownik-${System.currentTimeMillis()}"

        val request: StartTranscriptionJobRequest = StartTranscriptionJobRequest.builder()
            .transcriptionJobName(jobName)
            .languageCode(LanguageCode.KO_KR) // Language code for English (US)
            .media(media)
            .outputBucketName(AWSUtil.textBucketName)
            .build()

        val job = transcribeClient.startTranscriptionJob(request)
        job.whenComplete { r, e ->
            if (r != null) {
                EtcUtil.makeLog("Job ID: ${r.transcriptionJob().transcriptionJobName()}")
                while (true) {
                    val checkRequest = GetTranscriptionJobRequest.builder()
                        .transcriptionJobName(jobName)
                        .build()
                    val response = transcribeClient.getTranscriptionJob(checkRequest).join()
                    val status = response.transcriptionJob().transcriptionJobStatus()
                    if (status == TranscriptionJobStatus.COMPLETED) {
                        callback.invoke(response.transcriptionJob().transcriptionJobName())
                        break
                    }
                    CoroutineScope(scope.coroutineContext).launch { delay(2000L) }
                }
            } else {
                EtcUtil.makeLog("Error starting transcription job: ${e?.message}")
            }
        }.join()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun readJsonFromS3(uri: String) {
        val client = S3Client.builder()
            .credentialsProvider { AWSUtil.awsCredentials }
            .region(Region.US_EAST_1)
            .build()

        val request = GetObjectRequest.builder()
            .bucket(AWSUtil.textBucketName)
            .key(uri)
            .build()

        val inputStream: InputStream = client.getObject(request)

        val reader = BufferedReader(InputStreamReader(inputStream))
        val content = StringBuilder()
        var line = reader.readLine()

        while (line != null) {
            content.append(line)
            line = reader.readLine()
        }

        EtcUtil.makeLog("JSON Content: $content")
    }
}