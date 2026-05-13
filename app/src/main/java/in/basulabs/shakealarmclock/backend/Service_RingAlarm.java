/*
 * Copyright (c) 2026. Wrichik Basu (basulabs.developer@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
package in.basulabs.shakealarmclock.backend;

import android.annotation.SuppressLint;
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
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.Enumeration;
import java.util.Objects;

import in.basulabs.audiofocuscontroller.AudioFocusController;
import in.basulabs.shakealarmclock.R;
import in.basulabs.shakealarmclock.frontend.Activity_AlarmsList;
import in.basulabs.shakealarmclock.frontend.Activity_RingAlarm;
import in.basulabs.shakealarmclock.frontend.Activity_SnoozedAlarms;

public class Service_RingAlarm extends Service implements SensorEventListener {

	@Nullable
	private Bundle currentAlarm;

	@Nullable
	private MediaPlayer mediaPlayer;

	private AlarmDatabase alarmDatabase;

	@Nullable
	private CountDownTimer ringTimer;

	private SensorManager snsMgr;
	private Vibrator vibrator;
	private AudioManager audioManager;
	private NotificationManager notificationManager;

	private long lastShakeTime;

	private static final int MINIMUM_MILLIS_BETWEEN_SHAKES = 600;

	private int initialAlarmStreamVolume;

	/**
	 * Indicates whether this service is running or not.
	 */
	public static volatile boolean isThisServiceRunning = false;

	private SharedPreferences sharedPreferences;

	private boolean isShakeActive;

	private AudioFocusController.Builder afcBuilder;
	@Nullable
	private AudioFocusController afController;

	/**
	 * Indicates whether alarm ringing has already started, to avoid collision due to
	 * repeated calls to {@link AudioFocusController.OnAudioFocusChangeListener#resume()}.
	 */
	private boolean alarmRingingStarted;

	private int notifID;

	private int powerBtnAction;

	private static Service_RingAlarm self;

	private PowerManager.WakeLock wakeLock;
	private IntentFilter intentFilter;

	//----------------------------------------------------------------------------------

	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (Objects.equals(intent.getAction(), ConstantsAndStatics.ACTION_SNOOZE_ALARM))
				snoozeAlarm(Objects.requireNonNull(intent.getExtras()));

			else if (Objects.equals(intent.getAction(), ConstantsAndStatics.ACTION_CANCEL_ALARM))
				dismissAlarm(Objects.requireNonNull(intent.getExtras()), true);

			else if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_OFF)) {

				if (powerBtnAction == ConstantsAndStatics.DISMISS)
					dismissAlarm(currentAlarm, true);

				else if (powerBtnAction == ConstantsAndStatics.SNOOZE)
					snoozeAlarm(currentAlarm);
			}
		}
	};

	//---------------------------------------------------------------------------------

	@SuppressLint("WakelockTimeout")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		notifID = UniqueNotifID.getID();
		self = this;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
				startForeground(notifID, buildRingNotification(null),
				                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
			} else {
				startForeground(notifID, buildRingNotification(null),
				                ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE);
			}
		} else {
			startForeground(notifID, buildRingNotification(null));
		}

		Log.e(ConstantsAndStatics.DEBUG_TAG, "Service started");

		ConstantsAndStatics.cancelScheduledPeriodicWork(this);

		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
		                                    "in.basulabs.shakealarmclock::AlarmServiceWakeLock");
		wakeLock.acquire();

		sharedPreferences = ConstantsAndStatics.getSharedPref(this);

		afcBuilder = new AudioFocusController.Builder(this)
				.setAcceptsDelayedFocus(true)
				.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
				.setUsage(AudioAttributes.USAGE_ALARM)
				.setPauseWhenAudioIsNoisy(false)
				.setStream(AudioManager.STREAM_ALARM)
				.setDurationHint(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);

		isShakeActive = sharedPreferences.getInt(
				ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SHAKE_OPERATION,
				ConstantsAndStatics.SNOOZE) != ConstantsAndStatics.DO_NOTHING;

		snsMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		assert snsMgr != null;
		assert vibrator != null;
		assert audioManager != null;
		assert notificationManager != null;

		alarmDatabase = AlarmDatabase.getInstance(this);

		initialAlarmStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);

		powerBtnAction = sharedPreferences.getInt(
				ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_POWER_BTN_OPERATION,
				ConstantsAndStatics.DISMISS);

		intentFilter = new IntentFilter();
		intentFilter.addAction(ConstantsAndStatics.ACTION_SNOOZE_ALARM);
		intentFilter.addAction(ConstantsAndStatics.ACTION_CANCEL_ALARM);
		intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		intentFilter.addAction(Intent.ACTION_SCREEN_ON);

		isThisServiceRunning = true;
		alarmRingingStarted = false;

		tryRingNextAlarm();

		return START_NOT_STICKY;
	}

	/**
	 * Tries to ring the next alarm in the queue.
	 */
	public static void tryRingNextAlarm() {

		Log.e(ConstantsAndStatics.DEBUG_TAG, "In tryNextAlarm()");

		if (!isThisServiceRunning)
			return;

		if (!AlarmRingDS.isRingQEmpty() && self.currentAlarm == null) {
			self.ringAlarm();
			Log.e(ConstantsAndStatics.DEBUG_TAG, "Ringing alarm");
		}
	}

	//----------------------------------------------------------------------------------

	@Override
	public void onDestroy() {
		super.onDestroy();
		isThisServiceRunning = false;

		Log.e(ConstantsAndStatics.DEBUG_TAG, "In onDestroy()");

		try {
			unregisterReceiver(broadcastReceiver);
		} catch (IllegalArgumentException ignored) {}

		if (ringTimer != null && currentAlarm != null)
			dismissAlarm(currentAlarm);

		if (!AlarmRingDS.isSnoozedAlarmsEmpty()) {
			Enumeration<Integer> en = AlarmRingDS.getSnoozedAlarmIds();
			while (en.hasMoreElements()) {
				int i = en.nextElement();
				dismissAlarm(AlarmRingDS.getSnoozedAlarm(i));
			}
		}

		try {
			if (ringTimer != null)
				ringTimer.cancel();

			vibrator.cancel();
			if (mediaPlayer != null) {
				mediaPlayer.stop();
				mediaPlayer.release();
			}
		} catch (Exception ignored) {
		}
		if (isShakeActive) {
			snsMgr.unregisterListener(this);
		}
		if (notificationManager.isNotificationPolicyAccessGranted()) {
			audioManager.setStreamVolume(AudioManager.STREAM_ALARM, initialAlarmStreamVolume, 0);
		}

		isThisServiceRunning = false;
		ConstantsAndStatics.schedulePeriodicWork(this);

		if (wakeLock.isHeld())
			wakeLock.release();
	}

	//----------------------------------------------------------------------------------

	/**
	 * Reads the repeat days from alarm database.
	 * <p>
	 * I have received some crash reports from Google Play stating that
	 * {@code NullPointerException} is being thrown in {@code dismissAlarm()} at the
	 * statement {@code Collections.sort(repeatDays)}. It seems that even if repeat is
	 * ON, the repeat days list is null. That is why we are re-reading the repeat days from
	 * the database as a temporary fix.
	 * </p>
	 */
	private void loadRepeatDays(@NonNull Bundle alarmDetails) {

		if (alarmDetails.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_REPEAT_ON)) {

			Thread thread = new Thread(() -> {

				ArrayList<Integer> repeatDays = new ArrayList<>(
						alarmDatabase.alarmDAO().getAlarmRepeatDays(
								alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID)));

				Collections.sort(repeatDays);

				alarmDetails.remove(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS);
				alarmDetails.putIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS,
				                                 repeatDays);
			});
			thread.start();
		} else {
			alarmDetails.remove(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS);
		}
	}

	//----------------------------------------------------------------------------------

	/**
	 * Initializes the shake sensor.
	 */
	private void initialiseShakeSensor() {
		if (isShakeActive) {
			Sensor accelerometer = snsMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			snsMgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI,
			                        new Handler());
			lastShakeTime = System.currentTimeMillis();
		}
	}

	//----------------------------------------------------------------------------------

	/**
	 * Creates the notification channel.
	 */
	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			int importance = NotificationManager.IMPORTANCE_HIGH;
			NotificationChannel channel = new NotificationChannel(
					Integer.toString(ConstantsAndStatics.NOTIF_CHANNEL_ID_ALARM),
					getString(R.string.notif_channel_name_ring_alarms), importance);
			NotificationManager notificationManager
					= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			channel.setSound(null, null);
			notificationManager.createNotificationChannel(channel);
		}
	}

	//----------------------------------------------------------------------------------

	/**
	 * Creates a notification that can be shown when the alarm is ringing.
	 * <p>
	 * Has a full screen intent to {@link Activity_RingAlarm}. The content intent points
	 * to {@link Activity_AlarmsList}.
	 *
	 * @return A {@link Notification} instance that can be displayed to the user.
	 */
	@SuppressLint("FullScreenIntentPolicy")
	@NonNull
	private Notification buildRingNotification(@Nullable Bundle alarmDetails) {

		Log.e(ConstantsAndStatics.DEBUG_TAG, "In buildRingNotification()");

		createNotificationChannel();
		int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
		                                                                    Integer.toString(
				                                                                    ConstantsAndStatics.NOTIF_CHANNEL_ID_ALARM))
				.setContentTitle(getResources().getString(R.string.app_name))
				.setPriority(NotificationCompat.PRIORITY_MAX)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.setSmallIcon(R.drawable.ic_notif)
				.setOnlyAlertOnce(true)
				.setContentText(getString(R.string.notifContent_ring));

		if (alarmDetails != null) {

			Intent fullScreenIntent = new Intent(this, Activity_RingAlarm.class)
					.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
					.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
					.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
					.putExtras(alarmDetails);

			PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 3054,
			                                                                  fullScreenIntent,
			                                                                  flags);
			builder.setContentIntent(fullScreenPendingIntent)
			       .setFullScreenIntent(fullScreenPendingIntent, true);

			String alarmMessage = alarmDetails.getString(
					ConstantsAndStatics.BUNDLE_KEY_ALARM_MESSAGE, null);

			if (alarmMessage != null) {
				builder.setContentTitle(getString(R.string.app_name))
				       .setContentText(alarmMessage)
				       .setStyle(new NotificationCompat.BigTextStyle().bigText(alarmMessage));
			}
		}
		return builder.build();
	}

	/**
	 * Builds and returns the notification with the (original) time for each snoozed alarm.
	 *
	 * @return A {@link Notification} instance that can be displayed to the user.
	 */
	private Notification buildSnoozeNotification() {

		Log.e(ConstantsAndStatics.DEBUG_TAG, "In buildSnoozeNotification()");

		createNotificationChannel();

		Intent intent = new Intent(this, Activity_SnoozedAlarms.class)
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
				.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
				.setPackage(getPackageName());

		int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;

		PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 5017,
		                                                                intent, flags);

		NotificationCompat.Action notifAction = new NotificationCompat.Action.Builder(
				R.drawable.ic_notif, getString(R.string.dismiss_alarm), contentPendingIntent).build();

		NotificationCompat.Builder builder
				= new NotificationCompat.Builder(this,
				                                 Integer.toString(
						                                 ConstantsAndStatics.NOTIF_CHANNEL_ID_ALARM))
				.setContentTitle(getString(R.string.app_name))
				.setContentText(getString(R.string.notifContent_snooze))
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.setSmallIcon(R.drawable.ic_notif)
				.setOnlyAlertOnce(true)
				.addAction(notifAction)
				.setContentIntent(contentPendingIntent);

		StringBuilder alarmMessage = new StringBuilder();

		if (!AlarmRingDS.isSnoozedAlarmsEmpty()) {

			alarmMessage.append("Snoozed alarms:");

			Enumeration<Integer> en = AlarmRingDS.getSnoozedAlarmIds();

			while (en.hasMoreElements()) {

				int key = en.nextElement();
				String currentText;

				LocalTime alarmTime = LocalTime.of(
						AlarmRingDS.getSnoozedAlarm(key).getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
						AlarmRingDS.getSnoozedAlarm(key).getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE));

				if (DateFormat.is24HourFormat(this)) {
					currentText = getResources().getString(R.string.time_24hour,
					                                       alarmTime.getHour(),
					                                       alarmTime.getMinute());
				} else {
					String amPm = alarmTime.getHour() < 12 ? "AM" : "PM";

					if ((alarmTime.getHour() <= 12) && (alarmTime.getHour() > 0)) {

						currentText = getResources().getString(R.string.time_12hour,
						                                       alarmTime.getHour(),
						                                       alarmTime.getMinute(), amPm);

					} else if (alarmTime.getHour() > 12 && alarmTime.getHour() <= 23) {

						currentText = getResources().getString(R.string.time_12hour,
						                                       alarmTime.getHour() - 12,
						                                       alarmTime.getMinute(), amPm);

					} else {
						currentText = getResources().getString(R.string.time_12hour,
						                                       alarmTime.getHour() + 12,
						                                       alarmTime.getMinute(), amPm);
					}
				}
				alarmMessage.append("\n").append(currentText);
			}
		}

		//noinspection SizeReplaceableByIsEmpty
		if (alarmMessage.length() > 0) {
			builder.setContentTitle(getString(R.string.app_name))
			       .setContentText(alarmMessage)
			       .setStyle(new NotificationCompat.BigTextStyle().bigText(alarmMessage));
		}
		return builder.build();
	}

	//----------------------------------------------------------------------------------

	/**
	 * Dequeues an alarm from {@link AlarmRingDS}, and starts ringing the alarm.
	 */
	private void ringAlarm() {

		Log.e(ConstantsAndStatics.DEBUG_TAG, "In ringAlarm()");

		Bundle alarmDetails = AlarmRingDS.dequeueRingQ();
		loadRepeatDays(alarmDetails);

		Uri chosenToneUri = alarmDetails.getParcelable(
				ConstantsAndStatics.BUNDLE_KEY_ALARM_TONE_URI);
		Uri actualToneURI;
		try (InputStream ignored = getContentResolver().openInputStream(
				Objects.requireNonNull(chosenToneUri))) {
			// Alarm tone file exists.
			actualToneURI = chosenToneUri;
		} catch (Exception ex) {
			// Tone file can either not be accessed, or not available in the file system.
			// Fall back to default tone.
			actualToneURI = Settings.System.DEFAULT_ALARM_ALERT_URI;
		}

		CountDownTimer ringTimer = new CountDownTimer(60000, 1000) {

			@Override
			public void onTick(long millisUntilFinished) {
				if (!isThisServiceRunning)
					cancel();
			}

			@Override
			public void onFinish() {
				snoozeAlarm(alarmDetails);
			}
		};

		notificationManager.notify(notifID, buildRingNotification(alarmDetails));
		initialiseShakeSensor();

		if (!(alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE) ==
				ConstantsAndStatics.ALARM_TYPE_VIBRATE_ONLY)) {

			mediaPlayer = new MediaPlayer();
			AudioAttributes attributes = new AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_ALARM)
					.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
					.build();

			try {
				mediaPlayer.setDataSource(this, actualToneURI);
				mediaPlayer.setAudioAttributes(attributes);
				mediaPlayer.setLooping(true);
				mediaPlayer.prepare();
			} catch (IOException ignored) {
			}

			AudioFocusController.OnAudioFocusChangeListener afListener
					= new AudioFocusController.OnAudioFocusChangeListener() {
				@Override
				public void decreaseVolume() {}

				@Override
				public void increaseVolume() {}

				@Override
				public void pause() {

					if (!alarmRingingStarted)
						return;

					alarmRingingStarted = false;

					if (mediaPlayer != null)
						mediaPlayer.pause();

					vibrator.cancel();
				}

				@Override
				public void resume() {

					Log.e(ConstantsAndStatics.DEBUG_TAG, "Focus received");

					if (mediaPlayer == null)
						return;

					if (alarmRingingStarted)
						return;

					alarmRingingStarted = true;

					currentAlarm = alarmDetails;

					ContextCompat.registerReceiver(self, broadcastReceiver, intentFilter,
					                               ContextCompat.RECEIVER_NOT_EXPORTED);

					// Change volume of alarm stream, if permitted
					if (notificationManager.isNotificationPolicyAccessGranted()) {
						audioManager.setStreamVolume(AudioManager.STREAM_ALARM,
						                             alarmDetails.getInt(
								                             ConstantsAndStatics.BUNDLE_KEY_ALARM_VOLUME),
						                             0);
					}

					// Start alarm sound
					mediaPlayer.start();

					Log.e(ConstantsAndStatics.DEBUG_TAG, "RINGING: "
							+ alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR)
							+ ":" + alarmDetails.getInt(
							ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE));

					// Start vibration
					if (alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE) ==
							ConstantsAndStatics.ALARM_TYPE_SOUND_AND_VIBRATE) {
						alarmVibration();
					}

					// Now, start the timer
					ringTimer.start();
				}
			};

			afController = afcBuilder.setAudioFocusChangeListener(afListener).build();
			afController.requestFocus();
			Log.e(ConstantsAndStatics.DEBUG_TAG, "Focus requested");

		} else {  // Only vibration, no sound
			currentAlarm = alarmDetails;
			alarmVibration();
			ringTimer.start();
			ContextCompat.registerReceiver(this, broadcastReceiver, intentFilter,
			                               ContextCompat.RECEIVER_NOT_EXPORTED);
			Log.e(ConstantsAndStatics.DEBUG_TAG, "RINGING: "
					+ alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR)
					+ ":" + alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE));
		}

	}

	//----------------------------------------------------------------------------------

	/**
	 * Vibrate the phone for the alarm.
	 */
	private void alarmVibration() {

		long[] vibrationPattern = new long[]{0, 600, 200, 600, 200, 800, 200, 1000};
		int[] vibrationAmplitudes = new int[]{0, 255, 0, 255, 0, 255, 0, 255};
		// -1 : Play exactly once

		if (vibrator.hasVibrator()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				vibrator.vibrate(
						VibrationEffect.createWaveform(vibrationPattern, vibrationAmplitudes, 0));
			} else {
				vibrator.vibrate(vibrationPattern, 0);
			}
		}
	}

	//----------------------------------------------------------------------------------

	/**
	 * Snoozes the alarm. If snooze is off, or the snooze frequency has been reached, the
	 * alarm will be dismissed by calling {@link #dismissAlarm(Bundle)}.
	 * <p>
	 * Calls {@link Service_RingAlarm#tryRingNextAlarm()} after snoozing/dismissing the alarm.
	 * <p>
	 * NOTE: It is NECESSARY to calculate the duration of the snooze timer using {@link java.time.ZonedDateTime}
	 * and {@link java.time.Duration} in order to ring the alarm again at the exact minute instead of
	 * delaying by the time during which the alarm was ringing before being snoozed.
	 */
	private void snoozeAlarm(@NonNull Bundle alarmDetails) {

		Log.e(ConstantsAndStatics.DEBUG_TAG, "In snoozeAlarm()");

		if (AlarmRingDS.getSnoozedAlarm(
				alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID)) != null) {
			Log.e(ConstantsAndStatics.DEBUG_TAG, "Alarm already snoozed");
			return;
		}

		if (currentAlarm == alarmDetails)
			currentAlarm = null;

		stopRinging();

		int snoozeCount = alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_COUNT, 0);
		Log.e(ConstantsAndStatics.DEBUG_TAG, "snoozeCount = " + snoozeCount);

		if (alarmDetails.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_SNOOZE_ON)) {

			if (snoozeCount < alarmDetails.getInt(
					ConstantsAndStatics.BUNDLE_KEY_SNOOZE_FREQUENCY)) {

				alarmDetails.remove(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_COUNT);
				alarmDetails.putInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_COUNT, ++snoozeCount);

				final int alarmID = alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID);

				AlarmRingDS.addSnoozedAlarm(alarmID, alarmDetails, self);

				// Calculate the remaining snooze time after subtracting the time
				// for which the alarm was already ringing, so the next ring occurs
				// at the correct minute.

				ZonedDateTime alarmDateTime = ZonedDateTime.of(
						LocalDateTime.of(
								alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_YEAR),
								alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MONTH),
								alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_DAY),
								alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
								alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE), 0,
								0),
						ZoneId.systemDefault());

				ZonedDateTime newAlarmDateTime = alarmDateTime.plusMinutes(
						(long) snoozeCount * alarmDetails.getInt(
								ConstantsAndStatics.BUNDLE_KEY_SNOOZE_TIME_IN_MINS));

				long millisInFuture = Math.abs(
						Duration.between(ZonedDateTime.now(), newAlarmDateTime).toMillis());

				CountDownTimer snoozeTimer = new CountDownTimer(millisInFuture, 1000) {

					@Override
					public void onTick(long millisUntilFinished) {
						if (!isThisServiceRunning) {
							cancel();
							AlarmRingDS.removeSnoozedAlarm(alarmID, self);
						}
					}

					@Override
					public void onFinish() {
						Log.e(ConstantsAndStatics.DEBUG_TAG, "Snooze over");
						AlarmRingDS.enqueueRingQ(alarmDetails);
						tryRingNextAlarm();
						AlarmRingDS.removeSnoozedAlarm(alarmID, self);
					}
				};
				notificationManager.notify(notifID, buildSnoozeNotification());
				snoozeTimer.start();
				Log.e(ConstantsAndStatics.DEBUG_TAG, "SNOOZED: "
						+ alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR) + ":"
						+ alarmDetails.getInt(
						ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE) + "duration: " + millisInFuture / 1000 + "s");
				tryRingNextAlarm();

			} else { // Snooze frequency reached
				dismissAlarm(alarmDetails, true);
			}

		} else { // No snooze
			dismissAlarm(alarmDetails, true);
		}
	}

	//----------------------------------------------------------------------------------

	/**
	 * Dismisses the current alarm, toggles the alarm state in the db (or, sets the next alarm if
	 * repeat is enabled), then stops the service itself if there are no snoozed alarms and
	 * {@link AlarmRingDS} is empty.
	 *
	 * @param alarmDetails The details of the alarm to be dismissed.
	 */
	private void dismissAlarm(@NonNull Bundle alarmDetails) {

		Log.e(ConstantsAndStatics.DEBUG_TAG, "In dismissAlarm()");

		if (currentAlarm == alarmDetails)
			currentAlarm = null;

		stopRinging();
		cancelPendingIntent(alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID));
		AlarmRingDS.removeSnoozedAlarm(alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID), this);

		Thread thread_toggleAlarm =
				new Thread(() -> alarmDatabase.alarmDAO()
				                              .toggleAlarm(alarmDetails.getInt(
						                                           ConstantsAndStatics.BUNDLE_KEY_ALARM_ID), 0));

		ArrayList<Integer> repeatDays = alarmDetails.getIntegerArrayList(
				ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS);

		//////////////////////////////////////////////////////
		// If repeat is on, set another alarm. Otherwise,
		// toggle alarm state in database.
		/////////////////////////////////////////////////////
		if (alarmDetails.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_REPEAT_ON, false)
				&& !Objects.requireNonNull(repeatDays).isEmpty()) {

			LocalTime alarmTime = LocalTime.of(
					alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
					alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE));

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
					// There is a day available in the same week for the alarm to ring;
					// select that day and
					// break from loop.
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
			setAlarm(alarmDateTime, alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID));

		} else {

			thread_toggleAlarm.start();

			try {
				thread_toggleAlarm.join();
			} catch (InterruptedException ignored) {}
		}

		Log.e(ConstantsAndStatics.DEBUG_TAG, "DISMISSED: "
				+ alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR) + ":"
				+ alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE));

		if (AlarmRingDS.isRingQEmpty() && AlarmRingDS.isSnoozedAlarmsEmpty() && currentAlarm == null) {
			stopForeground(true);
			stopSelf();
		}
		if (AlarmRingDS.isRingQEmpty() && currentAlarm == null && !AlarmRingDS.isSnoozedAlarmsEmpty()) {
			notificationManager.notify(notifID, buildSnoozeNotification());
		}
	}

	/**
	 * Same as {@link #dismissAlarm(Bundle)}, but optionally tries to ring the next alarm.
	 *
	 * @param alarmDetails Details of the alarm to be dismissed.
	 * @param tryRingNextAlarm Whether to try ringing the next alarm.
	 */
	@SuppressWarnings("SameParameterValue")
	private void dismissAlarm(@NonNull Bundle alarmDetails, boolean tryRingNextAlarm) {
		dismissAlarm(alarmDetails);

		if (tryRingNextAlarm)
			tryRingNextAlarm();
	}

	//----------------------------------------------------------------------------------

	/**
	 * Stops the ringing alarm. Also sends a broadcast to {@link Activity_RingAlarm} to
	 * finish itself.
	 */
	private void stopRinging() {

		// Do NOT unregister the receiver here, as we may still get dismiss broadcasts from
		// Activity_RingAlarm.

		alarmRingingStarted = false;

		try {
			if (ringTimer != null) {
				ringTimer.cancel();
				ringTimer = null;
			}

			vibrator.cancel();

			if (mediaPlayer != null) {
				mediaPlayer.stop();
				mediaPlayer = null;
			}
		} catch (Exception ignored) {
		} finally {
			if (isShakeActive) {
				snsMgr.unregisterListener(this);
			}
			Intent intent = new Intent(ConstantsAndStatics.ACTION_DESTROY_RING_ALARM_ACTIVITY);
			intent.setPackage(getPackageName());
			sendBroadcast(intent);
		}
		if (afController != null)
			afController.abandonFocus();
		afController = null;
	}

	//----------------------------------------------------------------------------------

	/**
	 * Sets the next alarm in case of a repeat alarm.
	 *
	 * @param alarmDateTime The date and time when the alarm is to be set.
	 * @param alarmID       The internal ID of the alarm to be set, used as the request code for the {@link PendingIntent}.
	 */
	private void setAlarm(@NonNull LocalDateTime alarmDateTime, int alarmID) {

		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class)
				.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM)
				.setFlags(Intent.FLAG_RECEIVER_FOREGROUND)
				.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, currentAlarm);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
		                                                         alarmID, intent,
		                                                         PendingIntent.FLAG_IMMUTABLE);
		ZonedDateTime zonedDateTime = ZonedDateTime.of(alarmDateTime.withSecond(0),
		                                               ZoneId.systemDefault());

		alarmManager.setAlarmClock(
				new AlarmManager.AlarmClockInfo(zonedDateTime.toEpochSecond() * 1000,
				                                pendingIntent), pendingIntent);
	}

	//----------------------------------------------------------------------------------

	/**
	 * While testing, we found that sometimes the alarm was being reset at a later date
	 * unintentionally. This function cancels such an unintentional alarm.
	 */
	private void cancelPendingIntent(int alarmID) {

		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class)
				.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM)
				.setFlags(Intent.FLAG_RECEIVER_FOREGROUND)
				.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, currentAlarm);

		int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE;

		PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
		                                                         alarmID, intent, flags);

		if (pendingIntent != null) {
			alarmManager.cancel(pendingIntent);
		}
	}

	//----------------------------------------------------------------------------------

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	//----------------------------------------------------------------------------------

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (currentAlarm != null && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];

			float gX = x / SensorManager.GRAVITY_EARTH;
			float gY = y / SensorManager.GRAVITY_EARTH;
			float gZ = z / SensorManager.GRAVITY_EARTH;

			float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);
			// gForce will be close to 1 when there is no movement.

			if (gForce >= sharedPreferences.getFloat(
					ConstantsAndStatics.SHARED_PREF_KEY_SHAKE_SENSITIVITY,
					ConstantsAndStatics.DEFAULT_SHAKE_SENSITIVITY)) {

				long currTime = System.currentTimeMillis();

				if (Math.abs(currTime - lastShakeTime) > MINIMUM_MILLIS_BETWEEN_SHAKES) {

					lastShakeTime = currTime;
					shakeVibration();

					if (sharedPreferences.getInt(
							ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SHAKE_OPERATION,
							ConstantsAndStatics.SNOOZE) == ConstantsAndStatics.SNOOZE
							&& currentAlarm.getBoolean(
							ConstantsAndStatics.BUNDLE_KEY_IS_SNOOZE_ON)) {
						snoozeAlarm(currentAlarm);
					} else {
						dismissAlarm(currentAlarm, true);
					}
				}
			}
		}
	}

	//----------------------------------------------------------------------------------

	/**
	 * Creates a vibration for a small period of time, indicating that the app has
	 * registered a shake event.
	 */
	private void shakeVibration() {
		if (vibrator.hasVibrator()) {
			vibrator.cancel();
			SystemClock.sleep(100);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				vibrator.vibrate(VibrationEffect.createOneShot(200,
				                                               VibrationEffect.DEFAULT_AMPLITUDE));
			} else {
				vibrator.vibrate(200);
			}
			SystemClock.sleep(200);
		}
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {
	}

}
