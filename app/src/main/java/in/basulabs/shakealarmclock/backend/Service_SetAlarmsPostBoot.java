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
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import in.basulabs.shakealarmclock.R;
import in.basulabs.shakealarmclock.frontend.Activity_RequestPermIntro;

public class Service_SetAlarmsPostBoot extends Service {

	private AlarmDatabase alarmDatabase;

	public static boolean isThisServiceRunning = false;

	//----------------------------------------------------------------------------------

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
				startForeground(UniqueNotifID.getID(), buildForegroundNotification(),
					ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
			} else {
				startForeground(UniqueNotifID.getID(), buildForegroundNotification(),
					ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE);
			}
		} else {
			startForeground(UniqueNotifID.getID(), buildForegroundNotification());
		}
		isThisServiceRunning = true;

		ConstantsAndStatics.cancelScheduledPeriodicWork(this);

		alarmDatabase = AlarmDatabase.getInstance(this);

		ArrayList<AlarmEntity> alarmEntityArrayList = getActiveAlarms();

		if (alarmEntityArrayList != null && alarmEntityArrayList.size() > 0) {

			if (!ConstantsAndStatics.getEssentialPerms(this).isEmpty()) {
				postMissingPermsNotif();
			} else {
				cancelActiveAlarms(alarmEntityArrayList);
				activateAlarms(alarmEntityArrayList);
			}
		}

		stopSelf();

		return START_NOT_STICKY;
	}

	//----------------------------------------------------------------------------------

	@Override
	public void onDestroy() {
		super.onDestroy();
		isThisServiceRunning = false;
		ConstantsAndStatics.schedulePeriodicWork(this);
	}


	//----------------------------------------------------------------------------------

	/**
	 * Creates the notification channel for the foreground notification of this service.
	 */
	private void createForegroundNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			int importance = NotificationManager.IMPORTANCE_HIGH;
			NotificationChannel channel = new NotificationChannel(
				Integer.toString(ConstantsAndStatics.NOTIF_CHANNEL_ID_BOOT),
				getString(R.string.notif_channel_boot), importance);
			NotificationManager notificationManager
				= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			assert notificationManager != null;
			notificationManager.createNotificationChannel(channel);
		}
	}

	//----------------------------------------------------------------------------------

	/**
	 * Creates the error notification channel.
	 */
	private void createErrorNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			int importance = NotificationManager.IMPORTANCE_HIGH;
			NotificationChannel channel = new NotificationChannel(
				Integer.toString(ConstantsAndStatics.NOTIF_CHANNEL_ID_ERROR),
				getString(R.string.notif_channel_error), importance);
			NotificationManager notificationManager
				= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			assert notificationManager != null;
			notificationManager.createNotificationChannel(channel);
		}
	}

	//----------------------------------------------------------------------------------

	/**
	 * Builds the foreground notification for this service.
	 *
	 * @return A {@link Notification} that can be used with
	 *    {@link #startForeground(int, Notification)} or displayed with
	 *    {@link NotificationManager#notify(int, Notification)}.
	 */
	@NonNull
	private Notification buildForegroundNotification() {

		createForegroundNotificationChannel();

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
			Integer.toString(ConstantsAndStatics.NOTIF_CHANNEL_ID_BOOT))
			.setContentTitle(getResources().getString(R.string.app_name))
			.setContentText(getResources().getString(R.string.updateAlarm_notifMessage))
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setCategory(NotificationCompat.CATEGORY_STATUS)
			.setSmallIcon(R.drawable.ic_notif);

		return builder.build();
	}

	//----------------------------------------------------------------------------------

	/**
	 * Get a list of all the active alarms.
	 *
	 * @return An {@link ArrayList} of {@link AlarmEntity} type containing the data of
	 * the
	 * 	active alarms.
	 */
	@Nullable
	private ArrayList<AlarmEntity> getActiveAlarms() {

		AtomicReference<ArrayList<AlarmEntity>> alarmEntityArrayList
			= new AtomicReference<>(new ArrayList<>());

		Thread thread = new Thread(() -> alarmEntityArrayList.set(
			new ArrayList<>(alarmDatabase.alarmDAO().getActiveAlarms())));

		thread.start();
		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		return alarmEntityArrayList.get();

	}

	//----------------------------------------------------------------------------------

	/**
	 * Dismiss all the active alarms.
	 *
	 * @param alarmEntityArrayList The list of active alarms. Not null.
	 */
	private void cancelActiveAlarms(
		@NonNull ArrayList<AlarmEntity> alarmEntityArrayList) {

		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		for (AlarmEntity alarmEntity : alarmEntityArrayList) {

			ConstantsAndStatics.killServices(this, alarmEntity.alarmID);

			Intent intent = new Intent(Service_SetAlarmsPostBoot.this,
				AlarmBroadcastReceiver.class);
			intent.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM);
			intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

			int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
				PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE :
				PendingIntent.FLAG_NO_CREATE;

			PendingIntent pendingIntent = PendingIntent.getBroadcast(
				Service_SetAlarmsPostBoot.this, alarmEntity.alarmID, intent, flags);

			if (pendingIntent != null) {
				alarmManager.cancel(pendingIntent);
			}

		}
	}

	//----------------------------------------------------------------------------------

	/**
	 * Get the list of repeat days for a particular alarm.
	 *
	 * @param alarmID The unique alarm ID.
	 * @return An {@link ArrayList} of integers for the days in which the alarm is
	 * 	repeated. The day numbers follow the {@link DayOfWeek} enum.
	 */
	@Nullable
	private ArrayList<Integer> getRepeatDays(int alarmID) {

		AtomicReference<ArrayList<Integer>> repeatDays = new AtomicReference<>();

		Thread thread = new Thread(() -> repeatDays.set(
			new ArrayList<>(alarmDatabase.alarmDAO().getAlarmRepeatDays(alarmID))));
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		return repeatDays.get();

	}

	//----------------------------------------------------------------------------------

	/**
	 * Activates the alarms that are switched on, if possibe.
	 * <p>
	 * If repeat is ON for an alarm, then the alarm is set as usual. If, however, repeat
	 * is OFF, then it is first checked whether the time is reachable or not. If
	 * reachable, the alarm is set, otherwise the alarm is switched off in the database
	 * and a notification is posted using
	 * {@link #postAlarmMissedNotification(LocalTime)}.
	 * </p>
	 *
	 * @param alarmEntityArrayList The list of active alarms.
	 */
	private void activateAlarms(@NonNull ArrayList<AlarmEntity> alarmEntityArrayList) {

		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		for (AlarmEntity alarmEntity : alarmEntityArrayList) {

			ArrayList<Integer> repeatDays = getRepeatDays(alarmEntity.alarmID);

			LocalDateTime alarmDateTime;

			LocalDate alarmDate = LocalDate.of(alarmEntity.alarmYear,
				alarmEntity.alarmMonth, alarmEntity.alarmDay);
			LocalTime alarmTime = LocalTime.of(alarmEntity.alarmHour,
				alarmEntity.alarmMinutes);

			if (alarmEntity.isRepeatOn && repeatDays != null && repeatDays.size() > 0) {

				// If repeat is ON, set the alarm as we normally would.

				Collections.sort(repeatDays);

				alarmDateTime = LocalDateTime.of(LocalDate.now(), alarmTime);
				int dayOfWeek = alarmDateTime.getDayOfWeek().getValue();

				for (int i = 0; i < repeatDays.size(); i++) {
					if (repeatDays.get(i) == dayOfWeek) {
						if (alarmTime.isAfter(LocalTime.now())) {
							// Alarm possible today, nothing more to do, break out of
							// loop.
							break;
						}
					} else if (repeatDays.get(i) > dayOfWeek) {
						/////////////////////////////////////////////////////////////////////////
						// There is a day available in the same week for the alarm to
						// ring;
						// select that day and break from loop.
						////////////////////////////////////////////////////////////////////////
						alarmDateTime = alarmDateTime.with(
							TemporalAdjusters.next(DayOfWeek.of(repeatDays.get(i))));
						break;
					}
					if (i == repeatDays.size() - 1) {
						// No day possible in this week. Select the first available date
						// from next week.
						alarmDateTime = alarmDateTime.with(
							TemporalAdjusters.next(DayOfWeek.of(repeatDays.get(0))));
					}
				}

			} else {

				// If repeat is OFF, first check whether the alarm time is reachable. If
				// yes, then set the alarm, otherwise ignore this alarm and
				// switch it off in the database.

				alarmDateTime = LocalDateTime.of(alarmDate, alarmTime);

				if (!alarmDateTime.isAfter(LocalDateTime.now())) {
					alarmDateTime = null;
				}

			}

			if (alarmDateTime != null) {

				Intent intent = new Intent(getApplicationContext(),
					AlarmBroadcastReceiver.class);
				intent.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM);
				intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

				Bundle data = alarmEntity.getAlarmDetailsInABundle();
				data.putIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS,
					repeatDays);
				intent.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, data);

				int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
					? PendingIntent.FLAG_IMMUTABLE
					: 0;

				PendingIntent pendingIntent = PendingIntent.getBroadcast(
					getApplicationContext(), alarmEntity.alarmID, intent, flags);

				ZonedDateTime zonedDateTime = ZonedDateTime.of(alarmDateTime,
					ZoneId.systemDefault());

				alarmManager.setAlarmClock(
					new AlarmManager.AlarmClockInfo(zonedDateTime.toEpochSecond() * 1000,
						pendingIntent), pendingIntent);

			} else {

				Thread thread = new Thread(
					() -> alarmDatabase.alarmDAO().toggleAlarm(alarmEntity.alarmID, 0));

				thread.start();
				try {
					thread.join();
				} catch (InterruptedException ignored) {
				}

				postAlarmMissedNotification(alarmTime);

			}

		}

	}

	//----------------------------------------------------------------------------------

	/**
	 * Posts a notification informing the user that an alarm has been missed.
	 *
	 * @param alarmTime The alarm time.
	 */
	private void postAlarmMissedNotification(LocalTime alarmTime) {

		createErrorNotificationChannel();

		NotificationManager notificationManager = (NotificationManager) getSystemService(
			NOTIFICATION_SERVICE);

		DateTimeFormatter formatter;
		if (!DateFormat.is24HourFormat(this)) {
			formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault());
		} else {
			formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());
		}

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
			Integer.toString(ConstantsAndStatics.NOTIF_CHANNEL_ID_ERROR))
			.setContentTitle(
				getResources().getString(R.string.updateAlarm_alarmMissedTitle))
			.setContentText(getString(R.string.updateAlarm_alarmMissedText,
				alarmTime.format(formatter)))
			.setSmallIcon(R.drawable.ic_notif)
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setCategory(NotificationCompat.CATEGORY_ERROR)
			.setDefaults(Notification.DEFAULT_SOUND)
			.setAutoCancel(true)
			.setOngoing(false);

		notificationManager.notify(UniqueNotifID.getID(), builder.build());

	}

	/**
	 * Displays a notification when essential permissions are missing (i.e. when
	 * {@link ConstantsAndStatics#getEssentialPerms(Context)} is not empty.
	 * <p>
	 * The notification opens {@link Activity_RequestPermIntro}.
	 */
	private void postMissingPermsNotif() {

		NotificationManager notificationManager =
			(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		createErrorNotificationChannel();

		Intent intent = new Intent(getApplicationContext(),
			Activity_RequestPermIntro.class);

		int flags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
			?
			PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
			: PendingIntent.FLAG_UPDATE_CURRENT;

		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
			255, intent, flags);

		NotificationCompat.Action notifAction = new NotificationCompat.Action.Builder(
			R.drawable.ic_notif,
			getString(R.string.grant_permission), pendingIntent).build();

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
			Integer.toString(ConstantsAndStatics.NOTIF_CHANNEL_ID_ERROR))
			.setContentTitle(getString(R.string.error_notif_title))
			.setContentText(getString(R.string.error_notif_body))
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setCategory(NotificationCompat.CATEGORY_ERROR)
			.setSmallIcon(R.drawable.ic_notif)
			.setOngoing(true)
			.setAutoCancel(true)
			.addAction(notifAction);

		notificationManager.notify(UniqueNotifID.getID(), builder.build());

	}

	//----------------------------------------------------------------------------------

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
