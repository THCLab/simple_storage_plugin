import 'dart:async';
import 'package:flutter/services.dart';
import 'package:simple_signing_plugin/simple_signing_plugin.dart';
import 'package:simple_storage_plugin/exceptions.dart';

///Main class with methods to read, write, delete and edit data.
class SimpleStoragePlugin {
  ///Dart MethodChannel to connect native code necessary for keys and shared preferences to Dart platform.
  static const MethodChannel _channel = MethodChannel('simple_storage_plugin');

  ///Writes provided data under provided key in shared preferences. Data is signed and encrypted using AES.
  ///Works only if the device has a secure screen lock set, otherwise throws an exception. Returns true if data is successfully saved.
  static Future<bool> writeData(String key, String data) async {
    bool isDeviceSecure = await SimpleSigningPlugin.checkIfDeviceSecure();
    if (isDeviceSecure) {
      var signedData = await SimpleSigningPlugin.signData(data);
      var result = await _channel
          .invokeMethod('writeData', {'key': key, 'data': signedData});
      if (result == true) {
        return true;
      } else {
        throw SharedPreferencesException(
            'Writing to shared preferences failed. Consider reopening or reinstalling the app.');
      }
    }
    throw DeviceNotSecuredException(
        'Secure lock on this device is not set up. Consider setting a pin or pattern.');
  }

  ///Reads data saved under provided key from shared preferences. First, data signature is verified and if it is valid, data are decrypted.
  ///Works only if the device has a secure screen lock set, otherwise throws an exception. Returns data if it is successfully read.
  static Future<dynamic> readData(String key) async {
    bool isDeviceSecure = await SimpleSigningPlugin.checkIfDeviceSecure();
    if (isDeviceSecure) {
      var data = await _channel.invokeMethod('readData', {'key': key});
      if (data != false) {
        bool isValid = await SimpleSigningPlugin.verifyData(data);
        if (isValid) {
          return data.toString().substring(
              data.toString().indexOf(':') + 1, data.toString().length);
        }
        throw InvalidSignatureException('Data signature is not valid.');
      } else {
        throw NoKeyInStorageException(
            'No such key found in phone storage. Consider saving it to storage before reading.');
      }
    }
    throw DeviceNotSecuredException(
        'Secure lock on this device is not set up. Consider setting a pin or pattern.');
  }

  ///Deletes data saved under provided key from shared preferences.
  ///Works only if the device has a secure screen lock set, otherwise throws an exception. Returns true if data is successfully deleted.
  static Future<bool> deleteData(String key) async {
    bool isDeviceSecure = await SimpleSigningPlugin.checkIfDeviceSecure();
    if (isDeviceSecure) {
      var result = await _channel.invokeMethod('deleteData', {'key': key});
      if (result != false) {
        return true;
      } else {
        throw SharedPreferencesException(
            'Writing to shared preferences failed. Consider reopening or reinstalling the app.');
      }
    }
    throw DeviceNotSecuredException(
        'Secure lock on this device is not set up. Consider setting a pin or pattern.');
  }

  ///Edits data under provided key in shared preferences. Data is signed and encrypted using AES.
  ///Works only if the device has a secure screen lock set, otherwise throws an exception. Returns true if data is successfully saved.
  static Future<bool> editData(String key, String data) async {
    bool isDeviceSecure = await SimpleSigningPlugin.checkIfDeviceSecure();
    if (isDeviceSecure) {
      String signedData = await SimpleSigningPlugin.signData(data);
      var result = await _channel
          .invokeMethod('editData', {'key': key, 'data': signedData});
      if (result == true) {
        return true;
      } else {
        throw SharedPreferencesException(
            'Writing to shared preferences failed. Consider reopening or reinstalling the app.');
      }
    }
    throw DeviceNotSecuredException(
        'Secure lock on this device is not set up. Consider setting a pin or pattern.');
  }
}
