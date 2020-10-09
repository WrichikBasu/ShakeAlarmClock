package in.basulabs.shakealarmclock;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {AlarmEntity.class, RepeatEntity.class}, version = 1, exportSchema = false)
@TypeConverters({Convertors.class})
public abstract class AlarmDatabase extends RoomDatabase {

	private static final String DATABASE_NAME = "in_basulabs_shakeAlarmClock_AlarmDatabase";

	private static AlarmDatabase instance;

	public static synchronized AlarmDatabase getInstance(Context context) {
		if (instance == null) {
			instance = Room.databaseBuilder(context.getApplicationContext(), AlarmDatabase.class,
					DATABASE_NAME).fallbackToDestructiveMigration().build();
		}
		return instance;
	}

	public abstract AlarmDAO alarmDAO();

}
