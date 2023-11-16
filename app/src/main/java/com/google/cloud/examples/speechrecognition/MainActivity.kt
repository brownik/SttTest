/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.examples.speechrecognition

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.api.gax.rpc.ResponseObserver
import com.google.api.gax.rpc.StreamController
import com.google.cloud.examples.speechrecognition.aws.AWSTranslationUtil
import com.google.cloud.examples.speechrecognition.aws.AWSUtil
import com.google.cloud.examples.speechrecognition.aws.AWSS3Util
import com.google.cloud.examples.speechrecognition.aws.AWSSTTUtil
import com.google.cloud.examples.speechrecognition.aws.TranscribeData
import com.google.cloud.examples.speechrecognition.common.AudioEmitter
import com.google.cloud.examples.speechrecognition.common.EtcUtil
import com.google.cloud.examples.speechrecognition.common.VoiceRecorder
import com.google.cloud.examples.speechrecognition.databinding.ActivityMainBinding
import com.google.cloud.examples.speechrecognition.google.GoogleSTTUtil
import com.google.cloud.examples.speechrecognition.google.GoogleStorage
import com.google.cloud.examples.speechrecognition.google.GoogleTranslateUtil
import com.google.cloud.speech.v1.StreamingRecognizeResponse
import com.google.gson.Gson

private const val TAG = "qwe123"

class MainActivity : AppCompatActivity() {

    companion object {
        private val PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
        )
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

        private val storagePermissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            } else {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private var mPermissionToRecord = false
    private var mAudioEmitter: AudioEmitter? = null
    private val mVoiceRecorder: VoiceRecorder by lazy { VoiceRecorder(applicationContext) }

    private val responseObserver by lazy {
        object : ResponseObserver<StreamingRecognizeResponse> {
            override fun onStart(controller: StreamController?) {}

            override fun onError(t: Throwable?) {
                EtcUtil.makeLog("stream error: ${t?.message}")
            }

            override fun onComplete() {
                EtcUtil.makeLog("stream closed")
            }

            override fun onResponse(response: StreamingRecognizeResponse) {
                runOnUiThread {
                    when {
                        response.resultsCount > 0 -> {
                            val text = "${binding.sttText.text} ${
                                response.getResults(0).getAlternatives(0).transcript
                            }"
                            binding.sttText.text = text

                            GoogleTranslateUtil.translateText(false, text) {

                            }
                        }

                        else -> binding.sttText.text = getString(R.string.api_error)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // get permissions
        ActivityCompat.requestPermissions(
            this,
            PERMISSIONS + storagePermissions,
            REQUEST_RECORD_AUDIO_PERMISSION
        )

        addOnClickListener()
        addObserver()
        GoogleStorage.initStorage(this@MainActivity)
        GoogleSTTUtil.initClient(this@MainActivity)
        GoogleTranslateUtil.initClient(this@MainActivity)
    }

    override fun onPause() {
        super.onPause()

        // ensure mic data stops
        mAudioEmitter?.stop()
        mAudioEmitter = null
    }

    override fun onDestroy() {
        super.onDestroy()

        // cleanup
        GoogleSTTUtil.shutDown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            mPermissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED
        }

        // bail out if audio recording is not available
        if (!mPermissionToRecord) {
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addOnClickListener() = with(binding) {
        recordBtn.setOnClickListener {
            viewModel.changeRecordState(viewModel.recordState.value != true)
        }

        googleBtn.setOnClickListener {
            GoogleStorage.saveFile(mVoiceRecorder.recordingFilePath) { path ->
                GoogleSTTUtil.googleStt(GoogleStorage.gsUri) { text ->
                    EtcUtil.makeLog("Google-STT: $text")
                    binding.sttText.text = text
                    GoogleTranslateUtil.translateText(false, text) { first ->
                        EtcUtil.makeLog("Google-first: $first")
                        binding.firstTranslateText.text = first
                        GoogleTranslateUtil.translateText(true, first) { second ->
                            EtcUtil.makeLog("Google-second: $second")
                            binding.secondTranslateText.text = second
                        }
                    }
                }
            }
        }

        awsBtn.setOnClickListener {
            val name = AWSUtil.transcribeJobName
            EtcUtil.makeLog("jobName: $name")

            AWSS3Util.uploadFile(name, mVoiceRecorder.recordingFilePath) {
                if (it) {
                    AWSSTTUtil.transcribeAudio(
                        lifecycleScope,
                        AWSUtil.getFileUri(name)
                    ) { key ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            AWSS3Util.readJsonFromS3(key) { result ->
                                val data = Gson().fromJson(result, TranscribeData::class.java)
                                val text = data.results.transcripts.first().transcript
                                EtcUtil.makeLog("AWS-STT: $text")
                                binding.sttText.text = text

                                AWSTranslationUtil.textTranslation(false, text) { first ->
                                    EtcUtil.makeLog("AWS-first: $first")
                                    binding.firstTranslateText.text = first
                                    AWSTranslationUtil.textTranslation(true, first) { second ->
                                        EtcUtil.makeLog("AWS-second: $second")
                                        binding.secondTranslateText.text = second
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun addObserver() = with(viewModel) {
        recordState.observer {
            var color = R.color.black
            var text = "녹음 시작"
            if (it) {
                color = R.color.colorAccent
                text = "녹음 정지"

                mVoiceRecorder.recorderStart()
            } else {
                mVoiceRecorder.recorderStop()
                responseObserver.onComplete()
            }

            binding.recordBtn.apply {
                setTextColor(ActivityCompat.getColor(applicationContext, color))
                this.text = text
            }
        }
    }

    private fun <T> LiveData<T>.observer(callback: (T) -> Unit) = observe(
        this@MainActivity,
        Observer { callback.invoke(it) }
    )

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}