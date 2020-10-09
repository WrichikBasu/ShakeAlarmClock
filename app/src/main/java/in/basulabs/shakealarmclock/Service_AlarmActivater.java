package in.basulabs.shakealarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static android.os.SystemClock.elapsedRealtime;

public class Service_AlarmActivater extends Service {

	private AlarmDatabase alarmDatabase;

	public static boolean isThisServiceRunning;
	public static int pid = - 1;

	//-----------------------------------------------------------------------------------------------------

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		//Log.e(this.getClass().getSimpleName(), "service started.");
		isThisServiceRunning = true;

		Service_AlarmActivater obj = this;
		pid = android.os.Process.myPid();

		alarmDatabase = AlarmDatabase.getInstance(obj);

		AtomicReference<List<AlarmEntity>> alarmEntityList = new AtomicReference<>();

		Thread thread = new Thread(
				() -> alarmEntityList.set(alarmDatabase.alarmDAO().getActiveAlarms()));

		if (! Service_RingAlarm.isThisServiceRunning && ! Service_SnoozeAlarm.isThisServiceRunning
				&& ! Service_UpdateAlarm.isThisServiceRunning) {
			thread.start();
			try {
				thread.join();

				if (alarmEntityList.get().size() > 0) {
					activateAlarmsIfInactive(alarmEntityList.get());
				}
			} catch (InterruptedException ignored) {
			}
		}

		return START_STICKY;
	}

	//------------------------------------------------------------------------------------------------------

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		super.onTaskRemoved(rootIntent);

		//Log.e(this.getClass().getSimpleName(), "onTaskRemoved() called.");

		Intent restartServiceIntent = new Intent(this, AlarmBroadcastReceiver.class);
		restartServiceIntent.setAction(ConstantsAndStatics.ACTION_CREATE_BACKGROUND_SERVICE);
		restartServiceIntent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

		PendingIntent restartServicePendingIntent = PendingIntent.getBroadcast(
				getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);

		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, elapsedRealtime() + 3000,
				restartServicePendingIntent);
	}

	//------------------------------------------------------------------------------------------------------

	private void activateAlarmsIfInactive(@NonNull List<AlarmEntity> list) {

		final AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		final Service_AlarmActivater obj = this;

		for (AlarmEntity alarmEntity : list) {

			Thread thread = new Thread(() -> {

				ArrayList<Integer> repeatDays = new ArrayList<>(
						alarmDatabase.alarmDAO().getAlarmRepeatDays(alarmEntity.alarmID));

				Intent intent = new Intent(obj, AlarmBroadcastReceiver.class);
				intent.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM);
				intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

				Bundle data = alarmEntity.getAlarmDetailsInABundle();
				data.putIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS, repeatDays);
				intent.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, data);

				PendingIntent pendingIntent = PendingIntent
						.getBroadcast(obj, alarmEntity.alarmID, intent,
								PendingIntent.FLAG_NO_CREATE);

				if (pendingIntent == null) {
					//Log.e(this.getClass().getSimpleName(), "pending intent not found.");

					LocalDateTime alarmDateTime;
					LocalDate alarmDate = LocalDate
							.of(alarmEntity.alarmYear, alarmEntity.alarmMonth,
									alarmEntity.alarmDay);
					LocalTime alarmTime = LocalTime
							.of(alarmEntity.alarmHour, alarmEntity.alarmMinutes);

					if (alarmEntity.isRepeatOn && repeatDays.size() > 0) {

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
								// There is a day available in the same week for the alarm to ring; select that day and
								// break from loop.
								alarmDateTime =
										alarmDateTime.with(TemporalAdjusters
												.next(DayOfWeek.of(repeatDays.get(i))));
								break;
							}
							if (i == repeatDays.size() - 1) {
								// No day possible in this week. Select the first available date from next week.
								alarmDateTime = alarmDateTime
										.with(TemporalAdjusters
												.next(DayOfWeek.of(repeatDays.get(0))));
							}
						}

					} else {
						alarmDateTime = LocalDateTime.of(alarmDate, alarmTime);
						if (! alarmDateTime.isAfter(LocalDateTime.now())) {
							alarmDateTime.plusDays(1);
						}
					}

					ZonedDateTime zonedDateTime = ZonedDateTime
							.of(alarmDateTime, ZoneId.systemDefault());

					/*Calendar calendar = Calendar.getInstance();
					calendar.set(Calendar.YEAR, alarmDateTime.getYear());
					calendar.set(Calendar.MONTH, alarmDateTime.getMonthValue() - 1);
					calendar.set(Calendar.DAY_OF_MONTH, alarmDateTime.getDayOfMonth());
					calendar.set(Calendar.HOUR_OF_DAY, alarmDateTime.getHour());
					calendar.set(Calendar.MINUTE, alarmDateTime.getMinute());
					calendar.set(Calendar.SECOND, 0);*/

					PendingIntent pendingIntent1 = PendingIntent
							.getBroadcast(obj, alarmEntity.alarmID, intent, 0);

					alarmManager.setAlarmClock(
							new AlarmManager.AlarmClockInfo(zonedDateTime.toEpochSecond() * 1000,
									pendingIntent1), pendingIntent1);

					/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
								calendar.getTimeInMillis(),
								pendingIntent1);
					} else {
						alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
								pendingIntent1);
					}*/

				} /*else {
					//Log.e(this.getClass().getSimpleName(), "pending intent found.");
				}*/

			});
			thread.start();
			try {
				thread.join();
			} catch (InterruptedException ignored) {
			}
		}


	}

	//---------------------------------------------------------------------------------------------------

	@Override
	public void onDestroy() {
		super.onDestroy();
		isThisServiceRunning = false;
		pid = - 1;
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
