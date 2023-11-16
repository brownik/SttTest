package com.google.cloud.examples.speechrecognition.aws

import com.google.gson.annotations.SerializedName

data class TranscribeData(
    @SerializedName("jobName") val jobName: String,
    @SerializedName("accountId") val accountId: String,
    @SerializedName("status") val status: String,
    @SerializedName("results") val results: TranscribeResultData
)

data class TranscribeResultData(
    @SerializedName("transcripts") val transcripts: List<TranscriptData>,
    @SerializedName("items") val items: List<TranscribeResultItemData>
)

data class TranscriptData(
    @SerializedName("transcript") val transcript: String
)

data class TranscribeResultItemData(
    @SerializedName("type") val type: String,
    @SerializedName("alternatives") val alternatives: List<TranscribeResultItemAlternativeData>,
    @SerializedName("start_time") val startTime: String?,
    @SerializedName("end_time") val endTime: String?
)

data class TranscribeResultItemAlternativeData(
    @SerializedName("confidence") val confidence: String,
    @SerializedName("content") val content: String,
)