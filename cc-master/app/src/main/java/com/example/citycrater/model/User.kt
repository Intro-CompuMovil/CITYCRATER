package com.example.citycrater.model

class User {
    private lateinit var name: String
    private lateinit var email: String
    private lateinit var username: String
    private lateinit var password: String // Remember to store passwords securely (hashed)
    private lateinit var usertype: String

    constructor(name: String, email: String, username: String, password: String, usertype: String) {
        this.name = name
        this.email = email
        this.username = username
        this.password = password
        this.usertype = usertype
    }


}