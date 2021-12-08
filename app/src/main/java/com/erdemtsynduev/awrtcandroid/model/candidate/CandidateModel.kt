package com.erdemtsynduev.awrtcandroid.model.candidate

import com.google.gson.annotations.SerializedName

data class CandidateModel(
    @SerializedName("candidate") var candidate: String? = null,
    @SerializedName("sdpMLineIndex") var sdpMLineIndex: Int? = null,
    @SerializedName("sdpMid") var sdpMid: String? = null
)