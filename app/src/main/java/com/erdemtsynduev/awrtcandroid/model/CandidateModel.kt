package com.erdemtsynduev.awrtcandroid.model

import com.google.gson.annotations.SerializedName

data class CandidateModel(
    @SerializedName("candidate") val candidate: String? = null,
    @SerializedName("sdpMLineIndex") val sdpMLineIndex: Int? = null,
    @SerializedName("sdpMid") val sdpMid: String? = null
)