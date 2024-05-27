package com.example.citycrater.model

class User {
    var name: String = ""
    var email: String = ""
    var phone: String = ""
    var password: String = ""
    var usertype: String = ""
    var photo: String = ""
    var latitude: Double = 0.0
    var longitude: Double = 0.0

    constructor() {
        this.name = ""
        this.email = ""
        this.phone = ""
        this.usertype = ""
        this.photo = ""
        this.latitude = 0.0
        this.longitude = 0.0
    }


    constructor(name: String, email: String, phone: String, usertype: String, latitude: Double, longitude: Double) {
        this.name = name
        this.email = email
        this.phone = phone
        this.usertype = usertype
        this.photo = ""
        this.latitude = latitude
        this.longitude = longitude
    }


}