package in.basulabs.shakealarmclock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Objects;

public class Activity_AlarmDetails extends AppCompatActivity
		implements Fragment_AlarmDetails_Main.FragmentGUIListener, AlertDialog_DiscardChanges.DialogListener {

	private FragmentManager fragmentManager;
	private ActionBar actionBar;

	private static final String BACK_STACK_TAG = "activityAlarmDetails_fragment_stack";

	private static final int FRAGMENT_MAIN = 100, FRAGMENT_SNOOZE = 103, FRAGMENT_REPEAT = 110,
			FRAGMENT_PICK_DATE = 203;
	private static int whichFragment = 0;

	public static final int MODE_NEW_ALARM = 0, MODE_EXISTING_ALARM = 1;

	private ViewModel_AlarmDetails viewModel;

	private SharedPreferences sharedPreferences;

	//----------------------------------------------------------------------------------------------------

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_addalarm);
		setSupportActionBar(findViewById(R.id.toolbar2));

		actionBar = getSupportActionBar();
		assert actionBar != null;
		actionBar.setDisplayHomeAsUpEnabled(true);

		fragmentManager = getSupportFragmentManager();

		sharedPreferences = getSharedPreferences(ConstantsAndStatics.SHARED_PREF_FILE_NAME, MODE_PRIVATE);

		viewModel = new ViewModelProvider(this).get(ViewModel_AlarmDetails.class);

		int defaultTheme;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			defaultTheme = ConstantsAndStatics.THEME_SYSTEM;
		} else {
			defaultTheme = ConstantsAndStatics.THEME_TIME;
		}
		AppCompatDelegate.setDefaultNightMode(
				ConstantsAndStatics.getTheme(sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_THEME, defaultTheme)));

		if (savedInstanceState == null) {

			if (Objects.equals(getIntent().getAction(), ConstantsAndStatics.ACTION_NEW_ALARM)) {

				setVariablesInViewModel();

				fragmentManager.beginTransaction()
						.replace(R.id.addAlarmActFragHolder, new Fragment_AlarmDetails_Main())
						.addToBackStack(BACK_STACK_TAG)
						.commit();

			} else if (getIntent().getAction().equals(ConstantsAndStatics.ACTION_EXISTING_ALARM)) {

				Bundle data = Objects.requireNonNull(getIntent().getExtras())
						.getBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS);

				assert data != null;

				setVariablesInViewModel(data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
						data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE),
						data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_DAY),
						data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MONTH),
						data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_YEAR),
						data.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_SNOOZE_ON),
						data.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_REPEAT_ON),
						data.getInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_FREQUENCY),
						data.getInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_TIME_IN_MINS),
						data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE),
						data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_VOLUME),
						data.getIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS),
						Objects.requireNonNull(
								data.getParcelable(ConstantsAndStatics.BUNDLE_KEY_ALARM_TONE_URI)),
						data.getBoolean(ConstantsAndStatics.BUNDLE_KEY_HAS_USER_CHOSEN_DATE));

				fragmentManager.beginTransaction()
						.replace(R.id.addAlarmActFragHolder, new Fragment_AlarmDetails_Main())
						.addToBackStack(BACK_STACK_TAG)
						.commit();
			}

			fragmentManager.executePendingTransactions();
			whichFragment = FRAGMENT_MAIN;
		}

		setActionBarTitle();

		/*Log.e(this.getClass().getSimpleName(),
				"onCreate Back stack length = " + fragmentManager.getBackStackEntryCount());*/

	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Initialises all the variables in {@link #viewModel} to default values. Use this if the Fragment is to
	 * be created for a new alarm.
	 */
	private void setVariablesInViewModel() {
		viewModel.setMode(MODE_NEW_ALARM);

		viewModel.setAlarmDateTime(LocalDateTime.now().plusHours(1));

		viewModel.setIsSnoozeOn(sharedPreferences.getBoolean(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON,
				true));
		viewModel.setIsRepeatOn(false);

		viewModel.setAlarmToneUri(Uri.parse(sharedPreferences
				.getString(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI,
						RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
								.toString())));

		viewModel.setAlarmType(ConstantsAndStatics.ALARM_TYPE_SOUND_ONLY);

		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		viewModel.setAlarmVolume(sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_VOLUME,
				audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM) - 2));

		viewModel.setSnoozeFreq(sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ, 3));
		viewModel.setSnoozeIntervalInMins(
				sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL, 5));

		viewModel.setRepeatDays(new ArrayList<>());

		viewModel.setIsChosenDateToday(viewModel.getAlarmDateTime().toLocalDate().equals(LocalDate.now()));

		if (viewModel.getIsChosenDateToday()) {
			viewModel.setMinDate(viewModel.getAlarmDateTime().toLocalDate());
		} else {
			if (! viewModel.getAlarmDateTime().toLocalTime().isAfter(LocalTime.now())) {
				viewModel.setMinDate(LocalDate.now().plusDays(1));
			} else {
				viewModel.setMinDate(LocalDate.now());
			}
		}

		viewModel.setHasUserChosenDate(false);
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Initialises all the variables in {@link #viewModel} to the supplied parameters. Use this if an existing
	 * alarm is to be displayed.
	 *
	 * @param alarmHour The alarm hour.
	 * @param alarmMinute The alarm minutes.
	 * @param dayOfMonth The day of the month.
	 * @param month
	 * @param year
	 * @param isSnoozeOn
	 * @param isRepeatOn
	 * @param snoozeFreq
	 * @param snoozeIntervalInMins
	 * @param alarmType
	 * @param alarmVolume
	 * @param repeatDays
	 * @param alarmToneUri
	 * @param hasUserChosenDate
	 */
	private void setVariablesInViewModel(int alarmHour, int alarmMinute, int dayOfMonth, int month,
	                                     int year, boolean isSnoozeOn, boolean isRepeatOn, int snoozeFreq,
	                                     int snoozeIntervalInMins, int alarmType, int alarmVolume,
	                                     @Nullable ArrayList<Integer> repeatDays,
	                                     @NonNull Uri alarmToneUri, boolean hasUserChosenDate) {

		viewModel.setMode(MODE_EXISTING_ALARM);

		viewModel.setAlarmDateTime(LocalDateTime.of(year, month, dayOfMonth, alarmHour, alarmMinute));

		viewModel.setIsSnoozeOn(isSnoozeOn);
		viewModel.setIsRepeatOn(isRepeatOn);

		viewModel.setAlarmToneUri(alarmToneUri);

		viewModel.setAlarmType(alarmType);

		viewModel.setAlarmVolume(alarmVolume);

		if (isSnoozeOn) {
			viewModel.setSnoozeFreq(snoozeFreq);
			viewModel.setSnoozeIntervalInMins(snoozeIntervalInMins);
		} else {
			viewModel.setSnoozeFreq(
					sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ, 3));
			viewModel.setSnoozeIntervalInMins(
					sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL, 5));
		}

		if (isRepeatOn && repeatDays != null) {
			viewModel.setRepeatDays(repeatDays);
		} else {
			viewModel.setRepeatDays(new ArrayList<>());
		}

		viewModel.setIsChosenDateToday(viewModel.getAlarmDateTime().toLocalDate().equals(LocalDate.now()));

		if (viewModel.getIsChosenDateToday()) {
			viewModel.setMinDate(viewModel.getAlarmDateTime().toLocalDate());
		} else {
			if (! viewModel.getAlarmDateTime().toLocalTime().isAfter(LocalTime.now())) {
				viewModel.setMinDate(LocalDate.now().plusDays(1));
			} else {
				viewModel.setMinDate(LocalDate.now());
			}
		}

		viewModel.setHasUserChosenDate(hasUserChosenDate);

		viewModel.setOldAlarmHour(alarmHour);
		viewModel.setOldAlarmMinute(alarmMinute);

	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Sets the ActionBar title as per the created fragment. Uses {@link #whichFragment} to determine the
	 * current fragment.
	 */
	private void setActionBarTitle() {
		switch (whichFragment) {
			case FRAGMENT_MAIN:
				if (viewModel.getMode() == MODE_NEW_ALARM) {
					actionBar.setTitle(getResources().getString(R.string.actionBarTitle_newAlarm));
				} else if (viewModel.getMode() == MODE_EXISTING_ALARM) {
					actionBar.setTitle("Edit alarm");
				}
				break;
			case FRAGMENT_SNOOZE:
				actionBar.setTitle(getResources().getString(R.string.actionBarTitle_snoozeOptions));
				break;
			case FRAGMENT_REPEAT:
				actionBar.setTitle(getResources().getString(R.string.actionBarTitle_repeatOptions));
				break;
			case FRAGMENT_PICK_DATE:
				actionBar.setTitle(getResources().getString(R.string.actionBarTitle_dateOptions));
		}
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {
		//Log.e(this.getClass().getSimpleName(), "Back pressed.");
		/*Log.e(this.getClass().getSimpleName(),
				"onBackPressed Back stack length = " + fragmentManager.getBackStackEntryCount());*/

		if (fragmentManager.getBackStackEntryCount() > 1) {
			fragmentManager.popBackStackImmediate();
			whichFragment = FRAGMENT_MAIN;
			setActionBarTitle();
		} else {
			onCancelButtonClick();
			/*setResult(RESULT_CANCELED);
			this.finish();*/
			//NavUtils.navigateUpFromSameTask(this);
		}
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onSaveButtonClick() {

		Bundle data = new Bundle();
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR, viewModel.getAlarmDateTime().getHour());
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE, viewModel.getAlarmDateTime().getMinute());
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_DAY, viewModel.getAlarmDateTime().getDayOfMonth());
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MONTH, viewModel.getAlarmDateTime().getMonthValue());
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_YEAR, viewModel.getAlarmDateTime().getYear());
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE, viewModel.getAlarmType());
		data.putBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_SNOOZE_ON, viewModel.getIsSnoozeOn());
		data.putBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_REPEAT_ON, viewModel.getIsRepeatOn());
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_VOLUME, viewModel.getAlarmVolume());
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_TIME_IN_MINS, viewModel.getSnoozeIntervalInMins());
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_FREQUENCY, viewModel.getSnoozeFreq());
		data.putIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS, viewModel.getRepeatDays());
		data.putParcelable(ConstantsAndStatics.BUNDLE_KEY_ALARM_TONE_URI, viewModel.getAlarmToneUri());

		if (viewModel.getIsRepeatOn()) {
			data.putBoolean(ConstantsAndStatics.BUNDLE_KEY_HAS_USER_CHOSEN_DATE, false);
		} else {
			data.putBoolean(ConstantsAndStatics.BUNDLE_KEY_HAS_USER_CHOSEN_DATE, viewModel.getHasUserChosenDate());
		}

		if (viewModel.getMode() == MODE_EXISTING_ALARM) {
			data.putInt(ConstantsAndStatics.BUNDLE_KEY_OLD_ALARM_HOUR, viewModel.getOldAlarmHour());
			data.putInt(ConstantsAndStatics.BUNDLE_KEY_OLD_ALARM_MINUTE, viewModel.getOldAlarmMinute());
		}

		Intent intent = new Intent();
		intent.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, data);
		setResult(RESULT_OK, intent);
		this.finish();
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onRequestSnoozeFragCreation() {
		whichFragment = FRAGMENT_SNOOZE;
		FragmentTransaction fragmentTransaction =
				fragmentManager.beginTransaction()
						.replace(R.id.addAlarmActFragHolder, new Fragment_AlarmDetails_SnoozeOptions())
						.addToBackStack(BACK_STACK_TAG);
		fragmentTransaction.commit();
		fragmentManager.executePendingTransactions();
		setActionBarTitle();
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onRequestDatePickerFragCreation() {
		fragmentManager.beginTransaction()
				.replace(R.id.addAlarmActFragHolder, new Fragment_AlarmDetails_DatePicker())
				.addToBackStack(BACK_STACK_TAG)
				.commit();
		fragmentManager.executePendingTransactions();
		whichFragment = FRAGMENT_PICK_DATE;
		setActionBarTitle();
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onRequestRepeatFragCreation() {
		fragmentManager.beginTransaction()
				.replace(R.id.addAlarmActFragHolder,
						new Fragment_AlarmDetails_RepeatOptions())
				.addToBackStack(BACK_STACK_TAG)
				.commit();
		fragmentManager.executePendingTransactions();
		whichFragment = FRAGMENT_REPEAT;
		setActionBarTitle();
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onCancelButtonClick() {
		DialogFragment cancelDialog = new AlertDialog_DiscardChanges();
		cancelDialog.setCancelable(false);
		cancelDialog.show(fragmentManager, "");
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onDialogPositiveClick(DialogFragment dialogFragment) {
		if (dialogFragment.getClass() == AlertDialog_DiscardChanges.class) {
			setResult(RESULT_CANCELED);
			this.finish();
		}
	}
}
