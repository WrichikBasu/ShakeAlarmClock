## Shake Alarm Clock: Privacy policy

Welcome to the Shake Alarm Clock app for Android!

This is an open source Android app developed by Wrichik Basu. The source code is available on GitHub under the MIT license; the app is also available on Google Play.

As an avid Android user myself, I take privacy very seriously.
I know how irritating it is when apps collect your data without your knowledge.

I hereby state, to the best of my knowledge and belief, that I have not programmed this app to collect any personally identifiable information. All data (app preferences (like theme, etc.) and alarms) created by the you (the user) is stored on your device only, and can be simply erased by clearing the app's data or uninstalling it.

### Explanation of permissions requested in the app

The list of permissions required by the app can be found in the `AndroidManifest.xml` file:

https://github.com/WrichikBasu/ShakeAlarmClock/blob/1031bad5edd2e73eda091cd1e84746f4710c7528/app/src/main/AndroidManifest.xml#L7-L15

<br/>

| Permission | Why it is required |
| :---: | --- |
| `android.permission.SCHEDULE_EXACT_ALARM` | This is required to schedule an exact alarm, and was introduced in Android 12. You, as the user, or the system, can revoke this permission at any time from Settings. Revoking this permission will, however, kill the app immediately if it was alive, and cancel all alarms set by the app. |
| `android.permission.VIBRATE` | Required to vibrate the device when alarm is ringing. Permission automatically granted by the system; can't be revoked by user. |
| `android.permission.WAKE_LOCK` and `android.permission.DISABLE_KEYGUARD` | Required to show alarm screen when alarm is ringing. Permission automatically granted by the system; can't be revoked by user. |
| `android.permission.FOREGROUND_SERVICE` | Enables the app to create foreground services that will ring the alarm and keep it snoozed. Permission automatically granted by the system; can't be revoked by user. |
| `android.permission.SET_ALARM` | Required to set alarms, whether exact or inexact. Permission automatically granted by the system; can't be revoked by user. |
| `android.permission.READ_EXTERNAL_STORAGE` | The only sensitive permission that the app requests, and can be revoked by the system or the user at any time. This is required only if you want to set a custom alarm tone that is different from the default alarm tone of your phone. In order to read tones other than the default ones, the app needs permission to read the storage. Setting a custom tone but revoking this permission later _may_ cause the app to crash at the time the alarm rings when a security exception occurs while reading the tone. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | When your device restarts, all alarms set in the system are lost. This permission enables the app to receive a message from the system once the system has rebooted and you have unlocked your device the first time. When this message is received, the app creates a service to set all the active alarms in the system.|

 <hr style="border:1px solid gray">

If you find any security vulnerability that has been inadvertently caused by me, or have any question regarding how the app protectes your privacy, please send me an email or post a discussion on GitHub, and I will surely try to fix it/help you.

Yours sincerely,  
Wrichik Basu.  
Kolkata, India.  
basulabs.developer@gmail.com
