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

import java.util.Date;

/**
 * A class to generate unique Notification IDs.
 */
public class UniqueNotifID {

	/**
	 * Get a unique notification ID.
	 * <p>
	 * <a href="https://stackoverflow.com/questions/12978184/android-get-unique-id-of-notification#comment51322954_28251192">Courtesy</a>
	 *
	 * @return A unique notification ID.
	 */
	public static int getID(){
		return (int)((new Date().getTime() / 1000L) % Integer.MAX_VALUE);
	}

}
