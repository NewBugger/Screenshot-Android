## Screenshot

A Screenshot application for Android target 8.0+, using Media Projection (Android 5.0+) and the latest Apis which requires Android 8.0+ platform.

### Usage
use it via Notification Panel

### Details
 - package name: `io.github.newbugger.android.screenshot`
 - min SDK: `26`
 - target SDK: `28`
 - image saved location: `Environment.DIRECTORY_PICTURES/picture/`, while System Screenshot dir is in /Screenshots/
 - permission: `WRITE_EXTERNAL_STORAGE` and `FOREGROUND_SERVICE`

### TODO
 - call method improvement
 - bugfix: `logcat.Warning:: W/roid.screensho: Core platform API violation: Ljava/nio/Buffer;->address:J from Landroid/graphics/Bitmap; using JNI`
 - Storage Access Framework (compulsorily because Android 10 restriction) -> completed

### License
GPL V3
