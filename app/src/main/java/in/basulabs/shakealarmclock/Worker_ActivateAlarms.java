package in.basulabs.shakealarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

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

public class Worker_ActivateAlarms extends Worker {

	Context context;

	private boolean stopExecuting;

	//----------------------------------------------------------------------------------------------------------

	public Worker_ActivateAlarms(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
		this.context = context;
	}

	//----------------------------------------------------------------------------------------------------------

	@NonNull
	@Override
	public Result doWork() {

		stopExecuting = false;
		activateAlarmsIfInactive();

		return Result.success();
	}

	//----------------------------------------------------------------------------------------------------------

	/**
	 * Activates the alarms that are ON, but inactive because {@link AlarmManager} has cancelled them for no reason.
	 */
	private void activateAlarmsIfInactive() {

		final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		final AlarmDatabase alarmDatabase = AlarmDatabase.getInstance(context);

		List<AlarmEntity> list = alarmDatabase.alarmDAO().getActiveAlarms();

		if (list != null && list.size() > 0) {

			for (AlarmEntity alarmEntity : list) {

				AtomicReference<ArrayList<Integer>> repeatDaysAtomic = new AtomicReference<>();

				alarmDatabase.alarmDAO().getAlarmRepeatDays(alarmEntity.alarmID);

				ArrayList<Integer> repeatDays = repeatDaysAtomic.get();

				Intent intent = new Intent(context, AlarmBroadcastReceiver.class);
				intent.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM);
				intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

				Bundle data = alarmEntity.getAlarmDetailsInABundle();
				data.putIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS, repeatDays);
				intent.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, data);

				PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarmEntity.alarmID, intent,
						PendingIntent.FLAG_NO_CREATE);

				if (pendingIntent == null) {

					LocalDateTime alarmDateTime;
					LocalDate alarmDate = LocalDate.of(alarmEntity.alarmYear, alarmEntity.alarmMonth, alarmEntity.alarmDay);
					LocalTime alarmTime = LocalTime.of(alarmEntity.alarmHour, alarmEntity.alarmMinutes);

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
										alarmDateTime.with(TemporalAdjusters.next(DayOfWeek.of(repeatDays.get(i))));
								break;
							}
							if (i == repeatDays.size() - 1) {
								// No day possible in this week. Select the first available date from next week.
								alarmDateTime = alarmDateTime.with(TemporalAdjusters.next(DayOfWeek.of(repeatDays.get(0))));
							}
						}

					} else {
						alarmDateTime = LocalDateTime.of(alarmDate, alarmTime);
						if (! alarmDateTime.isAfter(LocalDateTime.now())) {
							alarmDateTime.plusDays(1);
						}
					}

					ZonedDateTime zonedDateTime = ZonedDateTime.of(alarmDateTime, ZoneId.systemDefault());

					PendingIntent pendingIntent1 = PendingIntent.getBroadcast(context, alarmEntity.alarmID, intent, 0);

					alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(zonedDateTime.toEpochSecond() * 1000,
							pendingIntent1), pendingIntent1);

				}

				if ((stopExecuting && ! isStopped()) || Service_RingAlarm.isThisServiceRunning || Service_SnoozeAlarm.isThisServiceRunning) {
					break;
				}
			}
		}
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onStopped() {
		super.onStopped();
		stopExecuting = true;
	}
}
