package com.meetfriend.app.api.places.model

import com.google.gson.annotations.SerializedName

data class PlaceSearchResponse(
    @field:SerializedName("html_attributions")
    val htmlAttributions: ArrayList<String> = arrayListOf(),

    @field:SerializedName("next_page_token")
    val nextPageToken: String? = null,

    @field:SerializedName("results")
    val results: ArrayList<ResultResponse> = arrayListOf(),

    @field:SerializedName("status")
    val status: String? = null,
)

data class ResultResponse(
    @field:SerializedName("business_status")
    val businessStatus: String? = null,

    @field:SerializedName("formatted_address")
    val formattedAddress: String? = null,

    @field:SerializedName("geometry")
    val geometry: GeoMetryResponse? = null,

    @field:SerializedName("icon")
    val icon: String? = null,

    @field:SerializedName("icon_background_color")
    val iconBackgroundColor: String? = null,

    @field:SerializedName("icon_mask_base_uri")
    val iconMaskBaseUri: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("photos")
    val photos: ArrayList<PhotoResponse> = arrayListOf(),

    @field:SerializedName("place_id")
    val placeId: String? = null,

    @field:SerializedName("plus_code")
    val plusCode: PlusCodeResponse? = null,

    @field:SerializedName("rating")
    val rating: Double = 0.0,

    @field:SerializedName("reference")
    val reference: String? = null,

    @field:SerializedName("types")
    val types: ArrayList<String> = arrayListOf(),

    @field:SerializedName("user_ratings_total")
    val userRatingsTotal: Int = 0
)

data class GeoMetryResponse(
    @field:SerializedName("location")
    val location: LocationResponse? = null,

    @field:SerializedName("viewport")
    val viewport: ViewPortResponse? = null,
)

data class ViewPortResponse(
    @field:SerializedName("northeast")
    val northeast: LocationResponse? = null,

    @field:SerializedName("southwest")
    val southwest: LocationResponse? = null,
)

data class LocationResponse(
    @field:SerializedName("lat")
    val lat: Double = 0.0,

    @field:SerializedName("lng")
    val lng: Double = 0.0,
)

data class PlusCodeResponse(
    @field:SerializedName("compound_code")
    val compoundCode: String? = null,

    @field:SerializedName("global_code")
    val globalCode: String? = null,
)

data class PhotoResponse(
    @field:SerializedName("height")
    val height: Int = 0,

    @field:SerializedName("html_attributions")
    val htmlAttributions: ArrayList<String> = arrayListOf(),

    @field:SerializedName("photo_reference")
    val photoReference: String? = null,

    @field:SerializedName("width")
    val width: Int = 0,
)
