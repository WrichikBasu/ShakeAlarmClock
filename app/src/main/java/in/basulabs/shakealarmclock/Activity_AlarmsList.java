package in.basulabs.shakealarmclock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Activity_AlarmsList extends AppCompatActivity implements AlarmAdapter.AdapterInterface,
		AlertDialog_PermissionReason.DialogListener {

	private AlarmAdapter alarmAdapter;
	private RecyclerView alarmsRecyclerView;

	private static final int NEW_ALARM_REQUEST_CODE = 2564, EXISTING_ALARM_REQUEST_CODE = 3198,
			MY_PERMISSIONS_REQUEST = 857;

	private static final int MODE_ADD_NEW_ALARM = 103, MODE_ACTIVATE_EXISTING_ALARM = 604;

	private static final int MODE_DELETE_ALARM = 504, MODE_DEACTIVATE_ONLY = 509;

	private AlarmDatabase alarmDatabase;

	private ViewModel_AlarmsList viewModel;

	private boolean showPermissionDialog = false;

	private ViewStub viewStub;

	@SuppressLint("StaticFieldLeak")
	private static Activity_AlarmsList myInstance;

	//--------------------------------------------------------------------------------------------------

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alarmslist);
		setSupportActionBar(findViewById(R.id.toolbar));

		alarmDatabase = AlarmDatabase.getInstance(this);
		viewModel = new ViewModelProvider(this).get(ViewModel_AlarmsList.class);

		SharedPreferences sharedPreferences = getSharedPreferences(ConstantsAndStatics.SHARED_PREF_FILE_NAME,
				MODE_PRIVATE);

		viewModel.init(alarmDatabase);

		int defaultTheme = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ConstantsAndStatics.THEME_SYSTEM : ConstantsAndStatics.THEME_AUTO_TIME;

		AppCompatDelegate.setDefaultNightMode(ConstantsAndStatics
				.getTheme(sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_THEME, defaultTheme)));

		myInstance = this;

		Button addAlarmButton = findViewById(R.id.addAlarmButton);
		addAlarmButton.setOnClickListener(view -> {
			Intent intent = new Intent(getApplicationContext(), Activity_AlarmDetails.class);
			intent.setAction(ConstantsAndStatics.ACTION_NEW_ALARM);
			startActivityForResult(intent, NEW_ALARM_REQUEST_CODE);
		});

		alarmsRecyclerView = findViewById(R.id.alarmsRecyclerView);
		alarmsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

		viewStub = findViewById(R.id.viewStub);

		SharedPreferences.Editor prefEditor =
				getSharedPreferences(ConstantsAndStatics.SHARED_PREF_FILE_NAME, MODE_PRIVATE)
						.edit()
						.remove(ConstantsAndStatics.SHARED_PREF_KEY_WAS_APP_RECENTLY_ACTIVE)
						.putBoolean(ConstantsAndStatics.SHARED_PREF_KEY_WAS_APP_RECENTLY_ACTIVE, true);
		prefEditor.commit();

		initAdapter();

		manageViewStub(viewModel.getAlarmsCount(alarmDatabase));

		viewModel.getLiveAlarmsCount().observe(this, this::manageViewStub);

		checkAndRequestPermission();
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_alarmlist_menu, menu);
		return true;
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == R.id.settingsMenuItem) {
			Intent intent = new Intent(this, Activity_Settings.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	//--------------------------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
		myInstance = null;
		ConstantsAndStatics.schedulePeriodicWork(this);
	}


	//--------------------------------------------------------------------------------------------------

	private void manageViewStub(int count) {
		if (count == 0) {
			viewStub.setVisibility(View.VISIBLE);
		} else {
			viewStub.setVisibility(View.GONE);
		}
	}

	//--------------------------------------------------------------------------------------------------

	public static void onDateChanged() {
		if (myInstance != null) {
			myInstance.viewModel.forceInit(myInstance.alarmDatabase);
			myInstance.alarmAdapter = new AlarmAdapter(myInstance.viewModel.getAlarmDataArrayList(),
					myInstance,
					myInstance);
			myInstance.alarmsRecyclerView.swapAdapter(myInstance.alarmAdapter, false);
		}
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		if (showPermissionDialog) {
			showPermissionDialog = false;
			showPermissionDialog();
		}
	}

	//--------------------------------------------------------------------------------------------------

	private void showPermissionDialog() {
		if (! viewModel.getHasPermissionsDialogBeenShownBefore()) {
			viewModel.setHasPermissionsDialogBeenShownBefore(true);
			DialogFragment dialogPermissionReason = new AlertDialog_PermissionReason(
					getResources().getString(R.string.permissionReasonExp_normal));
			dialogPermissionReason.setCancelable(false);
			dialogPermissionReason.show(getSupportFragmentManager(), "");
		}
	}

	//--------------------------------------------------------------------------------------------------

	@SuppressWarnings("StatementWithEmptyBody")
	private void checkAndRequestPermission() {
		if (! viewModel.getHasPermissionBeenRequested()) {
			viewModel.setHasPermissionBeenRequested(true);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
						== PackageManager.PERMISSION_GRANTED) {
				} else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
					showPermissionDialog();
				} else {
					ActivityCompat.requestPermissions(this,
							new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
							MY_PERMISSIONS_REQUEST);
				}
			}
		}
	}

	//--------------------------------------------------------------------------------------------------

	@RequiresApi(api = Build.VERSION_CODES.M)
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
	                                       @NonNull int[] grantResults) {
		if (requestCode == MY_PERMISSIONS_REQUEST) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
				initAdapter();
			} else {
				showPermissionDialog = true;
			}
		}
	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Initialises {@link #alarmAdapter}, and sets the adapter in {@link #alarmsRecyclerView}.
	 */
	private void initAdapter() {
		alarmAdapter = new AlarmAdapter(viewModel.getAlarmDataArrayList(), this, this);
		alarmsRecyclerView.setAdapter(alarmAdapter);
	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Adds an alarm to the database, updates {@link #alarmAdapter}, and activates it via {@link AlarmManager}.
	 *
	 * @param mode The mode. Can be either {@link #MODE_ADD_NEW_ALARM} or {@link #MODE_ACTIVATE_EXISTING_ALARM}.
	 * @param alarmEntity The {@link AlarmEntity} object representing the alarm.
	 * @param repeatDays The days in which the alarm is to repeat. Can be {@code null} is repeat is OFF.
	 */
	private void addOrActivateAlarm(int mode, AlarmEntity alarmEntity,
	                                @Nullable ArrayList<Integer> repeatDays) {

		ConstantsAndStatics.cancelScheduledPeriodicWork(this);

		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		LocalDateTime alarmDateTime;
		LocalDate alarmDate = LocalDate.of(alarmEntity.alarmYear, alarmEntity.alarmMonth,
				alarmEntity.alarmDay);
		LocalTime alarmTime = LocalTime.of(alarmEntity.alarmHour, alarmEntity.alarmMinutes);

		if (alarmEntity.isRepeatOn && repeatDays != null && repeatDays.size() > 0) {

			Collections.sort(repeatDays);

			alarmDateTime = LocalDateTime.of(LocalDate.now(), alarmTime);
			int dayOfWeek = alarmDateTime.getDayOfWeek().getValue();

			for (int i = 0; i < repeatDays.size(); i++) {
				if (repeatDays.get(i) == dayOfWeek) {
					if (alarmTime.isAfter(LocalTime.now())) {
						// Alarm possible today, nothing more to do, break out of loop.
						break;
					}
				} else if (repeatDays.get(i) > dayOfWeek) {
					/////////////////////////////////////////////////////////////////////////
					// There is a day available in the same week for the alarm to ring;
					// select that day and break from loop.
					////////////////////////////////////////////////////////////////////////
					alarmDateTime =
							alarmDateTime.with(TemporalAdjusters.next(DayOfWeek.of(repeatDays.get(i))));
					break;
				}
				if (i == repeatDays.size() - 1) {
					// No day possible in this week. Select the first available date from next week.
					alarmDateTime = alarmDateTime
							.with(TemporalAdjusters.next(DayOfWeek.of(repeatDays.get(0))));
				}
			}

		} else {

			alarmDateTime = LocalDateTime.of(alarmDate, alarmTime);

			if (! alarmDateTime.isAfter(LocalDateTime.now())) {
				alarmDateTime = alarmDateTime.plusDays(1);
			}

		}

		///////////////////////////////////////////////////////////////////////////////////////////
		// IMPORTANT:
		// The alarmEntity object does NOT have an ID. So the ID has to be extracted after
		// adding the alarm to the database because the ID will be auto-generated.
		///////////////////////////////////////////////////////////////////////////////////////////

		int alarmID;
		if (mode == MODE_ADD_NEW_ALARM) {
			int[] result = viewModel.addAlarm(alarmDatabase, alarmEntity, repeatDays);

			alarmID = result[0];

			alarmAdapter = new AlarmAdapter(viewModel.getAlarmDataArrayList(), this, this);
			alarmsRecyclerView.swapAdapter(alarmAdapter, false);
			alarmsRecyclerView.scrollToPosition(result[1]);

		} else {
			viewModel.toggleAlarmState(alarmDatabase, alarmEntity.alarmHour, alarmEntity.alarmMinutes, 1);
			alarmAdapter = new AlarmAdapter(viewModel.getAlarmDataArrayList(), this, this);
			alarmsRecyclerView.swapAdapter(alarmAdapter, false);
			alarmID = alarmEntity.alarmID;
		}

		Intent intent = new Intent(Activity_AlarmsList.this, AlarmBroadcastReceiver.class);
		intent.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM);
		intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

		Bundle data = alarmEntity.getAlarmDetailsInABundle();
		data.putIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS, repeatDays);
		data.remove(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID);
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID, alarmID);
		intent.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, data);

		PendingIntent pendingIntent = PendingIntent
				.getBroadcast(Activity_AlarmsList.this, alarmID, intent, 0);

		ZonedDateTime zonedDateTime = ZonedDateTime.of(alarmDateTime.withSecond(0), ZoneId.systemDefault());

		alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(zonedDateTime.toEpochSecond() * 1000,
				pendingIntent), pendingIntent);

		ConstantsAndStatics.schedulePeriodicWork(this);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Delete and/or deactivate an alarm.
	 *
	 * @param mode Can have only two values: {@link #MODE_DEACTIVATE_ONLY} or {@link #MODE_DELETE_ALARM}.
	 * @param hour The alarm hour.
	 * @param mins The alarm minutes.
	 */
	private void deleteOrDeactivateAlarm(int mode, int hour, int mins) {

		ConstantsAndStatics.cancelScheduledPeriodicWork(this);

		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		Intent intent = new Intent(Activity_AlarmsList.this, AlarmBroadcastReceiver.class);
		intent.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM);
		intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

		int alarmID = viewModel.getAlarmId(alarmDatabase, hour, mins);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(Activity_AlarmsList.this, alarmID, intent,
				PendingIntent.FLAG_NO_CREATE);

		if (pendingIntent != null) {
			alarmManager.cancel(pendingIntent);
		}

		if (mode == MODE_DELETE_ALARM) {
			viewModel.removeAlarm(alarmDatabase, hour, mins);
			alarmAdapter = new AlarmAdapter(viewModel.getAlarmDataArrayList(), this, this);
			alarmsRecyclerView.swapAdapter(alarmAdapter, false);
		} else {
			viewModel.toggleAlarmState(alarmDatabase, hour, mins, 0);
		}

		// Kill any foreground service based on this alarm:
		ConstantsAndStatics.killServices(this, alarmID);

		ConstantsAndStatics.schedulePeriodicWork(this);
	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Toggle the alarm state. Updates the database, and cancels/places an alarm via {@link AlarmManager}.
	 *
	 * @param hour The hour of the alarm
	 * @param mins The minute of the alarm.
	 * @param newAlarmState The new state of the alarm. {@code 0} means OFF and {@code 1} means ON.
	 */
	private void toggleAlarmState(int hour, int mins, final int newAlarmState) {

		if (newAlarmState == 0) {
			deleteOrDeactivateAlarm(MODE_DEACTIVATE_ONLY, hour, mins);
		} else {
			addOrActivateAlarm(MODE_ACTIVATE_EXISTING_ALARM, viewModel.getAlarmEntity(alarmDatabase, hour,
					mins), viewModel.getRepeatDays(alarmDatabase, hour, mins));
		}

	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (requestCode == NEW_ALARM_REQUEST_CODE) {

			if (resultCode == RESULT_OK) {

				if (intent != null) {

					Bundle data = Objects.requireNonNull(intent.getExtras())
							.getBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS);
					assert data != null;

					AlarmEntity alarmEntity = new AlarmEntity(data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE),
							true,
							data.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_SNOOZE_ON),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_TIME_IN_MINS),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_FREQUENCY),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_VOLUME),
							data.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_REPEAT_ON),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_DAY),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MONTH),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_YEAR),
							data.getParcelable(ConstantsAndStatics.BUNDLE_KEY_ALARM_TONE_URI),
							data.getBoolean(ConstantsAndStatics.BUNDLE_KEY_HAS_USER_CHOSEN_DATE));

					addOrActivateAlarm(MODE_ADD_NEW_ALARM, alarmEntity,
							data.getIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS));
				}
			}
		} else if (requestCode == EXISTING_ALARM_REQUEST_CODE) {

			if (resultCode == RESULT_OK) {
				if (intent != null) {
					Bundle data = Objects.requireNonNull(intent.getExtras())
							.getBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS);
					assert data != null;

					deleteOrDeactivateAlarm(MODE_DELETE_ALARM,
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_OLD_ALARM_HOUR),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_OLD_ALARM_MINUTE));

					AlarmEntity alarmEntity = new AlarmEntity(
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE),
							true,
							data.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_SNOOZE_ON),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_TIME_IN_MINS),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_FREQUENCY),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_VOLUME),
							data.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_REPEAT_ON),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_DAY),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MONTH),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_YEAR),
							data.getParcelable(ConstantsAndStatics.BUNDLE_KEY_ALARM_TONE_URI),
							data.getBoolean(ConstantsAndStatics.BUNDLE_KEY_HAS_USER_CHOSEN_DATE));

					addOrActivateAlarm(MODE_ADD_NEW_ALARM, alarmEntity,
							data.getIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS));

				}
			}
		}
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public void onOnOffButtonClick(int rowNumber, int hour, int mins, int currentAlarmState) {
		toggleAlarmState(hour, mins, currentAlarmState);
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public void onDeleteButtonClicked(int rowNumber, int hour, int mins) {
		deleteOrDeactivateAlarm(MODE_DELETE_ALARM, hour, mins);
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public void onItemClicked(int rowNumber, int hour, int mins) {
		Intent intent = new Intent(this, Activity_AlarmDetails.class);
		intent.setAction(ConstantsAndStatics.ACTION_EXISTING_ALARM);

		final String KEY_START_ACTIVITY = "startTheActivity";

		Handler handler = new Handler(Looper.getMainLooper()) {
			@Override
			public void handleMessage(@NonNull Message msg) {
				Bundle data = msg.getData();
				if (data.getBoolean(KEY_START_ACTIVITY)) {

					Bundle bundle = data.getBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS);
					assert bundle != null;
					bundle.putIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS,
							data.getIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS));

					intent.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, bundle);
					startActivityForResult(intent, EXISTING_ALARM_REQUEST_CODE);
				}
			}
		};

		Thread thread = new Thread(() -> {
			Looper.prepare();

			Bundle bundle = new Bundle();

			List<AlarmEntity> list = alarmDatabase.alarmDAO().getAlarmDetails(hour, mins);
			for (AlarmEntity entity : list) {
				bundle.putBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, entity.getAlarmDetailsInABundle());
				bundle.putIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS,
						new ArrayList<>(alarmDatabase.alarmDAO().getAlarmRepeatDays(entity.alarmID)));
			}
			bundle.putBoolean(KEY_START_ACTIVITY, true);

			Message message = Message.obtain();
			message.setData(bundle);
			handler.sendMessageAtFrontOfQueue(message);
		});
		thread.start();

	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public void onDialogPositiveClick(DialogFragment dialogFragment) {
		if (dialogFragment.getClass().equals(AlertDialog_PermissionReason.class)) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST);
		}
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public void onDialogNegativeClick(DialogFragment dialogFragment) {
		if (dialogFragment.getClass().equals(AlertDialog_PermissionReason.class)) {
			Toast.makeText(this, "Alarm may not ring without permission to read storage.",
					Toast.LENGTH_LONG).show();
		}
	}

	//--------------------------------------------------------------------------------------------------

	private boolean isInstalledOnInternalStorage() {
		ApplicationInfo io = getApplicationInfo();
		return ! io.sourceDir.startsWith("/mnt/") &&
				! io.sourceDir.startsWith(Environment.getExternalStorageDirectory().getPath());
	}

	//--------------------------------------------------------------------------------------------------

	private void checkForPlugin() {
		if (! isInstalledOnInternalStorage()) {
			try {
				getPackageManager().getApplicationInfo("in.basulabs.shakealarmclockplugin", 0);
				initialisePlugin();
				// No exception thrown; plugin is available.
			} catch (PackageManager.NameNotFoundException e) {
				// Exception thrown; plugin is not available.
				Intent intent = new Intent(this, Activity_Plugin.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(intent);
				finish();
			}
		}
	}

	//----------------------------------------------------------------------------------------------------

	private void initialisePlugin() {
		Intent intent = new Intent();
		intent.setAction("in.basulabs.shakealarmclock.PLUGIN_ACTIVITY");
		startActivity(intent);
	}

}