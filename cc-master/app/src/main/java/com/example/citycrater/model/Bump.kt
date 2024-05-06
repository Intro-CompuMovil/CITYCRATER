package com.example.citycrater.model

class Bump {
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var size: String = ""

    constructor() {}
    constructor(latitude: Double, longitude: Double, size: String) {
        this.latitude = latitude
        this.longitude = longitude
        this.size = size
    }
}