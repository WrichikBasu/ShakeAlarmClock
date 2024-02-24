## Shake Alarm Clock: Privacy policy

Welcome to the Shake Alarm Clock app for Android!

This is an open source Android app developed by Wrichik Basu. The source code is available on GitHub under the GNU AGPL license (3.0 or later).

As an avid Android user myself, I take privacy very seriously.
I know how frustrating it is when apps collect your data without your knowledge.

### Data collected by the app

I hereby state, to the best of my knowledge and belief, that I have not programmed this app to collect any personally identifiable information. All data (app preferences (like theme) and alarms) created by the you (the user) is stored locally in your device only, and can be simply erased by clearing the app's data or uninstalling it. No analytics software is present in the app either.

### Explanation of permissions requested in the app

The list of permissions required by the app can be found in the `AndroidManifest.xml` file:

https://github.com/WrichikBasu/ShakeAlarmClock/blob/d7f323c7769ad600d4f97eb499f3d7fa34a2a1fc/app/src/main/AndroidManifest.xml#L21-L38
<br/>

| Permission | Why it is required |
| :---: | --- |
| `android.permission.USE_EXACT_ALARM` | Introduced in Android 13 (API level 33), this permission allows the app to set an exact alarm with the Android system, ensuring that the system will wake up from doze mode when the alarm rings. Granted by the system by default and cannot be revoked by the user. Requested in Android 13 and above. |
| `android.permission.SCHEDULE_EXACT_ALARM` | Was introduced in Android 12 and required to set an exact alarm. If your device is running Android 12, the app requests this permission to set an exact alarm. This is the same as `USE_EXACT_ALARM`, except that you, the user, or the system, can revoke this permission at any time from Settings. Revoking this permission will, however, kill the app immediately if it was in foreground, and cancel all alarms set by the app. |
| `com.android.alarm.permission.SET_ALARM` | Basic permission required to set alarms, whether exact or inexact. Automatically granted by the system; cannot be revoked by user. |
| `android.permission.READ_MEDIA_AUDIO` (API level >= 33) and `android.permission.READ_EXTERNAL_STORAGE` | An optional permission. Has to be granted by the user manually; can be revoked by the system or the user at any time. This is required only if you want to set a custom alarm tone.  In some devices, the app may function properly if you choose an alarm tone from the default set of alarm tones provided by the device. However, if you want to select a song or a tone that is not supplied by your device by default, then this permission is absolutely essential. Besides, tests reveal that setting a tone other than the default alarm tone and revoking this permission later _often_ results in the app not being able to access the custom tone when the alarm rings, because the app is not permitted by the system to read the tone file. In that case, the app will fallback to the default alarm tone of the device when the alarm rings. `READ_MEDIA_AUDIO` is a granular permission introduced in API level 33, and is, therefore, requested in Android 13 and higher. For lower Android versions upto Android 12, the app requests `READ_EXTERNAL_STORAGE`. |
| `android.permission.POST_NOTIFICATIONS` | Required by the app to post notifications. Has to be granted by the user manually; can be revoked by the system or the user at any time. It is highly recommended that you allow this permission so that the app can show the alarm dismissal screen when the alarm rings. |
| `android.permission.ACCESS_NOTIFICATION_POLICY` | Allows the app to change the Do Not Disturb settings of the device to make sure that an alarm rings. It is an optional permission. If not granted, the app will respect the DND settings of your device when ringing the alarm. |
| `android.permission.VIBRATE` | Required to vibrate the device when alarm is ringing. Permission automatically granted by the system; can't be revoked by user. |
| `android.permission.WAKE_LOCK` , `android.permission.DISABLE_KEYGUARD` and `android.permission.USE_FULL_SCREEN_INTENT` | Required to the show alarm screen when an alarm is ringing. Automatically granted by the system; cannot be revoked by user. |
| `android.permission.FOREGROUND_SERVICE` | Enables the app to create foreground services that will ring the alarm and keep it snoozed. Permission automatically granted by the system; can't be revoked by user. |
| `android.permission.FOREGROUND_SERVICE_SPECIAL_USE` | Conveys to the system the type of foreground Service that the app uses. Cannot be revoked by the user. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | When your device restarts, all alarms set in the system are lost. This permission enables the app to receive a message from the system once the system has rebooted and you have unlocked your device the first time. When this message is received, the app creates a service to set all the active alarms in the system.|

 <hr style="border:1px solid gray">

If you find any security vulnerability that has been inadvertently caused by me, or have any question regarding how the app protectes your privacy, please send me an email or post a discussion on GitHub, and I will surely try to fix it/help you.

Yours sincerely,  
Wrichik Basu.  
Kolkata, India.  
basulabs.developer@gmail.com
