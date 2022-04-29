package in.basulabs.shakealarmclock;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * A class containing all the constants required by this app.
 */
final class ConstantsAndStatics {

	/**
	 * Bundle key for the Bundle that is passed with intent from {@link Activity_AlarmDetails} to {@link Activity_AlarmsList} containing the data set
	 * by the user.
	 */
	static final String BUNDLE_KEY_ALARM_DETAILS = "in.basulabs.shakealarmclock.ALARM_DETAILS_BUNDLE";

	/**
	 * Bundle key for the alarm hour. The value is an integer.
	 */
	static final String BUNDLE_KEY_ALARM_HOUR = "in.basulabs.shakealarmclock.ALARM_HOUR";

	static final String BUNDLE_KEY_DATE_TIME = "in.basulabs.shakealarmclock.ALARM_DATE_TIME";

	/**
	 * Bundle key for the alarm minute. The value is an integer.
	 */
	static final String BUNDLE_KEY_ALARM_MINUTE = "in.basulabs.shakealarmclock.ALARM_MINUTES";

	/**
	 * Bundle key for the alarm type. The value is one of {@link #ALARM_TYPE_SOUND_ONLY}, {@link #ALARM_TYPE_VIBRATE_ONLY} or {@link
	 * #ALARM_TYPE_SOUND_AND_VIBRATE}.
	 */
	static final String BUNDLE_KEY_ALARM_TYPE = "in.basulabs.shakealarmclock.ALARM_TYPE";

	/**
	 * Bundle key for the alarm volume. The value is an integer.
	 */
	static final String BUNDLE_KEY_ALARM_VOLUME = "in.basulabs.shakealarmclock.ALARM_VOLUME";

	/**
	 * Bundle key for the alarm snooze interval. The value is an integer.
	 */
	static final String BUNDLE_KEY_SNOOZE_TIME_IN_MINS = "in.basulabs.shakealarmclock.SNOOZE_TIME_IN_MINS";

	/**
	 * Bundle key for the number of times the alarm should snooze itself. The value is an integer.
	 */
	static final String BUNDLE_KEY_SNOOZE_FREQUENCY = "in.basulabs.shakealarmclock.SNOOZE_FREQUENCY";

	/**
	 * Bundle key denoting whether repeat is on or off. Value is boolean.
	 */
	static final String BUNDLE_KEY_IS_REPEAT_ON = "in.basulabs.shakealarmclock.IS_REPEAT_ON";

	/**
	 * Bundle key denoting whether snooze is on or off. Value is boolean.
	 */
	static final String BUNDLE_KEY_IS_SNOOZE_ON = "in.basulabs.shakealarmclock.IS_SNOOZE_ON";

	/**
	 * Bundle key denoting whether alarm is on or off. Value is boolean.
	 */
	static final String BUNDLE_KEY_IS_ALARM_ON = "in.basulabs.shakealarmclock.IS_ALARM_ON";

	/**
	 * Bundle key for the alarm repeat days. The value is an ArrayList of Integer type. Monday is 1 and Sunday is 7.
	 */
	static final String BUNDLE_KEY_REPEAT_DAYS = "in.basulabs.shakealarmclock.REPEAT_DAYS";

	/**
	 * Denotes that the alarm type will be "Sound".
	 */
	static final int ALARM_TYPE_SOUND_ONLY = 0;

	/**
	 * Denotes that the alarm type will be "Vibrate".
	 */
	static final int ALARM_TYPE_VIBRATE_ONLY = 1;

	/**
	 * Denotes that the alarm type will be "Sound and vibrate".
	 */
	static final int ALARM_TYPE_SOUND_AND_VIBRATE = 2;

	/**
	 * Intent action to be sent to broadcast receiver for sounding alarm.
	 */
	static final String ACTION_DELIVER_ALARM = "in.basulabs.shakealarmclock.DELIVER_ALARM";

	/**
	 * The day on which the alarm is supposed to ring.
	 */
	static final String BUNDLE_KEY_ALARM_DAY = "in.basulabs.shakealarmclock.ALARM_DAY";

	/**
	 * The month in which the alarm will ring.
	 */
	static final String BUNDLE_KEY_ALARM_MONTH = "in.basulabs.shakealarmclock.ALARM_MONTH";

	/**
	 * The year in which the alarm will ring.
	 */
	static final String BUNDLE_KEY_ALARM_YEAR = "in.basulabs.shakealarmclock.ALARM_YEAR";

	/**
	 * Bundle key for the personalised alarm message.
	 */
	static final String BUNDLE_KEY_ALARM_MESSAGE = "in.basulabs.shakealarmclock.ALARM_MESSAGE";

	/**
	 * Bundle key for Uri of the alarm tone.
	 */
	static final String BUNDLE_KEY_ALARM_TONE_URI = "in.basulabs.shakealarmclock.ALARM_TONE_URI";

	/**
	 * Bundle key: Indicates whether the user has explicitly chosen a date for that alarm.
	 */
	static final String BUNDLE_KEY_HAS_USER_CHOSEN_DATE = "in.basulabs.shakealarmclock.HAS_USER_CHOSEN_DATE";

	/**
	 * Intent action delivered to {@link android.content.BroadcastReceiver} in {@link Service_RingAlarm} instructing it to snooze the alarm.
	 */
	static final String ACTION_SNOOZE_ALARM = "in.basulabs.shakealarmclock.SNOOZE_ALARM";

	/**
	 * Intent action delivered to {@link android.content.BroadcastReceiver} in {@link Service_RingAlarm} instructing it to cancel the alarm.
	 */
	static final String ACTION_CANCEL_ALARM = "in.basulabs.shakealarmclock.CANCEL_ALARM";

	/**
	 * The name of the {@link android.content.SharedPreferences} file for this app.
	 */
	static final String SHARED_PREF_FILE_NAME = "in.basulabs.shakealarmclock.SHARED_PREF_FILE";

	/**
	 * Intent action indicating that {@link Activity_AlarmDetails} should prepare for a new alarm.
	 */
	static final String ACTION_NEW_ALARM = "in.basulabs.shakealarmclock.ACTION_NEW_ALARM";

	/**
	 * Intent action indicating that {@link Activity_AlarmDetails} should show the details of an old alarm.
	 */
	static final String ACTION_EXISTING_ALARM = "in.basulabs.shakealarmclock.ACTION_EXISTING_ALARM";

	/**
	 * Intent action indicating that a new alarm is being requested to be created from a direct intent to the app rather than the user clicking on
	 * the
	 * "Add" button.
	 */
	static final String ACTION_NEW_ALARM_FROM_INTENT = "in.basulabs.shakealarmclock.ACTION_NEW_ALARM_FROM_INTENT";

	/**
	 * Indicates whether {@link Activity_RingtonePicker} should play the ringtone when the user clicks on a {@link android.widget.RadioButton}.
	 * Default: {@code true}.
	 */
	static final String EXTRA_PLAY_RINGTONE = "in.basulabs.shakealarmclock.EXTRA_PLAY_RINGTONE";

	/**
	 * Bundle key for the old alarm hour.
	 * <p>This is passed from {@link Activity_AlarmDetails} to
	 * {@link Activity_AlarmsList} if the user saves the edits made to an existing alarm. Using this and {@link #BUNDLE_KEY_OLD_ALARM_MINUTE}, {@link
	 * Activity_AlarmsList} deletes the old alarm and adds/activates the new alarm.
	 * </p>
	 *
	 * @see #BUNDLE_KEY_OLD_ALARM_MINUTE
	 */
	static final String BUNDLE_KEY_OLD_ALARM_HOUR = "in.basulabs.shakealarmclock.OLD_ALARM_HOUR";

	/**
	 * Bundle key for the old alarm minute.
	 * <p>This is passed from {@link Activity_AlarmDetails} to
	 * {@link Activity_AlarmsList} if the user saves the edits made to an existing alarm. Using this and {@link #BUNDLE_KEY_OLD_ALARM_HOUR}, {@link
	 * Activity_AlarmsList} deletes the old alarm and adds/activates the new alarm.
	 * </p>
	 *
	 * @see #BUNDLE_KEY_OLD_ALARM_HOUR
	 */
	static final String BUNDLE_KEY_OLD_ALARM_MINUTE = "in.basulabs.shakealarmclock.OLD_ALARM_MINUTE";

	/**
	 * Bundle key for the alarm ID.
	 */
	static final String BUNDLE_KEY_ALARM_ID = "in.basulabs.shakealarmclock.OLD_ALARM_ID";

	/**
	 * Broadcast action: {@link Activity_RingAlarm} should now be destroyed.
	 */
	static final String ACTION_DESTROY_RING_ALARM_ACTIVITY = "in.basulabs.shakealarmclock.DESTROY_RING_ALARM_ACTIVITY";

	/**
	 * {@link android.content.SharedPreferences} key indicating whether the read storage permission was asked before. This is used to determine if
	 * the
	 * user had chosen "Don't ask again" before denying the permission. The value is {@code boolean}.
	 */
	static final String SHARED_PREF_KEY_PERMISSION_WAS_ASKED_BEFORE = "in.basulabs.shakealarmclock.PERMISSION_WAS_ASKED_BEFORE";

	/**
	 * {@link android.content.SharedPreferences} key to store the default shake operation. Can be either {@link #DISMISS} or {@link #SNOOZE}.
	 */
	static final String SHARED_PREF_KEY_DEFAULT_SHAKE_OPERATION = "in.basulabs.shakealarmclock.DEFAULT_SHAKE_OPERATION";

	/**
	 * {@link android.content.SharedPreferences} key to store the default power button operation. Can be either {@link #DISMISS} or {@link #SNOOZE}.
	 */
	static final String SHARED_PREF_KEY_DEFAULT_POWER_BTN_OPERATION = "in.basulabs.shakealarmclock.DEFAULT_POWER_BTN_OPERATION";

	/**
	 * {@link android.content.SharedPreferences} key to store the sensitivity of the shake detector. The data type is {@code float}.
	 */
	static final String SHARED_PREF_KEY_SHAKE_SENSITIVITY = "in.basulabs.shakealarmclock.SHAKE_SENSITIVITY";

	/**
	 * {@link android.content.SharedPreferences} key indicating whether {@link AlertDialog_BatteryOptimizations} should be shown in {@link
	 * Activity_AlarmsList}. Data type is {@code boolean}.
	 */
	static final String SHARED_PREF_KEY_SHOW_BATTERY_OPTIM_DIALOG = "in.basulabs.shakealarmclock.SHOW_BATTERY_OPTIM_DIALOG";

	/**
	 * The default sensitivity of the shake detector.
	 */
	static final float DEFAULT_SHAKE_SENSITIVITY = 3.2f;

	/**
	 * Indicates that the ringing alarm should be snoozed.
	 */
	static final int SNOOZE = 0;

	/**
	 * Indicates that the ringing alarm should be dismissed completely.
	 */
	static final int DISMISS = 1;

	static final int DO_NOTHING = 2;

	/**
	 * {@link android.content.SharedPreferences} key to store the default snooze state. The value is {@code boolean}.
	 */
	static final String SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON = "in.basulabs.shakealarmclock.DEFAULT_SNOOZE_STATE";

	/**
	 * {@link android.content.SharedPreferences} key to store the default snooze interval in minutes.
	 */
	static final String SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL = "in.basulabs.shakealarmclock.DEFAULT_SNOOZE_INTERVAL";

	/**
	 * {@link android.content.SharedPreferences} key to store the default snooze frequency, i.e. the number of times the alarm will ring before being
	 * cancelled automatically.
	 */
	static final String SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ = "in.basulabs.shakealarmclock.DEFAULT_SNOOZE_FREQUENCY";

	/**
	 * {@link android.content.SharedPreferences} key to store the default alarm tone Uri. If the file is unavailable, it will be replaced by the
	 * default alarm tone during runtime. The value is {@code String}; should be converted to Uri using {@link android.net.Uri#parse(String)}.
	 */
	static final String SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI = "in.basulabs.shakealarmclock.DEFAULT_ALARM_TONE_URI";

	/**
	 * {@link android.content.SharedPreferences} key to store the default alarm volume.
	 */
	static final String SHARED_PREF_KEY_DEFAULT_ALARM_VOLUME = "in.basulabs.shakealarmclock.DEFAULT_ALARM_VOLUME";

	/**
	 * The app will set its theme according to time. From 10:00 PM to 6:00 AM, the theme will be dark, and light otherwise.
	 */
	static final int THEME_AUTO_TIME = 0;

	/**
	 * Indicates that the theme of the app should be light. Corresponds to {@link androidx.appcompat.app.AppCompatDelegate#MODE_NIGHT_NO}.
	 */
	static final int THEME_LIGHT = 1;

	/**
	 * Indicates that the theme of the app should be light. Corresponds to {@link androidx.appcompat.app.AppCompatDelegate#MODE_NIGHT_YES}.
	 */
	static final int THEME_DARK = 2;

	/**
	 * Indicates that the theme of the app should be light. Corresponds to {@link androidx.appcompat.app.AppCompatDelegate#MODE_NIGHT_FOLLOW_SYSTEM}.
	 * Available only on Android Q+.
	 */
	static final int THEME_SYSTEM = 3;

	/**
	 * {@link android.content.SharedPreferences} key to store the current theme. Can only have the values {@link #THEME_AUTO_TIME}, {@link
	 * #THEME_LIGHT}, {@link #THEME_DARK} or {@link #THEME_SYSTEM}.
	 */
	static final String SHARED_PREF_KEY_THEME = "in.basulabs.shakealarmclock.THEME";

	/**
	 * {@link android.content.SharedPreferences} key indicating whether a new alarm tone chosen by the user should be set as the default tone for
	 * future alarms. Data type: {@code boolean}.
	 */
	static final String SHARED_PREF_KEY_AUTO_SET_TONE = "in.basulabs.shakealarmclock.AUTO_SET_TONE";

	/**
	 * Unique name for work.
	 */
	static final String WORK_TAG_ACTIVATE_ALARMS = "in.basulabs.WORK_ACTIVATE_ALARMS";

	//---------------------------------------------------------------------------------------------------------

	/**
	 * Creates a {@link PeriodicWorkRequest} and enqueues a unique work using {@link WorkManager#enqueueUniquePeriodicWork(String,
	 * ExistingPeriodicWorkPolicy, PeriodicWorkRequest)}.
	 *
	 * @param context The {@link Context} that is scheduling the work.
	 */
	static void schedulePeriodicWork(Context context) {

		try {
			WorkManager.initialize(context, new Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).build());
		} catch (Exception ignored) {
		}

		Constraints constraint = new Constraints.Builder()
				.setRequiresBatteryNotLow(true)
				.build();

		PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(Worker_ActivateAlarms.class, 1, TimeUnit.HOURS)
				.setInitialDelay(30, TimeUnit.MINUTES)
				.setConstraints(constraint)
				.build();

		WorkManager.getInstance(context)
		           .enqueueUniquePeriodicWork(WORK_TAG_ACTIVATE_ALARMS, ExistingPeriodicWorkPolicy.REPLACE, periodicWorkRequest);
	}

	//---------------------------------------------------------------------------------------------------------

	/**
	 * Cancels a scheduled work using {@link WorkManager#cancelUniqueWork(String)}.
	 *
	 * @param context The {@link Context} that is requesting the work to be cancelled.
	 */
	static void cancelScheduledPeriodicWork(Context context) {

		try {
			WorkManager.initialize(context, new Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).build());
		} catch (Exception ignored) {
		}

		WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG_ACTIVATE_ALARMS);
	}

	//---------------------------------------------------------------------------------------------------------

	/**
	 * Get the theme that can be applied using {@link AppCompatDelegate#setDefaultNightMode(int)}.
	 *
	 * @param theme The theme value as stored in {@link android.content.SharedPreferences}. Can only have the values {@link #THEME_AUTO_TIME}, {@link
	 * #THEME_LIGHT}, {@link #THEME_DARK} or {@link #THEME_SYSTEM}.
	 * @return Can have the values {@link AppCompatDelegate#MODE_NIGHT_YES}, {@link AppCompatDelegate#MODE_NIGHT_NO} or {@link
	 * AppCompatDelegate#MODE_NIGHT_FOLLOW_SYSTEM}.
	 */
	static int getTheme(int theme) {
		switch (theme) {
			case THEME_SYSTEM:
				return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
			case THEME_LIGHT:
				return AppCompatDelegate.MODE_NIGHT_NO;
			case THEME_DARK:
				return AppCompatDelegate.MODE_NIGHT_YES;
			default:
				if (LocalTime.now().isAfter(LocalTime.of(21, 59)) || LocalTime.now().isBefore(LocalTime.of(6, 0))) {
					return AppCompatDelegate.MODE_NIGHT_YES;
				} else {
					return AppCompatDelegate.MODE_NIGHT_NO;
				}
		}
	}

	//---------------------------------------------------------------------------------------------------------

	static void killServices(Context context, int alarmID) {

		if (Service_RingAlarm.isThisServiceRunning && Service_RingAlarm.alarmID == alarmID) {
			Intent intent1 = new Intent(context, Service_RingAlarm.class);
			context.stopService(intent1);
		} else if (Service_SnoozeAlarm.isThisServiceRunning && Service_SnoozeAlarm.alarmID == alarmID) {
			Intent intent1 = new Intent(context, Service_SnoozeAlarm.class);
			context.stopService(intent1);
		}
	}

	//---------------------------------------------------------------------------------------------------------

	/**
	 * Get the date and time when the alarm should ring.
	 *
	 * @param alarmDate The alarm date as chosen by the user.
	 * @param alarmTime The alarm time as chosen by the user.
	 * @param isRepeatOn Whether repeat is on or off.
	 * @param repeatDays The days when the alarm should be repeated. Should follow {@link DayOfWeek} enum.
	 * @return A {@link LocalDateTime} object representing when the alarm should ring. This should be transformed into a {@link
	 * java.time.ZonedDateTime} object and then passed to {@link android.app.AlarmManager}.
	 */
	static LocalDateTime getAlarmDateTime(LocalDate alarmDate, LocalTime alarmTime, boolean isRepeatOn, @Nullable ArrayList<Integer> repeatDays) {

		LocalDateTime alarmDateTime;

		if (isRepeatOn && repeatDays != null && repeatDays.size() > 0) {

			Collections.sort(repeatDays);

			alarmDateTime = LocalDateTime.of(LocalDate.now(), alarmTime);
			int dayOfWeek = alarmDateTime.getDayOfWeek().getValue();

			for (int i = 0; i < repeatDays.size(); i++) {
				if (repeatDays.get(i) == dayOfWeek) {
					if (alarmTime.isAfter(LocalTime.now())) {
						// Alarm possible today, nothing more to do, break out of loop.
						break;
					}
				} else if (repeatDays.get(i) > dayOfWeek) {
					/////////////////////////////////////////////////////////////////////////
					// There is a day available in the same week for the alarm to ring;
					// select that day and break from loop.
					////////////////////////////////////////////////////////////////////////
					alarmDateTime = alarmDateTime.with(TemporalAdjusters.next(DayOfWeek.of(repeatDays.get(i))));
					break;
				}
				if (i == repeatDays.size() - 1) {
					// No day possible in this week. Select the first available date from next week.
					alarmDateTime = alarmDateTime.with(TemporalAdjusters.next(DayOfWeek.of(repeatDays.get(0))));
				}
			}

		} else {

			alarmDateTime = LocalDateTime.of(alarmDate, alarmTime);

			if (!alarmDateTime.isAfter(LocalDateTime.now())) {
				alarmDateTime = alarmDateTime.plusDays(1);
			}
		}

		return alarmDateTime.withSecond(0).withNano(0);
	}

	//---------------------------------------------------------------------------------------------------------

	/**
	 * Notification ID for the channels used for ringing alarms.
	 */
	static final int NOTIF_CHANNEL_ID_ALARM = 621;

	/**
	 * Notification ID for the channels used for snooze alarms.
	 */
	static final int NOTIF_CHANNEL_ID_SNOOZE = 622;

	/**
	 * Notification ID for error channel.
	 */
	static final int NOTIF_CHANNEL_ID_ERROR = 623;

	/**
	 * Notification Channel ID for update service.
	 */
	static final int NOTIF_CHANNEL_ID_UPDATE = 625;

	/**
	 * {@link android.content.SharedPreferences} key to store whether existing notification
	 * channels have been deleted once after the update.
	 */
	static final String SHARED_PREF_KEY_NOTIF_CHANNELS_DELETED = "in.basulabs.shakealarmclock.NotifChannelsDeleted";


}
