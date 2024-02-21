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

import android.util.Log;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import java.util.Objects;

public class MyWorkManagerInitializer extends MyContentProvider {

	@Override
	public boolean onCreate() {
		WorkManager.initialize(Objects.requireNonNull(getContext()),
			new Configuration.Builder()
				.setMinimumLoggingLevel(Log.DEBUG)
				.build());
		return true;
	}
}
