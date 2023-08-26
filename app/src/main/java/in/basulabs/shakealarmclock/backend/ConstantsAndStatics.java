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

import static android.content.Context.POWER_SERVICE;
import static android.content.res.Configuration.UI_MODE_NIGHT_MASK;
import static android.content.res.Configuration.UI_MODE_NIGHT_YES;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
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

import in.basulabs.shakealarmclock.frontend.Activity_AlarmDetails;
import in.basulabs.shakealarmclock.frontend.Activity_AlarmsList;
import in.basulabs.shakealarmclock.frontend.Activity_RingAlarm;
import in.basulabs.shakealarmclock.frontend.Activity_RingtonePicker;

/**
 * A class containing all the constants required by this app.
 */
public final class ConstantsAndStatics {

	/**
	 * Bundle key for the Bundle that is passed with intent from
	 * {@link Activity_AlarmDetails} to {@link Activity_AlarmsList} containing the data
	 * set by the user.
	 */
	public static final String BUNDLE_KEY_ALARM_DETAILS =
		"in.basulabs.shakealarmclock.ALARM_DETAILS_BUNDLE";

	/**
	 * Bundle key for the alarm hour. The value is an integer.
	 */
	public static final String BUNDLE_KEY_ALARM_HOUR =
		"in.basulabs.shakealarmclock.ALARM_HOUR";

	public static final String BUNDLE_KEY_DATE_TIME =
		"in.basulabs.shakealarmclock.ALARM_DATE_TIME";

	/**
	 * Bundle key for the alarm minute. The value is an integer.
	 */
	public static final String BUNDLE_KEY_ALARM_MINUTE =
		"in.basulabs.shakealarmclock.ALARM_MINUTES";

	/**
	 * Bundle key for the alarm type. The value is one of {@link #ALARM_TYPE_SOUND_ONLY},
	 * {@link #ALARM_TYPE_VIBRATE_ONLY} or {@link #ALARM_TYPE_SOUND_AND_VIBRATE}.
	 */
	public static final String BUNDLE_KEY_ALARM_TYPE =
		"in.basulabs.shakealarmclock.ALARM_TYPE";

	/**
	 * Bundle key for the alarm volume. The value is an integer.
	 */
	public static final String BUNDLE_KEY_ALARM_VOLUME =
		"in.basulabs.shakealarmclock.ALARM_VOLUME";

	/**
	 * Bundle key for the alarm snooze interval. The value is an integer.
	 */
	public static final String BUNDLE_KEY_SNOOZE_TIME_IN_MINS =
		"in.basulabs.shakealarmclock.SNOOZE_TIME_IN_MINS";

	/**
	 * Bundle key for the number of times the alarm should snooze itself. The value is an
	 * integer.
	 */
	public static final String BUNDLE_KEY_SNOOZE_FREQUENCY =
		"in.basulabs.shakealarmclock.SNOOZE_FREQUENCY";

	/**
	 * Bundle key denoting whether repeat is on or off. Value is boolean.
	 */
	public static final String BUNDLE_KEY_IS_REPEAT_ON =
		"in.basulabs.shakealarmclock.IS_REPEAT_ON";

	/**
	 * Bundle key denoting whether snooze is on or off. Value is boolean.
	 */
	public static final String BUNDLE_KEY_IS_SNOOZE_ON =
		"in.basulabs.shakealarmclock.IS_SNOOZE_ON";

	/**
	 * Bundle key denoting whether alarm is on or off. Value is boolean.
	 */
	public static final String BUNDLE_KEY_IS_ALARM_ON =
		"in.basulabs.shakealarmclock.IS_ALARM_ON";

	/**
	 * Bundle key for the alarm repeat days. The value is an ArrayList of Integer type.
	 * Monday is 1 and Sunday is 7.
	 */
	public static final String BUNDLE_KEY_REPEAT_DAYS =
		"in.basulabs.shakealarmclock.REPEAT_DAYS";

	/**
	 * Denotes that the alarm type will be "Sound".
	 */
	public static final int ALARM_TYPE_SOUND_ONLY = 0;

	/**
	 * Denotes that the alarm type will be "Vibrate".
	 */
	public static final int ALARM_TYPE_VIBRATE_ONLY = 1;

	/**
	 * Denotes that the alarm type will be "Sound and vibrate".
	 */
	public static final int ALARM_TYPE_SOUND_AND_VIBRATE = 2;

	/**
	 * Intent action to be sent to broadcast receiver for sounding alarm.
	 */
	public static final String ACTION_DELIVER_ALARM =
		"in.basulabs.shakealarmclock.DELIVER_ALARM";

	/**
	 * The day on which the alarm is supposed to ring.
	 */
	public static final String BUNDLE_KEY_ALARM_DAY =
		"in.basulabs.shakealarmclock.ALARM_DAY";

	/**
	 * The month in which the alarm will ring.
	 */
	public static final String BUNDLE_KEY_ALARM_MONTH =
		"in.basulabs.shakealarmclock.ALARM_MONTH";

	/**
	 * The year in which the alarm will ring.
	 */
	public static final String BUNDLE_KEY_ALARM_YEAR =
		"in.basulabs.shakealarmclock.ALARM_YEAR";

	/**
	 * Bundle key for the personalised alarm message.
	 */
	public static final String BUNDLE_KEY_ALARM_MESSAGE =
		"in.basulabs.shakealarmclock.ALARM_MESSAGE";

	/**
	 * Bundle key for Uri of the alarm tone.
	 */
	public static final String BUNDLE_KEY_ALARM_TONE_URI =
		"in.basulabs.shakealarmclock.ALARM_TONE_URI";

	/**
	 * Bundle key: Indicates whether the user has explicitly chosen a date for that
	 * alarm.
	 */
	public static final String BUNDLE_KEY_HAS_USER_CHOSEN_DATE =
		"in.basulabs.shakealarmclock.HAS_USER_CHOSEN_DATE";

	/**
	 * Intent action delivered to {@link android.content.BroadcastReceiver} in
	 * {@link Service_RingAlarm} instructing it to snooze the alarm.
	 */
	public static final String ACTION_SNOOZE_ALARM =
		"in.basulabs.shakealarmclock.SNOOZE_ALARM";

	/**
	 * Intent action delivered to {@link android.content.BroadcastReceiver} in
	 * {@link Service_RingAlarm} instructing it to cancel the alarm.
	 */
	public static final String ACTION_CANCEL_ALARM =
		"in.basulabs.shakealarmclock.CANCEL_ALARM";

	/**
	 * The name of the {@link android.content.SharedPreferences} file for this app.
	 */
	public static final String SHARED_PREF_FILE_NAME =
		"in.basulabs.shakealarmclock.SHARED_PREF_FILE";

	/**
	 * Intent action indicating that {@link Activity_AlarmDetails} should prepare for a
	 * new alarm.
	 */
	public static final String ACTION_NEW_ALARM =
		"in.basulabs.shakealarmclock.ACTION_NEW_ALARM";

	/**
	 * Intent action indicating that {@link Activity_AlarmDetails} should show the
	 * details
	 * of an old alarm.
	 */
	public static final String ACTION_EXISTING_ALARM =
		"in.basulabs.shakealarmclock.ACTION_EXISTING_ALARM";

	/**
	 * Intent action indicating that a new alarm is being requested to be created from a
	 * direct intent to the app rather than the user clicking on the "Add" button.
	 */
	public static final String ACTION_NEW_ALARM_FROM_INTENT =
		"in.basulabs.shakealarmclock.ACTION_NEW_ALARM_FROM_INTENT";

	/**
	 * Indicates whether {@link Activity_RingtonePicker} should play the ringtone when
	 * the
	 * user clicks on a {@link android.widget.RadioButton}. Default: {@code true}.
	 */
	public static final String EXTRA_PLAY_RINGTONE =
		"in.basulabs.shakealarmclock.EXTRA_PLAY_RINGTONE";

	/**
	 * Bundle key for the old alarm hour.
	 * <p>This is passed from {@link Activity_AlarmDetails} to
	 * {@link Activity_AlarmsList} if the user saves the edits made to an existing alarm.
	 * Using this and {@link #BUNDLE_KEY_OLD_ALARM_MINUTE}, {@link Activity_AlarmsList}
	 * deletes the old alarm and adds/activates the new alarm.
	 * </p>
	 *
	 * @see #BUNDLE_KEY_OLD_ALARM_MINUTE
	 */
	public static final String BUNDLE_KEY_OLD_ALARM_HOUR =
		"in.basulabs.shakealarmclock.OLD_ALARM_HOUR";

	/**
	 * Bundle key for the old alarm minute.
	 * <p>This is passed from {@link Activity_AlarmDetails} to
	 * {@link Activity_AlarmsList} if the user saves the edits made to an existing alarm.
	 * Using this and {@link #BUNDLE_KEY_OLD_ALARM_HOUR}, {@link Activity_AlarmsList}
	 * deletes the old alarm and adds/activates the new alarm.
	 * </p>
	 *
	 * @see #BUNDLE_KEY_OLD_ALARM_HOUR
	 */
	public static final String BUNDLE_KEY_OLD_ALARM_MINUTE =
		"in.basulabs.shakealarmclock.OLD_ALARM_MINUTE";

	/**
	 * Bundle key for the alarm ID.
	 */
	public static final String BUNDLE_KEY_ALARM_ID =
		"in.basulabs.shakealarmclock.OLD_ALARM_ID";

	/**
	 * Broadcast action: {@link Activity_RingAlarm} should now be destroyed.
	 */
	public static final String ACTION_DESTROY_RING_ALARM_ACTIVITY =
		"in.basulabs.shakealarmclock.DESTROY_RING_ALARM_ACTIVITY";

	/**
	 * {@link android.content.SharedPreferences} key to store the default shake
	 * operation.
	 * Can be either {@link #DISMISS} or {@link #SNOOZE}.
	 */
	public static final String SHARED_PREF_KEY_DEFAULT_SHAKE_OPERATION =
		"in.basulabs.shakealarmclock.DEFAULT_SHAKE_OPERATION";

	/**
	 * {@link android.content.SharedPreferences} key to store the default power button
	 * operation. Can be either {@link #DISMISS} or {@link #SNOOZE}.
	 */
	public static final String SHARED_PREF_KEY_DEFAULT_POWER_BTN_OPERATION =
		"in.basulabs.shakealarmclock.DEFAULT_POWER_BTN_OPERATION";

	/**
	 * {@link android.content.SharedPreferences} key to store the sensitivity of the
	 * shake
	 * detector. The data type is {@code float}.
	 */
	public static final String SHARED_PREF_KEY_SHAKE_SENSITIVITY =
		"in.basulabs.shakealarmclock.SHAKE_SENSITIVITY";

	/**
	 * The default sensitivity of the shake detector.
	 */
	public static final float DEFAULT_SHAKE_SENSITIVITY = 3.2f;

	/**
	 * Indicates that the ringing alarm should be snoozed.
	 */
	public static final int SNOOZE = 0;

	/**
	 * Indicates that the ringing alarm should be dismissed completely.
	 */
	public static final int DISMISS = 1;

	public static final int DO_NOTHING = 2;

	/**
	 * {@link android.content.SharedPreferences} key to store the default snooze state.
	 * The value is {@code boolean}.
	 */
	public static final String SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON =
		"in.basulabs.shakealarmclock.DEFAULT_SNOOZE_STATE";

	/**
	 * {@link android.content.SharedPreferences} key to store the default snooze interval
	 * in minutes.
	 */
	public static final String SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL =
		"in.basulabs.shakealarmclock.DEFAULT_SNOOZE_INTERVAL";

	/**
	 * {@link android.content.SharedPreferences} key to store the default snooze
	 * frequency, i.e. the number of times the alarm will ring before being cancelled
	 * automatically.
	 */
	public static final String SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ =
		"in.basulabs.shakealarmclock.DEFAULT_SNOOZE_FREQUENCY";

	/**
	 * {@link android.content.SharedPreferences} key to store the default alarm tone Uri.
	 * If the file is unavailable, it will be replaced by the default alarm tone during
	 * runtime. The value is {@code String}; should be converted to Uri using
	 * {@link android.net.Uri#parse(String)}.
	 */
	public static final String SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI =
		"in.basulabs.shakealarmclock.DEFAULT_ALARM_TONE_URI";

	/**
	 * {@link android.content.SharedPreferences} key to store the default alarm volume.
	 */
	public static final String SHARED_PREF_KEY_DEFAULT_ALARM_VOLUME =
		"in.basulabs.shakealarmclock.DEFAULT_ALARM_VOLUME";

	/**
	 * The app will set its theme according to time. From 10:00 PM to 6:00 AM, the theme
	 * will be dark, and light otherwise.
	 */
	public static final int THEME_AUTO_TIME = 0;

	/**
	 * Indicates that the theme of the app should be light. Corresponds to
	 * {@link androidx.appcompat.app.AppCompatDelegate#MODE_NIGHT_NO}.
	 */
	public static final int THEME_LIGHT = 1;

	/**
	 * Indicates that the theme of the app should be light. Corresponds to
	 * {@link androidx.appcompat.app.AppCompatDelegate#MODE_NIGHT_YES}.
	 */
	public static final int THEME_DARK = 2;

	/**
	 * Indicates that the theme of the app should be light. Corresponds to
	 * {@link androidx.appcompat.app.AppCompatDelegate#MODE_NIGHT_FOLLOW_SYSTEM}.
	 * Available only on Android Q+.
	 */
	public static final int THEME_SYSTEM = 3;

	/**
	 * {@link android.content.SharedPreferences} key to store the current theme. Can only
	 * have the values {@link #THEME_AUTO_TIME}, {@link #THEME_LIGHT},
	 * {@link #THEME_DARK}
	 * or {@link #THEME_SYSTEM}.
	 */
	public static final String SHARED_PREF_KEY_THEME =
		"in.basulabs.shakealarmclock.THEME";

	/**
	 * {@link android.content.SharedPreferences} key indicating whether a new alarm tone
	 * chosen by the user should be set as the default tone for future alarms. Data type:
	 * {@code boolean}.
	 */
	public static final String SHARED_PREF_KEY_AUTO_SET_TONE =
		"in.basulabs.shakealarmclock.AUTO_SET_TONE";

	/**
	 * Unique name for work.
	 */
	public static final String WORK_TAG_ACTIVATE_ALARMS =
		"in.basulabs.WORK_ACTIVATE_ALARMS";

	//-----------------------------------------------------------------------------------

	/**
	 * Creates a {@link PeriodicWorkRequest} and enqueues a unique work using
	 * {@link WorkManager#enqueueUniquePeriodicWork(String, ExistingPeriodicWorkPolicy,
	 * PeriodicWorkRequest)}.
	 *
	 * @param context The {@link Context} that is scheduling the work.
	 */
	public static void schedulePeriodicWork(Context context) {

		try {
			WorkManager.initialize(context,
				new Configuration.Builder()
					.setMinimumLoggingLevel(Log.DEBUG)
					.build());
		} catch (Exception ignored) {
		}

		Constraints constraint = new Constraints.Builder()
			.setRequiresBatteryNotLow(true)
			.build();

		PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(
			Worker_ActivateAlarms.class, 1, TimeUnit.HOURS)
			.setInitialDelay(30, TimeUnit.MINUTES)
			.setConstraints(constraint)
			.build();

		WorkManager.getInstance(context)
			.enqueueUniquePeriodicWork(WORK_TAG_ACTIVATE_ALARMS,
				ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
				periodicWorkRequest);
	}

	//-----------------------------------------------------------------------------------

	/**
	 * Cancels a scheduled work using {@link WorkManager#cancelUniqueWork(String)}.
	 *
	 * @param context The {@link Context} that is requesting the work to be cancelled.
	 */
	public static void cancelScheduledPeriodicWork(Context context) {

		try {
			WorkManager.initialize(context,
				new Configuration.Builder()
					.setMinimumLoggingLevel(Log.DEBUG).build());
		} catch (Exception ignored) {
		}

		WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG_ACTIVATE_ALARMS);
	}

	//-----------------------------------------------------------------------------------

	/**
	 * Get the theme that can be applied using
	 * {@link AppCompatDelegate#setDefaultNightMode(int)}.
	 *
	 * @param theme The theme value as stored in
	 *    {@link android.content.SharedPreferences}. Can only have the values
	 *    {@link #THEME_AUTO_TIME}, {@link #THEME_LIGHT}, {@link #THEME_DARK} or
	 *    {@link #THEME_SYSTEM}.
	 * @return Can have the values {@link AppCompatDelegate#MODE_NIGHT_YES},
	 *    {@link AppCompatDelegate#MODE_NIGHT_NO} or
	 *    {@link AppCompatDelegate#MODE_NIGHT_FOLLOW_SYSTEM}.
	 */
	public static int getTheme(int theme) {
		switch (theme) {
			case THEME_SYSTEM -> {
				return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
			}
			case THEME_LIGHT -> {
				return AppCompatDelegate.MODE_NIGHT_NO;
			}
			case THEME_DARK -> {
				return AppCompatDelegate.MODE_NIGHT_YES;
			}
			default -> {
				if (LocalTime.now().isAfter(LocalTime.of(21, 59)) ||
					LocalTime.now().isBefore(LocalTime.of(6, 0))) {
					return AppCompatDelegate.MODE_NIGHT_YES;
				} else {
					return AppCompatDelegate.MODE_NIGHT_NO;
				}
			}
		}
	}

	//-----------------------------------------------------------------------------------

	public static void killServices(Context context, int alarmID) {

		if (Service_RingAlarm.isThisServiceRunning &&
			Service_RingAlarm.alarmID == alarmID) {
			Intent intent1 = new Intent(context, Service_RingAlarm.class);
			context.stopService(intent1);
		} else if (Service_SnoozeAlarm.isThisServiceRunning &&
			Service_SnoozeAlarm.alarmID == alarmID) {
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
	 * @param repeatDays The days when the alarm should be repeated. Should follow
	 *    {@link DayOfWeek} enum.
	 * @return A {@link LocalDateTime} object representing when the alarm should ring.
	 * 	This should be transformed into a {@link java.time.ZonedDateTime} object and then
	 * 	passed to {@link android.app.AlarmManager}.
	 */
	public static LocalDateTime getAlarmDateTime(LocalDate alarmDate,
		LocalTime alarmTime, boolean isRepeatOn,
		@Nullable ArrayList<Integer> repeatDays) {

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

			alarmDateTime = LocalDateTime.of(alarmDate, alarmTime);

			if (!alarmDateTime.isAfter(LocalDateTime.now())) {
				alarmDateTime = alarmDateTime.plusDays(1);
			}
		}

		return alarmDateTime.withSecond(0).withNano(0);
	}

	//----------------------------------------------------------------------------------

	/**
	 * Notification ID for the channels used for ringing alarms.
	 */
	public static final int NOTIF_CHANNEL_ID_ALARM = 621;

	/**
	 * Notification ID for the channels used for snooze alarms.
	 */
	public static final int NOTIF_CHANNEL_ID_SNOOZE = 622;

	/**
	 * Notification ID for error channel.
	 */
	public static final int NOTIF_CHANNEL_ID_ERROR = 623;

	/**
	 * Notification Channel ID for update service.
	 */
	public static final int NOTIF_CHANNEL_ID_BOOT = 625;

	/**
	 * {@link android.content.SharedPreferences} key to store whether existing
	 * notification channels have been deleted once after the update.
	 */
	public static final String SHARED_PREF_KEY_NOTIF_CHANNELS_DELETED =
		"in.basulabs.shakealarmclock.NotifChannelsDeleted";

	//-----------------------------------------------------------------------------

	/**
	 * Indicates that the requested permission is essential and the app won't work
	 * without
	 * it.
	 */
	public static final int PERMISSION_LEVEL_ESSENTIAL = 1;

	public static final int PERMISSION_LEVEL_OPTIONAL = -1;

	public static final int PERMISSION_LEVEL_RECOMMENDED = 0;

	public static String EXTRA_PERMS_REQUESTED =
		"in.basulabs.shakealarmclock.PERMS_TO_BE_REQUESTED";

	/**
	 * A {@code Bundle} containing the level of the permissions being requested.
	 * <p>
	 * Key-value pairs expected in the {@code Bundle} should be of the following
	 * form:<br>
	 * <b>Key:</b> {@link Permission#androidString()}.<br>
	 * <b>Corresponding value:</b> One of
	 * {@link #PERMISSION_LEVEL_ESSENTIAL}, {@link #PERMISSION_LEVEL_RECOMMENDED} or
	 * {@link #PERMISSION_LEVEL_OPTIONAL}.
	 */
	public static final String EXTRA_PERMS_REQUESTED_LEVEL =
		"in.basulabs.shakealarmclock.PERMS_REQUESTED_LEVEL";

	/**
	 * {@link android.content.SharedPreferences} key for a
	 * {@code HashMap<String, Integer>} that stores how many times a permission has been
	 * requested by this app since installation/data clear.
	 * <p>
	 * Key-value pairs expected in the {@code Bundle} should be of the following
	 * form:<br>
	 * <b>Key:</b> {@link Permission#androidString()}.<br>
	 * <b>Corresponding value:</b> Number of times this particular permission has been
	 * requested by the app (any component) in the past.
	 * <p>
	 * An entry should be cleared when a certain permission has been given.
	 */
	public static final String SHARED_PREF_KEY_TIMES_PERMS_REQUESTED =
		"in.basulabs.shakealarmclock.SHARED_PREF_KEY_TIMES_PERMS_REQUESTED";

	public static final String SHARED_PREF_KEY_NO_OF_TIMES_APP_OPENED =
		"in.basulabs.shakealarmclock.SHARED_PREF_KEY_NUMBER_OF_TIMES_APP_OPENED";

	public static final String SHARED_PREF_KEY_REQUESTED_NON_ESSENTIAL_PERMS_RECENTLY =
		"in.basulabs.shakealarmclock" +
			".SHARED_PREF_KEY_REQUESTED_NON_ESSENTIAL_PERMS_RECENTLY";

	@NonNull
	public static ArrayList<String> getEssentialPerms(@NonNull Context context) {

		ArrayList<String> reqdPermsList = new ArrayList<>();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			if (!((AlarmManager) context.getSystemService(
				Context.ALARM_SERVICE)).canScheduleExactAlarms()) {
				reqdPermsList.add(Manifest.permission.SCHEDULE_EXACT_ALARM);
			}
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (ContextCompat.checkSelfPermission(context,
				Manifest.permission.POST_NOTIFICATIONS)
				!= PackageManager.PERMISSION_GRANTED) {

				reqdPermsList.add(Manifest.permission.POST_NOTIFICATIONS);
			}
		}

		return reqdPermsList;
	}

	@NonNull
	public static ArrayList<String> getNonEssentialPerms(@NonNull Context context) {

		ArrayList<String> permsList = new ArrayList<>();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

			PowerManager powerManager =
				(PowerManager) context.getSystemService(POWER_SERVICE);
			if (!powerManager.isIgnoringBatteryOptimizations(context.getPackageName())) {
				permsList.add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
			}

			NotificationManager notifManager =
				(NotificationManager) context.getSystemService(
					Context.NOTIFICATION_SERVICE);
			if (!notifManager.isNotificationPolicyAccessGranted()) {
				permsList.add(Manifest.permission.ACCESS_NOTIFICATION_POLICY);
			}
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (ContextCompat.checkSelfPermission(context,
				Manifest.permission.READ_MEDIA_AUDIO)
				!= PackageManager.PERMISSION_GRANTED) {

				permsList.add(Manifest.permission.READ_MEDIA_AUDIO);
			}
		} else {
			if (ContextCompat.checkSelfPermission(context,
				Manifest.permission.READ_EXTERNAL_STORAGE) !=
				PackageManager.PERMISSION_GRANTED) {

				permsList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
			}
		}

		return permsList;
	}

	public static final String DEBUG_TAG = "basulabsDebug";

	/**
	 * Determines whether the app theme is dark or light.
	 * <p>
	 * <a href="https://stackoverflow.com/a/58525184/8387076">Courtesy</a>
	 *
	 * @param context The {@link Context} of the caller.
	 * @return {@code true} if the theme is dark, {@code false} otherwise (including if
	 * 	the theme is undefined).
	 */
	public static boolean isNightModeActive(@NonNull Context context) {
		int defaultNightMode = AppCompatDelegate.getDefaultNightMode();
		if (defaultNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
			return true;
		}
		if (defaultNightMode == AppCompatDelegate.MODE_NIGHT_NO) {
			return false;
		}

		int currentNightMode = context.getResources().getConfiguration().uiMode
			& UI_MODE_NIGHT_MASK;

		return switch (currentNightMode) {
			case UI_MODE_NIGHT_YES -> true;
			default -> false;
		};
	}


}
