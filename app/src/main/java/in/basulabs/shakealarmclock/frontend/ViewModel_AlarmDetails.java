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

import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;

import in.basulabs.shakealarmclock.backend.ConstantsAndStatics;

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
	 * The snooze frequency, i.e. the number of times the alarm will be snoozed before it
	 * is cancelled automatically.
	 */
	private MutableLiveData<Integer> snoozeFreq;

	/**
	 * Represents the alarm type. Can have only three values:
	 * {@link ConstantsAndStatics#ALARM_TYPE_SOUND_ONLY},
	 * {@link ConstantsAndStatics#ALARM_TYPE_VIBRATE_ONLY} or
	 * {@link ConstantsAndStatics#ALARM_TYPE_SOUND_AND_VIBRATE}.
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
	 * This variable indicates whether the date for the alarm is today. It will be
	 * {@code true} if the user does not choose a date via the date picker, or chooses
	 * today as the alarm date.
	 */
	private MutableLiveData<Boolean> isChosenDateToday;

	/**
	 * The Uri of the alarm tone. Default value is
	 * {@link RingtoneManager#getActualDefaultRingtoneUri(Context, int)} with type
	 * {@link RingtoneManager#TYPE_ALARM}.
	 */
	private MutableLiveData<Uri> alarmToneUri;

	/**
	 * Represents the smallest date that the date picker should support.
	 */
	private MutableLiveData<LocalDate> minDate;

	/**
	 * An integer ArrayList containing the days on which the alarm is to repeat.
	 * <p>
	 * The values follow {@link java.time.DayOfWeek} enum, i.e. Monday is 1 and Sunday is
	 * 7.
	 * </p>
	 */
	private MutableLiveData<ArrayList<Integer>> repeatDays;

	/**
	 * This variable indicates whether the fragment has been created for a new alarm, or
	 * to show the details of an existing alarm. It can have two values only -
	 * {@link Activity_AlarmDetails#MODE_EXISTING_ALARM} or
	 * {@link Activity_AlarmDetails#MODE_NEW_ALARM}.
	 */
	private MutableLiveData<Integer> mode;

	/**
	 * The old alarm hour. This variable is useful only when {@link #mode} =
	 * {@link Activity_AlarmDetails#MODE_EXISTING_ALARM}. This is passed to
	 * {@link Activity_AlarmsList} along with {@link #oldAlarmMinute} so that the old
	 * alarm can be identified and deleted.
	 */
	private MutableLiveData<Integer> oldAlarmHour;

	/**
	 * The old alarm hour. This variable is useful only when {@link #mode} =
	 * {@link Activity_AlarmDetails#MODE_EXISTING_ALARM}. This is passed to
	 * {@link Activity_AlarmsList} along with {@link #oldAlarmHour} so that the old alarm
	 * can be identified and deleted.
	 */
	private MutableLiveData<Integer> oldAlarmMinute;

	/**
	 * Indicates whether the user has chosen the date explicitly.
	 * <p>
	 * Say the user selects a time that is possible to reach today, but then explicitly
	 * choses tomorrow. In that case, the date will not be reverted to today even if the
	 * time is reachable today.
	 * </p>
	 */
	private MutableLiveData<Boolean> hasUserChosenDate;

	/**
	 * The message that will be displayed when the alarm rings. May be {@code null}.
	 */
	private MutableLiveData<String> alarmMessage;

	//------------------------------------------------------------------------------------------------------

	/**
	 * Get whether the user has manually chosen a date. Default: {@code false}.
	 *
	 * @return Same as in description.
	 */
	@SuppressWarnings("SimplifiableConditionalExpression")
	public boolean getHasUserChosenDate() {
		if (hasUserChosenDate == null) {
			hasUserChosenDate = new MutableLiveData<>(false);
		}
		return hasUserChosenDate.getValue() == null
			? false
			: hasUserChosenDate.getValue();
	}

	//------------------------------------------------------------------------------------------------------


	/**
	 * Set whether the user has manually chosen a date.
	 *
	 * @param hasUserChosenDate The value to be set.
	 */
	public void setHasUserChosenDate(boolean hasUserChosenDate) {
		if (this.hasUserChosenDate == null) {
			this.hasUserChosenDate = new MutableLiveData<>();
		}
		this.hasUserChosenDate.setValue(hasUserChosenDate);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Get the alarm date and time. If {@code null}, throws a
	 * {@link NullPointerException}.
	 *
	 * @return Same as in description.
	 */
	@NonNull
	public LocalDateTime getAlarmDateTime() {
		return Objects.requireNonNull(alarmDateTime.getValue(), "Alarm date-time was " +
			"null.");
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Set the alarm date and time.
	 *
	 * @param alarmDateTime The value to be set. Cannot be null.
	 */
	public void setAlarmDateTime(@NonNull LocalDateTime alarmDateTime) {
		if (this.alarmDateTime == null) {
			this.alarmDateTime = new MutableLiveData<>();
		}
		this.alarmDateTime.setValue(alarmDateTime);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Get the snooze interval, i.e. the period after which the alarm should ring again.
	 * Returns 5 if not set previously.
	 *
	 * @return Same as in description.
	 */
	public int getSnoozeIntervalInMins() {
		if (snoozeIntervalInMins == null) {
			snoozeIntervalInMins = new MutableLiveData<>(5);
		}
		return snoozeIntervalInMins.getValue() == null ? 5 :
			snoozeIntervalInMins.getValue();
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Set the snooze interval, i.e. the period after which the alarm should ring again.
	 *
	 * @param snoozeIntervalInMins The value to be set.
	 */
	public void setSnoozeIntervalInMins(int snoozeIntervalInMins) {
		if (this.snoozeIntervalInMins == null) {
			this.snoozeIntervalInMins = new MutableLiveData<>();
		}
		this.snoozeIntervalInMins.setValue(snoozeIntervalInMins);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Get the number of times the alarm will be snoozed. Returns 3 if not set
	 * previously.
	 *
	 * @return Same as in description.
	 */
	public int getSnoozeFreq() {

		if (snoozeFreq == null) {
			snoozeFreq = new MutableLiveData<>(3);
		}
		return snoozeFreq.getValue() == null ? 3 : snoozeFreq.getValue();
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Set the number of times the alarm will be snoozed before being dismissed.
	 *
	 * @param snoozeFreq The value to be set.
	 */
	public void setSnoozeFreq(int snoozeFreq) {
		if (this.snoozeFreq == null) {
			this.snoozeFreq = new MutableLiveData<>();
		}
		this.snoozeFreq.setValue(snoozeFreq);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Get the alarm type.
	 *
	 * @return One of {@link ConstantsAndStatics#ALARM_TYPE_SOUND_ONLY},
	 *    {@link ConstantsAndStatics#ALARM_TYPE_VIBRATE_ONLY} or
	 *    {@link ConstantsAndStatics#ALARM_TYPE_SOUND_AND_VIBRATE}. Default is
	 *    {@code ConstantsAndStatics#ALARM_TYPE_SOUND_ONLY}.
	 */
	public int getAlarmType() {
		if (alarmType == null) {
			alarmType = new MutableLiveData<>(ConstantsAndStatics.ALARM_TYPE_SOUND_ONLY);
		}
		return alarmType.getValue() == null ? ConstantsAndStatics.ALARM_TYPE_SOUND_ONLY
			: alarmType.getValue();

	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Set the alarm type.
	 *
	 * @param alarmType Should be one of
	 *    {@link ConstantsAndStatics#ALARM_TYPE_SOUND_ONLY},
	 *    {@link ConstantsAndStatics#ALARM_TYPE_VIBRATE_ONLY} or
	 *    {@link ConstantsAndStatics#ALARM_TYPE_SOUND_AND_VIBRATE}.
	 */
	public void setAlarmType(int alarmType) {
		if (this.alarmType == null) {
			this.alarmType = new MutableLiveData<>();
		}
		this.alarmType.setValue(alarmType);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Get the alarm volume. Returns 3 if not set previously.
	 *
	 * @return Same as in description.
	 */
	public int getAlarmVolume() {
		if (alarmVolume == null) {
			alarmVolume = new MutableLiveData<>(3);
		}
		return alarmVolume.getValue() == null ? 3 : alarmVolume.getValue();
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Set the alarm volume.
	 *
	 * @param alarmVolume The value to be set.
	 */
	public void setAlarmVolume(int alarmVolume) {
		if (this.alarmVolume == null) {
			this.alarmVolume = new MutableLiveData<>();
		}
		this.alarmVolume.setValue(alarmVolume);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Get whether snooze is ON or OFF. Default: {@code true}.
	 *
	 * @return {@code true} is snooze is ON, otherwise {@code false}. Default:
	 *    {@code true}.
	 */
	@SuppressWarnings("SimplifiableConditionalExpression")
	public boolean getIsSnoozeOn() {
		if (isSnoozeOn == null) {
			isSnoozeOn = new MutableLiveData<>(true);
		}
		return isSnoozeOn.getValue() == null ? true : isSnoozeOn.getValue();

	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Set whether snooze is ON or OFF.
	 *
	 * @param isSnoozeOn The value to be set.
	 */
	public void setIsSnoozeOn(boolean isSnoozeOn) {
		if (this.isSnoozeOn == null) {
			this.isSnoozeOn = new MutableLiveData<>();
		}
		this.isSnoozeOn.setValue(isSnoozeOn);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Get whether repeat is ON or OFF. Default: {@code false}.
	 *
	 * @return {@code true} is repeat is ON, otherwise {@code false}. Default:
	 *    {@code false}.
	 */
	@SuppressWarnings("SimplifiableConditionalExpression")
	public boolean getIsRepeatOn() {
		if (isRepeatOn == null) {
			isRepeatOn = new MutableLiveData<>(false);
		}
		return isRepeatOn.getValue() == null ? false : isRepeatOn.getValue();

	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Set whether repeat is ON or OFF.
	 *
	 * @param isRepeatOn The value to be set.
	 */
	public void setIsRepeatOn(boolean isRepeatOn) {
		if (this.isRepeatOn == null) {
			this.isRepeatOn = new MutableLiveData<>();
		}
		this.isRepeatOn.setValue(isRepeatOn);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Get whether the chosen date is same as today's date.
	 *
	 * @return Same as in description.
	 */
	public boolean getIsChosenDateToday() {
		if (isChosenDateToday == null) {
			isChosenDateToday = new MutableLiveData<>(
				getAlarmDateTime().toLocalDate().equals(LocalDate.now()));
		}
		return isChosenDateToday.getValue() == null ? getAlarmDateTime().toLocalDate()
			.equals(LocalDate.now()) : isChosenDateToday.getValue();
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Set whether the chosen date is same as today.
	 *
	 * @param isChosenDateToday The value to be set.
	 */
	public void setIsChosenDateToday(boolean isChosenDateToday) {
		if (this.isChosenDateToday == null) {
			this.isChosenDateToday = new MutableLiveData<>();
		}
		this.isChosenDateToday.setValue(isChosenDateToday);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Get the alarm tone Uri.
	 *
	 * @return The alarm tone Uri.
	 */
	@NonNull
	public Uri getAlarmToneUri() {
		if (alarmToneUri == null) {
			alarmToneUri =
				new MutableLiveData<>(Settings.System.DEFAULT_ALARM_ALERT_URI);
		}
		return alarmToneUri.getValue() == null
			? Settings.System.DEFAULT_ALARM_ALERT_URI
			: alarmToneUri.getValue();
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Set the alarm tone Uri.
	 *
	 * @param alarmToneUri The value to be set.
	 */
	public void setAlarmToneUri(@NonNull Uri alarmToneUri) {
		if (this.alarmToneUri == null) {
			this.alarmToneUri = new MutableLiveData<>();
		}
		this.alarmToneUri.setValue(alarmToneUri);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Get the minimum date allowed.
	 * <p>
	 * This will be set as the min date for the calendar shown while choosing date.
	 *
	 * @return Same as in description.
	 */
	@NonNull
	public LocalDate getMinDate() {
		return Objects.requireNonNull(minDate.getValue(), "Minimum date was null.");
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Set the minimum allowed date. This will be set as the min date for the calendar
	 * shown while choosing date.
	 *
	 * @param minDate The value to be set.
	 */
	public void setMinDate(@NonNull LocalDate minDate) {
		if (this.minDate == null) {
			this.minDate = new MutableLiveData<>();
		}
		this.minDate.setValue(minDate);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Get the days on which the alarm should repeat.
	 *
	 * @return An ArrayList specifying the days on which the alarm should repeat. Follows
	 *    {@link java.time.DayOfWeek} enum.
	 */
	@Nullable
	public ArrayList<Integer> getRepeatDays() {
		return repeatDays.getValue();
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Set the days on which the alarm should repeat.
	 *
	 * @param repeatDays An ArrayList specifying the days on which the alarm should
	 * 	repeat. Must follow {@link java.time.DayOfWeek} enum.
	 */
	public void setRepeatDays(@Nullable ArrayList<Integer> repeatDays) {
		if (this.repeatDays == null) {
			this.repeatDays = new MutableLiveData<>();
		}
		this.repeatDays.setValue(repeatDays);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * The mode in which this activity has been opened.
	 *
	 * @return Either {@link Activity_AlarmDetails#MODE_EXISTING_ALARM} or
	 *    {@link Activity_AlarmDetails#MODE_NEW_ALARM}.
	 */
	public int getMode() {
		if (mode == null) {
			mode = new MutableLiveData<>(Activity_AlarmDetails.MODE_NEW_ALARM);
		}
		return mode.getValue() == null
			? Activity_AlarmDetails.MODE_NEW_ALARM
			: mode.getValue();
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * The mode in which this activity has been opened.
	 *
	 * @param mode The value to be set. Either
	 *    {@link Activity_AlarmDetails#MODE_EXISTING_ALARM} or
	 *    {@link Activity_AlarmDetails#MODE_NEW_ALARM}.
	 */
	public void setMode(int mode) {
		if (this.mode == null) {
			this.mode = new MutableLiveData<>();
		}
		this.mode.setValue(mode);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Get the old alarm hour.
	 * <p>
	 * This should be called if and only if {@code mode} =
	 * {@link Activity_AlarmDetails#MODE_EXISTING_ALARM}. Otherwise this method will
	 * throw
	 * a {@code NullPointerException}.
	 * </p>
	 *
	 * @return The old alarm hour, if available. Otherwise throws
	 *    {@code NullPointerException}.
	 */
	public int getOldAlarmHour() {
		if (oldAlarmHour == null || oldAlarmHour.getValue() == null) {
			throw new NullPointerException("Old alarm hour was null.");
		}
		return oldAlarmHour.getValue();
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Set the old alarm hour.
	 * <p>
	 * This should be called if and only if {@code mode} =
	 * {@link Activity_AlarmDetails#MODE_EXISTING_ALARM}.
	 * </p>
	 *
	 * @param oldAlarmHour The value to be set.
	 */
	public void setOldAlarmHour(int oldAlarmHour) {
		if (this.oldAlarmHour == null) {
			this.oldAlarmHour = new MutableLiveData<>();
		}
		this.oldAlarmHour.setValue(oldAlarmHour);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Get the old alarm minute.
	 * <p>
	 * This should be called if and only if {@code mode} =
	 * {@link Activity_AlarmDetails#MODE_EXISTING_ALARM}. Otherwise this method will
	 * throw
	 * a {@code NullPointerException}.
	 * </p>
	 *
	 * @return The old alarm minute, if available. Otherwise throws
	 *    {@code NullPointerException}.
	 */
	public int getOldAlarmMinute() {
		if (oldAlarmMinute == null || oldAlarmMinute.getValue() == null) {
			throw new NullPointerException("Old alarm minute was null.");
		}
		return oldAlarmMinute.getValue();
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Set the old alarm minute.
	 * <p>
	 * This should be called if and only if {@code mode} =
	 * {@link Activity_AlarmDetails#MODE_EXISTING_ALARM}.
	 * </p>
	 *
	 * @param oldAlarmMinute The value to be set.
	 */
	public void setOldAlarmMinute(int oldAlarmMinute) {
		if (this.oldAlarmMinute == null) {
			this.oldAlarmMinute = new MutableLiveData<>();
		}
		this.oldAlarmMinute.setValue(oldAlarmMinute);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Get an observable instance of alarm volume.
	 *
	 * @return Same as in description.
	 */
	public LiveData<Integer> getLiveAlarmVolume() {
		return alarmVolume;
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Get an observable instance of whether repeat is ON or OFF.
	 *
	 * @return Same as in description.
	 */
	public LiveData<Boolean> getLiveIsRepeatOn() {
		return isRepeatOn;
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Get an observable instance of the alarm type.
	 *
	 * @return Same as in description.
	 */
	public LiveData<Integer> getLiveAlarmType() {
		return alarmType;
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Returns the alarm message.
	 *
	 * @return Same as in description.
	 */
	@Nullable
	public String getAlarmMessage() {
		if (alarmMessage == null) {
			alarmMessage = new MutableLiveData<>(null);
		}
		return alarmMessage.getValue();
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Set the alarm message.
	 *
	 * @param alarmMessage The alarm message to be set. May be {@code null}.
	 */
	public void setAlarmMessage(@Nullable String alarmMessage) {
		if (this.alarmMessage == null) {
			this.alarmMessage = new MutableLiveData<>();
		}
		this.alarmMessage.setValue(alarmMessage);
	}
}
