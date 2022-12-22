package com.example.maptestapp

import org.osmdroid.util.GeoPoint

data class PlaceData(
    val id: Int,
    val name: String,
    val geoPoint: GeoPoint,
    val address: String = "",
    val distance: String = "",
    val travelTime: String = ""
    )
