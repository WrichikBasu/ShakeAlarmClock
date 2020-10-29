package in.basulabs.shakealarmclock;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Activity_AlarmsList extends AppCompatActivity implements AlarmAdapter.AdapterInterface {

	private AlarmAdapter alarmAdapter;
	private RecyclerView alarmsRecyclerView;
	private AlarmDatabase alarmDatabase;
	private ViewModel_AlarmsList viewModel;
	private ViewStub viewStub;

	/**
	 * Request code: Used when {@link Activity_AlarmDetails} is created for adding a new alarm.
	 */
	private static final int NEW_ALARM_REQUEST_CODE = 2564;

	/**
	 * Request code: Used when {@link Activity_AlarmDetails} is created for displaying the details of an existing alarm.
	 */
	private static final int EXISTING_ALARM_REQUEST_CODE = 3198;

	private static final int MODE_ADD_NEW_ALARM = 103;
	private static final int MODE_ACTIVATE_EXISTING_ALARM = 604;

	private static final int MODE_DELETE_ALARM = 504;
	private static final int MODE_DEACTIVATE_ONLY = 509;

	@SuppressLint("StaticFieldLeak")
	private static Activity_AlarmsList myInstance;

	//--------------------------------------------------------------------------------------------------

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alarmslist);
		setSupportActionBar(findViewById(R.id.toolbar));

		Log.e(this.getClass().getSimpleName(), "Inside onCreate()");

		alarmDatabase = AlarmDatabase.getInstance(this);
		viewModel = new ViewModelProvider(this).get(ViewModel_AlarmsList.class);

		SharedPreferences sharedPreferences = getSharedPreferences(ConstantsAndStatics.SHARED_PREF_FILE_NAME,
				MODE_PRIVATE);

		viewModel.init(alarmDatabase);

		int defaultTheme = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ConstantsAndStatics.THEME_SYSTEM : ConstantsAndStatics.THEME_AUTO_TIME;

		if (savedInstanceState == null) {
			AppCompatDelegate.setDefaultNightMode(ConstantsAndStatics
					.getTheme(sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_THEME, defaultTheme)));
		}

		myInstance = this;

		Button addAlarmButton = findViewById(R.id.addAlarmButton);
		addAlarmButton.setOnClickListener(view -> {
			Intent intent = new Intent(this, Activity_AlarmDetails.class);
			intent.setAction(ConstantsAndStatics.ACTION_NEW_ALARM);
			startActivityForResult(intent, NEW_ALARM_REQUEST_CODE);
		});

		alarmsRecyclerView = findViewById(R.id.alarmsRecyclerView);
		alarmsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

		viewStub = findViewById(R.id.viewStub);

		initAdapter();

		manageViewStub(viewModel.getAlarmsCount(alarmDatabase));

		viewModel.getLiveAlarmsCount().observe(this, this::manageViewStub);

		boolean showAppUpdate = true;

		if (getIntent().getAction() != null) {

			if (getIntent().getAction().equals(ConstantsAndStatics.ACTION_NEW_ALARM_FROM_INTENT)) {

				Log.e(this.getClass().getSimpleName(), "Received intent.");

				showAppUpdate = false;

				Intent intent = new Intent(this, Activity_AlarmDetails.class);
				intent.setAction(ConstantsAndStatics.ACTION_NEW_ALARM_FROM_INTENT);

				if (getIntent().getExtras() != null) {
					Log.e(this.getClass().getSimpleName(), "Extras received too!");
					intent.putExtras(getIntent().getExtras());
				}

				startActivityForResult(intent, NEW_ALARM_REQUEST_CODE);

			}
		}

		if (savedInstanceState == null && showAppUpdate) {
			checkForUpdates();
		}
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
	private void addOrActivateAlarm(int mode, AlarmEntity alarmEntity, @Nullable ArrayList<Integer> repeatDays) {

		ConstantsAndStatics.cancelScheduledPeriodicWork(this);

		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		if (repeatDays != null) {
			Collections.sort(repeatDays);
		}

		LocalDateTime alarmDateTime = ConstantsAndStatics.getAlarmDateTime(LocalDate.of(alarmEntity.alarmYear,
				alarmEntity.alarmMonth, alarmEntity.alarmDay), LocalTime.of(alarmEntity.alarmHour,
				alarmEntity.alarmMinutes), alarmEntity.isRepeatOn, repeatDays);

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

		PendingIntent pendingIntent = PendingIntent.getBroadcast(Activity_AlarmsList.this, alarmID, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);

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
			addOrActivateAlarm(MODE_ACTIVATE_EXISTING_ALARM, viewModel.getAlarmEntity(alarmDatabase, hour, mins),
					viewModel.getRepeatDays(alarmDatabase, hour, mins));
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

	//-------------------------------------------------------------------------------------------------------------

	private void checkForUpdates() {
		Context context = this;

		new AppUpdater(this)
				.setUpdateFrom(UpdateFrom.GOOGLE_PLAY)
				.setDisplay(Display.DIALOG)
				.setCancelable(true)
				.setButtonDoNotShowAgain(null)
				.setButtonUpdateClickListener((dialog, which) -> {
					dialog.cancel();

					try {
						context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=in.basulabs" +
								".shakealarmclock")));
					} catch (android.content.ActivityNotFoundException exception) {
						context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google" +
								".com/store/apps/details?id=in.basulabs.shakealarmclock")));
					}
				})
				.start();
	}

}