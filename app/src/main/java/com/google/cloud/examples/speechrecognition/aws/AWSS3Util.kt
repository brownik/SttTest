package com.google.cloud.examples.speechrecognition.aws

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.cloud.examples.speechrecognition.aws.AWSUtil.awsCredentials
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.nio.charset.StandardCharsets

object AWSS3Util {

    private val s3Client = S3AsyncClient.builder()
        .credentialsProvider { awsCredentials }
        .region(Region.US_EAST_1)
        .build()

    @RequiresApi(Build.VERSION_CODES.N)
    fun uploadFile(key: String, filePath: String?, callback: (Boolean) -> Unit) {
        val request = PutObjectRequest.builder()
            .bucket(AWSUtil.audioBucketName)
            .key("$key.m4a")
            .build()

        val file = File(filePath ?: "")
        val requestBody = AsyncRequestBody.fromFile(file)

        s3Client.putObject(request, requestBody).whenComplete { r, _ ->
            callback.invoke(r != null)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun readJsonFromS3(key: String, callback: (String?) -> Unit) {
        val request = GetObjectRequest.builder()
            .bucket(AWSUtil.textBucketName)
            .key("$key.json")
            .build()
        s3Client.getObject(
            request,
            AsyncResponseTransformer.toBytes()
        ).thenApply {
            String(it.asByteArray(), StandardCharsets.UTF_8)
        }.whenComplete { r, e ->
            callback.invoke(if (e != null) e.message else r)
        }
    }
}