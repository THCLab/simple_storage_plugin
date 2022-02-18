package com.thc.simple_storage_plugin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.widget.Toast
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/** SimpleStoragePlugin */
class SimpleStoragePlugin: FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener{
  private lateinit var channel : MethodChannel
  private lateinit var context: Context
  private lateinit var activity: Activity
  private var keyToSign: String = ""


  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "simple_storage_plugin")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext

    if (!checkAESKeyExists()) {
      createAESKey()
    }
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "writeData"){
      val key = call.argument<String>("key")
      val dataToWrite = call.argument<String>("data")
      if (key != null && dataToWrite != null) {
        writeData(key, dataToWrite)
        result.success(true)
      }else{
        result.success(false)
      }
    }
    else if (call.method == "readData"){
      val key = call.argument<String>("key")
      if(key != null){
        val userData = readData(key)
        if(userData != false){
          result.success(userData)
        }else{
          result.success(false)
        }
      }
    }
    else if (call.method == "deleteData"){
      val key = call.argument<String>("key")
      if (key != null) {
        deleteData(key)
        result.success(true)
      }else{
        result.success(false)
      }
    }
    else if (call.method == "editData"){
      val key = call.argument<String>("key")
      val dataToWrite = call.argument<String>("data")
      if (key != null && dataToWrite != null) {
        editData(key, dataToWrite)
        result.success(true)
      }else{
        result.success(false)
      }
    }
    else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  private fun writeData(key: String, data: String){
    try{
      val encryptedData = encrypt(data)
      val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return
      with (sharedPref.edit()) {
        putString(key, encryptedData)
        apply()
      }
    }catch (e: Exception){
      Toast.makeText(context, "Something went wrong, try again!", Toast.LENGTH_SHORT).show()
    }
  }

  private fun readData(key: String): Any {
    val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
    val textToRead : String? = sharedPref.getString(key, null)
    if(textToRead.isNullOrEmpty()){
      return false
    }else{
      val userData = decrypt(textToRead)
      if(userData != null){
        return userData
      }
      Toast.makeText(context, "Signature not valid!", Toast.LENGTH_LONG).show()
      return false
    }
  }

  private fun deleteData(key: String){
    try{
      val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return
      with (sharedPref.edit()) {
        remove(key)
        apply()
      }
    }catch (e: Exception){
      Toast.makeText(context, "Something went wrong, try again!", Toast.LENGTH_SHORT).show()
    }
  }

  private fun editData(key: String, data: String){
    keyToSign = key
    try{
      val encryptedStringConcat = encrypt(data)
      val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return
      with (sharedPref.edit()) {
        putString(key, encryptedStringConcat)
        apply()
      }
    }catch (e: Exception){
      Toast.makeText(context, "Something went wrong, try again!", Toast.LENGTH_SHORT).show()
    }
  }

  //FUNCTION TO ENCRYPT DATA WHEN WRITTEN INTO STORAGE
  private fun encrypt(strToEncrypt: String) :  String? {
    try
    {
      val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
      }
      //We get the aes key from the keystore if they exists
      val secretKey = keyStore.getKey(ANDROID_AES_ALIAS, null) as SecretKey
      var result = ""
      val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
      cipher.init(Cipher.ENCRYPT_MODE, secretKey)
      val iv = cipher.iv
      val ivString = Base64.encodeToString(iv, Base64.DEFAULT)
      result += Base64.encodeToString(cipher.doFinal(strToEncrypt.toByteArray(Charsets.UTF_8)), Base64.DEFAULT)
      result += IV_SEPARATOR + ivString
      println(result)
      return result
    }
    catch (e: Exception) {
    }
    return null
  }

  //FUNCTION TO DECRYPT DATA WHEN READ FROM STORAGE
  private fun decrypt(strToDecrypt : String) : String? {
    try{
      val split = strToDecrypt.split(IV_SEPARATOR.toRegex())
      val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
      }
      val ivString = split[1]
      val encodedData = split[0]
      //We get the aes key from the keystore if they exists
      val secretKey = keyStore.getKey(ANDROID_AES_ALIAS, null) as SecretKey
      val ivSpec = IvParameterSpec(Base64.decode(ivString, Base64.DEFAULT))
      val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")

      cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
      return  String(cipher.doFinal(Base64.decode(encodedData, Base64.DEFAULT)))
    }catch (e: Exception) {
    }
    return null
  }

  //FUNCTION TO CREATE AES KEY FOR ENCRYPTION AND DECRYPTION
  private fun createAESKey() {
    val keyGenerator = KeyGenerator.getInstance(
      KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
    )
    keyGenerator.init(
      KeyGenParameterSpec.Builder(
        ANDROID_AES_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
        .build()
    )
    keyGenerator.generateKey()
  }

  //FUNCTION TO CHECK IF KEY FOR ENCRYPTION AND DECRYPTION EXISTS
  private fun checkAESKeyExists() :Boolean{
    val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
      load(null)
    }
    //We get the aes key from the keystore if they exists
    val secretKey = keyStore.getKey(ANDROID_AES_ALIAS, null) as SecretKey?
    return secretKey != null
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    binding.addActivityResultListener(this)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    TODO("Not yet implemented")
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    TODO("Not yet implemented")
  }

  override fun onDetachedFromActivity() {
    TODO("Not yet implemented")
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    return true
  }
}
//KEYSTORE NAME
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
//ENCRYPT/DECRYPT KEY ALIAS
private const val ANDROID_AES_ALIAS = "UserAESKey"
//IV STRING SEPARATOR
private const val IV_SEPARATOR = ";"
