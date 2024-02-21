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

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import in.basulabs.shakealarmclock.R;
import in.basulabs.shakealarmclock.frontend.Activity_RequestPermIntro;

public class Worker_ActivateAlarms extends Worker {

	Context context;

	private boolean stopExecuting;

	//---------------------------------------------------------------------------------

	public Worker_ActivateAlarms(@NonNull Context context,
		@NonNull WorkerParameters workerParams) {
		super(context, workerParams);
		this.context = context;
	}

	//---------------------------------------------------------------------------------

	@NonNull
	@Override
	public Result doWork() {

		stopExecuting = false;

		if (Service_RingAlarm.isThisServiceRunning ||
			Service_SnoozeAlarm.isThisServiceRunning) {
			return Result.failure();
		} else {
			return activateAlarmsIfInactive();
		}
	}

	//--------------------------------------------------------------------------------

	/**
	 * Activates the alarms that are ON, but inactive because {@link AlarmManager} has
	 * cancelled them for no reason.
	 */
	private Result activateAlarmsIfInactive() {

		final AlarmManager alarmManager = (AlarmManager) context.getSystemService(
			Context.ALARM_SERVICE);
		final AlarmDatabase alarmDatabase = AlarmDatabase.getInstance(context);

		List<AlarmEntity> list = alarmDatabase.alarmDAO().getActiveAlarms();

		if (list != null && list.size() > 0) {

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				if (!ConstantsAndStatics.getEssentialPerms(context).isEmpty()) {
					postMissingPermNotif();
					return Result.failure();
				}
			}

			for (AlarmEntity alarmEntity : list) {

				AtomicReference<ArrayList<Integer>> repeatDaysAtomic
					= new AtomicReference<>();

				alarmDatabase.alarmDAO().getAlarmRepeatDays(alarmEntity.alarmID);

				ArrayList<Integer> repeatDays = repeatDaysAtomic.get();

				Intent intent = new Intent(context.getApplicationContext(),
					AlarmBroadcastReceiver.class);
				intent.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM);
				intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

				Bundle data = alarmEntity.getAlarmDetailsInABundle();
				data.putIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS,
					repeatDays);
				intent.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, data);

				int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
					PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
					: PendingIntent.FLAG_NO_CREATE;

				PendingIntent pendingIntent = PendingIntent.getBroadcast(
					context.getApplicationContext(), alarmEntity.alarmID, intent, flags);

				if (pendingIntent == null) {

					LocalDateTime alarmDateTime = ConstantsAndStatics.getAlarmDateTime(
						LocalDate.of(alarmEntity.alarmYear, alarmEntity.alarmMonth,
							alarmEntity.alarmDay),
						LocalTime.of(alarmEntity.alarmHour, alarmEntity.alarmMinutes),
						alarmEntity.isRepeatOn,
						repeatDays);

					ZonedDateTime zonedDateTime = ZonedDateTime.of(alarmDateTime,
						ZoneId.systemDefault());

					int flags2 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
						? PendingIntent.FLAG_IMMUTABLE
						: 0;

					PendingIntent pendingIntent1 = PendingIntent.getBroadcast(
						context.getApplicationContext(), alarmEntity.alarmID, intent,
						flags2);

					alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(
							zonedDateTime.toEpochSecond() * 1000, pendingIntent1),
						pendingIntent1);

				}

				if ((stopExecuting && !isStopped()) ||
					Service_RingAlarm.isThisServiceRunning ||
					Service_SnoozeAlarm.isThisServiceRunning) {
					return Result.failure();
				}
			}
		}
		return Result.success();
	}

	//---------------------------------------------------------------------------------

	/**
	 * Displays a notification when {@link AlarmManager#canScheduleExactAlarms()} returns
	 * {@code false}.
	 * <p>
	 * The notification opens {@link Activity_RequestPermIntro}.
	 */
	private void postMissingPermNotif() {

		NotificationManager notificationManager
			= (NotificationManager) context.getSystemService(
			Context.NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			int importance = NotificationManager.IMPORTANCE_HIGH;
			NotificationChannel channel = new NotificationChannel(
				Integer.toString(ConstantsAndStatics.NOTIF_CHANNEL_ID_ERROR),
				context.getString(R.string.notif_channel_error), importance);
			notificationManager.createNotificationChannel(channel);
		}

		Intent intent = new Intent(context.getApplicationContext(),
			Activity_RequestPermIntro.class);

		int flags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
			?
			PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
			: PendingIntent.FLAG_UPDATE_CURRENT;

		PendingIntent pendingIntent = PendingIntent.getActivity(
			context.getApplicationContext(), 255, intent, flags);

		NotificationCompat.Action notifAction = new NotificationCompat.Action.Builder(
			R.drawable.ic_notif,
			context.getString(R.string.grant_permission), pendingIntent).build();

		NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
			Integer.toString(ConstantsAndStatics.NOTIF_CHANNEL_ID_ERROR))
			.setContentTitle(context.getString(R.string.error_notif_title))
			.setContentText(context.getString(R.string.error_notif_body))
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setCategory(NotificationCompat.CATEGORY_ERROR)
			.setSmallIcon(R.drawable.ic_notif)
			.setOngoing(true)
			.setAutoCancel(true)
			.setOnlyAlertOnce(true)
			.addAction(notifAction);

		notificationManager.notify(UniqueNotifID.getID(), builder.build());

	}


	@Override
	public void onStopped() {
		super.onStopped();
		stopExecuting = true;
	}

}
