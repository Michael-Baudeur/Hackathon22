/*
 * Copyright 2022 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.samples.codescanner.kotlin

/*import org.jetbrains.anko.doAsync
import org.jetbrains.anko.longToast
import org.jetbrains.anko.uiThread*/
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.common.MlKitException
import com.google.mlkit.samples.codescanner.R
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL
import java.util.*


/** Demonstrates the code scanner powered by Google Play Services. */
class MainActivity : AppCompatActivity() {
  var barCodeValue: String = ""
  var product_data: String = ""

  //API code
  /*private fun executeCall() {
    launch(Dispatchers.Main) {
      try {
        val response = ApiClient.apiService.getPostById(1)

        if (response.isSuccessful && response.body() != null) {
          val content = response.body()
  //do something
        } else {
          Toast.makeText(
            this@MainActivity,
            "Error Occurred: ${response.message()}",
            Toast.LENGTH_LONG
          ).show()
        }

      } catch (e: Exception) {
        Toast.makeText(
          this@MainActivity,
          "Error Occurred: ${e.message}",
          Toast.LENGTH_LONG
        ).show()
      }
    }
  }*/
  //--------------------------

  fun get_product_data(barCode: String): String {
    val productURL = "https://world.openfoodfacts.org/api/v0/product/$barCode.json"
    //val productURL = "https://pastebin.com/raw/2bW31yqa"
    var apiResponse = ""

    try {
      if(isNetworkConnected()){
        val url: URL? = try {
          URL(productURL)
        }catch (e: MalformedURLException){
          Log.d("Exception", e.toString())
          null
        }
        url?.let { findViewById<TextView>(R.id.products_data).text = it.toString() }

        // io dispatcher for networking operation
        lifecycleScope.launch(Dispatchers.IO) {
          url?.getString()?.apply {

            // default dispatcher for json parsing, cpu intensive work
            withContext(Dispatchers.Default) {
              val list = parseJson(this@apply)

              // main dispatcher for interaction with ui objects
              withContext(Dispatchers.Main) {
                findViewById<TextView>(R.id.products_data).append("\n\nReading data from json....\n")

                list?.forEach {
                  findViewById<TextView>(R.id.products_data).append(
                    "\n${it.packaging}"
                  )
                }

              }

            }
          }
        }
      }
      else{
        AlertDialog.Builder(this).setTitle("No Internet Connection")
          .setMessage("Please check your internet connection and try again")
          .setPositiveButton(android.R.string.ok) { _, _ -> }
          .setIcon(android.R.drawable.ic_dialog_alert).show()
      }

    } catch (e: Exception) {
      e.printStackTrace()
      apiResponse = e.toString()
    }



    val productDataView = findViewById<TextView>(R.id.products_data)
    productDataView.text = apiResponse
    return apiResponse
  }

  override fun onCreate(savedInstanceState: Bundle?) {

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val barcodeResultView = findViewById<TextView>(R.id.barcode_result_view)
    findViewById<View>(R.id.scan_barcode_button).setOnClickListener {
      val gmsBarcodeScanner = GmsBarcodeScanning.getClient(this)
      gmsBarcodeScanner
        .startScan()
        .addOnSuccessListener { barcode: Barcode ->
          barcodeResultView.text = getSuccessfulMessage(barcode)
          get_product_data(barCodeValue)
        }
        .addOnFailureListener { e: Exception ->
          barcodeResultView.text = getErrorMessage(e as MlKitException)
        }
    }
  }

  private fun getSuccessfulMessage(barcode: Barcode): String {
    barCodeValue = String.format("%s",barcode.rawValue)
    val barcodeValue =
      String.format(
        Locale.FRANCE,
        "Display Value: %s\nRaw Value: %s\nFormat: %s\nValue Type: %s",
        barcode.displayValue,
        barcode.rawValue,
        barcode.format,
        barcode.valueType
      )
    return getString(R.string.barcode_result, barcodeValue)
  }

  private fun getErrorMessage(e: MlKitException): String {
    return when (e.errorCode) {
      MlKitException.CODE_SCANNER_CANCELLED -> getString(R.string.error_scanner_cancelled)
      MlKitException.CODE_SCANNER_CAMERA_PERMISSION_NOT_GRANTED ->
        getString(R.string.error_camera_permission_not_granted)
      MlKitException.CODE_SCANNER_APP_NAME_UNAVAILABLE ->
        getString(R.string.error_app_name_unavailable)
      else -> getString(R.string.error_default_message, e)
    }
  }


  private fun isNetworkConnected(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = connectivityManager.activeNetwork
    val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
    return networkCapabilities != null &&
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  // extension function to get string data from url
  fun URL.getString(): String? {
    val stream = openStream()
    return try {
      val r = BufferedReader(InputStreamReader(stream))
      val result = StringBuilder()
      var line: String?
      while (r.readLine().also { line = it } != null) {
        result.append(line).appendln()
      }
      result.toString()
    }catch (e: IOException){
      e.toString()
    }
  }


  // data class to hold student instance
  data class Product(
    val packaging:String
  )


  // parse json data
  fun parseJson(data:String):List<Product>?{
    val list = mutableListOf<Product>()

    try {
      val array = JSONObject(data).getJSONArray("students")
      //list.add(Product(array1))
      //val packaging = JSONObject(data).getJSONObject("\"packaging\"").toString()
      for(i in 0 until array.length()){
        val obj = JSONObject(array[i].toString())
        val packaging = obj.getString("firstname")
      list.add(Product(packaging))
      }
    }catch (e: JSONException){
      Log.d("Exception", e.toString())
    }

    return list
  }

}
