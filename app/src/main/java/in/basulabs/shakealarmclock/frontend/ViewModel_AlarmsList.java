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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import in.basulabs.shakealarmclock.backend.AlarmData;
import in.basulabs.shakealarmclock.backend.AlarmDatabase;
import in.basulabs.shakealarmclock.backend.AlarmEntity;
import in.basulabs.shakealarmclock.backend.RepeatEntity;

public class ViewModel_AlarmsList extends ViewModel implements LifecycleObserver {

	private MutableLiveData<ArrayList<AlarmData>> alarmDataArrayList;
	private final MutableLiveData<Integer> alarmsCount = new MutableLiveData<>(0);
	private final MutableLiveData<Boolean> isAlarmPending = new MutableLiveData<>(false);
	private MutableLiveData<Bundle> pendingAlarmDetails;
	private final MutableLiveData<Boolean> canRequestNonEssentialPerms =
		new MutableLiveData<>(false);

	/**
	 * Denotes whether the data on alarms is already in the memory.
	 * <p>
	 * This relies on the fact that if the ViewModel is not initialized, then this
	 * variable will also have default value.
	 */
	private final MutableLiveData<Boolean> alreadyInitialized = new MutableLiveData<>(
		false);

	//--------------------------------------------------------------------------------------------------

	/**
	 * Get an observable instance of the number of alarms.
	 *
	 * @return A {@link LiveData} instance of the number of alarms.
	 */
	public LiveData<Integer> getLiveAlarmsCount() {
		return alarmsCount;
	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Increments the number of alarms by 1.
	 */
	private void incrementAlarmsCount() {

		if (alarmsCount.getValue() == null) {
			alarmsCount.setValue(1);
		} else {
			alarmsCount.setValue(alarmsCount.getValue() + 1);
		}
	}

	//----------------------------------------------------------------------------------

	/**
	 * Decrements the number of alarms by 1.
	 */
	private void decrementAlarmsCount() {
		if (alarmsCount.getValue() != null && alarmsCount.getValue() > 0) {
			alarmsCount.setValue(alarmsCount.getValue() - 1);
		}
	}

	//----------------------------------------------------------------------------------

	/**
	 * Get the total number of alarms in the database.
	 *
	 * @param alarmDatabase The {@link AlarmDatabase} object.
	 * @return The total number of alarms in the database.
	 */
	public int getAlarmsCount(@NonNull AlarmDatabase alarmDatabase) {

		AtomicInteger count = new AtomicInteger(0);

		Thread thread = new Thread(
			() -> count.set(alarmDatabase.alarmDAO().getNumberOfAlarms()));
		thread.start();

		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		alarmsCount.setValue(count.get());

		return count.get();
	}

	//---------------------------------------------------------------------------------

	/**
	 * Updates the date of the alarm to the next feasible date, and then reads data into
	 * {@link #alarmDataArrayList}.
	 *
	 * @param alarmDatabase The {@link AlarmDatabase} object to be used to read
	 * 	from/write to the database.
	 * @param wait If this is {@code true}, the method will not return until the
	 * 	background thread has completed execution. Otherwise the background thread
	 * 	will be
	 * 	started and not waited upon for completion.
	 */
	private void init(@NonNull AlarmDatabase alarmDatabase, boolean wait) {

		if (alarmDataArrayList == null || alarmDataArrayList.getValue() == null || wait) {

			alarmDataArrayList = new MutableLiveData<>(new ArrayList<>());

			Thread thread = new Thread(() -> {

				// Retrieve the list of alarms:
				List<AlarmEntity> alarmEntityList = alarmDatabase.alarmDAO().getAlarms();

				if (alarmEntityList != null) {

					///////////////////////////////////////
					// Update the date
					///////////////////////////////////////
					for (AlarmEntity entity : alarmEntityList) {

						LocalDateTime alarmDateTime;

						// Update the date iff the alarm is OFF and the repeat is OFF.
						if (!entity.isRepeatOn && !entity.isAlarmOn) {

							alarmDateTime = LocalDateTime.of(entity.alarmYear,
								entity.alarmMonth, entity.alarmDay, entity.alarmHour,
								entity.alarmMinutes);

							if (alarmDateTime.isBefore(LocalDateTime.now())) {
								while (alarmDateTime.isBefore(LocalDateTime.now())) {
									alarmDateTime = alarmDateTime.plusDays(1);
								}
								alarmDatabase.alarmDAO()
									.updateAlarmDate(entity.alarmHour,
										entity.alarmMinutes,
										alarmDateTime.getDayOfMonth(),
										alarmDateTime.getMonthValue(),
										alarmDateTime.getYear());
								alarmDatabase.alarmDAO()
									.toggleHasUserChosenDate(entity.alarmID, 0);
							}
						}
					}

					/////////////////////////////////////////////////////////////////////
					// Now retrieve the alarms list again and fill up alarmDataArrayList
					// for the RecyclerView.
					/////////////////////////////////////////////////////////////////////
					alarmEntityList = alarmDatabase.alarmDAO().getAlarms();

					for (AlarmEntity entity : alarmEntityList) {

						LocalDateTime alarmDateTime = LocalDateTime.of(entity.alarmYear,
							entity.alarmMonth,
							entity.alarmDay, entity.alarmHour, entity.alarmMinutes);

						ArrayList<Integer> repeatDays = entity.isRepeatOn
							? new ArrayList<>(alarmDatabase.alarmDAO()
							.getAlarmRepeatDays(entity.alarmID))
							: null;

						Objects.requireNonNull(alarmDataArrayList.getValue())
							.add(getAlarmDataObject(entity, alarmDateTime, repeatDays));

					}

					alarmsCount.postValue(alarmEntityList.size());
					alreadyInitialized.postValue(true);
				}

			});

			thread.start();

			if (wait) {
				try {
					thread.join();
				} catch (InterruptedException ignored) {
				}
			}


		}

	}

	//-----------------------------------------------------------------------------------

	/**
	 * Re-reads data from the database if and only if the data is not in the memory, and
	 * waits for the background thread to be completed.
	 *
	 * @param alarmDatabase The {@link AlarmDatabase} object to be used to read the
	 * 	database.
	 */
	public void initAndWait(@NonNull AlarmDatabase alarmDatabase) {
		if (alreadyInitialized.getValue() == null || !alreadyInitialized.getValue()) {
			init(alarmDatabase, true);
		}
	}

	//-----------------------------------------------------------------------------------

	/**
	 * Reads from the database if and only if the ArrayList hasn't been initialized yet.
	 * Doesn't wait for background thread to be completed.
	 *
	 * @param alarmDatabase The {@link AlarmDatabase} object to be used to read the
	 * 	database.
	 */
	public void init(@NonNull AlarmDatabase alarmDatabase) {
		if (alreadyInitialized.getValue() == null || !alreadyInitialized.getValue()) {
			init(alarmDatabase, false);
		}
	}

	//-----------------------------------------------------------------------------------

	/**
	 * Re-reads data from the database regardless of whether the data is already in the
	 * memory.
	 *
	 * @param alarmDatabase The {@link AlarmDatabase} object to be used to read the
	 * 	database.
	 */
	public void forceInitAndWait(@NonNull AlarmDatabase alarmDatabase) {
		init(alarmDatabase, true);
	}

	//-----------------------------------------------------------------------------------

	/**
	 * Get an {@link ArrayList} of {@link AlarmData} objects that can be used to
	 * instantiate the adapter.
	 *
	 * @return An {@link ArrayList} of {@link AlarmData} objects
	 */
	public ArrayList<AlarmData> getAlarmDataArrayList() {
		if (alarmDataArrayList.getValue() == null) {
			return new ArrayList<>();
		} else {
			return alarmDataArrayList.getValue();
		}
	}

	//-----------------------------------------------------------------------------------

	/**
	 * Adds an alarm to the database.
	 *
	 * @param alarmDatabase The {@link AlarmDatabase} object.
	 * @param alarmEntity The {@link AlarmEntity} object that contanins all the alarm
	 * 	details.
	 * @param repeatDays The days in which the alarm is to be repeated, if repeat is ON.
	 * 	Otherwise, this value can be null.
	 * @return An array consiting of TWO elements: the alarm ID at index 0 and the
	 * 	position at which the alarm was inserted at index 1. The latter can be used to
	 * 	scroll the {@link androidx.recyclerview.widget.RecyclerView} using
	 *    {@link androidx.recyclerview.widget.RecyclerView#scrollToPosition(int)}.
	 */
	public int[] addAlarm(@NonNull AlarmDatabase alarmDatabase,
		@NonNull AlarmEntity alarmEntity, @Nullable ArrayList<Integer> repeatDays) {

		AtomicInteger alarmID = new AtomicInteger();

		Thread thread = new Thread(() -> {

			///////////////////////////////////////////////////////////////
			// First, add the alarm to the database.
			//////////////////////////////////////////////////////////////
			alarmDatabase.alarmDAO().addAlarm(alarmEntity);

			alarmID.set(alarmDatabase.alarmDAO()
				.getAlarmId(alarmEntity.alarmHour, alarmEntity.alarmMinutes));

			if (alarmEntity.isRepeatOn && repeatDays != null) {
				Collections.sort(repeatDays);
				for (int day : repeatDays) {
					alarmDatabase.alarmDAO()
						.insertRepeatData(new RepeatEntity(alarmID.get(), day));
				}
			}

		});
		thread.start();

		LocalDateTime alarmDateTime = LocalDateTime.of(alarmEntity.alarmYear,
			alarmEntity.alarmMonth,
			alarmEntity.alarmDay, alarmEntity.alarmHour, alarmEntity.alarmMinutes);

		int scrollToPosition = 0;

		AlarmData newAlarmData = getAlarmDataObject(alarmEntity, alarmDateTime,
			repeatDays);

		if (alarmDataArrayList.getValue() == null ||
			alarmDataArrayList.getValue().size() == 0) {

			alarmDataArrayList = new MutableLiveData<>(new ArrayList<>());
			Objects.requireNonNull(alarmDataArrayList.getValue()).add(newAlarmData);

		} else {

			// Check if the array list already has an alarm with same time, and remove
			// it:
			int index = isAlarmInTheList(alarmEntity.alarmHour,
				alarmEntity.alarmMinutes);
			if (index != -1) {
				alarmDataArrayList.getValue().remove(index);
			}

			// Insert the new alarm at the correct position:
			for (int i = 0;
			     i < Objects.requireNonNull(alarmDataArrayList.getValue()).size(); i++) {

				if (alarmDataArrayList.getValue()
					.get(i)
					.getAlarmTime()
					.isBefore(alarmDateTime.toLocalTime())) {

					if ((i + 1) < alarmDataArrayList.getValue().size()) {

						if (alarmDataArrayList.getValue()
							.get(i + 1)
							.getAlarmTime()
							.isAfter(alarmDateTime.toLocalTime())) {
							alarmDataArrayList.getValue().add(i + 1, newAlarmData);
							scrollToPosition = i + 1;
							break;
						}
					} else {
						alarmDataArrayList.getValue().add(newAlarmData);
						scrollToPosition = alarmDataArrayList.getValue().size() - 1;
						break;
					}
				}

				if (i == alarmDataArrayList.getValue().size() - 1) {
					alarmDataArrayList.getValue().add(0, newAlarmData);
					break;
				}
			}
		}

		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		incrementAlarmsCount();

		return new int[]{alarmID.get(), scrollToPosition};

	}

	//-----------------------------------------------------------------------------------

	/**
	 * Get an {@link AlarmData} object that can be added to {@code alarmDataArrayList}.
	 *
	 * @param entity The {@link AlarmEntity} object representing the alarm.
	 * @param alarmDateTime The alarm date and time.
	 * @return An {@link AlarmData} object that can be added to
	 *    {@code alarmDataArrayList}.
	 */
	private AlarmData getAlarmDataObject(@NonNull AlarmEntity entity,
		@NonNull LocalDateTime alarmDateTime,
		@Nullable ArrayList<Integer> repeatDays) {

		if (!entity.isRepeatOn) {
			return new AlarmData(entity.isAlarmOn, alarmDateTime, entity.alarmType,
				entity.alarmMessage);
		} else {
			assert repeatDays != null;
			return new AlarmData(entity.isAlarmOn, alarmDateTime.toLocalTime(),
				entity.alarmType, entity.alarmMessage, repeatDays);
		}

	}

	//-----------------------------------------------------------------------------------

	/**
	 * Removes an alarm from the database and {@code #alarmDataArrayList}.
	 *
	 * @param alarmDatabase The {@link AlarmDatabase} object used to access the
	 * 	database.
	 * @param hour The alarm hour.
	 * @param mins The alarm minutes.
	 * @return The position in the {@code #alarmDataArrayList} from where the alarm was
	 * 	removed.
	 */
	public int removeAlarm(@NonNull AlarmDatabase alarmDatabase, int hour, int mins) {

		int position = -1;

		AtomicInteger alarmId = new AtomicInteger();

		Thread thread = new Thread(() -> {
			alarmId.set(alarmDatabase.alarmDAO().getAlarmId(hour, mins));
			alarmDatabase.alarmDAO().deleteAlarm(hour, mins);
		});
		thread.start();

		for (int i = 0;
		     i < Objects.requireNonNull(alarmDataArrayList.getValue()).size(); i++) {
			AlarmData alarmData = alarmDataArrayList.getValue().get(i);

			if (alarmData.getAlarmTime().equals(LocalTime.of(hour, mins))) {
				alarmDataArrayList.getValue().remove(i);
				position = i;
				break;
			}
		}

		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		decrementAlarmsCount();

		return position;

	}

	//-----------------------------------------------------------------------------------

	/**
	 * Toggles the ON/OFF state of an alarm.
	 *
	 * @param alarmDatabase The {@link AlarmDatabase} object used to access the
	 * 	database.
	 * @param hour The alarm hour.
	 * @param mins The alarm minute.
	 * @param newAlarmState The new alarm state. 0 means OFF and 1 means ON.
	 */
	public int toggleAlarmState(@NonNull AlarmDatabase alarmDatabase, int hour, int mins
		, int newAlarmState) {

		AtomicInteger alarmId = new AtomicInteger();

		Thread thread = new Thread(() -> {
			alarmId.set(alarmDatabase.alarmDAO().getAlarmId(hour, mins));

			alarmDatabase.alarmDAO().toggleAlarm(alarmId.get(), newAlarmState);
		});
		thread.start();

		// Toggle the alarm status in the alarmDataArrayList:
		int index = isAlarmInTheList(hour, mins);
		AlarmData alarmData = Objects.requireNonNull(alarmDataArrayList.getValue())
			.get(index);
		alarmData.setSwitchedOn(newAlarmState == 1);
		alarmDataArrayList.getValue().set(index, alarmData);

		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		return index;

	}

	//----------------------------------------------------------------------------------

	/**
	 * Get the unique alarm ID.
	 *
	 * @param alarmDatabase The {@link AlarmDatabase} object used to access the
	 * 	database.
	 * @param hour The alarm hour.
	 * @param mins The alarm minute.
	 * @return The unique alarm ID if the alarm is present in the database, otherwise 0
	 * 	(zero).
	 */
	public int getAlarmId(@NonNull AlarmDatabase alarmDatabase, int hour, int mins) {
		AtomicInteger alarmId = new AtomicInteger(0);

		Thread thread = new Thread(() -> {
			try {
				alarmId.set(alarmDatabase.alarmDAO().getAlarmId(hour, mins));
			} catch (Exception ex) {
				alarmId.set(0);
			}
		});
		thread.start();

		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		return alarmId.get();
	}

	//----------------------------------------------------------------------------------

	/**
	 * Get the repeat days corresponding to a certain alarm.
	 *
	 * @param alarmDatabase The {@link AlarmDatabase} object used to access the
	 * 	database.
	 * @param hour The alarm hour.
	 * @param mins The alarm minute.
	 * @return An {@link ArrayList} containing the days in which the alarm is set to
	 * 	repeat. Will return an empty {@link ArrayList} if repeat is OFF.
	 */
	public ArrayList<Integer> getRepeatDays(@NonNull AlarmDatabase alarmDatabase,
		int hour, int mins) {
		AtomicReference<ArrayList<Integer>> repeatDays =
			new AtomicReference<>(new ArrayList<>());

		Thread thread =
			new Thread(() -> repeatDays.set(new ArrayList<>(alarmDatabase.alarmDAO()
				.getAlarmRepeatDays(getAlarmId(alarmDatabase, hour, mins)))));
		thread.start();

		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		return repeatDays.get();
	}

	//----------------------------------------------------------------------------------

	/**
	 * Get the {@link AlarmEntity} object for a certain alarm.
	 *
	 * @param alarmDatabase The {@link AlarmDatabase} object used to access the
	 * 	database.
	 * @param hour The alarm hour.
	 * @param mins The alarm minute.
	 * @return The {@link AlarmEntity} object for the alarm specified by {@code hour} and
	 *    {@code mins}.
	 */
	public AlarmEntity getAlarmEntity(@NonNull AlarmDatabase alarmDatabase, int hour,
		int mins) {

		AtomicReference<AlarmEntity> alarmEntity = new AtomicReference<>();

		Thread thread =
			new Thread(() -> alarmEntity.set(
				alarmDatabase.alarmDAO().getAlarmDetails(hour, mins).get(0)));
		thread.start();

		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		return alarmEntity.get();
	}

	//----------------------------------------------------------------------------------

	/**
	 * Checks whether the alarm is already present in {@link #alarmDataArrayList}.
	 *
	 * @param hour The alarm hour.
	 * @param mins The alarm minutes.
	 * @return {@code -1} if the alarm is not present in the list, otherwise the index
	 * 	where the alarm is present.
	 */
	private int isAlarmInTheList(int hour, int mins) {

		if (alarmDataArrayList.getValue() != null &&
			alarmDataArrayList.getValue().size() > 0) {
			for (AlarmData alarmData : alarmDataArrayList.getValue()) {
				if (alarmData.getAlarmTime().equals(LocalTime.of(hour, mins))) {
					return alarmDataArrayList.getValue().indexOf(alarmData);
				}
			}
		}

		return -1;
	}

	//----------------------------------------------------------------------------------

	/**
	 * Returns whether there is a pending alarm that has to be switched on.
	 * <p>
	 * The alarm is pending because
	 * {@link android.Manifest.permission#SCHEDULE_EXACT_ALARM} has not been granted to
	 * the app.
	 *
	 * @return {@code true} if an alarm is pending to be switched on, otherwise
	 *    {@code false}.
	 */
	public boolean getPendingStatus() {
		return isAlarmPending.getValue() != null && isAlarmPending.getValue();
	}

	//----------------------------------------------------------------------------------

	/**
	 * Set whether an alarm is pending to be switched on.
	 * <p>
	 * The alarm is pending because
	 * {@link android.Manifest.permission#SCHEDULE_EXACT_ALARM} has not been granted to
	 * the app.
	 *
	 * @param status {@code true} if an alarm is pending to be switched on, otherwise
	 *    {@code false}.
	 */
	public void setPendingStatus(boolean status) {
		isAlarmPending.setValue(status);
	}

	//----------------------------------------------------------------------------------

	/**
	 * Save the details of a pending alarm.
	 *
	 * @param data The details of the pending alarm. May be {@code null}.
	 */
	public void savePendingAlarm(@Nullable Bundle data) {
		pendingAlarmDetails = new MutableLiveData<>();
		pendingAlarmDetails.setValue(data);
	}

	//----------------------------------------------------------------------------------

	/**
	 * Get the details of a pending alarm.
	 *
	 * @return A {@link Bundle} with the details of the pending alarm.
	 */
	@Nullable
	public Bundle getPendingALarmData() {
		return pendingAlarmDetails.getValue();
	}

	//----------------------------------------------------------------------------------

	public boolean getCanRequestNonEssentialPerms() {
		return Boolean.TRUE.equals(canRequestNonEssentialPerms.getValue());
	}

	public void setCanRequestNonEssentialPerms(boolean canRequestNonEssentialPerms) {
		this.canRequestNonEssentialPerms.setValue(canRequestNonEssentialPerms);
	}
}
