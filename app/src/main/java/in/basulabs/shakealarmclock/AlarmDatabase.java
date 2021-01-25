package in.basulabs.shakealarmclock;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {AlarmEntity.class, RepeatEntity.class}, version = 2, exportSchema = false)
@TypeConverters({Convertors.class})
public abstract class AlarmDatabase extends RoomDatabase {

	private static final String DATABASE_NAME = "in_basulabs_shakeAlarmClock_AlarmDatabase";

	private static AlarmDatabase instance;

	static final Migration MIGRATION_1_2 = new Migration(1, 2) {
		@Override
		public void migrate(@NonNull SupportSQLiteDatabase database) {
			database.execSQL("ALTER TABLE alarm_entity ADD COLUMN alarmMessage VARCHAR(50)");
		}
	};

	public static synchronized AlarmDatabase getInstance(Context context) {
		if (instance == null) {
			instance = Room.databaseBuilder(context.getApplicationContext(), AlarmDatabase.class, DATABASE_NAME)
					.addMigrations(MIGRATION_1_2)
					.fallbackToDestructiveMigrationOnDowngrade()
					.build();
		}
		return instance;
	}

	public abstract AlarmDAO alarmDAO();

}
