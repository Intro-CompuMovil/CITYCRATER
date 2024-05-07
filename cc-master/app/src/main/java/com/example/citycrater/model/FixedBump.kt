package com.example.citycrater.model

class FixedBump {
    var keyBump: String = ""
    var keyUser: String = ""
    var size: String = ""
    var latitude: Double = 0.0
    var longitude: Double = 0.0

    constructor() {}
    constructor(
        keyBump: String,
        keyUser: String,
        size: String,
        latitude: Double,
        longitude: Double
    ) {
        this.keyBump = keyBump
        this.keyUser = keyUser
        this.size = size
        this.latitude = latitude
        this.longitude = longitude
    }


}