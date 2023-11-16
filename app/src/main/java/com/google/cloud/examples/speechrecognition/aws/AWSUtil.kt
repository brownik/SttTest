package com.google.cloud.examples.speechrecognition.aws

import software.amazon.awssdk.auth.credentials.AwsCredentials

object AWSUtil {
    val awsCredentials = object : AwsCredentials {
        override fun accessKeyId(): String = "AKIAR26O4CMBPKY3HRVH"
        override fun secretAccessKey(): String = "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"
    }

    val audioBucketName = "brownik-audiofile"
    val textBucketName = "brownik-textfile"
    val transcribeJobName = "brownik-${System.currentTimeMillis()}"

    fun getFileUri(name: String) = "s3://$audioBucketName/$name.m4a"
}