package com.example.citycrater.model

class User {
    var name: String = ""
    var email: String = ""
    var phone: String = ""
    var password: String = ""
    var usertype: String = ""
    var photo: String = ""

    constructor() {
        this.name = ""
        this.email = ""
        this.phone = ""
        this.password = ""
        this.usertype = ""
        this.photo = ""
    }


    constructor(name: String, email: String, phone: String, password: String, usertype: String) {
        this.name = name
        this.email = email
        this.phone = phone
        this.password = password
        this.usertype = usertype
        this.photo = ""
    }


}