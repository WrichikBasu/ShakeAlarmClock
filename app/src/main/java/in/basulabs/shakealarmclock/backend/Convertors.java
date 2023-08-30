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
package in.basulabs.shakealarmclock.backend;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

public class Convertors {

	@TypeConverter
	public static Uri stringToUri(@NonNull String str) {
		return Uri.parse(str);
	}

	@TypeConverter
	public static String uriToString(@NonNull Uri uri) {
		return uri.toString();
	}


}
