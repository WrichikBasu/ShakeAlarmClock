package in.basulabs.shakealarmclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;

import androidx.core.content.ContextCompat;

import java.util.Objects;

import static android.content.Context.POWER_SERVICE;

public class AlarmBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		if (Objects.equals(intent.getAction(), ConstantsAndStatics.ACTION_DELIVER_ALARM)) {

			PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
			PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					"in.basulabs.shakealarmclock::AlarmRingServiceWakelockTag");
			wakeLock.acquire(60000);

			Intent intent1 = new Intent(context, Service_RingAlarm.class);
			intent1.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS,
					Objects.requireNonNull(intent.getExtras())
							.getBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS));
			ContextCompat.startForegroundService(context, intent1);

			/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				context.startForegroundService(intent1);
			} else {
				context.startService(intent1);
			}*/

		} else if (Objects.equals(intent.getAction(), ConstantsAndStatics.ACTION_CREATE_BACKGROUND_SERVICE)) {

			SharedPreferences sharedPreferences =
					context.getSharedPreferences(ConstantsAndStatics.SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);

			if (sharedPreferences.getBoolean(ConstantsAndStatics.SHARED_PREF_KEY_WAS_APP_RECENTLY_ACTIVE, true)) {
				Intent intent1 = new Intent(context, Service_AlarmActivater.class);
				context.startService(intent1);

				SharedPreferences.Editor editor = sharedPreferences.edit()
						.remove(ConstantsAndStatics.SHARED_PREF_KEY_WAS_APP_RECENTLY_ACTIVE)
						.putBoolean(ConstantsAndStatics.SHARED_PREF_KEY_WAS_APP_RECENTLY_ACTIVE, false);
				editor.commit();
			}

		} else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)
				|| intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)
				|| intent.getAction().equals(Intent.ACTION_TIME_CHANGED)) {

			PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
			PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					"in.basulabs.shakealarmclock::AlarmUpdateServiceWakelockTag");
			wakeLock.acquire(60000);

			Intent intent1 = new Intent(context, Service_UpdateAlarm.class);
			ContextCompat.startForegroundService(context, intent1);
			/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				context.startForegroundService(intent1);
			} else {
				context.startService(intent1);
			}*/
		}

	}
}
