# Android overlay viewer + loopback frame bridge (Android 8–15)

This is a standalone Android overlay viewer/demo. It draws normalized test rectangles after the user grants `SYSTEM_ALERT_WINDOW`, and accepts newline-delimited JSON frames over a TCP socket bound only to `127.0.0.1:39472`.

## Frame protocol

Send one UTF-8 JSON object per line:

```json
{"boxes":[{"l":0.25,"t":0.15,"r":0.45,"b":0.82,"label":"PLAYER","distance":18.0}]}
```

Coordinates are normalized to `[0,1]` with a top-left origin. `distance` is optional and measured in metres if supplied by the producer. Up to 128 boxes are accepted per frame; malformed, non-finite, degenerate, or out-of-range boxes are discarded. A line is limited to 64 KiB. The server binds to loopback only; it is not exposed to the LAN.

## Build

Open this directory in Android Studio with JDK 17 and Android SDK 35 installed. Build `:app:assembleDebug`. This environment did not have Gradle/Android SDK installed, so no APK build is claimed.

## Test

1. Install the debug APK.
2. Grant "Display over other apps".
3. Tap "Start test overlay" to show the built-in sample rectangles.
4. A local producer can connect to `127.0.0.1:39472` and send JSON lines. The socket is active while the overlay service is running.
5. Tap "Stop overlay" to stop the service and close the socket.

A same-device sender is included at `../tools/send_test_frame.py`; run it in Termux while the overlay is active.

## Boundary / integration status

The bridge transports screen-space data only. It does not load code into Oxide, access another process, bypass its protection, or install a game hook. `NativeEspCore.Reference.cpp` contains a separate reference collector whose runtime calls and main-thread integration still require exact-build/device validation. The overlay and native collector are not yet connected into a verified, installable game ESP.
