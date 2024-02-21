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

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {AlarmEntity.class, RepeatEntity.class}, version = 2,
	exportSchema = false)
@TypeConverters({Convertors.class})
public abstract class AlarmDatabase extends RoomDatabase {

	private static AlarmDatabase instance;

	static final Migration MIGRATION_1_2 = new Migration(1, 2) {
		@Override
		public void migrate(@NonNull SupportSQLiteDatabase database) {
			database.execSQL(
				"ALTER TABLE alarm_entity ADD COLUMN alarmMessage VARCHAR(50)");
		}
	};

	public static synchronized AlarmDatabase getInstance(@NonNull Context context) {

		if (instance == null) {

			instance = Room.databaseBuilder(
					Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
						? context.createDeviceProtectedStorageContext()
						: context.getApplicationContext(),
					AlarmDatabase.class, ConstantsAndStatics.DATABASE_NAME)
				.addMigrations(MIGRATION_1_2)
				.fallbackToDestructiveMigrationOnDowngrade()
				.build();
		}
		return instance;
	}

	public abstract AlarmDAO alarmDAO();

}
