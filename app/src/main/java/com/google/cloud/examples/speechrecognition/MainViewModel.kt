package com.google.cloud.examples.speechrecognition

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {

    private val _recordState = MutableLiveData<Boolean>()
    val recordState: LiveData<Boolean> = _recordState

    fun changeRecordState(state: Boolean) {
        _recordState.value = state
    }
}