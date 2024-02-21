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

import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(tableName = "alarm_repeat_entity", primaryKeys = {"alarmID", "repeatDay"},
	foreignKeys = @ForeignKey(entity = AlarmEntity.class,
		parentColumns = "alarmID",
		childColumns = "alarmID",
		onDelete = ForeignKey.CASCADE,
		onUpdate = ForeignKey.CASCADE))
public class RepeatEntity {

	/**
	 * The unique id of the alarm. Follows from {@link AlarmEntity}.
	 */
	public int alarmID;

	/**
	 * The day on which the alarm is to be repeated. Follows {@link java.time.DayOfWeek}
	 * enum.
	 */
	public int repeatDay;

	public RepeatEntity(int alarmID, int repeatDay) {
		this.alarmID = alarmID;
		this.repeatDay = repeatDay;
	}
}
