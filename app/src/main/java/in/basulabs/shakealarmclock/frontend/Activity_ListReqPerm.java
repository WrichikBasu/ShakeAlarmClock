package in.basulabs.shakealarmclock.frontend;

import android.Manifest;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
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

	private RecyclerView permRecyclerView;
	private PermissionAdapter permAdapter;
	private ViewModel_ListReqPerm viewModel;
	private ActivityResultLauncher<String> reqPermsLauncher;
	private SharedPreferences sharedPreferences;
	private SharedPreferences.Editor sharedPrefEditor;

	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (Objects.equals(intent.getAction(),
					AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) {

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

		sharedPreferences = getSharedPreferences(
				ConstantsAndStatics.SHARED_PREF_FILE_NAME, MODE_PRIVATE);
		sharedPrefEditor = sharedPreferences.edit();

		viewModel = new ViewModelProvider(this).get(ViewModel_ListReqPerm.class);

		if (getIntent().hasExtra(ConstantsAndStatics.EXTRA_PERMS_REQUESTED)) {
			viewModel.setPermsRequested(Objects.requireNonNull(getIntent().getExtras())
					.getStringArrayList(ConstantsAndStatics.EXTRA_PERMS_REQUESTED));
		} else {
			viewModel.setPermsRequested(null);
		}

		viewModel.init(sharedPreferences);

		permRecyclerView = findViewById(R.id.permsRecyclerView);
		permRecyclerView.setLayoutManager(new LinearLayoutManager(this));

		permAdapter = new PermissionAdapter(viewModel.getPermsQueue(), this, this);
		permRecyclerView.setAdapter(permAdapter);

		reqPermsLauncher = registerForActivityResult(
				new ActivityResultContracts.RequestPermission(), isGranted -> {

					Log.println(Log.ERROR, getClass().getSimpleName(),
							"Current permission = " + viewModel.getCurrentPermission());

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
			if (ContextCompat.checkSelfPermission(this, viewModel.getCurrentPermission()
					.androidString()) == PackageManager.PERMISSION_GRANTED) {
				onPermissionGranted();
			} else {
				onPermissionDenied();
			}
		}
	}

	@Override
	public void onGrantBtnClick(@NonNull Permission permission) {

		viewModel.setCurrentPermission(permission);
		viewModel.incrementPermsRequested(sharedPreferences, permission.androidString());

		Log.println(Log.ERROR, getClass().getSimpleName(),
				"Set current permission = " + viewModel.getCurrentPermission());

		int numberOfTimesRequested = viewModel.getPermsRequestStatus(sharedPreferences,
				permission.androidString());

		switch (permission.androidString()) {

			case Manifest.permission.POST_NOTIFICATIONS ->
					requestPostNotifPerm(numberOfTimesRequested);

			case Manifest.permission.SCHEDULE_EXACT_ALARM -> requestAlarmPerm();

			case Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS ->
					requestIgnBatOptPerm();

			case Manifest.permission.ACCESS_NOTIFICATION_POLICY ->
					requestNotifPolicyPerm(numberOfTimesRequested);

			case Manifest.permission.READ_MEDIA_AUDIO ->
					requestMediaAudioPerm(numberOfTimesRequested);

			case Manifest.permission.READ_EXTERNAL_STORAGE ->
					requestExtStoragePerm(numberOfTimesRequested);
		}
	}

	@Override
	public void onDenyBtnClick(Permission permission) {
		onPermissionDenied();
	}

	@Override
	public void onBackPressed() {
		if (viewModel.areEssentialPermsPresent()) {
			Toast.makeText(this, "App won't work without the Essential permissions!",
					Toast.LENGTH_LONG).show();
		} else {
			finish();
		}
	}

	private void observePermsQueue(ArrayList<Permission> permsQueue) {
		Log.e(getClass().getSimpleName(), "Queue: " + permsQueue.toString());
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
		viewModel.setCurrentPermission(null);
		int pos = viewModel.deleteItemFromQueue(
				Objects.requireNonNull(viewModel.getCurrentPermission()));
		if (pos != -1) {
			permAdapter.notifyItemRemoved(pos);
		}
	}

	/**
	 * Invoke when a permission has been denied.
	 */
	private void onPermissionDenied() {
		if (Objects.requireNonNull(viewModel.getCurrentPermission())
				.permType() == ConstantsAndStatics.PERMISSION_TYPE_ESSENTIAL) {
			Toast.makeText(this, "App won't work without this permission!",
					Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "Sadly, permission denied.", Toast.LENGTH_LONG).show();
		}
		viewModel.setCurrentPermission(null);
	}

	/**
	 * Requests the {@link Manifest.permission#POST_NOTIFICATIONS} permission.
	 *
	 * @param numberOfTimesRequested The number of times the permission has already been
	 * requested. Based on this parameter, either the permission is requested
	 * directly, or
	 * Settings is opened.
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
	 * This permission can only be requested via Settings.
	 */
	private void requestAlarmPerm() {
		Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
		intent.setData(
				new Uri.Builder().scheme("package").opaquePart(getPackageName()).build());
		registerReceiver(broadcastReceiver, new IntentFilter(
				AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED));
		this.startActivity(intent);
	}

	/**
	 * Requests the {@link Manifest.permission#REQUEST_IGNORE_BATTERY_OPTIMIZATIONS}
	 * permission.
	 * <p>
	 * Can only be requested via Settings.
	 */
	private void requestIgnBatOptPerm() {
		Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
		intent.setData(
				new Uri.Builder().scheme("package").opaquePart(getPackageName()).build());
		this.startActivity(intent);
	}

	/**
	 * Requests the {@link Manifest.permission#ACCESS_NOTIFICATION_POLICY} permission
	 * (DND
	 * override permission).
	 *
	 * @param numberOfTimesRequested The number of times the permission has already been
	 * requested. Based on this parameter, either the permission is requested
	 * directly, or
	 * Settings is opened.
	 */
	private void requestNotifPolicyPerm(int numberOfTimesRequested) {

		switch (numberOfTimesRequested) {
			// TODO Can only be asked via Settings?
			case 0, 1, 2 -> reqPermsLauncher.launch(
					Manifest.permission.ACCESS_NOTIFICATION_POLICY);

			default -> new MaterialAlertDialogBuilder(this).setMessage(
							R.string.perm_requested_twice)
					.setPositiveButton(R.string.yes, (dialog, which) -> {
						Intent intent = new Intent(
								Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
						this.startActivity(intent);
					})
					.setNegativeButton(R.string.no,
							((dialog, which) -> onPermissionDenied()))
					.show();
		}
	}

	/**
	 * Requests the granular {@link Manifest.permission#READ_MEDIA_AUDIO} permission.
	 *
	 * @param numberOfTimesRequested The number of times the permission has already been
	 * requested. Based on this parameter, either the permission is requested
	 * directly, or
	 * Settings is opened.
	 */
	private void requestMediaAudioPerm(int numberOfTimesRequested) {

		switch (numberOfTimesRequested) {
			// TODO How to ask via Settings?
			case 0, 1, 2 -> reqPermsLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO);

			default -> new MaterialAlertDialogBuilder(this).setMessage(
							R.string.perm_requested_twice)
					.setPositiveButton(R.string.yes, (dialog, which) -> {
						Intent intent = new Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA);
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

	/**
	 * Requests the {@link Manifest.permission#READ_EXTERNAL_STORAGE} permission.
	 *
	 * @param numberOfTimesRequested The number of times the permission has already been
	 * requested. Based on this parameter, either the permission is requested
	 * directly, or
	 * Settings is opened.
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
