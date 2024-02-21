/*
Copyright (C) 2024  Wrichik Basu (basulabs.developer@gmail.com)

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

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * An interface of {@link Dao} containing queries to be made to the database.
 */
@Dao
public interface AlarmDAO {

	/**
	 * Inserts an alarm into the database. On conflict, the old alarm is replaced by the
	 * new alarm.
	 *
	 * @param alarmEntity The {@link AlarmEntity} object that is to be inserted.
	 */
	@Insert(entity = AlarmEntity.class, onConflict = OnConflictStrategy.REPLACE)
	void addAlarm(AlarmEntity alarmEntity);

	/**
	 * Executes a query to delete an alarm. The specific alarm is determined by the
	 * supplied arguments.
	 *
	 * @param hour The hour of the alarm.
	 * @param mins The minute of the alarm.
	 */
	@Query("DELETE FROM alarm_entity WHERE alarmHour = :hour AND alarmMinutes = :mins")
	void deleteAlarm(int hour, int mins);

	/**
	 * Retrieves the alarm list from the database, ordered by the alarm hour and minutes.
	 *
	 * @return A list of {@link AlarmEntity} type containing objects of the aforesaid
	 * 	class representing alarms.
	 */
	@Query("SELECT * FROM alarm_entity ORDER BY alarmHour, alarmMinutes")
	List<AlarmEntity> getAlarms();

	/**
	 * Toggles the alarm state in the database.
	 *
	 * @param hour The alarm hour.
	 * @param mins The alarm minutes.
	 * @param newAlarmState The new alarm state. {@code 0} means OFF and {code 1} means
	 * 	ON.
	 */
	@Query("UPDATE alarm_entity SET isAlarmOn = :newAlarmState WHERE alarmHour = :hour " +
		"AND alarmMinutes = :mins")
	void toggleAlarm(int hour, int mins, int newAlarmState);

	/**
	 * Returns the alarm details.
	 *
	 * @param hour The hour of the alarm.
	 * @param mins The minute of the alarm.
	 * @return An {@link AlarmEntity} object containing the details of the alarm.
	 */
	@Query("SELECT * FROM alarm_entity WHERE alarmHour = :hour AND alarmMinutes = :mins")
	List<AlarmEntity> getAlarmDetails(int hour, int mins);

	/**
	 * Update an alarm date.
	 *
	 * @param hour The alarm hour.
	 * @param mins The alarm mins.
	 * @param newDayOfMonth The NEW day of month.
	 * @param newMonth The NEW Month.
	 * @param newYear The NEW Year.
	 */
	@Query("UPDATE alarm_entity SET alarmDay = :newDayOfMonth, alarmMonth = :newMonth," +
		" " +
		"alarmYear = :newYear WHERE " +
		"alarmHour = :hour AND alarmMinutes = :mins")
	void updateAlarmDate(int hour, int mins, int newDayOfMonth, int newMonth,
		int newYear);

	/**
	 * Toggles the alarm state in the database.
	 *
	 * @param alarmId The unique id of the alarm.
	 * @param newAlarmState The new alarm state. {@code 0} means OFF and {code 1} means
	 * 	ON.
	 */
	@Query("UPDATE alarm_entity SET isAlarmOn = :newAlarmState WHERE alarmID = :alarmId")
	void toggleAlarm(int alarmId, int newAlarmState);

	/**
	 * Get the days in which the alarm is to be repeated.
	 *
	 * @param alarmId The unique ID of the alarm.
	 * @return The days in which the alarm is to be repeated. If repeat is off, this may
	 * 	return null.
	 */
	@Query("SELECT repeatDay from alarm_repeat_entity WHERE alarmID = :alarmId")
	List<Integer> getAlarmRepeatDays(int alarmId);

	/**
	 * Inserts the repeat days into {@link RepeatEntity}.
	 *
	 * @param repeatEntity The object to be inserted.
	 */
	@Insert(entity = RepeatEntity.class, onConflict = OnConflictStrategy.REPLACE)
	void insertRepeatData(RepeatEntity repeatEntity);

	/**
	 * Get only the alarms that are currently active, i.e., in ON state.
	 *
	 * @return The alarms that are currently active, i.e., in ON state.
	 */
	@Query("SELECT * FROM alarm_entity WHERE isAlarmOn = 1 ORDER BY alarmHour, " +
		"alarmMinutes")
	List<AlarmEntity> getActiveAlarms();

	/**
	 * Get the unique id of an alarm.
	 *
	 * @param hour The alarm hour.
	 * @param mins The alarm mins.
	 * @return The unique alarm ID.
	 */
	@Query(
		"SELECT alarmID FROM alarm_entity WHERE alarmHour = :hour AND alarmMinutes = " +
			":mins")
	int getAlarmId(int hour, int mins);

	/**
	 * Get the number of alarms in the database.
	 *
	 * @return Number of alarms in the database.
	 */
	@Query("SELECT COUNT(*) FROM alarm_entity")
	int getNumberOfAlarms();

	/**
	 * Changes the {@code hasUserChosenDate} variable in the database.
	 *
	 * @param alarmId The alarm ID.
	 * @param newState The new state. Pass 0 if {@code false} and 1 if {@code true}.
	 */
	@Query("UPDATE alarm_entity SET hasUserChosenDate = :newState WHERE alarmID = " +
		":alarmId")
	void toggleHasUserChosenDate(int alarmId, int newState);

	/**
	 * Get the alarms that are currently inactive, i.e., in OFF state.
	 *
	 * @return The alarms that are currently inactive, i.e., in OFF state.
	 */
	@Query("SELECT * FROM alarm_entity WHERE isAlarmOn = 0")
	List<AlarmEntity> getInactiveAlarms();

}
