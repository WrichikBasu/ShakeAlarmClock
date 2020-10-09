package in.basulabs.shakealarmclock;

import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(tableName = "alarm_repeat_entity", primaryKeys = {"alarmID", "repeatDay"},
		foreignKeys = @ForeignKey(entity = AlarmEntity.class,
				parentColumns = "alarmID",
				childColumns = "alarmID",
				onDelete = ForeignKey.CASCADE,
				onUpdate = ForeignKey.CASCADE))
public class RepeatEntity {

	public int alarmID;

	public int repeatDay;

	public RepeatEntity(int alarmID, int repeatDay) {
		this.alarmID = alarmID;
		this.repeatDay = repeatDay;
	}
}
