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
import android.os.Process;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
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

public class Service_RingAlarm extends Service implements SensorEventListener {

	private Bundle alarmDetails;

	private MediaPlayer mediaPlayer;

	private static final int NOTIFICATION_ID = 20153;

	private AlarmDatabase alarmDatabase;

	private CountDownTimer ringTimer;

	private SensorManager snsMgr;
	private Vibrator vibrator;
	private AudioManager audioManager;
	private NotificationManager notificationManager;

	private long lastShakeTime;

	static final int MINIMUM_MILLIS_BETWEEN_SHAKES = 600;

	private int initialAlarmStreamVolume;

	private int numberOfTimesTheAlarmHasBeenSnoozed;

	private Uri alarmToneUri;

	public static boolean isThisServiceRunning = false;
	public static int alarmID;

	private SharedPreferences sharedPreferences;

	private boolean isShakeActive;

	public static final String BUNDLE_KEY_NO_OF_TIMES_SNOOZED = "in.basulabs.shakealarmclock" +
			".NO_OF_TIMES_SNOOZED";

	//--------------------------------------------------------------------------------------------------

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Objects.equals(intent.getAction(), ConstantsAndStatics.ACTION_SNOOZE_ALARM)) {
				//Log.e(this.getClass().toString(), "Received broadcast to snooze alarm.");
				snoozeAlarm();
			} else if (Objects
					.equals(intent.getAction(), ConstantsAndStatics.ACTION_CANCEL_ALARM)) {
				//Log.e(this.getClass().toString(), "Received broadcast to cancel alarm.");
				dismissAlarm();
			}
		}
	};

	//--------------------------------------------------------------------------------------------------

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Log.e(this.getClass().toString(), "Inside onStartCommand");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			startForeground(NOTIFICATION_ID, buildRingNotification(),
					ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE);
		} else {
			startForeground(NOTIFICATION_ID, buildRingNotification());
		}
		isThisServiceRunning = true;

		if (Service_AlarmActivater.isThisServiceRunning) {
			Intent intent1 = new Intent(this, Service_AlarmActivater.class);
			stopService(intent1);
		}
		if (Service_AlarmActivater.pid != - 1) {
			Process.killProcess(Service_AlarmActivater.pid);
		}

		alarmDetails = Objects.requireNonNull(intent.getExtras())
				.getBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS);

		numberOfTimesTheAlarmHasBeenSnoozed = intent.getExtras()
				.getInt(BUNDLE_KEY_NO_OF_TIMES_SNOOZED, 0);

		assert alarmDetails != null;

		sharedPreferences = getSharedPreferences(ConstantsAndStatics.SHARED_PREF_FILE_NAME,
				MODE_PRIVATE);

		isShakeActive = sharedPreferences
				.getInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SHAKE_OPERATION,
						ConstantsAndStatics.SNOOZE) != ConstantsAndStatics.DO_NOTHING;

		assert alarmDetails != null;
		alarmToneUri = alarmDetails.getParcelable(ConstantsAndStatics.BUNDLE_KEY_ALARM_TONE_URI);
		//Log.e(this.getClass().toString(), "Received Uri: " + alarmToneUri.toString());

		alarmID = alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID);

		//Log.e(this.getClass().getSimpleName(), "alarmID = " + alarmID);

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
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		assert snsMgr != null;
		assert vibrator != null;
		assert audioManager != null;
		assert notificationManager != null;

		alarmDatabase = AlarmDatabase.getInstance(this);

		initialAlarmStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConstantsAndStatics.ACTION_SNOOZE_ALARM);
		intentFilter.addAction(ConstantsAndStatics.ACTION_CANCEL_ALARM);
		registerReceiver(broadcastReceiver, intentFilter);

		ringAlarm();

		return START_NOT_STICKY;
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public void onDestroy() {
		super.onDestroy();
		//Log.e(this.getClass().getSimpleName(), "onDestroy() called.");
		isThisServiceRunning = false;
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
		audioManager.setStreamVolume(AudioManager.STREAM_ALARM, initialAlarmStreamVolume, 0);
		unregisterReceiver(broadcastReceiver);
	}

	//--------------------------------------------------------------------------------------------------

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

	//--------------------------------------------------------------------------------------------------

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

	//--------------------------------------------------------------------------------------------------

	/**
	 * Creates a notification that can be shown when the alarm is ringing. Has a full screen intent
	 * to {@link Activity_RingAlarm}. The content intent points to {@link Activity_AlarmsList}.
	 *
	 * @return A {@link Notification} that can be used with {@link #startForeground(int,
	 *        Notification)} or displayed with {@link NotificationManager#notify(int, Notification)}.
	 */
	private Notification buildRingNotification() {
		createNotificationChannel();

		Intent fullScreenIntent = new Intent(this, Activity_RingAlarm.class);
		fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 3054,
				fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
				Integer.toString(NOTIFICATION_ID))
				.setContentTitle(getResources().getString(R.string.app_name))
				.setContentText(getResources().getString(R.string.notifContent_ring))
				.setPriority(NotificationCompat.PRIORITY_MAX)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.setSmallIcon(R.drawable.ic_notif)
				.setContentIntent(fullScreenPendingIntent)
				.setFullScreenIntent(fullScreenPendingIntent, true);

		return builder.build();
	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Initialises the {@link MediaPlayer}, and starts ringing the alarm.
	 */
	private void ringAlarm() {

		//Log.e(this.getClass().getSimpleName(), "Ring Alarm called.");

		notificationManager.notify(NOTIFICATION_ID, buildRingNotification());
		initialiseShakeSensor();

		if (! (alarmDetails
				.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE) == ConstantsAndStatics.ALARM_TYPE_VIBRATE_ONLY)) {

			mediaPlayer = new MediaPlayer();
			AudioAttributes attributes = new AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_ALARM)
					.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
					.build();

			audioManager.setStreamVolume(AudioManager.STREAM_ALARM,
					alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_VOLUME), 0);

			try {
				mediaPlayer.setDataSource(this, alarmToneUri);
				mediaPlayer.setAudioAttributes(attributes);
				mediaPlayer.setLooping(true);
				mediaPlayer.prepare();
			} catch (IOException ignored) {
			}

			if (alarmDetails
					.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE) == ConstantsAndStatics.ALARM_TYPE_SOUND_AND_VIBRATE) {
				alarmVibration();
			}
			mediaPlayer.start();

		} else {
			alarmVibration();
		}

		ringTimer.start();
	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Vibrate the phone for the alarm.
	 */
	private void alarmVibration() {
		long[] vibrationPattern = new long[]{0, 600, 200, 600, 200, 800, 200, 1000};
		int[] vibrationAmplitudes = new int[]{0, 255, 0, 255, 0, 255, 0, 255};
		// -1 : Play exactly once

		if (vibrator.hasVibrator()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				if (vibrator.hasAmplitudeControl()) {
					vibrator.vibrate(
							VibrationEffect
									.createWaveform(vibrationPattern, vibrationAmplitudes, 0));
				}
			} else {
				vibrator.vibrate(vibrationPattern, 0);
			}
		}
	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Snoozes the alarm. If snooze is off, or the snoze frequency has been reached, the alarm will
	 * be cancelled by calling {@link #dismissAlarm()}.
	 */
	private void snoozeAlarm() {

		stopRinging();

		if (alarmDetails.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_SNOOZE_ON)) {

			if (numberOfTimesTheAlarmHasBeenSnoozed < alarmDetails
					.getInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_FREQUENCY)) {

				numberOfTimesTheAlarmHasBeenSnoozed++;

				Intent intent = new Intent(this, Service_SnoozeAlarm.class);
				intent.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, alarmDetails);
				intent.putExtra(BUNDLE_KEY_NO_OF_TIMES_SNOOZED,
						numberOfTimesTheAlarmHasBeenSnoozed);
				ContextCompat.startForegroundService(this, intent);

				stopSelf();
			} else {
				dismissAlarm();
			}
		} else {
			dismissAlarm();
		}
	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Dismisses the current alarm, and sets the next alarm if repeat is enabled.
	 */
	private void dismissAlarm() {

		stopRinging();
		cancelPendingIntent();

		Thread thread_toggleAlarm = new Thread(
				() -> alarmDatabase.alarmDAO()
						.toggleAlarm(alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID),
								0));

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
			LocalTime alarmTime = LocalTime
					.of(alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
							alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE));

			ArrayList<Integer> repeatDays = alarmDetails
					.getIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS);

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
					alarmDateTime = alarmDateTime
							.with(TemporalAdjusters.next(DayOfWeek.of(repeatDays.get(i))));
					break;
				}
				if (i == repeatDays.size() - 1) {
					// No day possible in this week. Select the first available date from next week.
					alarmDateTime = alarmDateTime
							.with(TemporalAdjusters.next(DayOfWeek.of(repeatDays.get(0))));
				}
			}
			setAlarm(alarmDateTime);
		}
		stopSelf();

	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Stops the ringing alarm. Also sends a broadcast to {@link Activity_RingAlarm} to finish
	 * itsef.
	 */
	private void stopRinging() {
		try {
			ringTimer.cancel();
			//snoozeTimer.cancel();
			if ((alarmDetails
					.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE) == ConstantsAndStatics.ALARM_TYPE_VIBRATE_ONLY) || (alarmDetails
					.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE) == ConstantsAndStatics.ALARM_TYPE_SOUND_AND_VIBRATE)) {
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
			Intent intent = new Intent(ConstantsAndStatics.ACTION_DESTROY_RING_ALARM_ACTIVITY);
			sendBroadcast(intent);
		}
	}

	//--------------------------------------------------------------------------------------------------

	private void setAlarm(LocalDateTime alarmDateTime) {
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(this, AlarmBroadcastReceiver.class);
		intent.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM);
		intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		intent.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, alarmDetails);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarmID, intent, 0);

		ZonedDateTime zonedDateTime = ZonedDateTime
				.of(alarmDateTime.withSecond(0), ZoneId.systemDefault());

		alarmManager.setAlarmClock(
				new AlarmManager.AlarmClockInfo(zonedDateTime.toEpochSecond() * 1000,
						pendingIntent), pendingIntent);
	}

	//---------------------------------------------------------------------------------------------------

	private void cancelPendingIntent() {
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(this, AlarmBroadcastReceiver.class);
		intent.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM);
		intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		intent.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, alarmDetails);

		PendingIntent pendingIntent = PendingIntent
				.getBroadcast(this, alarmID, intent, PendingIntent.FLAG_NO_CREATE);

		if (pendingIntent != null) {
			alarmManager.cancel(pendingIntent);
		}
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	//--------------------------------------------------------------------------------------------------

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
			//Log.e(this.getClass().getSimpleName(), "gForce: " + gForce);
			// gForce will be close to 1 when there is no movement.

			if (gForce >= 3.8f) {
				long currTime = System.currentTimeMillis();
				if (Math.abs(currTime - lastShakeTime) > MINIMUM_MILLIS_BETWEEN_SHAKES) {
					//Log.e(this.getClass().getSimpleName(), "Event detected, ");
					lastShakeTime = currTime;
					shakeVibration();
					if (sharedPreferences
							.getInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SHAKE_OPERATION,
									ConstantsAndStatics.SNOOZE) == ConstantsAndStatics.SNOOZE
							&& alarmDetails
							.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_SNOOZE_ON)) {
						snoozeAlarm();
					} else {
						dismissAlarm();
					}
				}

			}
		}
	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Creates a vibration for a small period of time, indicating that the app has registered a
	 * shake event.
	 */
	private void shakeVibration() {
		if (vibrator.hasVibrator()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				vibrator.vibrate(
						VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
			} else {
				vibrator.vibrate(200);
			}
			Thread thread = new Thread(() -> {
				try {
					Thread.sleep(200);
				} catch (InterruptedException ignored) {
				}
			});
			thread.start();
			try {
				thread.join();
			} catch (InterruptedException ignored) {
			}
		}
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {
	}
}
