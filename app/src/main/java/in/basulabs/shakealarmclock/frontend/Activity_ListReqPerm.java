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
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Objects;

import in.basulabs.shakealarmclock.R;
import in.basulabs.shakealarmclock.backend.ConstantsAndStatics;
import in.basulabs.shakealarmclock.backend.Permission;
import in.basulabs.shakealarmclock.backend.PermissionAdapter;

public class Activity_ListReqPerm extends AppCompatActivity implements
	PermissionAdapter.EventListener {

	private PermissionAdapter permAdapter;
	private ViewModel_ListReqPerm viewModel;
	private ActivityResultLauncher<String> reqPermsLauncher;
	private SharedPreferences sharedPreferences;

	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (Objects.equals(intent.getAction(),
				AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) ||
				Objects.equals(intent.getAction(),
					NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED)) {

				if (!Objects.isNull(viewModel.getCurrentPermission())) {
					onPermissionGranted();
				}
			}
		}
	};

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_req_perm_list);
		setSupportActionBar(findViewById(R.id.toolbar6));

		Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

		sharedPreferences = ConstantsAndStatics.getSharedPref(this);

		viewModel = new ViewModelProvider(this).get(ViewModel_ListReqPerm.class);

		viewModel.setPermsRequested(getIntent()
			.getStringArrayListExtra(ConstantsAndStatics.EXTRA_PERMS_REQUESTED));

		viewModel.setPermsLevelMap(getIntent()
			.getBundleExtra(ConstantsAndStatics.EXTRA_PERMS_REQUESTED_LEVEL));

		viewModel.init(sharedPreferences);

		RecyclerView permRecyclerView = findViewById(R.id.permsRecyclerView);
		permRecyclerView.setLayoutManager(new LinearLayoutManager(this));

		permAdapter = new PermissionAdapter(viewModel.getPermsQueue(), this, this);
		permRecyclerView.setAdapter(permAdapter);

		reqPermsLauncher = registerForActivityResult(
			new ActivityResultContracts.RequestPermission(), isGranted -> {

				if (viewModel.getCurrentPermission() != null) {
					if (isGranted) {
						onPermissionGranted();
					} else {
						onPermissionDenied();
					}
				}
			});

		viewModel.observePermsQueue().observe(this, this::observePermsQueue);

	}

	@Override
	protected void onResume() {
		super.onResume();

		if (viewModel.getCurrentPermission() != null) {

			switch (viewModel.getCurrentPermission().androidString()) {

				case Manifest.permission.SCHEDULE_EXACT_ALARM -> {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

						AlarmManager alarmManager =
							(AlarmManager) getSystemService(Context.ALARM_SERVICE);

						if (alarmManager.canScheduleExactAlarms()) {
							onPermissionGranted();
						} else {
							onPermissionDenied();
						}
					}
				}

				case Manifest.permission.ACCESS_NOTIFICATION_POLICY -> {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

						NotificationManager notifManager =
							(NotificationManager) getSystemService(
								Context.NOTIFICATION_SERVICE);

						if (notifManager.isNotificationPolicyAccessGranted()) {
							onPermissionGranted();
						} else {
							onPermissionDenied();
						}
					}
				}

				case Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

						PowerManager powerManager =
							(PowerManager) getSystemService(POWER_SERVICE);

						if (powerManager.isIgnoringBatteryOptimizations(
							getPackageName())) {
							onPermissionGranted();
						} else {
							onPermissionDenied();
						}
					}
				}

				default -> {
					if (ContextCompat.checkSelfPermission(this,
						viewModel.getCurrentPermission()
							.androidString()) == PackageManager.PERMISSION_GRANTED) {
						onPermissionGranted();
					} else {
						onPermissionDenied();
					}
				}
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			this.onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			unregisterReceiver(broadcastReceiver);
		} catch (Exception ignored) {
		}
	}

	@Override
	public void onGrantBtnClick(@NonNull Permission permission) {

		viewModel.setCurrentPermission(permission);
		viewModel.incrementTimesPermRequested(sharedPreferences,
			permission.androidString());

		int numberOfTimesRequested = viewModel.getTimesPermRequested(sharedPreferences,
			permission.androidString());

		switch (permission.androidString()) {

			case Manifest.permission.POST_NOTIFICATIONS ->
				requestPostNotifPerm(numberOfTimesRequested);

			case Manifest.permission.SCHEDULE_EXACT_ALARM -> requestAlarmPerm();

			case Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS ->
				requestIgnBatOptPerm();

			case Manifest.permission.ACCESS_NOTIFICATION_POLICY ->
				requestNotifPolicyPerm();

			case Manifest.permission.READ_MEDIA_AUDIO ->
				requestMediaAudioPerm(numberOfTimesRequested);

			case Manifest.permission.READ_EXTERNAL_STORAGE ->
				requestExtStoragePerm(numberOfTimesRequested);
		}
	}

	@Override
	public void onDenyBtnClick(Permission permission) {
		int pos = viewModel.deleteItemFromQueue(permission);
		if (pos != -1) {
			permAdapter.notifyItemRemoved(pos);
		}
		onPermissionDenied();
	}

	@Override
	public void onBackPressed() {
		if (viewModel.areEssentialPermsPresent()) {
			setResult(RESULT_CANCELED);
			finish();
		} else {
			setResult(RESULT_OK);
			finish();
		}
	}

	private void observePermsQueue(ArrayList<Permission> permsQueue) {
		if (permsQueue.isEmpty()) {
			this.onBackPressed();
		}
	}

	/**
	 * Call when a permission has been checked to be granted.
	 * <p>
	 * Removes the granted permission from the queue.
	 */
	private void onPermissionGranted() {
		Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
		int pos = viewModel.deleteItemFromQueue(
			Objects.requireNonNull(viewModel.getCurrentPermission()));
		if (pos != -1) {
			permAdapter.notifyItemRemoved(pos);
		}
		viewModel.setCurrentPermission(null);
	}

	/**
	 * Invoke when a permission has been denied.
	 */
	private void onPermissionDenied() {
		if (viewModel.getCurrentPermission() != null &&
			viewModel.getCurrentPermission().permType()
				== ConstantsAndStatics.PERMISSION_LEVEL_ESSENTIAL) {
			Toast.makeText(this, "App won't work without this permission!",
				Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "OK, I will live with it. :(",
				Toast.LENGTH_LONG).show();
		}
		viewModel.setCurrentPermission(null);
	}

	/**
	 * Requests the {@link Manifest.permission#POST_NOTIFICATIONS} permission.
	 *
	 * @param numberOfTimesRequested The number of times the permission has already been
	 * 	requested. Based on this parameter, either the permission is requested directly,
	 * 	or Settings is opened.
	 */
	private void requestPostNotifPerm(int numberOfTimesRequested) {

		switch (numberOfTimesRequested) {

			case 0, 1, 2 ->
				reqPermsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);

			default -> new MaterialAlertDialogBuilder(this).setMessage(
					R.string.perm_requested_twice)
				.setPositiveButton(R.string.yes, (dialog, which) -> {
					Intent intent = new Intent(
						Settings.ACTION_APP_NOTIFICATION_SETTINGS);
					intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
					this.startActivity(intent);
					//TODO
				})
				.setNegativeButton(R.string.no, ((dialog, which) -> {
					onPermissionDenied();
				}))
				.show();
		}
	}

	/**
	 * Requests the {@link Manifest.permission#SCHEDULE_EXACT_ALARM} permission.
	 * <p>
	 * Has to be checked via {@link AlarmManager#canScheduleExactAlarms()}.
	 * <p>
	 * Can only be requested via Settings.
	 * <p>
	 * Broadcasts
	 * {@link AlarmManager#ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED} when
	 * granted.
	 */
	private void requestAlarmPerm() {
		Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
		intent.setData(new Uri.Builder()
			.scheme("package")
			.opaquePart(getPackageName())
			.build());
		registerReceiver(broadcastReceiver, new IntentFilter(
			AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED));
		startActivity(intent);
	}

	/**
	 * Requests the {@link Manifest.permission#REQUEST_IGNORE_BATTERY_OPTIMIZATIONS}
	 * permission.
	 * <p>
	 * Has to be checked via {@link PowerManager#isIgnoringBatteryOptimizations(String)}.
	 * <p>
	 * Can only be requested via Settings. See docs of
	 * {@link Settings#ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS}.
	 */
	private void requestIgnBatOptPerm() {
		Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
		//intent.setData(Uri.parse("package:in.basulabs.shakealarmclock"));
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException ex) {
			Intent intent2 = new Intent(
				Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
			startActivity(intent2);
		}
	}

	/**
	 * Requests the {@link Manifest.permission#ACCESS_NOTIFICATION_POLICY} permission
	 * (DND
	 * override permission).
	 * <p>
	 * Has to be checked via
	 * {@link NotificationManager#isNotificationPolicyAccessGranted()}.
	 * <p>
	 * Can only be asked via Settings. For details, see docs of
	 * {@link NotificationManager#isNotificationPolicyAccessGranted()}.
	 * <p>
	 * Broadcasts
	 * {@link NotificationManager#ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED} when
	 * granted.
	 */
	private void requestNotifPolicyPerm() {

		Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
		registerReceiver(broadcastReceiver, new IntentFilter(
			NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED));
		startActivity(intent);
	}

	/**
	 * Requests the granular {@link Manifest.permission#READ_MEDIA_AUDIO} permission.
	 *
	 * @param numberOfTimesRequested The number of times the permission has already been
	 * 	requested. Based on this parameter, either the permission is requested directly,
	 * 	or Settings is opened.
	 */
	private void requestMediaAudioPerm(int numberOfTimesRequested) {

		switch (numberOfTimesRequested) {
			// TODO How to ask via Settings?
			case 0, 1, 2 -> reqPermsLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO);

			default -> new MaterialAlertDialogBuilder(this).setMessage(
					R.string.perm_requested_twice)
				.setPositiveButton(R.string.yes, (dialog, which) -> {
						/*Intent intent = new Intent(Settings
						.ACTION_APPLICATION_SETTINGS);
						intent.setData(new Uri.Builder().scheme("package")
								.opaquePart(getPackageName())
								.build());
						this.startActivity(intent);*/
				})
				.setNegativeButton(R.string.no,
					((dialog, which) -> onPermissionDenied()))
				.show();
		}
	}

	/**
	 * Requests the {@link Manifest.permission#READ_EXTERNAL_STORAGE} permission.
	 *
	 * @param numberOfTimesRequested The number of times the permission has already been
	 * 	requested. Based on this parameter, either the permission is requested directly,
	 * 	or Settings is opened.
	 */
	private void requestExtStoragePerm(int numberOfTimesRequested) {

		switch (numberOfTimesRequested) {

			case 0, 1, 2 ->
				reqPermsLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);

			default -> new MaterialAlertDialogBuilder(this).setMessage(
					R.string.perm_requested_twice)
				.setPositiveButton(R.string.yes, (dialog, which) -> {
					Intent intent = new Intent(
						Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
					intent.setData(new Uri.Builder().scheme("package")
						.opaquePart(getPackageName())
						.build());
					this.startActivity(intent);
				})
				.setNegativeButton(R.string.no,
					((dialog, which) -> onPermissionDenied()))
				.show();
		}
	}
}
