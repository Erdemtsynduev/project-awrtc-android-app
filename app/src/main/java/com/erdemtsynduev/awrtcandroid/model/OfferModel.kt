package com.erdemtsynduev.awrtcandroid.model

import com.google.gson.annotations.SerializedName

data class OfferModel(
    @SerializedName("type") val type: String? = null,
    @SerializedName("sdp") val sdp: String? = null,
)