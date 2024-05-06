package com.example.citycrater.mapsUtils

import android.app.Activity
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets

class MapManager {
    companion object{
        val jsonFile = "reparados.json"
        fun getAddressFromCoordinates(latitude: Double, longitude: Double): String {
            val client = OkHttpClient()
            val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude&zoom=17&addressdetails=1"

            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            var displayName = ""

            // Procesado del json recibido especificando el atributo requerido
            val gson = Gson()
            val jsonObject = gson.fromJson(responseBody, com.google.gson.JsonObject::class.java)
            displayName = jsonObject.getAsJsonPrimitive("display_name").asString

            return displayName
        }

        fun loadJSONFromAsset(activity: Activity): String? {
            try {
                val assets = activity.assets
                val inputStream = assets.open(jsonFile)
                val buffer = ByteArray(inputStream.available())
                inputStream.read(buffer)
                inputStream.close()
                return String(buffer, StandardCharsets.UTF_8)
            } catch (ex: IOException) {
                ex.printStackTrace()
                return null
            }
        }

        fun getFacts(activity: Activity): JSONArray {
            val json = JSONObject(loadJSONFromAsset(activity))
            return json.getJSONArray("locations")
        }

        fun buildAdapter(activity: Activity): MutableList<String> {
            try {
                val jsonArray = getFacts(activity)
                val locationArr = mutableListOf<String>()

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)

                    val latitude: String = jsonObject.getString("latitude")
                    val logitude: String = jsonObject.getString("logitude")

                    locationArr.add(latitude + ";" + logitude)

                }
                return locationArr
            } catch (ex: Exception) {
                ex.printStackTrace()
                return mutableListOf()
            }
        }

    }
}