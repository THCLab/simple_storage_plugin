
import 'dart:async';
import 'package:flutter/services.dart';
import 'package:simple_signing_plugin/simple_signing_plugin.dart';
import 'package:simple_storage_plugin/exceptions.dart';

class SimpleStoragePlugin {
  static const MethodChannel _channel = MethodChannel('simple_storage_plugin');

  static Future<bool> writeData(String key, String data) async{
    bool isDeviceSecure = await SimpleSigningPlugin.checkIfDeviceSecure();
    if(isDeviceSecure){
      try{
        var signedData = await SimpleSigningPlugin.signData(data);
        var result = await _channel.invokeMethod('writeData', {'key':key, 'data': signedData});
        if(result == true){
          return true;
        }else{
          throw SharedPreferencesException('Writing to shared preferences failed. Consider reopening or reinstalling the app.');
        }
      }on PlatformException{
        return false;
      }
    }
    throw DeviceNotSecuredException('Secure lock on this device is not set up. Consider setting a pin or pattern.');
  }

  static Future<dynamic> readData(String key) async{
    bool isDeviceSecure = await SimpleSigningPlugin.checkIfDeviceSecure();
    if(isDeviceSecure){
      try{
        var data = await _channel.invokeMethod('readData', {'key':key});
        if(data != false){
          bool isValid = await SimpleSigningPlugin.verifyData(data);
          if(isValid){
            return data.toString().substring(data.toString().indexOf(':')+1, data.toString().length);
          }
          throw InvalidSignatureException('Data signature is not valid.');
        }else{
          throw NoKeyInStorageException('No such key found in phone storage. Consider saving it to storage before reading.');
        }
      }on PlatformException {
        return false;
      }
    }
    throw DeviceNotSecuredException('Secure lock on this device is not set up. Consider setting a pin or pattern.');
  }

  static Future<bool> deleteData(String key) async{
    bool isDeviceSecure = await SimpleSigningPlugin.checkIfDeviceSecure();
    if(isDeviceSecure){
      try{
        var result = await _channel.invokeMethod('deleteData', {'key':key});
        if(result != false){
          return true;
        }else{
          throw SharedPreferencesException('Writing to shared preferences failed. Consider reopening or reinstalling the app.');
        }
      }on PlatformException {
        return false;
      }
    }
    throw DeviceNotSecuredException('Secure lock on this device is not set up. Consider setting a pin or pattern.');
  }

  static Future<bool> editData(String key, String data) async{
    bool isDeviceSecure = await SimpleSigningPlugin.checkIfDeviceSecure();
    if(isDeviceSecure){
      try{
        String signedData = await SimpleSigningPlugin.signData(data);
        var result = await _channel.invokeMethod('editData', {'key':key, 'data': signedData});
        if(result == true){
          return true;
        }else{
          throw SharedPreferencesException('Writing to shared preferences failed. Consider reopening or reinstalling the app.');
        }
      }on PlatformException {
        return false;
      }
    }
    throw DeviceNotSecuredException('Secure lock on this device is not set up. Consider setting a pin or pattern.');
  }


}
