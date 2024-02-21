/*
Copyright (C) 2024  Wrichik Basu (basulabs.developer@gmail.com)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package in.basulabs.shakealarmclock.backend;

import static android.content.Context.POWER_SERVICE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import androidx.core.content.ContextCompat;

import java.util.Objects;

import in.basulabs.shakealarmclock.backend.ConstantsAndStatics;
import in.basulabs.shakealarmclock.backend.Service_RingAlarm;
import in.basulabs.shakealarmclock.backend.Service_SetAlarmsPostBoot;

public class AlarmBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		if (Objects.equals(intent.getAction(),
			ConstantsAndStatics.ACTION_DELIVER_ALARM)) {

			PowerManager powerManager = (PowerManager) context.getSystemService(
				POWER_SERVICE);
			PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK,
				"in.basulabs.shakealarmclock::AlarmRingServiceWakelockTag");
			wakeLock.acquire(60000);

			Intent intent1 = new Intent(context, Service_RingAlarm.class)
				.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS,
					Objects.requireNonNull(intent.getExtras())
						.getBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS));
			ContextCompat.startForegroundService(context, intent1);

		} else if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED) ||
			Objects.equals(intent.getAction(), Intent.ACTION_LOCKED_BOOT_COMPLETED)) {

			PowerManager powerManager = (PowerManager) context.getSystemService(
				POWER_SERVICE);
			PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK,
				"in.basulabs.shakealarmclock::AlarmUpdateServiceWakelockTag");
			wakeLock.acquire(60000);

			Intent intent1 = new Intent(context, Service_SetAlarmsPostBoot.class);
			ContextCompat.startForegroundService(context, intent1);
		}

	}
}
