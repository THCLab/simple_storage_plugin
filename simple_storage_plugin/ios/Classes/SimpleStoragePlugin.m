#import "SimpleStoragePlugin.h"
#if __has_include(<simple_storage_plugin/simple_storage_plugin-Swift.h>)
#import <simple_storage_plugin/simple_storage_plugin-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "simple_storage_plugin-Swift.h"
#endif

@implementation SimpleStoragePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftSimpleStoragePlugin registerWithRegistrar:registrar];
}
@end
