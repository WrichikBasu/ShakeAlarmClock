/*
Copyright (C) 2022  Wrichik Basu (basulabs.developer@gmail.com)

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
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import in.basulabs.audiofocuscontroller.AudioFocusController;
import in.basulabs.shakealarmclock.R;
import in.basulabs.shakealarmclock.frontend.Activity_AlarmsList;
import in.basulabs.shakealarmclock.frontend.Activity_RingAlarm;

public class Service_RingAlarm extends Service implements SensorEventListener,
	AudioFocusController.OnAudioFocusChangeListener {

	private Bundle alarmDetails;

	private MediaPlayer mediaPlayer;

	private AlarmDatabase alarmDatabase;

	private CountDownTimer ringTimer;

	private SensorManager snsMgr;
	private Vibrator vibrator;
	private AudioManager audioManager;
	private NotificationManager notificationManager;

	private long lastShakeTime;

	private static final int MINIMUM_MILLIS_BETWEEN_SHAKES = 600;

	private int initialAlarmStreamVolume;

	private int numberOfTimesTheAlarmHasBeenSnoozed;

	private Uri alarmToneUri;

	/**
	 * The unique ID of the currently ringing alarm.
	 */
	public static int alarmID = -1;

	/**
	 * Indicates whether this service is running or not.
	 */
	public static boolean isThisServiceRunning = false;

	private SharedPreferences sharedPreferences;

	private boolean isShakeActive;

	/**
	 * Intent extra: Number of times this alarm has been snoozed.
	 */
	public static final String EXTRA_NO_OF_TIMES_SNOOZED
		= "in.basulabs.shakealarmclock.NO_OF_TIMES_SNOOZED";

	private boolean preMatureDeath;

	private ArrayList<Integer> repeatDays;

	private AudioFocusController audioFocusController;

	/**
	 * Indicates whether alarm ringing has already started, and prevents
	 * {@code ringAlarm()} to be called more than once by
	 * {@link AudioFocusController.OnAudioFocusChangeListener#resume()}.
	 */
	private boolean alarmRingingStarted;

	private int notifID;

	private int powerBtnAction;

	//----------------------------------------------------------------------------------

	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Objects.equals(intent.getAction(),
				ConstantsAndStatics.ACTION_SNOOZE_ALARM)) {
				snoozeAlarm();
			} else if (Objects.equals(intent.getAction(),
				ConstantsAndStatics.ACTION_CANCEL_ALARM)) {
				dismissAlarm();
			} else if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_OFF)) {
				if (powerBtnAction == ConstantsAndStatics.DISMISS) {
					dismissAlarm();
				} else if (powerBtnAction == ConstantsAndStatics.SNOOZE) {
					snoozeAlarm();
				}
			}
		}
	};

	//---------------------------------------------------------------------------------

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		notifID = UniqueNotifID.getID();

		// Do NOT move this!!!!
		alarmDetails = Objects.requireNonNull(Objects.requireNonNull(intent.getExtras())
			.getBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS));

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
				startForeground(notifID, buildRingNotification(),
					ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
			} else {
				startForeground(notifID, buildRingNotification(),
					ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE);
			}
		} else {
			startForeground(notifID, buildRingNotification());
		}
		isThisServiceRunning = true;
		preMatureDeath = true;
		alarmRingingStarted = false;

		ConstantsAndStatics.cancelScheduledPeriodicWork(this);

		sharedPreferences = getSharedPreferences(
			ConstantsAndStatics.SHARED_PREF_FILE_NAME, MODE_PRIVATE);

		audioFocusController = new AudioFocusController.Builder(this)
			.setAcceptsDelayedFocus(true)
			.setAudioFocusChangeListener(this)
			.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
			.setUsage(AudioAttributes.USAGE_ALARM)
			.setPauseWhenAudioIsNoisy(false)
			.setStream(AudioManager.STREAM_ALARM)
			.setDurationHint(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
			.build();

		isShakeActive = sharedPreferences.getInt(
			ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SHAKE_OPERATION,
			ConstantsAndStatics.SNOOZE) != ConstantsAndStatics.DO_NOTHING;

		alarmID = alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID);

		// Kill Service_SnoozeAlarm if it is running for a different alarm.
		if (Service_SnoozeAlarm.isThisServiceRunning &&
			Service_SnoozeAlarm.alarmID != alarmID) {
			stopService(new Intent(this, Service_SnoozeAlarm.class));
		}

		Uri chosenToneUri = alarmDetails.getParcelable(
			ConstantsAndStatics.BUNDLE_KEY_ALARM_TONE_URI);
		try (InputStream ignored = getContentResolver().openInputStream(
			Objects.requireNonNull(chosenToneUri))) {
			// Alarm tone file exists.
			alarmToneUri = chosenToneUri;
		} catch (Exception ex) {
			// Tone file can either not be accessed, or not available in the file system.
			// Fall back to default tone.
			alarmToneUri = Settings.System.DEFAULT_ALARM_ALERT_URI;
		}

		numberOfTimesTheAlarmHasBeenSnoozed = intent.getExtras()
			.getInt(EXTRA_NO_OF_TIMES_SNOOZED, 0);

		ringTimer = new CountDownTimer(60000, 1000) {

			@Override
			public void onTick(long millisUntilFinished) {
			}

			@Override
			public void onFinish() {
				snoozeAlarm();
			}
		};

		snsMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		notificationManager = (NotificationManager) getSystemService(
			NOTIFICATION_SERVICE);

		assert snsMgr != null;
		assert vibrator != null;
		assert audioManager != null;
		assert notificationManager != null;

		alarmDatabase = AlarmDatabase.getInstance(this);

		initialAlarmStreamVolume = audioManager.getStreamVolume(
			AudioManager.STREAM_ALARM);

		powerBtnAction = sharedPreferences.getInt(
			ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_POWER_BTN_OPERATION,
			ConstantsAndStatics.DISMISS);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConstantsAndStatics.ACTION_SNOOZE_ALARM);
		intentFilter.addAction(ConstantsAndStatics.ACTION_CANCEL_ALARM);
		intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		intentFilter.addAction(Intent.ACTION_SCREEN_ON);
		ContextCompat.registerReceiver(this, broadcastReceiver, intentFilter,
			ContextCompat.RECEIVER_NOT_EXPORTED);

		audioFocusController.requestFocus();

		loadRepeatDays();

		return START_NOT_STICKY;
	}

	//----------------------------------------------------------------------------------

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (preMatureDeath) {
			dismissAlarm();
		}

		try {
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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (notificationManager.isNotificationPolicyAccessGranted()) {
				audioManager.setStreamVolume(AudioManager.STREAM_ALARM,
					initialAlarmStreamVolume, 0);
			}
		}
		unregisterReceiver(broadcastReceiver);
		isThisServiceRunning = false;
		alarmID = -1;
	}

	//----------------------------------------------------------------------------------

	/**
	 * Reads the repeat days from alarm database.
	 * <p>
	 * I have received some crash reports from Google Play stating that
	 * {@code NullPointerException} is being thrown in {@code dismissAlarm()} at the
	 * statement {@code Collections.sort(repeatDays)}. It seems that even if repeat is
	 * ON,
	 * the repeat days list is null. That is why we are re-reading the repeat days from
	 * the database as a temporary fix.
	 * </p>
	 */
	private void loadRepeatDays() {
		if (alarmDetails.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_REPEAT_ON)) {
			AlarmDatabase alarmDatabase = AlarmDatabase.getInstance(this);
			Thread thread = new Thread(() -> repeatDays = new ArrayList<>(
				alarmDatabase.alarmDAO()
					.getAlarmRepeatDays(
						alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID))));
			thread.start();
		} else {
			repeatDays = null;
		}
	}

	//----------------------------------------------------------------------------------

	/**
	 * Initialises the shake sensor.
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
	 * </p>
	 *
	 * @return A {@link Notification} instance that can be displayed to the user.
	 */
	@NonNull
	private Notification buildRingNotification() {

		createNotificationChannel();

		Intent fullScreenIntent = new Intent(this, Activity_RingAlarm.class)
			.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
			.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
			.putExtras(alarmDetails);

		int flags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
			?
			PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
			: PendingIntent.FLAG_UPDATE_CURRENT;

		PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 3054,
			fullScreenIntent, flags);

		String alarmMessage = alarmDetails.getString(
			ConstantsAndStatics.BUNDLE_KEY_ALARM_MESSAGE, null);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
			Integer.toString(ConstantsAndStatics.NOTIF_CHANNEL_ID_ALARM))
			.setContentTitle(getResources().getString(R.string.app_name))
			.setPriority(NotificationCompat.PRIORITY_MAX)
			.setCategory(NotificationCompat.CATEGORY_ALARM)
			.setSmallIcon(R.drawable.ic_notif)
			.setContentIntent(fullScreenPendingIntent)
			.setOnlyAlertOnce(true)
			.setFullScreenIntent(fullScreenPendingIntent, true);

		if (alarmMessage != null) {
			builder.setContentTitle(getString(R.string.app_name))
				.setContentText(alarmMessage)
				.setStyle(new NotificationCompat.BigTextStyle().bigText(alarmMessage));
		} else {
			builder.setContentText(getString(R.string.notifContent_ring));
		}

		return builder.build();
	}

	//----------------------------------------------------------------------------------

	/**
	 * Initialises the {@link MediaPlayer}, and starts ringing the alarm.
	 */
	private void ringAlarm() {

		notificationManager.notify(notifID, buildRingNotification());
		initialiseShakeSensor();

		if (!(alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE) ==
			ConstantsAndStatics.ALARM_TYPE_VIBRATE_ONLY)) {

			mediaPlayer = new MediaPlayer();
			AudioAttributes attributes = new AudioAttributes.Builder()
				.setUsage(AudioAttributes.USAGE_ALARM)
				.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
				.build();

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				if (notificationManager.isNotificationPolicyAccessGranted()) {

					audioManager.setStreamVolume(AudioManager.STREAM_ALARM,
						alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_VOLUME),
						0);
				}
			} else {
				audioManager.setStreamVolume(AudioManager.STREAM_ALARM,
					alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_VOLUME), 0);
			}

			try {
				mediaPlayer.setDataSource(this, alarmToneUri);
				mediaPlayer.setAudioAttributes(attributes);
				mediaPlayer.setLooping(true);
				mediaPlayer.prepare();
			} catch (IOException ignored) {
			}

			if (alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE) ==
				ConstantsAndStatics.ALARM_TYPE_SOUND_AND_VIBRATE) {
				alarmVibration();
			}
			mediaPlayer.start();

		} else {
			alarmVibration();
		}

		ringTimer.start();
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
					VibrationEffect.createWaveform(vibrationPattern, vibrationAmplitudes,
						0));
			} else {
				vibrator.vibrate(vibrationPattern, 0);
			}
		}
	}

	//----------------------------------------------------------------------------------

	/**
	 * Snoozes the alarm. If snooze is off, or the snooze frequency has been reached, the
	 * alarm will be cancelled by calling {@link #dismissAlarm()}.
	 */
	private void snoozeAlarm() {

		stopRinging();

		if (alarmDetails.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_SNOOZE_ON)) {

			if (numberOfTimesTheAlarmHasBeenSnoozed <
				alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_FREQUENCY)) {

				numberOfTimesTheAlarmHasBeenSnoozed++;

				Intent intent = new Intent(this, Service_SnoozeAlarm.class)
					.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, alarmDetails)
					.putExtra(EXTRA_NO_OF_TIMES_SNOOZED,
						numberOfTimesTheAlarmHasBeenSnoozed);
				ContextCompat.startForegroundService(this, intent);

				preMatureDeath = false;
				stopForeground(true);
				stopSelf();
			} else {
				dismissAlarm();
			}
		} else {
			dismissAlarm();
		}
	}

	//----------------------------------------------------------------------------------

	/**
	 * Dismisses the current alarm, and sets the next alarm if repeat is enabled.
	 */
	private void dismissAlarm() {

		stopRinging();
		cancelPendingIntent();

		Thread thread_toggleAlarm =
			new Thread(() -> alarmDatabase.alarmDAO()
				.toggleAlarm(alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID),
					0));

		//////////////////////////////////////////////////////
		// If repeat is on, set another alarm. Otherwise
		// toggle alarm state in database.
		/////////////////////////////////////////////////////
		if (alarmDetails.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_REPEAT_ON,
			false) &&
			repeatDays != null && repeatDays.size() > 0) {

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
			setAlarm(alarmDateTime);

		} else {

			thread_toggleAlarm.start();

			try {
				thread_toggleAlarm.join();
			} catch (InterruptedException ignored) {
			}
		}

		ConstantsAndStatics.schedulePeriodicWork(this);
		preMatureDeath = false;
		stopForeground(true);
		stopSelf();

	}

	//----------------------------------------------------------------------------------

	/**
	 * Stops the ringing alarm. Also sends a broadcast to {@link Activity_RingAlarm} to
	 * finish itsef.
	 */
	private void stopRinging() {
		try {
			ringTimer.cancel();

			if ((alarmDetails
				.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE) ==
				ConstantsAndStatics.ALARM_TYPE_VIBRATE_ONLY) || (alarmDetails
				.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE) ==
				ConstantsAndStatics.ALARM_TYPE_SOUND_AND_VIBRATE)) {
				vibrator.cancel();
			}
			if (mediaPlayer != null) {
				mediaPlayer.stop();
			}
		} catch (Exception ignored) {
		} finally {
			if (isShakeActive) {
				snsMgr.unregisterListener(this);
			}
			Intent intent = new Intent(
				ConstantsAndStatics.ACTION_DESTROY_RING_ALARM_ACTIVITY);
			sendBroadcast(intent);
		}
		audioFocusController.abandonFocus();
	}

	//----------------------------------------------------------------------------------

	/**
	 * Sets the next alarn in case of a repeat alarm.
	 *
	 * @param alarmDateTime The date and time when the alarm is to be set.
	 */
	private void setAlarm(@NonNull LocalDateTime alarmDateTime) {

		AlarmManager alarmManager = (AlarmManager) getSystemService(
			Context.ALARM_SERVICE);

		Intent intent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class)
			.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM)
			.setFlags(Intent.FLAG_RECEIVER_FOREGROUND)
			.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, alarmDetails);

		int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
			? PendingIntent.FLAG_IMMUTABLE
			: 0;

		PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
			alarmID, intent, flags);

		ZonedDateTime zonedDateTime = ZonedDateTime.of(alarmDateTime.withSecond(0),
			ZoneId.systemDefault());

		alarmManager.setAlarmClock(
			new AlarmManager.AlarmClockInfo(zonedDateTime.toEpochSecond() * 1000,
				pendingIntent), pendingIntent);
	}

	//----------------------------------------------------------------------------------
	/**
	 * While testing, we found that sometimes, the alarm was being reset at a later date
	 * unintentionally. This function cancels such an unintentional alarm.
	 */
	private void cancelPendingIntent() {

		AlarmManager alarmManager =
			(AlarmManager) getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class)
			.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM)
			.setFlags(Intent.FLAG_RECEIVER_FOREGROUND)
			.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, alarmDetails);

		int flags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
			?
			PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE
			: PendingIntent.FLAG_NO_CREATE;

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
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
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
						ConstantsAndStatics.SNOOZE)
						== ConstantsAndStatics.SNOOZE && alarmDetails.getBoolean(
						ConstantsAndStatics.BUNDLE_KEY_IS_SNOOZE_ON)) {
						snoozeAlarm();
					} else {
						dismissAlarm();
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

	//----------------------------------------------------------------------------------

	@Override
	public void decreaseVolume() {
		// No ducking.
	}

	//----------------------------------------------------------------------------------

	@Override
	public void increaseVolume() {
		// No ducking.
	}

	//----------------------------------------------------------------------------------

	@Override
	public void pause() {
		// No pause.
	}

	//----------------------------------------------------------------------------------

	@Override
	public void resume() {
		if (!alarmRingingStarted) {
			alarmRingingStarted = true;
			ringAlarm();
		}
	}

}
