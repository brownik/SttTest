package com.google.cloud.examples.speechrecognition.google

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.examples.speechrecognition.R
import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import java.nio.file.Files
import java.nio.file.Paths

object GoogleStorage {

    var storage: Storage? = null

    val bucketName = "brownik-audiofile"
    val fileName = "test.mp4"

    val gsUri = "gs://$bucketName/test.flac"

    fun initStorage(context: Context) {
        val credentials = GoogleCredentials.fromStream(context.resources.openRawResource(R.raw.credential))
//        storage = StorageOptions.getDefaultInstance().service
        storage = StorageOptions.newBuilder().setCredentials(credentials).build().service
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveFile(path: String, callback: (String?) -> Unit) {
        Thread {
            val bucket = storage?.get(bucketName)
            val blobId = BlobId.of(bucket?.name, fileName)
            val blob = storage?.create(
                Blob.newBuilder(blobId)
                    .setContentType("audio/mp4")
                    .build(),
                Files.readAllBytes(Paths.get(path))
            )
            callback.invoke(blob?.selfLink)
        }.start()
    }

    fun getUri(callback: (String) -> Unit) {
        Thread {
            val blobId = BlobId.of(bucketName, fileName)
            storage?.get(blobId)
            callback.invoke("gs://$bucketName/$fileName")
        }.start()
    }
}