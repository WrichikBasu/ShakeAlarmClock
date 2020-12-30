package in.basulabs.shakealarmclock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

public class AlarmData {

	private boolean isSwitchedOn;
	private LocalDateTime alarmDateTime;
	private LocalTime alarmTime;
	private int alarmType;
	private boolean isRepeatOn;
	private ArrayList<Integer> repeatDays;

	//------------------------------------------------------------------------------------------------------------------------

	/**
	 * Use this constructor if repeat is OFF for the alarm.
	 *
	 * @param isSwitchedOn Whether alarm is switched on.
	 * @param alarmDateTime A {@link LocalDateTime} object representing the date and time of the alarm. Cannot be null.
	 * @param alarmType One of {@link ConstantsAndStatics#ALARM_TYPE_SOUND_ONLY}, {@link ConstantsAndStatics#ALARM_TYPE_VIBRATE_ONLY} or {@link
	 *        ConstantsAndStatics#ALARM_TYPE_SOUND_AND_VIBRATE}.
	 */
	public AlarmData(boolean isSwitchedOn, @NonNull LocalDateTime alarmDateTime, int alarmType) {
		this.isSwitchedOn = isSwitchedOn;
		this.alarmDateTime = alarmDateTime;
		this.alarmType = alarmType;
		this.isRepeatOn = false;
		this.repeatDays = null;
		this.alarmTime = alarmDateTime.toLocalTime();
	}

	//------------------------------------------------------------------------------------------------------------------------

	/**
	 * Use this constructor if repeat is ON.
	 *
	 * @param isSwitchedOn Whether alarm is switched on.
	 * @param alarmTime A {@link LocalTime} object representing the time of the alarm. Must not be null.
	 * @param alarmType One of {@link ConstantsAndStatics#ALARM_TYPE_SOUND_ONLY}, {@link ConstantsAndStatics#ALARM_TYPE_VIBRATE_ONLY} or {@link
	 *        ConstantsAndStatics#ALARM_TYPE_SOUND_AND_VIBRATE}.
	 * @param repeatDays An <code>ArrayList</code> containing the days in which the alarm is to be repeated. Follows {@link java.time.DayOfWeek} enum. Cannot
	 * 		be null.
	 */
	public AlarmData(boolean isSwitchedOn, @NonNull LocalTime alarmTime, int alarmType, @NonNull ArrayList<Integer> repeatDays) {
		this.isSwitchedOn = isSwitchedOn;
		this.alarmTime = alarmTime;
		this.alarmType = alarmType;
		this.isRepeatOn = true;
		this.repeatDays = repeatDays;
		this.alarmDateTime = null;
	}

	//------------------------------------------------------------------------------------------------------------------------

	/**
	 * Get whether the alarm will repeat.
	 *
	 * @return {@code true} if repeat is on, otherwise {@code false}.
	 */
	public boolean isRepeatOn() {
		return isRepeatOn;
	}

	//------------------------------------------------------------------------------------------------------------------------

	/**
	 * Sets whether repeat is on.
	 *
	 * @param repeatOn Pass {@code true} if repeat is on, otherwise {@code false}.
	 */
	public void setRepeatOn(boolean repeatOn) {
		isRepeatOn = repeatOn;
	}

	//------------------------------------------------------------------------------------------------------------------------

	/**
	 * Get the days on which the alarm will repeat.
	 *
	 * @return An {@code ArrayList} containing the days on which the alarm will repeat. Must follow {@link java.time.DayOfWeek} enum. May be null in case repeat
	 * 		is off.
	 */
	@Nullable
	public ArrayList<Integer> getRepeatDays() {
		return repeatDays;
	}

	//------------------------------------------------------------------------------------------------------------------------

	/**
	 * Set the days on which the alarm is to be repeated.
	 *
	 * @param repeatDays An {@code ArrayList} containing the days on which the alarm will repeat. Must follow {@link java.time.DayOfWeek} enum. May be null in
	 * 		case repeat is off.
	 */
	public void setRepeatDays(@Nullable ArrayList<Integer> repeatDays) {
		this.repeatDays = repeatDays;
	}

	//------------------------------------------------------------------------------------------------------------------------

	/**
	 * Get whether the alarm is switched on or off.
	 *
	 * @return {@code true} if the alarm is on, otherwise {@code false}.
	 */
	public boolean isSwitchedOn() {
		return isSwitchedOn;
	}

	//------------------------------------------------------------------------------------------------------------------------

	/**
	 * Set whether the alarm is switched on or off.
	 *
	 * @param switchedOn Pass {@code true} if the alarm is on, otherwise {@code false}.
	 */
	public void setSwitchedOn(boolean switchedOn) {
		isSwitchedOn = switchedOn;
	}

	//------------------------------------------------------------------------------------------------------------------------

	/**
	 * Get the alarm date and time.
	 *
	 * @return The date and time of the alarm. Returns null in case of repetitive alarms.
	 */
	@Nullable
	public LocalDateTime getAlarmDateTime() {
		return alarmDateTime;
	}

	//------------------------------------------------------------------------------------------------------------------------

	/**
	 * Set the alarm date and time.
	 *
	 * @param alarmDateTime A {@link LocalDateTime} object representing the alarm date and time. May be null if repeat is on.
	 */
	public void setAlarmDateTime(@Nullable LocalDateTime alarmDateTime) {
		this.alarmDateTime = alarmDateTime;
	}

	//------------------------------------------------------------------------------------------------------------------------

	/**
	 * Get the alarm time.
	 *
	 * @return A {@link LocalTime} object representing the alarm time. Will never be null.
	 */
	@NonNull
	public LocalTime getAlarmTime() {
		return alarmTime;
	}

	//------------------------------------------------------------------------------------------------------------------------

	/**
	 * Set the alarm time.
	 *
	 * @param alarmTime A {@link LocalTime} object representing the alarm time. Cannot be null.
	 */
	public void setAlarmTime(@NonNull LocalTime alarmTime) {
		this.alarmTime = alarmTime;
	}

	//------------------------------------------------------------------------------------------------------------------------

	/**
	 * Get the alarm type.
	 *
	 * @return One of {@link ConstantsAndStatics#ALARM_TYPE_SOUND_ONLY}, {@link ConstantsAndStatics#ALARM_TYPE_VIBRATE_ONLY} or {@link
	 *        ConstantsAndStatics#ALARM_TYPE_SOUND_AND_VIBRATE}.
	 */
	public int getAlarmType() {
		return alarmType;
	}

	//------------------------------------------------------------------------------------------------------------------------

	/**
	 * Set the alarm type.
	 *
	 * @param alarmType One of {@link ConstantsAndStatics#ALARM_TYPE_SOUND_ONLY}, {@link ConstantsAndStatics#ALARM_TYPE_VIBRATE_ONLY} or {@link
	 *        ConstantsAndStatics#ALARM_TYPE_SOUND_AND_VIBRATE}.
	 */
	public void setAlarmType(int alarmType) {
		this.alarmType = alarmType;
	}

}
