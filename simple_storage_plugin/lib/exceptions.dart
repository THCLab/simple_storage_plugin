class NoKeyInStorageException implements Exception {
  String cause;
  NoKeyInStorageException(this.cause);
  @override
  String toString() => "NoKeyInStorageException: $cause";
}

class DeviceNotSecuredException implements Exception{
  String cause;
  DeviceNotSecuredException(this.cause);
  @override
  String toString() => "DeviceNotSecuredException: $cause";
}

class InvalidSignatureException implements Exception{
  String cause;
  InvalidSignatureException(this.cause);
  @override
  String toString() => "InvalidSignatureException: $cause";
}

