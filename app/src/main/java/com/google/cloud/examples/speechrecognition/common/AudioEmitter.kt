package com.google.cloud.examples.speechrecognition.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import com.google.protobuf.ByteString
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * Emits microphone audio using a background [ScheduledExecutorService].
 */
internal class AudioEmitter(private val context: Context) {

    private var mAudioRecorder: AudioRecord? = null
    private var mAudioExecutor: ScheduledExecutorService? = null
    private var sendBuffer: ByteArray? = null
    private var recordingBuffer: ByteArray? = null
    private var recording = false
    private var recordThread: Thread? = null
    val filePath = context.getExternalFilesDir(null)!!.absolutePath + "/record.wav"
    private var audioTrack: AudioTrack? = null
    private var playThread: Thread? = null

    /** Start streaming  */
    fun start(
        encoding: Int = AudioFormat.ENCODING_PCM_16BIT,
        channel: Int = AudioFormat.CHANNEL_IN_MONO,
        sampleRate: Int = 16000,
        subscriber: (ByteString) -> Unit
    ) {
        recording = true
        mAudioExecutor = Executors.newSingleThreadScheduledExecutor()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        mAudioRecorder = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(encoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channel)
                    .build()
            )
            .build()

        sendBuffer = ByteArray(2 * AudioRecord.getMinBufferSize(sampleRate, channel, encoding))

        mAudioRecorder?.startRecording()

        // stream bytes as they become available in chunks equal to the buffer size
        recordThread = Thread {
            val outputFile = File(filePath)
            val outputStream = FileOutputStream(outputFile)


//                mAudioExecutor?.scheduleAtFixedRate({
            while (recording) {
                // read audio data
                val read = mAudioRecorder?.read(
                    sendBuffer!!,
                    0,
                    sendBuffer!!.size,
                    AudioRecord.READ_BLOCKING
                ) ?: 0

                // send next chunk
                if (read > 0) {
                    subscriber(ByteString.copyFrom(sendBuffer!!, 0, read))
                }

                outputStream.write(sendBuffer!!, 0, read)
            }
//                }, 0, 10, TimeUnit.MILLISECONDS)
            outputStream.write(sendBuffer, 0, sendBuffer!!.size)
            outputStream.close()
        }

        // start!
        recordThread?.start()
    }

    /** Stop Streaming  */
    fun stop() {
        recording = false
        // stop events
        mAudioExecutor?.shutdown()
        mAudioExecutor = null

        // stop recording
        mAudioRecorder?.stop()
        mAudioRecorder?.release()
        mAudioRecorder = null
    }

    fun checkRecordingThread() = recordThread?.isAlive

    fun getAudioData(): ByteArray? {
        val file = File(filePath)
        return if (!file.exists()) {
            null
        } else {
            val inputStream = DataInputStream(FileInputStream(filePath))
            val audioData = ByteArray(file.length().toInt())
            try {
                inputStream.readFully(audioData)
            } catch (_: Exception) {

            } finally {
                inputStream.close()
            }
            audioData
        }
    }

    fun playAudioData() {
        val sampleRate = 16000
        val channel = AudioFormat.CHANNEL_OUT_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val buffer = AudioTrack.getMinBufferSize(sampleRate, channel, encoding) * 2
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            channel,
            encoding,
            buffer,
            AudioTrack.MODE_STREAM
        )

        playThread = Thread {
            val writeData = ByteArray(buffer)
            var fis: FileInputStream? = null
            try {
                fis = FileInputStream(filePath)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

            val dis = DataInputStream(fis)
            audioTrack?.play() // write 하기 전에 play 를 먼저 수행해 주어야 함


            while (true) {
                try {
                    val ret = dis.read(writeData, 0, writeData.size)
                    if (ret <= 0) {
                        break
                    }
                    audioTrack?.write(writeData, 0, ret) // AudioTrack 에 write 를 하면 스피커로 송출됨
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null

            try {
                dis.close()
                fis!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        playThread?.start()
    }
}