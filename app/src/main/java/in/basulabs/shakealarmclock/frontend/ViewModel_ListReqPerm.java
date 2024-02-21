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

package in.basulabs.shakealarmclock.frontend;

import android.Manifest;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

import in.basulabs.shakealarmclock.R;
import in.basulabs.shakealarmclock.backend.ConstantsAndStatics;
import in.basulabs.shakealarmclock.backend.Permission;

public class ViewModel_ListReqPerm extends ViewModel {

	private MutableLiveData<ArrayList<String>> permsRequested = null;

	private final MutableLiveData<HashMap<String, Integer>> permsLevelMap =
		new MutableLiveData<>(new HashMap<>());

	private final MutableLiveData<ArrayList<Permission>> permsQueue =
		new MutableLiveData<>(new ArrayList<>());

	private final MutableLiveData<Permission> currentPermission =
		new MutableLiveData<>();

	void init(@NonNull SharedPreferences sharedPref) {

		if (permsRequested != null && permsRequested.getValue() != null
			&& Objects.requireNonNull(permsQueue.getValue()).isEmpty()) {

			for (String perm : permsRequested.getValue()) {

				switch (perm) {

					case Manifest.permission.SCHEDULE_EXACT_ALARM ->
						Objects.requireNonNull(permsQueue.getValue())
							.add(new Permission(
								Manifest.permission.SCHEDULE_EXACT_ALARM,
								R.string.perm_title_exact_alarms,
								R.string.perm_expln_exact_alarms,
								Objects.requireNonNull(permsLevelMap.getValue())
									.containsKey(perm) ?
									permsLevelMap.getValue().get(perm) :
									ConstantsAndStatics.PERMISSION_LEVEL_ESSENTIAL,
								getTimesPermRequested(sharedPref,
									Manifest.permission.SCHEDULE_EXACT_ALARM)));

					case Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS ->
						Objects.requireNonNull(permsQueue.getValue())
							.add(new Permission(
								Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
								R.string.perm_title_ign_bat_optim,
								R.string.perm_exp_ign_bat_optim,
								Objects.requireNonNull(permsLevelMap.getValue())
									.containsKey(perm) ?
									permsLevelMap.getValue().get(perm) :
									ConstantsAndStatics.PERMISSION_LEVEL_RECOMMENDED,
								getTimesPermRequested(sharedPref,
									Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)));

					case Manifest.permission.ACCESS_NOTIFICATION_POLICY ->
						Objects.requireNonNull(permsQueue.getValue())
							.add(new Permission(
								Manifest.permission.ACCESS_NOTIFICATION_POLICY,
								(R.string.perm_title_notif_policy),
								(R.string.perm_exp_notif_policy),
								Objects.requireNonNull(permsLevelMap.getValue())
									.containsKey(perm) ?
									permsLevelMap.getValue().get(perm) :
									ConstantsAndStatics.PERMISSION_LEVEL_RECOMMENDED,
								getTimesPermRequested(sharedPref,
									Manifest.permission.ACCESS_NOTIFICATION_POLICY)));

					case Manifest.permission.READ_MEDIA_AUDIO ->
						Objects.requireNonNull(permsQueue.getValue())
							.add(new Permission(
								Manifest.permission.READ_MEDIA_AUDIO,
								R.string.perm_title_read_media_audio,
								R.string.perm_exp_media_audio,
								Objects.requireNonNull(permsLevelMap.getValue())
									.containsKey(perm) ?
									permsLevelMap.getValue().get(perm) :
									ConstantsAndStatics.PERMISSION_LEVEL_OPTIONAL,
								getTimesPermRequested(sharedPref,
									Manifest.permission.READ_MEDIA_AUDIO)));

					case Manifest.permission.POST_NOTIFICATIONS ->
						Objects.requireNonNull(permsQueue.getValue())
							.add(new Permission(
								Manifest.permission.POST_NOTIFICATIONS,
								R.string.perm_title_post_notif,
								R.string.perm_exp_post_notif,
								Objects.requireNonNull(permsLevelMap.getValue())
									.containsKey(perm) ?
									permsLevelMap.getValue().get(perm) :
									ConstantsAndStatics.PERMISSION_LEVEL_ESSENTIAL,
								getTimesPermRequested(sharedPref,
									Manifest.permission.POST_NOTIFICATIONS)));

					case Manifest.permission.READ_EXTERNAL_STORAGE ->
						Objects.requireNonNull(permsQueue.getValue())
							.add(new Permission(
								Manifest.permission.READ_EXTERNAL_STORAGE,
								R.string.perm_title_ext_storage,
								R.string.perm_exp_ext_storage,
								Objects.requireNonNull(permsLevelMap.getValue())
									.containsKey(perm) ?
									permsLevelMap.getValue().get(perm) :
									ConstantsAndStatics.PERMISSION_LEVEL_OPTIONAL,
								getTimesPermRequested(sharedPref,
									Manifest.permission.READ_EXTERNAL_STORAGE)));

				}
			}

			permsRequested = null;
		}


	}

	/**
	 * Get the permissions in the queue that have not been granted yet.
	 *
	 * @return Permissions in the queue that have not yet been granted. Can return an
	 * 	empty list, but not null.
	 */
	@NonNull
	ArrayList<Permission> getPermsQueue() {
		if (permsQueue.getValue() == null) {
			return new ArrayList<>();
		}
		return Objects.requireNonNull(permsQueue.getValue());
	}

	/**
	 * Set the permissions to be requested. Sent by the calling activity.
	 *
	 * @param permsRequested The permissions to be requested.
	 */
	void setPermsRequested(@Nullable ArrayList<String> permsRequested) {
		this.permsRequested = new MutableLiveData<>(permsRequested);
	}

	/**
	 * Deletes a permission from the queue.
	 *
	 * @param permission The {@link Permission} object to be deleted.
	 * @return The position of the deleted element in the queue, or -1 if the object is
	 * 	absent.
	 */
	int deleteItemFromQueue(@NonNull Permission permission) {
		ArrayList<Permission> permsQueue = getPermsQueue();
		if (permsQueue.contains(permission)) {
			int pos = permsQueue.indexOf(permission);
			permsQueue.remove(pos);
			this.permsQueue.setValue(permsQueue);
			return pos;
		}
		return -1;
	}

	/**
	 * Checks whether any essential permissions are present in the queue.
	 * <p>
	 * Essential permissions are those without which the app won't work.
	 *
	 * @return {@code true} is any essential permissions are present, otherwise
	 *    {@code false}.
	 */
	boolean areEssentialPermsPresent() {
		for (Permission permission : getPermsQueue()) {
			if (permission.permType() == ConstantsAndStatics.PERMISSION_LEVEL_ESSENTIAL) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Sets the permission that is currently being requested.
	 *
	 * @param currentPermission The {@link Permission} being requested currently.
	 */
	void setCurrentPermission(@Nullable Permission currentPermission) {
		this.currentPermission.setValue(currentPermission);
	}

	/**
	 * Returns the {@link Permission} being requested currently.
	 *
	 * @return The {@link Permission} being requested currently. Can be {@code null}
	 * if no
	 * 	permission is being requested.
	 */
	@Nullable
	Permission getCurrentPermission() {
		return currentPermission.getValue();
	}

	/**
	 * Returns an observable instance of the permissions queue.
	 *
	 * @return An observable instance of the permissions queue.
	 */
	@NonNull
	LiveData<ArrayList<Permission>> observePermsQueue() {
		return permsQueue;
	}

	/**
	 * Returns how many times a particular permission has already been asked by this app.
	 * <p>
	 * <a href="https://stackoverflow.com/a/39204743/8387076">Courtesy</a>
	 *
	 * @param sharedPref A non-null instance of the {@link SharedPreferences} where this
	 * 	data is stored.
	 * @param permAndroidString The permission string. Eg.
	 *    {@link Manifest.permission#READ_MEDIA_AUDIO}.
	 * @return How many times a permission has been requested by the app.
	 */
	int getTimesPermRequested(@NonNull SharedPreferences sharedPref,
		@NonNull String permAndroidString) {

		String defValue = new Gson().toJson(new HashMap<String, Integer>());
		String json = sharedPref.getString(
			ConstantsAndStatics.SHARED_PREF_KEY_TIMES_PERMS_REQUESTED, defValue);
		TypeToken<HashMap<String, Integer>> token = new TypeToken<>() {
		};
		HashMap<String, Integer> retrievedMap = new Gson().fromJson(json,
			token.getType());

		if (retrievedMap.containsKey(permAndroidString)) {
			return Objects.requireNonNull(retrievedMap.get(permAndroidString));
		} else {
			return 0;
		}

	}

	/**
	 * Increment by 1 the number of times a particular permission has been requested by
	 * the app.
	 * <p>
	 * <a href="https://stackoverflow.com/a/39204743/8387076">Courtesy</a>
	 *
	 * @param sharedPref A non-null instance of the {@link SharedPreferences} where this
	 * 	data is stored.
	 * @param permAndroidString The permission string. Eg.
	 *    {@link Manifest.permission#READ_MEDIA_AUDIO}.
	 */
	void incrementTimesPermRequested(@NonNull SharedPreferences sharedPref,
		@NonNull String permAndroidString) {

		// First retrieve the entire map
		String defValue = new Gson().toJson(new HashMap<String, Integer>());
		String json = sharedPref.getString(
			ConstantsAndStatics.SHARED_PREF_KEY_TIMES_PERMS_REQUESTED, defValue);
		TypeToken<HashMap<String, Integer>> token = new TypeToken<>() {
		};
		HashMap<String, Integer> retrievedMap = new Gson().fromJson(json,
			token.getType());

		Integer newNumberOfTimesRequested = Objects.isNull(
			retrievedMap.get(permAndroidString))
			? 1 : retrievedMap.get(permAndroidString) + 1;

		// Put the new value in it
		retrievedMap.put(permAndroidString, newNumberOfTimesRequested);

		// Write it to SharedPreferences
		String jsonString = new Gson().toJson(retrievedMap);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(ConstantsAndStatics.SHARED_PREF_KEY_TIMES_PERMS_REQUESTED,
			jsonString);
		editor.commit();
	}

	public void setPermsLevelMap(@Nullable Bundle permsLevelBundle) {
		// http://www.java2s.com/example/android/android-os/convert-bundle-to-map.html
		HashMap<String, Integer> map = new HashMap<>();
		if (permsLevelBundle != null) {
			Set<String> bundleKeys = permsLevelBundle.keySet();
			for (String key : bundleKeys) {
				map.put(key, permsLevelBundle.getInt(key));
			}
		}
		this.permsLevelMap.setValue(map);
	}
}
