package in.basulabs.shakealarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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

		if (Service_RingAlarm.isThisServiceRunning || Service_SnoozeAlarm.isThisServiceRunning) {
			return Result.failure();
		} else {
			return activateAlarmsIfInactive();
		}
	}

	//----------------------------------------------------------------------------------------------------------

	/**
	 * Activates the alarms that are ON, but inactive because {@link AlarmManager} has cancelled them for no reason.
	 */
	private Result activateAlarmsIfInactive() {

		final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		final AlarmDatabase alarmDatabase = AlarmDatabase.getInstance(context);

		List<AlarmEntity> list = alarmDatabase.alarmDAO().getActiveAlarms();

		if (list != null && list.size() > 0) {

			for (AlarmEntity alarmEntity : list) {

				AtomicReference<ArrayList<Integer>> repeatDaysAtomic = new AtomicReference<>();

				alarmDatabase.alarmDAO().getAlarmRepeatDays(alarmEntity.alarmID);

				ArrayList<Integer> repeatDays = repeatDaysAtomic.get();

				Intent intent = new Intent(context.getApplicationContext(), AlarmBroadcastReceiver.class);
				intent.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM);
				intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

				Bundle data = alarmEntity.getAlarmDetailsInABundle();
				data.putIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS, repeatDays);
				intent.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, data);

				PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), alarmEntity.alarmID, intent,	PendingIntent.FLAG_NO_CREATE);

				if (pendingIntent == null) {

					LocalDateTime alarmDateTime = ConstantsAndStatics.getAlarmDateTime(LocalDate.of(alarmEntity.alarmYear, alarmEntity.alarmMonth,
							alarmEntity.alarmDay), LocalTime.of(alarmEntity.alarmHour, alarmEntity.alarmMinutes), alarmEntity.isRepeatOn, repeatDays);

					ZonedDateTime zonedDateTime = ZonedDateTime.of(alarmDateTime, ZoneId.systemDefault());

					PendingIntent pendingIntent1 = PendingIntent.getBroadcast(context.getApplicationContext(), alarmEntity.alarmID, intent, 0);

					alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(zonedDateTime.toEpochSecond() * 1000, pendingIntent1), pendingIntent1);

				}

				if ((stopExecuting && ! isStopped()) || Service_RingAlarm.isThisServiceRunning || Service_SnoozeAlarm.isThisServiceRunning) {
					return Result.failure();
				}
			}
		}
		return Result.success();
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onStopped() {
		super.onStopped();
		stopExecuting = true;
	}

}
