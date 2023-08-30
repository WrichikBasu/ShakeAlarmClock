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
package in.basulabs.shakealarmclock.frontend;

import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.ACTION_EXISTING_ALARM;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.ACTION_NEW_ALARM;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.ACTION_NEW_ALARM_FROM_INTENT;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.ALARM_TYPE_SOUND_ONLY;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_DAY;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_MONTH;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_TONE_URI;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_VOLUME;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_ALARM_YEAR;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_HAS_USER_CHOSEN_DATE;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_IS_REPEAT_ON;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_IS_SNOOZE_ON;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_OLD_ALARM_HOUR;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_OLD_ALARM_MINUTE;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_SNOOZE_FREQUENCY;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.BUNDLE_KEY_SNOOZE_TIME_IN_MINS;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.SHARED_PREF_FILE_NAME;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_VOLUME;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL;
import static in.basulabs.shakealarmclock.backend.ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Objects;

import in.basulabs.shakealarmclock.R;
import in.basulabs.shakealarmclock.backend.ConstantsAndStatics;

public class Activity_AlarmDetails extends AppCompatActivity implements
	Fragment_AlarmDetails_Main.FragmentGUIListener,
	AlertDialog_DiscardChanges.DialogListener {

	private FragmentManager fragmentManager;
	private ActionBar actionBar;
	private ViewModel_AlarmDetails viewModel;
	private SharedPreferences sharedPreferences;

	private static final String BACK_STACK_TAG = "activityAlarmDetails_fragment_stack";

	private static final int FRAGMENT_MAIN = 100;
	private static final int FRAGMENT_SNOOZE = 103;
	private static final int FRAGMENT_REPEAT = 110;
	private static final int FRAGMENT_PICK_DATE = 203;
	private static final int FRAGMENT_ALARM_MESSAGE = 401;

	private static int whichFragment = 0;

	public static final int MODE_NEW_ALARM = 0, MODE_EXISTING_ALARM = 1;

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
		sharedPreferences = getSharedPreferences(SHARED_PREF_FILE_NAME, MODE_PRIVATE);
		viewModel = new ViewModelProvider(this).get(ViewModel_AlarmDetails.class);

		if (savedInstanceState == null) {

			if (Objects.equals(getIntent().getAction(), ACTION_NEW_ALARM)) {

				setVariablesInViewModel();

				fragmentManager.beginTransaction()
					.replace(R.id.addAlarmActFragHolder,
						new Fragment_AlarmDetails_Main())
					.addToBackStack(BACK_STACK_TAG)
					.commit();

			} else if (Objects.requireNonNull(getIntent().getAction())
				.equals(ACTION_EXISTING_ALARM)) {

				Bundle data = Objects.requireNonNull(getIntent().getExtras())
					.getBundle(BUNDLE_KEY_ALARM_DETAILS);

				assert data != null;

				setVariablesInViewModel(MODE_EXISTING_ALARM,
					data.getInt(BUNDLE_KEY_ALARM_HOUR),
					data.getInt(BUNDLE_KEY_ALARM_MINUTE),
					data.getInt(BUNDLE_KEY_ALARM_DAY),
					data.getInt(BUNDLE_KEY_ALARM_MONTH),
					data.getInt(BUNDLE_KEY_ALARM_YEAR),
					data.getBoolean(BUNDLE_KEY_IS_SNOOZE_ON),
					data.getBoolean(BUNDLE_KEY_IS_REPEAT_ON),
					data.getInt(BUNDLE_KEY_SNOOZE_FREQUENCY),
					data.getInt(BUNDLE_KEY_SNOOZE_TIME_IN_MINS),
					data.getInt(BUNDLE_KEY_ALARM_TYPE),
					data.getInt(BUNDLE_KEY_ALARM_VOLUME),
					data.getIntegerArrayList(BUNDLE_KEY_REPEAT_DAYS),
					data.getString(ConstantsAndStatics.BUNDLE_KEY_ALARM_MESSAGE),
					Objects.requireNonNull(data.getParcelable(BUNDLE_KEY_ALARM_TONE_URI)),
					data.getBoolean(BUNDLE_KEY_HAS_USER_CHOSEN_DATE));

				fragmentManager.beginTransaction()
					.replace(R.id.addAlarmActFragHolder,
						new Fragment_AlarmDetails_Main())
					.addToBackStack(BACK_STACK_TAG)
					.commit();

			} else if (getIntent().getAction().equals(ACTION_NEW_ALARM_FROM_INTENT)) {

				Bundle data = getIntent().getExtras();

				if (data == null) {

					setVariablesInViewModel();

				} else {

					setVariablesInViewModel(MODE_NEW_ALARM,
						data.getInt(BUNDLE_KEY_ALARM_HOUR),
						data.getInt(BUNDLE_KEY_ALARM_MINUTE),
						data.getInt(BUNDLE_KEY_ALARM_DAY),
						data.getInt(BUNDLE_KEY_ALARM_MONTH),
						data.getInt(BUNDLE_KEY_ALARM_YEAR),
						sharedPreferences.getBoolean(SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON,
							true),
						data.getBoolean(BUNDLE_KEY_IS_REPEAT_ON),
						sharedPreferences.getInt(SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ, 3),
						sharedPreferences.getInt(SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL,
							5),
						data.getInt(BUNDLE_KEY_ALARM_TYPE),
						data.getInt(BUNDLE_KEY_ALARM_VOLUME),
						data.getIntegerArrayList(BUNDLE_KEY_REPEAT_DAYS),
						data.getString(ConstantsAndStatics.BUNDLE_KEY_ALARM_MESSAGE,
							null),
						Objects.requireNonNull(
							data.getParcelable(BUNDLE_KEY_ALARM_TONE_URI)), false);

				}

				fragmentManager.beginTransaction()
					.replace(R.id.addAlarmActFragHolder,
						new Fragment_AlarmDetails_Main())
					.addToBackStack(BACK_STACK_TAG)
					.commit();
			}

			fragmentManager.executePendingTransactions();
			whichFragment = FRAGMENT_MAIN;
		}

		setActionBarTitle();

	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Initialises all the variables in {@link #viewModel} to default values.
	 */
	private void setVariablesInViewModel() {
		viewModel.setMode(MODE_NEW_ALARM);

		viewModel.setAlarmDateTime(LocalDateTime.now().plusHours(1));

		viewModel.setIsSnoozeOn(
			sharedPreferences.getBoolean(SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON, true));
		viewModel.setIsRepeatOn(false);

		String alarmTone = sharedPreferences.getString(
			SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI, null);

		viewModel.setAlarmToneUri(alarmTone != null
			? Uri.parse(alarmTone)
			: Settings.System.DEFAULT_ALARM_ALERT_URI);

		viewModel.setAlarmType(ALARM_TYPE_SOUND_ONLY);

		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		viewModel.setAlarmVolume(
			sharedPreferences.getInt(SHARED_PREF_KEY_DEFAULT_ALARM_VOLUME,
				audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM) - 2));

		viewModel.setSnoozeFreq(
			sharedPreferences.getInt(SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ, 3));
		viewModel.setSnoozeIntervalInMins(
			sharedPreferences.getInt(SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL, 5));

		viewModel.setRepeatDays(null);

		viewModel.setIsChosenDateToday(
			viewModel.getAlarmDateTime().toLocalDate().equals(LocalDate.now()));

		if (viewModel.getIsChosenDateToday()) {
			viewModel.setMinDate(viewModel.getAlarmDateTime().toLocalDate());
		} else {
			if (!viewModel.getAlarmDateTime().toLocalTime().isAfter(LocalTime.now())) {
				viewModel.setMinDate(LocalDate.now().plusDays(1));
			} else {
				viewModel.setMinDate(LocalDate.now());
			}
		}

		viewModel.setAlarmMessage(null);

		viewModel.setHasUserChosenDate(false);
	}

	//----------------------------------------------------------------------------------------------------

	private void setVariablesInViewModel(int mode, int alarmHour, int alarmMinute,
		int dayOfMonth, int month, int year, boolean isSnoozeOn,
		boolean isRepeatOn, int snoozeFreq, int snoozeIntervalInMins, int alarmType,
		int alarmVolume,
		@Nullable ArrayList<Integer> repeatDays, @Nullable String alarmMessage,
		@NonNull Uri alarmToneUri, boolean hasUserChosenDate) {

		viewModel.setMode(mode);

		viewModel.setAlarmDateTime(
			LocalDateTime.of(year, month, dayOfMonth, alarmHour, alarmMinute));

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
				sharedPreferences.getInt(SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ, 3));
			viewModel.setSnoozeIntervalInMins(
				sharedPreferences.getInt(SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL, 5));
		}

		if (isRepeatOn && repeatDays != null) {
			viewModel.setRepeatDays(repeatDays);
		} else {
			viewModel.setRepeatDays(null);
		}

		viewModel.setIsChosenDateToday(
			viewModel.getAlarmDateTime().toLocalDate().equals(LocalDate.now()));

		if (viewModel.getIsChosenDateToday()) {
			viewModel.setMinDate(viewModel.getAlarmDateTime().toLocalDate());
		} else {
			if (!viewModel.getAlarmDateTime().toLocalTime().isAfter(LocalTime.now())) {
				viewModel.setMinDate(LocalDate.now().plusDays(1));
			} else {
				viewModel.setMinDate(LocalDate.now());
			}
		}

		viewModel.setAlarmMessage(alarmMessage);

		viewModel.setHasUserChosenDate(hasUserChosenDate);

		if (mode == MODE_EXISTING_ALARM) {
			viewModel.setOldAlarmHour(alarmHour);
			viewModel.setOldAlarmMinute(alarmMinute);
		}

	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Sets the ActionBar title as per the created fragment. Uses {@link #whichFragment}
	 * to determine the current fragment.
	 */
	private void setActionBarTitle() {
		switch (whichFragment) {
			case FRAGMENT_MAIN -> {
				if (viewModel.getMode() == MODE_NEW_ALARM) {
					actionBar.setTitle(R.string.actionBarTitle_newAlarm);
				} else if (viewModel.getMode() == MODE_EXISTING_ALARM) {
					actionBar.setTitle(R.string.actionBarTitle_editAlarm);
				}
			}
			case FRAGMENT_SNOOZE ->
				actionBar.setTitle(R.string.actionBarTitle_snoozeOptions);
			case FRAGMENT_REPEAT ->
				actionBar.setTitle(R.string.actionBarTitle_repeatOptions);
			case FRAGMENT_PICK_DATE ->
				actionBar.setTitle(R.string.actionBarTitle_dateOptions);
			case FRAGMENT_ALARM_MESSAGE ->
				actionBar.setTitle(R.string.actionBarTitle_alarmMessage);
		}
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {

		if (fragmentManager.getBackStackEntryCount() > 1) {
			fragmentManager.popBackStackImmediate();
			whichFragment = FRAGMENT_MAIN;
			setActionBarTitle();
		} else {
			onCancelButtonClick();
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
		data.putInt(BUNDLE_KEY_ALARM_HOUR, viewModel.getAlarmDateTime().getHour());
		data.putInt(BUNDLE_KEY_ALARM_MINUTE, viewModel.getAlarmDateTime().getMinute());
		data.putInt(BUNDLE_KEY_ALARM_DAY, viewModel.getAlarmDateTime().getDayOfMonth());
		data.putInt(BUNDLE_KEY_ALARM_MONTH,
			viewModel.getAlarmDateTime().getMonthValue());
		data.putInt(BUNDLE_KEY_ALARM_YEAR, viewModel.getAlarmDateTime().getYear());
		data.putInt(BUNDLE_KEY_ALARM_TYPE, viewModel.getAlarmType());
		data.putBoolean(BUNDLE_KEY_IS_SNOOZE_ON, viewModel.getIsSnoozeOn());
		data.putBoolean(BUNDLE_KEY_IS_REPEAT_ON, viewModel.getIsRepeatOn());
		data.putInt(BUNDLE_KEY_ALARM_VOLUME, viewModel.getAlarmVolume());
		data.putInt(BUNDLE_KEY_SNOOZE_TIME_IN_MINS, viewModel.getSnoozeIntervalInMins());
		data.putInt(BUNDLE_KEY_SNOOZE_FREQUENCY, viewModel.getSnoozeFreq());
		data.putIntegerArrayList(BUNDLE_KEY_REPEAT_DAYS, viewModel.getRepeatDays());
		data.putParcelable(BUNDLE_KEY_ALARM_TONE_URI, viewModel.getAlarmToneUri());
		data.putString(ConstantsAndStatics.BUNDLE_KEY_ALARM_MESSAGE,
			viewModel.getAlarmMessage());

		if (viewModel.getIsRepeatOn()) {
			data.putBoolean(BUNDLE_KEY_HAS_USER_CHOSEN_DATE, false);
		} else {
			data.putBoolean(BUNDLE_KEY_HAS_USER_CHOSEN_DATE,
				viewModel.getHasUserChosenDate());
		}

		if (viewModel.getMode() == MODE_EXISTING_ALARM) {
			data.putInt(BUNDLE_KEY_OLD_ALARM_HOUR, viewModel.getOldAlarmHour());
			data.putInt(BUNDLE_KEY_OLD_ALARM_MINUTE, viewModel.getOldAlarmMinute());
		}

		Intent intent = new Intent().putExtra(BUNDLE_KEY_ALARM_DETAILS, data);
		setResult(RESULT_OK, intent);
		this.finish();
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onRequestSnoozeFragCreation() {
		whichFragment = FRAGMENT_SNOOZE;
		FragmentTransaction fragmentTransaction =
			fragmentManager.beginTransaction()
				.replace(R.id.addAlarmActFragHolder,
					new Fragment_AlarmDetails_SnoozeOptions())
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
	public void onRequestMessageFragCreation() {
		fragmentManager.beginTransaction()
			.replace(R.id.addAlarmActFragHolder, new Fragment_AlarmDetails_Message())
			.addToBackStack(BACK_STACK_TAG)
			.commit();
		fragmentManager.executePendingTransactions();
		whichFragment = FRAGMENT_ALARM_MESSAGE;
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
