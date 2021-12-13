package com.erdemtsynduev.awrtcandroid.model

import com.google.gson.annotations.SerializedName

data class OfferModel(
    @SerializedName("sdp") val sdp: String? = null,
    @SerializedName("type") val type: String? = null
)