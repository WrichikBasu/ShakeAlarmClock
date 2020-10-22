package in.basulabs.shakealarmclock;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class Service_SnoozeAlarm extends Service {

	public static boolean isThisServiceRunning = false;
	public static int alarmID;
	private Bundle alarmDetails;
	private static final int NOTIFICATION_ID = 651;
	private int numberOfTimesTheAlarmhasBeenSnoozed;

	private CountDownTimer snoozeTimer;

	//--------------------------------------------------------------------------------------------------

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Objects.equals(intent.getAction(), ConstantsAndStatics.ACTION_CANCEL_ALARM)) {
				dismissAlarm();
			}
		}
	};

	//----------------------------------------------------------------------------------------------------

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			startForeground(NOTIFICATION_ID, buildSnoozeNotification(),
					ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE);
		} else {
			startForeground(NOTIFICATION_ID, buildSnoozeNotification());
		}
		isThisServiceRunning = true;

		ConstantsAndStatics.cancelScheduledPeriodicWork(this);

		Bundle data = Objects.requireNonNull(intent.getExtras());
		alarmDetails = data.getBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS);
		assert alarmDetails != null;
		alarmID = alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID);

		numberOfTimesTheAlarmhasBeenSnoozed =
				intent.getExtras().getInt(Service_RingAlarm.BUNDLE_KEY_NO_OF_TIMES_SNOOZED);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConstantsAndStatics.ACTION_CANCEL_ALARM);
		registerReceiver(broadcastReceiver, intentFilter);

		Service_SnoozeAlarm myInstance = this;

		ZonedDateTime alarmDateTime = ZonedDateTime.of(LocalDateTime.of(
				alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_YEAR),
				alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MONTH),
				alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_DAY),
				alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
				alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE), 0, 0),
				ZoneId.systemDefault());

		ZonedDateTime newAlarmDateTime = alarmDateTime.plusMinutes(numberOfTimesTheAlarmhasBeenSnoozed * alarmDetails
								.getInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_TIME_IN_MINS));

		snoozeTimer = new CountDownTimer(Math.abs(Duration.between(ZonedDateTime.now(),
				newAlarmDateTime).toMillis()),	500) {

			@Override
			public void onTick(long l) {
			}

			@Override
			public void onFinish() {
				Intent intent1 = new Intent(myInstance, Service_RingAlarm.class);
				intent1.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, alarmDetails);
				intent1.putExtra(Service_RingAlarm.BUNDLE_KEY_NO_OF_TIMES_SNOOZED, numberOfTimesTheAlarmhasBeenSnoozed);
				ContextCompat.startForegroundService(myInstance, intent1);
				myInstance.stopSelf();
			}
		};

		snoozeTimer.start();

		return START_NOT_STICKY;
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Creates the notification channel.
	 */
	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			int importance = NotificationManager.IMPORTANCE_HIGH;
			NotificationChannel channel = new NotificationChannel(Integer.toString(NOTIFICATION_ID),
					"in.basulabs.shakealarmclock Notifications", importance);
			NotificationManager notificationManager = (NotificationManager) getSystemService(
					NOTIFICATION_SERVICE);
			channel.setSound(null, null);
			assert notificationManager != null;
			notificationManager.createNotificationChannel(channel);
		}
	}

	//---------------------------------------------------------------------------------------------------

	private Notification buildSnoozeNotification() {
		createNotificationChannel();

		Intent intent = new Intent();
		intent.setAction(ConstantsAndStatics.ACTION_CANCEL_ALARM);
		PendingIntent contentPendingIntent = PendingIntent.getBroadcast(this, 5017, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
				Integer.toString(NOTIFICATION_ID))
				.setContentTitle(getResources().getString(R.string.app_name))
				.setContentText(getResources().getString(R.string.notifContent_snooze))
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setCategory(NotificationCompat.CATEGORY_STATUS)
				.setSmallIcon(R.drawable.ic_notif)
				.setContentIntent(contentPendingIntent);

		return builder.build();
	}

	//-----------------------------------------------------------------------------------------------------

	/**
	 * Dismisses the current alarm, and sets the next alarm if repeat is enabled.
	 */
	private void dismissAlarm() {

		snoozeTimer.cancel();
		cancelPendingIntent();

		AlarmDatabase alarmDatabase = AlarmDatabase.getInstance(this);
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		Thread thread_toggleAlarm = new Thread(() ->
				alarmDatabase.alarmDAO().toggleAlarm(alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID),0));

		/////////////////////////////////////
		// Dismiss the snoozed alarm
		/////////////////////////////////////
		Intent intent = new Intent(this, AlarmBroadcastReceiver.class);
		intent.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM);
		intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID),
						intent,	PendingIntent.FLAG_NO_CREATE);

		if (pendingIntent != null) {
			alarmManager.cancel(pendingIntent);
		}

		//////////////////////////////////////////////////////
		// If repeat is on, set another alarm. Otherwise
		// toggle alarm state in database.
		/////////////////////////////////////////////////////
		if (! alarmDetails.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_REPEAT_ON)) {
			thread_toggleAlarm.start();
			try {
				thread_toggleAlarm.join();
			} catch (InterruptedException ignored) {
			}
		} else {
			LocalTime alarmTime = LocalTime.of(alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
							alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE));

			ArrayList<Integer> repeatDays = alarmDetails.getIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS);

			assert repeatDays != null;
			Collections.sort(repeatDays);

			LocalDateTime alarmDateTime = LocalDateTime.of(LocalDate.now(), alarmTime);
			int dayOfWeek = alarmDateTime.getDayOfWeek().getValue();

			for (int i = 0; i < repeatDays.size(); i++) {
				if (repeatDays.get(i) == dayOfWeek) {
					if (alarmTime.isAfter(LocalTime.now())) {
						// Alarm possible today, nothing more to do, break out of loop.
						break;
					}
				} else if (repeatDays.get(i) > dayOfWeek) {
					// There is a day available in the same week for the alarm to ring; select that day and
					// break from loop.
					alarmDateTime = alarmDateTime.with(TemporalAdjusters.next(DayOfWeek.of(repeatDays.get(i))));
					break;
				}
				if (i == repeatDays.size() - 1) {
					// No day possible in this week. Select the first available date from next week.
					alarmDateTime = alarmDateTime.with(TemporalAdjusters.next(DayOfWeek.of(repeatDays.get(0))));
				}
			}

			intent.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, alarmDetails);

			PendingIntent pendingIntent2 = PendingIntent.getBroadcast(this,
							alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID), intent, 0);

			ZonedDateTime zonedDateTime = ZonedDateTime.of(alarmDateTime.withSecond(0),	ZoneId.systemDefault());

			alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(zonedDateTime.toEpochSecond() * 1000,
							pendingIntent2), pendingIntent2);
		}
		ConstantsAndStatics.schedulePeriodicWork(this);
		stopSelf();

	}

	//-----------------------------------------------------------------------------------------------------

	private void cancelPendingIntent() {
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(this, AlarmBroadcastReceiver.class);
		intent.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM);
		intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		intent.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, alarmDetails);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarmID, intent, PendingIntent.FLAG_NO_CREATE);

		if (pendingIntent != null) {
			alarmManager.cancel(pendingIntent);
		}
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(broadcastReceiver);
		isThisServiceRunning = false;
	}


}
