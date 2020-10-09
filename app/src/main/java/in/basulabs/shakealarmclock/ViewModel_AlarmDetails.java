package in.basulabs.shakealarmclock;

import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class ViewModel_AlarmDetails extends ViewModel {

	/**
	 * A {@link LocalDateTime} object representing the alarm date and time.
	 */
	private MutableLiveData<LocalDateTime> alarmDateTime;

	/**
	 * The snooze interval in minutes.
	 */
	private MutableLiveData<Integer> snoozeIntervalInMins;

	/**
	 * The snooze frequency, i.e. the number of times the alarm will be snoozed before it is cancelled
	 * automatically.
	 */
	private MutableLiveData<Integer> snoozeFreq;

	/**
	 * Represents the alarm type. Can have only three values: {@link ConstantsAndStatics#ALARM_TYPE_SOUND_ONLY}, {@link
	 * ConstantsAndStatics#ALARM_TYPE_VIBRATE_ONLY} or {@link ConstantsAndStatics#ALARM_TYPE_SOUND_AND_VIBRATE}.
	 */
	private MutableLiveData<Integer> alarmType;

	/**
	 * The alarm volume.
	 */
	private MutableLiveData<Integer> alarmVolume;

	/**
	 * Indicates whether snooze is ON or OFF.
	 */
	private MutableLiveData<Boolean> isSnoozeOn;

	/**
	 * Represents whether repeat is ON or OFF.
	 */
	private MutableLiveData<Boolean> isRepeatOn;

	/**
	 * This variable indicates whether the date for the alarm is today. It will be {@code true} if the user
	 * does not choose a date via the date picker, or chooses today as the alarm date.
	 */
	private MutableLiveData<Boolean> isChosenDateToday;

	/**
	 * The Uri of the alarm tone. Default value is {@link RingtoneManager#getActualDefaultRingtoneUri(Context,
	 * int)} with type {@link RingtoneManager#TYPE_ALARM}.
	 */
	private MutableLiveData<Uri> alarmToneUri;

	/**
	 * Represents the smallest date that the date picker should support.
	 */
	private MutableLiveData<LocalDate> minDate;

	/**
	 * An integer ArrayList containing the days on which the alarm is to repeat. The values follow {@link
	 * java.time.DayOfWeek} enum, i.e. Monday is 1 and Sunday is 7.
	 */
	private MutableLiveData<ArrayList<Integer>> repeatDays;

	/**
	 * This variable indicates whether the fragment has been created for a new alarm, or to show the details
	 * of an existing alarm. It can have two values only - {@link Activity_AlarmDetails#MODE_EXISTING_ALARM}
	 * and {@link Activity_AlarmDetails#MODE_NEW_ALARM}.
	 */
	private MutableLiveData<Integer> mode;

	/**
	 * The old alarm hour. This variable is useful only when {@link #mode} = {@link
	 * Activity_AlarmDetails#MODE_EXISTING_ALARM}. This is passed to {@link Activity_AlarmsList} along with
	 * {@link #oldAlarmMinute} so that the old alarm can be identified and deleted.
	 */
	private MutableLiveData<Integer> oldAlarmHour;

	/**
	 * The old alarm hour. This variable is useful only when {@link #mode} = {@link
	 * Activity_AlarmDetails#MODE_EXISTING_ALARM}. This is passed to {@link Activity_AlarmsList} along with
	 * {@link #oldAlarmHour} so that the old alarm can be identified and deleted.
	 */
	private MutableLiveData<Integer> oldAlarmMinute;

	private MutableLiveData<Boolean> hasUserChosenDate;

	//------------------------------------------------------------------------------------------------------

	public boolean getHasUserChosenDate() {
		return hasUserChosenDate.getValue();
	}

	public void setHasUserChosenDate(boolean hasUserChosenDate) {
		if (this.hasUserChosenDate == null){
			this.hasUserChosenDate = new MutableLiveData<>();
		}
		this.hasUserChosenDate.setValue(hasUserChosenDate);
	}

	public LocalDateTime getAlarmDateTime() {
		return alarmDateTime.getValue();
	}

	public void setAlarmDateTime(LocalDateTime alarmDateTime) {
		if (this.alarmDateTime == null) {
			this.alarmDateTime = new MutableLiveData<>();
		}
		this.alarmDateTime.setValue(alarmDateTime);
	}

	public int getSnoozeIntervalInMins() {
		if (snoozeIntervalInMins.getValue() != null) {
			return snoozeIntervalInMins.getValue();
		} else {
			return 5;
		}
	}

	public void setSnoozeIntervalInMins(int snoozeIntervalInMins) {
		if (this.snoozeIntervalInMins == null) {
			this.snoozeIntervalInMins = new MutableLiveData<>();
		}
		this.snoozeIntervalInMins.setValue(snoozeIntervalInMins);
	}

	public int getSnoozeFreq() {
		if (snoozeFreq.getValue() != null) {
			return snoozeFreq.getValue();
		} else {
			return 3;
		}
	}

	public void setSnoozeFreq(int snoozeFreq) {
		if (this.snoozeFreq == null) {
			this.snoozeFreq = new MutableLiveData<>();
		}
		this.snoozeFreq.setValue(snoozeFreq);
	}

	public int getAlarmType() {
		if (alarmType.getValue() != null) {
			return alarmType.getValue();
		} else {
			return ConstantsAndStatics.ALARM_TYPE_SOUND_ONLY;
		}
	}

	public void setAlarmType(int alarmType) {
		if (this.alarmType == null) {
			this.alarmType = new MutableLiveData<>();
		}
		this.alarmType.setValue(alarmType);
	}

	public int getAlarmVolume() {
		if (alarmVolume.getValue() != null) {
			return alarmVolume.getValue();
		} else {
			return 3;
		}
	}

	public void setAlarmVolume(int alarmVolume) {
		if (this.alarmVolume == null) {
			this.alarmVolume = new MutableLiveData<>();
		}
		this.alarmVolume.setValue(alarmVolume);
	}

	public boolean getIsSnoozeOn() {
		if (isSnoozeOn.getValue() != null) {
			return isSnoozeOn.getValue();
		} else {
			return true;
		}
	}

	public void setIsSnoozeOn(boolean isSnoozeOn) {
		if (this.isSnoozeOn == null) {
			this.isSnoozeOn = new MutableLiveData<>();
		}
		this.isSnoozeOn.setValue(isSnoozeOn);
	}

	public boolean getIsRepeatOn() {
		if (isRepeatOn.getValue() != null) {
			return isRepeatOn.getValue();
		} else {
			return false;
		}
	}

	public void setIsRepeatOn(boolean isRepeatOn) {
		if (this.isRepeatOn == null) {
			this.isRepeatOn = new MutableLiveData<>();
		}
		this.isRepeatOn.setValue(isRepeatOn);
	}

	public boolean getIsChosenDateToday() {
		return isChosenDateToday.getValue();
	}

	public void setIsChosenDateToday(boolean isChosenDateToday) {
		if (this.isChosenDateToday == null) {
			this.isChosenDateToday = new MutableLiveData<>();
		}
		this.isChosenDateToday.setValue(isChosenDateToday);
	}

	public Uri getAlarmToneUri() {
		return alarmToneUri.getValue();
	}

	public void setAlarmToneUri(Uri alarmToneUri) {
		if (this.alarmToneUri == null) {
			this.alarmToneUri = new MutableLiveData<>();
		}
		this.alarmToneUri.setValue(alarmToneUri);
	}

	public LocalDate getMinDate() {
		return minDate.getValue();
	}

	public void setMinDate(LocalDate minDate) {
		if (this.minDate == null) {
			this.minDate = new MutableLiveData<>();
		}
		this.minDate.setValue(minDate);
	}

	public ArrayList<Integer> getRepeatDays() {
		return repeatDays.getValue();
	}

	public void setRepeatDays(ArrayList<Integer> repeatDays) {
		if (this.repeatDays == null) {
			this.repeatDays = new MutableLiveData<>();
		}
		this.repeatDays.setValue(repeatDays);
	}

	public int getMode() {
		return mode.getValue();
	}

	public void setMode(int mode) {
		if (this.mode == null) {
			this.mode = new MutableLiveData<>();
		}
		this.mode.setValue(mode);
	}

	public int getOldAlarmHour() {
		return oldAlarmHour.getValue();
	}

	public void setOldAlarmHour(int oldAlarmHour) {
		if (this.oldAlarmHour == null) {
			this.oldAlarmHour = new MutableLiveData<>();
		}
		this.oldAlarmHour.setValue(oldAlarmHour);
	}

	public int getOldAlarmMinute() {
		return oldAlarmMinute.getValue();
	}

	public void setOldAlarmMinute(int oldAlarmMinute) {
		if (this.oldAlarmMinute == null) {
			this.oldAlarmMinute = new MutableLiveData<>();
		}
		this.oldAlarmMinute.setValue(oldAlarmMinute);
	}

	public LiveData<Integer> getLiveAlarmVolume() {
		return alarmVolume;
	}

	public LiveData<Boolean> getLiveIsRepeatOn() {
		return isRepeatOn;
	}

	public LiveData<Integer> getLiveAlarmType() {
		return alarmType;
	}
}
