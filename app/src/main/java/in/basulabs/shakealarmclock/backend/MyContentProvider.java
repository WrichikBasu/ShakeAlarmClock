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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class MyContentProvider extends ContentProvider {

	@Override
	public boolean onCreate() {
		return true;
	}

	@Nullable
	@Override
	public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
		return null;
	}

	@Nullable
	@Override
	public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
		@Nullable String selection,
		@Nullable String[] selectionArgs, @Nullable String sortOrder) {
		return null;
	}

	@Override
	public int update(@NonNull Uri uri, @Nullable ContentValues values,
		@Nullable String selection,
		@Nullable String[] selectionArgs) {
		return 0;
	}

	@Override
	public int delete(@NonNull Uri uri, @Nullable String selection,
		@Nullable String[] selectionArgs) {
		return 0;
	}

	@Nullable
	@Override
	public String getType(@NonNull Uri uri) {
		return null;
	}
}
