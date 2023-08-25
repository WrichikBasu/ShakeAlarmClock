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
package in.basulabs.shakealarmclock.frontend;

import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.ACTION_NEW_ALARM_FROM_INTENT;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.ALARM_TYPE_SOUND_AND_VIBRATE;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.ALARM_TYPE_SOUND_ONLY;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.ALARM_TYPE_VIBRATE_ONLY;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_DAY;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_ID;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_MONTH;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_TONE_URI;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_VOLUME;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_YEAR;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_IS_REPEAT_ON;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.SHARED_PREF_FILE_NAME;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_VOLUME;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.VoiceInteractor;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import in.basulabs.shakealarmclock.backend.AlarmBroadcastReceiver;
import in.basulabs.shakealarmclock.backend.AlarmDatabase;
import in.basulabs.shakealarmclock.backend.AlarmEntity;
import in.basulabs.shakealarmclock.backend.ConstantsAndStatics;
import in.basulabs.shakealarmclock.backend.Service_RingAlarm;
import in.basulabs.shakealarmclock.backend.Service_SnoozeAlarm;

/**
 * An activity to manage activity actions for this app.
 * <p>
 * This activity has no display and is transparent. It handles incoming intents and
 * finishes itself in {@link #onCreate(Bundle)}.
 * <p>
 * The following activity actions are handled:
 * <ul>
 *     <li>{@link android.provider.AlarmClock#ACTION_SET_ALARM}</li>
 *     <li>{@link android.provider.AlarmClock#ACTION_DISMISS_ALARM}</li>
 *     <li>{@link android.provider.AlarmClock#ACTION_SNOOZE_ALARM}</li>
 * </ul>
 * </p>
 */
public class Activity_IntentManager extends AppCompatActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();

		switch (Objects.requireNonNull(intent.getAction())) {
			case AlarmClock.ACTION_SET_ALARM -> {
				if (!intent.hasExtra(AlarmClock.EXTRA_HOUR) || !intent.hasExtra(
					AlarmClock.EXTRA_MINUTES)) {
					///////////////////////////////////////////////////////////////////////
					// These two extras are necessary for an alarm to be set. Without
					// these, the user will be redirected to Activity_AlarmsList.
					///////////////////////////////////////////////////////////////////////

					Intent intent1 = new Intent(this, Activity_AlarmsList.class);
					intent1.setAction(ACTION_NEW_ALARM_FROM_INTENT)
						.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
						.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
					startActivity(intent1);

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						if (isVoiceInteraction()) {

							Bundle status = new Bundle();
							VoiceInteractor.Prompt prompt = new VoiceInteractor.Prompt(
								new String[]{"You can do that in the app."},
								"You can do that in the app.");

							VoiceInteractor.Request request =
								new VoiceInteractor.CompleteVoiceRequest(
									prompt, status);
							getVoiceInteractor().submitRequest(request);
						}
					}
				} else {
					setAlarm();

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						if (isVoiceInteraction()) {

							Bundle status = new Bundle();
							VoiceInteractor.Prompt prompt = new VoiceInteractor.Prompt(
								new String[]{"Your alarm has been set by Shake Alarm" +
									" Clock."},
								"Your alarm has been set by Shake Alarm Clock.");

							VoiceInteractor.Request request =
								new VoiceInteractor.CompleteVoiceRequest(
									prompt, status);
							getVoiceInteractor().submitRequest(request);
						}
					}
				}
			}
			case AlarmClock.ACTION_DISMISS_ALARM -> {
				if (Service_RingAlarm.isThisServiceRunning ||
					Service_SnoozeAlarm.isThisServiceRunning) {
					sendBroadcast(new Intent(ConstantsAndStatics.ACTION_CANCEL_ALARM));
				} else {
					Intent intent2 = new Intent(this, Activity_AlarmsList.class)
						.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
						.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
					startActivity(intent2);
				}
			}
			case AlarmClock.ACTION_SNOOZE_ALARM -> {
				Intent intent1 = new Intent();
				intent1.setAction(ConstantsAndStatics.ACTION_SNOOZE_ALARM);
				sendBroadcast(intent1);
			}
		}

		finish();

	}

	//-------------------------------------------------------------------------------------------------------------

	private void setAlarm() {

		SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF_FILE_NAME,
			MODE_PRIVATE);

		///////////////////////////////////////////////
		// Retrieve all data passed with intent:
		//////////////////////////////////////////////

		Intent intent = getIntent();

		LocalTime alarmTime = LocalTime.of(
			Objects.requireNonNull(intent.getExtras()).getInt(AlarmClock.EXTRA_HOUR),
			intent.getExtras().getInt(AlarmClock.EXTRA_MINUTES));

		ArrayList<Integer> repeatDays;
		if (intent.hasExtra(AlarmClock.EXTRA_DAYS)) {

			repeatDays = intent.getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS);
			assert repeatDays != null;

			// The EXTRA_DAYS follow java.util.Calendar (Sunday is 1 and Saturday is 7).
			// In this app, we follow
			// java.time.DayOfWeek enum (Monday is 1 and Sunday is 7). We change
			// repeatDays accordingly.
			ArrayList<Integer> temp = new ArrayList<>();
			for (int i : repeatDays) {
				if (i == 1) {
					temp.add(7);
				} else {
					temp.add(i - 1);
				}
			}
			repeatDays = temp;

			Collections.sort(repeatDays);
		} else {
			repeatDays = null;
		}

		boolean isRepeatOn = repeatDays != null;

		Uri alarmToneUri;
		if (intent.hasExtra(AlarmClock.EXTRA_RINGTONE)) {

			if (Objects.equals(intent.getExtras().getString(AlarmClock.EXTRA_RINGTONE),
				AlarmClock.VALUE_RINGTONE_SILENT)) {
				alarmToneUri = null;
			} else {

				alarmToneUri = Uri.parse(
					intent.getExtras().getString(AlarmClock.EXTRA_RINGTONE));

				if (!doesFileExist(alarmToneUri)) {
					// Uri invalid or file doesn't exist; fall back to default tone
					alarmToneUri = Uri.parse(sharedPreferences.getString(
						SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI,
						"content://settings/system" +
							"/alarm_alert"));
				}

			}
		} else {
			alarmToneUri = Uri.parse(
				sharedPreferences.getString(SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI,
					"content://settings/system/alarm_alert"));
		}

		int volume;
		if (alarmToneUri != null) {
			AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
			volume = sharedPreferences.getInt(SHARED_PREF_KEY_DEFAULT_ALARM_VOLUME,
				audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM) - 1);
		} else {
			volume = 0;
		}

		int alarmType;
		if (intent.hasExtra(AlarmClock.EXTRA_VIBRATE)) {
			if (intent.getExtras().getBoolean(AlarmClock.EXTRA_VIBRATE)) {
				alarmType = volume > 0
					? ALARM_TYPE_SOUND_AND_VIBRATE
					: ALARM_TYPE_VIBRATE_ONLY;
			} else {
				alarmType = ALARM_TYPE_SOUND_ONLY;
			}
		} else {
			alarmType = volume > 0
				? ALARM_TYPE_SOUND_AND_VIBRATE
				: ALARM_TYPE_VIBRATE_ONLY;
		}

		String alarmMessage = intent.getExtras()
			.getString(AlarmClock.EXTRA_MESSAGE, null);

		////////////////////////////////
		// Now set the alarm:
		////////////////////////////////
		AlarmDatabase alarmDatabase = AlarmDatabase.getInstance(this);

		LocalDateTime alarmDateTime = ConstantsAndStatics.getAlarmDateTime(
			LocalDate.now(), alarmTime, isRepeatOn, repeatDays);

		if (intent.getExtras().getBoolean(AlarmClock.EXTRA_SKIP_UI, false)) {
			// We have been asked to skip the UI. Alarm will be set by this activity
			// itself.

			AlarmEntity alarmEntity = new AlarmEntity(alarmTime.getHour(),
				alarmTime.getMinute(), true,
				sharedPreferences.getBoolean(SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON, true),
				sharedPreferences.getInt(SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL, 5),
				sharedPreferences.getInt(SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ, 3),
				volume, isRepeatOn, alarmType, alarmDateTime.getDayOfMonth(),
				alarmDateTime.getMonthValue(),
				alarmDateTime.getYear(), alarmToneUri, alarmMessage, false);

			AtomicInteger alarmID = new AtomicInteger();

			Thread thread = new Thread(() -> {
				alarmDatabase.alarmDAO().addAlarm(alarmEntity);
				alarmID.set(alarmDatabase.alarmDAO()
					.getAlarmId(alarmEntity.alarmHour, alarmEntity.alarmMinutes));
			});

			thread.start();
			try {
				thread.join();
			} catch (InterruptedException ignored) {
			}

			AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

			Intent intent1 = new Intent(this, AlarmBroadcastReceiver.class)
				.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM)
				.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

			alarmEntity.alarmID = alarmID.get();
			Bundle data = alarmEntity.getAlarmDetailsInABundle();
			data.putIntegerArrayList(BUNDLE_KEY_REPEAT_DAYS, repeatDays);
			data.remove(BUNDLE_KEY_ALARM_ID);
			data.putInt(BUNDLE_KEY_ALARM_ID, alarmID.get());
			intent1.putExtra(BUNDLE_KEY_ALARM_DETAILS, data);

			PendingIntent pendingIntent =
				PendingIntent.getBroadcast(getApplicationContext(), alarmID.get(),
					intent1, PendingIntent.FLAG_IMMUTABLE);

			ZonedDateTime zonedDateTime = ZonedDateTime.of(
				alarmDateTime.withSecond(0).withNano(0), ZoneId.systemDefault());

			alarmManager.setAlarmClock(
				new AlarmManager.AlarmClockInfo(zonedDateTime.toEpochSecond() * 1000,
					pendingIntent), pendingIntent);

			ConstantsAndStatics.schedulePeriodicWork(this);

		} else {
			// We have been asked not to skip the UI. We have some data, and with that
			// data we will start
			// Activity_AlarmsList, which will, in turn, start Activity_AlarmDetails,
			// and thereafter the UI will
			// be shown. Any missing data will be filled in by Activity_AlarmDetails.

			Intent intent1 = new Intent(this, Activity_AlarmsList.class);
			intent1.setAction(ACTION_NEW_ALARM_FROM_INTENT)
				.putExtra(BUNDLE_KEY_ALARM_HOUR, alarmDateTime.getHour())
				.putExtra(BUNDLE_KEY_ALARM_MINUTE, alarmDateTime.getMinute())
				.putExtra(BUNDLE_KEY_ALARM_DAY, alarmDateTime.getDayOfMonth())
				.putExtra(BUNDLE_KEY_ALARM_MONTH, alarmDateTime.getMonthValue())
				.putExtra(BUNDLE_KEY_ALARM_YEAR, alarmDateTime.getYear())
				.putExtra(BUNDLE_KEY_ALARM_VOLUME, volume)
				.putExtra(BUNDLE_KEY_ALARM_TONE_URI, alarmToneUri)
				.putExtra(BUNDLE_KEY_ALARM_TYPE, alarmType)
				.putExtra(BUNDLE_KEY_IS_REPEAT_ON, isRepeatOn)
				.putExtra(BUNDLE_KEY_REPEAT_DAYS, repeatDays)
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(intent1);

		}

	}

	//--------------------------------------------------------------------------------------------------------------

	/**
	 * Finds whether a file exists or not.
	 *
	 * @param uri The Uri of the file.
	 * @return {@code true} if the file exists, otherwise {@code false}.
	 */
	private boolean doesFileExist(Uri uri) {
		try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
			return cursor != null;
		}
	}

}
