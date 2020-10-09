package in.basulabs.shakealarmclock;

import androidx.annotation.NonNull;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

public class AlarmData {

	private boolean isSwitchedOn;
	private LocalDateTime alarmDateTime;
	private LocalTime alarmTime;
	private int soundVibrateSetting;
	private boolean isRepeatOn;
	private ArrayList<Integer> repeatDays;

	/**
	 * Use this constructor if repeat is OFF for the alarm.
	 *
	 * @param isSwitchedOn
	 * @param alarmDateTime
	 * @param soundVibrateSetting
	 */
	public AlarmData(boolean isSwitchedOn, @NonNull LocalDateTime alarmDateTime, int soundVibrateSetting) {
		this.isSwitchedOn = isSwitchedOn;
		this.alarmDateTime = alarmDateTime;
		this.soundVibrateSetting = soundVibrateSetting;
		this.isRepeatOn = false;
		this.repeatDays = null;
		this.alarmTime = alarmDateTime.toLocalTime();
	}

	/**
	 * Use this constructor if repeat is ON.
	 *
	 * @param isSwitchedOn
	 * @param alarmTime
	 * @param soundVibrateSetting
	 * @param repeatDays
	 */
	public AlarmData(boolean isSwitchedOn, @NonNull LocalTime alarmTime, int soundVibrateSetting,
	                 @NonNull ArrayList<Integer> repeatDays) {
		this.isSwitchedOn = isSwitchedOn;
		this.alarmTime = alarmTime;
		this.soundVibrateSetting = soundVibrateSetting;
		this.isRepeatOn = true;
		this.repeatDays = repeatDays;
		this.alarmDateTime = null;
	}

	public boolean isRepeatOn() {
		return isRepeatOn;
	}

	public void setRepeatOn(boolean repeatOn) {
		isRepeatOn = repeatOn;
	}

	public ArrayList<Integer> getRepeatDays() {
		return repeatDays;
	}

	public void setRepeatDays(ArrayList<Integer> repeatDays) {
		this.repeatDays = repeatDays;
	}

	public boolean isSwitchedOn() {
		return isSwitchedOn;
	}

	public void setSwitchedOn(boolean switchedOn) {
		isSwitchedOn = switchedOn;
	}

	public LocalDateTime getAlarmDateTime() {
		return alarmDateTime;
	}

	public void setAlarmDateTime(LocalDateTime alarmDateTime) {
		this.alarmDateTime = alarmDateTime;
	}

	public LocalTime getAlarmTime() {
		return alarmTime;
	}

	public void setAlarmTime(LocalTime alarmTime) {
		this.alarmTime = alarmTime;
	}

	public int getSoundVibrateSetting() {
		return soundVibrateSetting;
	}

	public void setSoundVibrateSetting(int soundVibrateSetting) {
		this.soundVibrateSetting = soundVibrateSetting;
	}

}
