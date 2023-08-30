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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import in.basulabs.shakealarmclock.R;
import in.basulabs.shakealarmclock.backend.ConstantsAndStatics;

public class Activity_Settings extends AppCompatActivity implements
	AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener,
	View.OnClickListener, SeekBar.OnSeekBarChangeListener, SensorEventListener,
	AlertDialog_TestShakeSensitivity.DialogListener {

	private SharedPreferences.Editor prefEditor;
	private SharedPreferences sharedPreferences;

	private ExpandableLayout snoozeOptionsExpandableLayout,
		shakeSensitivityExpandableLayout;
	private TextView snoozeIntvLabel;
	private TextView snoozeFreqLabel;
	private TextView toneTextView;
	private EditText snoozeIntvEditText, snoozeFreqEditText;
	private ImageView snoozeImageView, volumeImageView, shakeImageView;
	private SwitchCompat snoozeStateSwitch;
	private CheckBox autoSelectToneCheckBox;

	private static final String SAVE_INSTANCE_SNOOZE_LAYOUT_EXPANDED
		= "in.basulabs.shakealarmclock.frontend.Activity_Settings" +
		".SNOOZE_LAYOUT_EXPANDED",
		SAVE_INSTANCE_SHAKE_LAYOUT_EXPANDED
			= "in.basulabs.shakealarmclock.frontend.Activity_Settings" +
			".SHAKE_LAYOUT_EXPANDED",
		SAVE_INSTANCE_IS_SHAKE_ONGOING
			= "in.basulabs.shakealarmclock.frontend.Activity_Settings.IS_SHAKE_ONGOING";

	private int defaultTheme;

	private long lastShakeTime;

	private boolean isShakeTestGoingOn;

	static final int MINIMUM_MILLIS_BETWEEN_SHAKES = 400;

	private SensorManager sensorManager;
	private Sensor acclerometer;

	private ActivityResultLauncher<Intent> ringPickActLauncher;

	//-----------------------------------------------------------------------------------------------------

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		/////////////////////////////////////
		// Set the ActionBar:
		////////////////////////////////////
		setSupportActionBar(findViewById(R.id.toolbar5));
		Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(R.string.settings_title);

		//////////////////////////////////////////////////
		// Get SharedPreferences and its editor:
		/////////////////////////////////////////////////
		sharedPreferences = getSharedPreferences(
			ConstantsAndStatics.SHARED_PREF_FILE_NAME, MODE_PRIVATE);
		prefEditor = sharedPreferences.edit();

		/////////////////////////////////////////////////
		// Find default theme; set theme.
		////////////////////////////////////////////////
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			defaultTheme = ConstantsAndStatics.THEME_SYSTEM;
		} else {
			defaultTheme = ConstantsAndStatics.THEME_AUTO_TIME;
		}

		applyTheme();

		////////////////////////////////////////////////////
		// Initialise spinner for shake operation:
		///////////////////////////////////////////////////
		Spinner shakeOpSpinner = findViewById(R.id.shakeOperationSpinner);
		ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(this,
			R.array.shakeAndPowerOptions, android.R.layout.simple_spinner_item);
		arrayAdapter.setDropDownViewResource(
			android.R.layout.simple_spinner_dropdown_item);
		shakeOpSpinner.setAdapter(arrayAdapter);
		shakeOpSpinner.setSelection(sharedPreferences.getInt(
			ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SHAKE_OPERATION,
			ConstantsAndStatics.SNOOZE));
		shakeOpSpinner.setOnItemSelectedListener(this);

		/////////////////////////////////////////////////////////
		// Initialise spinner for power button operation:
		////////////////////////////////////////////////////////
		Spinner powerBtnOpSpinner = findViewById(R.id.powerBtnOpSpinner);
		ArrayAdapter<CharSequence> arrayAdapter2 = ArrayAdapter.createFromResource(this,
			R.array.shakeAndPowerOptions, android.R.layout.simple_spinner_item);
		arrayAdapter2.setDropDownViewResource(
			android.R.layout.simple_spinner_dropdown_item);
		powerBtnOpSpinner.setAdapter(arrayAdapter2);
		powerBtnOpSpinner.setSelection(sharedPreferences.getInt(
			ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_POWER_BTN_OPERATION,
			ConstantsAndStatics.DISMISS));
		powerBtnOpSpinner.setOnItemSelectedListener(this);

		////////////////////////////////////////////////////
		// Initialise spinner for theme:
		///////////////////////////////////////////////////
		Spinner themeSpinner = findViewById(R.id.themeSpinner);
		ArrayAdapter<CharSequence> arrayAdapter3;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			arrayAdapter3 = ArrayAdapter.createFromResource(this,
				R.array.themeOptions_all, android.R.layout.simple_spinner_item);
		} else {
			arrayAdapter3 = ArrayAdapter.createFromResource(this,
				R.array.themeOptions_noSystem, android.R.layout.simple_spinner_item);
		}
		arrayAdapter3.setDropDownViewResource(
			android.R.layout.simple_spinner_dropdown_item);
		themeSpinner.setAdapter(arrayAdapter3);
		themeSpinner.setSelection(
			sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_THEME,
				ConstantsAndStatics.THEME_SYSTEM));
		themeSpinner.setSelection(
			sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_THEME,
				defaultTheme));
		themeSpinner.setOnItemSelectedListener(this);

		///////////////////////////////////////////////////////////
		// Initialise expandable layout for snooze options:
		//////////////////////////////////////////////////////////
		snoozeOptionsExpandableLayout = findViewById(R.id.snoozeOptionsExpandableLayout);
		if (savedInstanceState == null) {
			snoozeOptionsExpandableLayout.setExpanded(false);
		} else {
			snoozeOptionsExpandableLayout.setExpanded(
				savedInstanceState.getBoolean(SAVE_INSTANCE_SNOOZE_LAYOUT_EXPANDED));
		}
		snoozeOptionsExpandableLayout.setOnExpansionUpdateListener(
			(expansionFraction, state) -> setSnoozeImageView(state));

		ConstraintLayout snoozeOptionsConstraintLayout = findViewById(
			R.id.settings_snoozeOptionsConstraintLayout);
		snoozeOptionsConstraintLayout.setOnClickListener(this);

		//////////////////////////////////////////////
		// Initialise snooze ON/OFF switch:
		/////////////////////////////////////////////
		snoozeStateSwitch = findViewById(R.id.settingsSnoozeOnOffSwitch);
		snoozeStateSwitch.setChecked(sharedPreferences.getBoolean(
			ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON, true));
		setSwitchText();
		snoozeStateSwitch.setOnCheckedChangeListener(this);

		snoozeIntvLabel = findViewById(R.id.settings_snoozeIntervalLabel);
		snoozeFreqLabel = findViewById(R.id.settings_snoozeFreqLabel);
		snoozeIntvEditText = findViewById(R.id.settings_snoozeIntervalEditText);
		snoozeFreqEditText = findViewById(R.id.settings_snoozeFreqEditText2);

		snoozeIntvEditText.setText(String.valueOf(sharedPreferences.getInt(
			ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL, 5)));
		snoozeFreqEditText.setText(String.valueOf(sharedPreferences.getInt(
			ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ, 3)));

		activateOrDeactivateSnoozeOptions(sharedPreferences.getBoolean(
			ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON, true));

		snoozeImageView = findViewById(R.id.snoozeExpColapImage);
		setSnoozeImageView(snoozeOptionsExpandableLayout.getState());

		snoozeFreqEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int start,
				int count,
				int after) {
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int start, int before,
				int count) {
				if (charSequence.length() != 0) {
					prefEditor.remove(
						ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ);
					prefEditor.putInt(
						ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ,
						Integer.parseInt("" + charSequence));
					prefEditor.commit();
				}
			}

			@Override
			public void afterTextChanged(Editable editable) {
			}
		});

		snoozeIntvEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int start,
				int count,
				int after) {
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int start, int before,
				int count) {
				if (charSequence.length() != 0) {
					prefEditor.remove(
						ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL);
					prefEditor.putInt(
						ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL,
						Integer.parseInt("" + charSequence));
					prefEditor.commit();
				}
			}

			@Override
			public void afterTextChanged(Editable editable) {
			}
		});

		snoozeIntvEditText.clearFocus();
		snoozeFreqEditText.clearFocus();

		///////////////////////////////////////////////
		// Initialise volume SeekBar:
		///////////////////////////////////////////////
		SeekBar volumeSeekbar = findViewById(R.id.settings_volumeSeekbar);
		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		volumeSeekbar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM));
		volumeSeekbar.setProgress(sharedPreferences.getInt(
			ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_VOLUME,
			audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM) - 2));
		volumeSeekbar.setOnSeekBarChangeListener(this);

		////////////////////////////////////////////////
		// Initialise volume ImageView:
		///////////////////////////////////////////////
		volumeImageView = findViewById(R.id.settings_volumeImageView);
		setVolumeImageView(volumeSeekbar.getProgress());

		//////////////////////////////////////////////////////////////////
		// Initialise tone ConstraintLayout and TextView:
		/////////////////////////////////////////////////////////////////
		ConstraintLayout toneConstraintLayout = findViewById(
			R.id.settings_toneConstraintLayout);
		toneConstraintLayout.setOnClickListener(this);

		toneTextView = findViewById(R.id.settings_toneTextView);
		setToneTextView(getCurrentToneUri());

		///////////////////////////////////////////
		// Auto select tone enable/disable:
		//////////////////////////////////////////
		autoSelectToneCheckBox = findViewById(R.id.autoToneSelectionCheckBox);
		autoSelectToneCheckBox.setChecked(sharedPreferences.getBoolean(
			ConstantsAndStatics.SHARED_PREF_KEY_AUTO_SET_TONE, true));
		autoSelectToneCheckBox.setOnCheckedChangeListener(this);

		////////////////////////////////////////////////////////
		// Initialise shakeSencitivityExpandableLayout:
		///////////////////////////////////////////////////////
		shakeSensitivityExpandableLayout = findViewById(
			R.id.shakeSensitivityExpandableLayout);
		if (savedInstanceState == null) {
			shakeSensitivityExpandableLayout.setExpanded(false, false);
		} else {
			shakeSensitivityExpandableLayout.setExpanded(
				savedInstanceState.getBoolean(SAVE_INSTANCE_SHAKE_LAYOUT_EXPANDED));
		}
		shakeSensitivityExpandableLayout.setOnExpansionUpdateListener(
			(expansionFraction, state) -> setShakeImageView(state));

		////////////////////////////////////////////////////////
		// Initialise shakeSensitivityConstraintLayout:
		///////////////////////////////////////////////////////
		ConstraintLayout shakeOptionsConstraintLayout = findViewById(
			R.id.shakeSensitivityConstarintLayout);
		shakeOptionsConstraintLayout.setOnClickListener(this);

		////////////////////////////////////////////////////////////
		// Initialize ActivityResultLauncher for RingtonePicker:
		///////////////////////////////////////////////////////////
		initializeActLauncher();

		////////////////////////////////////////////////////////
		// Initialise shakeImageView:
		///////////////////////////////////////////////////////
		shakeImageView = findViewById(R.id.shakeExpColImage);
		setShakeImageView(shakeSensitivityExpandableLayout.getState());

		////////////////////////////////////////////////////////
		// Initialise sensitivitySeekBar:
		///////////////////////////////////////////////////////
		SeekBar shakeSensitivitySeekBar = findViewById(R.id.shakeSensitivitySeekBar);

		double min = 1.5, max = 6.5, stepSize = 0.2;
		int steps = (int) ((max - min) / stepSize);

		shakeSensitivitySeekBar.setMax(steps);
		shakeSensitivitySeekBar.setProgress(getStepValue(sharedPreferences.getFloat(
			ConstantsAndStatics.SHARED_PREF_KEY_SHAKE_SENSITIVITY,
			ConstantsAndStatics.DEFAULT_SHAKE_SENSITIVITY)));
		shakeSensitivitySeekBar.setOnSeekBarChangeListener(this);

		TextView testShakeTextView = findViewById(R.id.testShakeSenstivityTextView);
		testShakeTextView.setOnClickListener(this);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		acclerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		if (savedInstanceState == null) {
			isShakeTestGoingOn = false;
		} else {
			isShakeTestGoingOn = savedInstanceState.getBoolean(
				SAVE_INSTANCE_IS_SHAKE_ONGOING);
			lastShakeTime = System.currentTimeMillis();
		}

		// Set app version
		TextView appVersionTextView = findViewById(R.id.appVersionTextView);
		try {
			String versionName =
				getPackageManager().getPackageInfo(getPackageName(),0).versionName;
			appVersionTextView.setText(versionName);
			appVersionTextView.setVisibility(View.VISIBLE);
		} catch (PackageManager.NameNotFoundException e) {
			appVersionTextView.setVisibility(View.GONE);
		}


	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();

		if (isShakeTestGoingOn) {
			sensorManager.registerListener(this, acclerometer,
				SensorManager.SENSOR_DELAY_UI, new Handler());
		}
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this, acclerometer);
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putBoolean(SAVE_INSTANCE_SNOOZE_LAYOUT_EXPANDED,
			snoozeOptionsExpandableLayout.isExpanded());
		outState.putBoolean(SAVE_INSTANCE_SHAKE_LAYOUT_EXPANDED,
			shakeSensitivityExpandableLayout.isExpanded());
		outState.putBoolean(SAVE_INSTANCE_IS_SHAKE_ONGOING, isShakeTestGoingOn);
	}


	//-----------------------------------------------------------------------------------------------------

	/**
	 * Finds whether a file exists or not.
	 *
	 * @param uri The Uri of the file.
	 * @return {@code true} if the file exists, otherwise {@code false}.
	 */
	private boolean doesFileExist(Uri uri) {

		try {
			try (
				Cursor cursor = getContentResolver().query(uri, null, null, null,
					null)) {
				return cursor != null;
			}
		} catch (java.lang.SecurityException exception) {
			return false;
		}
	}

	//-----------------------------------------------------------------------------------------------------

	/**
	 * Displays the name of the currently selected default alarm tone.
	 *
	 * @param uri The Uri of the tone.
	 */
	private void setToneTextView(@NonNull Uri uri) {

		if (uri.equals(Settings.System.DEFAULT_ALARM_ALERT_URI)) {
			toneTextView.setText(R.string.defaultAlarmToneText);
		} else {

			String fileName = null;

			try (
				Cursor cursor = getContentResolver().query(uri, null, null, null,
					null)) {

				try {
					if (cursor != null && cursor.moveToFirst()) {

						int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
						if (index != -1) {
							fileName = cursor.getString(index);
						} else {
							fileName = cursor.getString(
								RingtoneManager.TITLE_COLUMN_INDEX);
						}
					}
				} catch (Exception ignored) {
				}
			}

			if (fileName != null) {
				toneTextView.setText(fileName);
			} else {
				toneTextView.setText(uri.getLastPathSegment());
			}
		}
	}

	//-----------------------------------------------------------------------------------------------------

	/**
	 * Get the current default alarm tone Uri from SharedPreferences.
	 *
	 * @return The current default alarm tone Uri.
	 */
	private Uri getCurrentToneUri() {

		String tone = sharedPreferences.getString(
			ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI, null);

		if (tone != null && doesFileExist(Uri.parse(tone))) {
			return Uri.parse(tone);
		} else {
			setDefaultTone(Settings.System.DEFAULT_ALARM_ALERT_URI);
			return Settings.System.DEFAULT_ALARM_ALERT_URI;
		}
	}

	//-----------------------------------------------------------------------------------------------------

	/**
	 * Changes the default alarm tone Uri in SharedPreferences.
	 *
	 * @param uri The new default alarm tone Uri.
	 */
	private void setDefaultTone(Uri uri) {
		prefEditor.remove(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI)
			.putString(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI,
				uri.toString())
			.commit();
	}

	//-----------------------------------------------------------------------------------------------------

	/**
	 * Sets the image in the {@link #volumeImageView} as per the progress in the volume
	 * seekbar.
	 *
	 * @param volume The current volume (or progress in volume SeekBar.
	 */
	private void setVolumeImageView(int volume) {

		if (volume == 0) {
			volumeImageView.setImageResource(R.drawable.ic_volume_mute);
		} else {
			volumeImageView.setImageResource(R.drawable.ic_volume_high);
		}
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Change the image in {@link #shakeImageView}.
	 *
	 * @param state The state of the shake ExpandableLayout. Either
	 *    {@link ExpandableLayout.State#EXPANDED} or
	 *    {@link ExpandableLayout.State#COLLAPSED}.
	 */
	private void setShakeImageView(int state) {

		if (state == ExpandableLayout.State.EXPANDED) {
			shakeImageView.setImageResource(R.drawable.ic_collapse);
		} else if (state == ExpandableLayout.State.COLLAPSED) {
			shakeImageView.setImageResource(R.drawable.ic_expand);
		}
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Change the image in {@link #snoozeImageView}.
	 *
	 * @param state The state of the snooze ExpandableLayout. Either
	 *    {@link ExpandableLayout.State#EXPANDED} or
	 *    {@link ExpandableLayout.State#COLLAPSED}.
	 */
	private void setSnoozeImageView(int state) {
		if (state == ExpandableLayout.State.EXPANDED) {
			snoozeImageView.setImageResource(R.drawable.ic_collapse);
		} else if (state == ExpandableLayout.State.COLLAPSED) {
			snoozeImageView.setImageResource(R.drawable.ic_expand);
		}
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	//-----------------------------------------------------------------------------------------------------

	/**
	 * Changes the text of {@link #snoozeStateSwitch} to ON or OFF as appropriate.
	 */
	private void setSwitchText() {
		if (snoozeStateSwitch.isChecked()) {
			snoozeStateSwitch.setText(getResources().getString(R.string.switchOn));
		} else {
			snoozeStateSwitch.setText(getResources().getString(R.string.switchOff));
		}
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
		int position, long id) {

		if (parentView.getId() == R.id.shakeOperationSpinner) {

			prefEditor.remove(
				ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SHAKE_OPERATION);
			prefEditor.putInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SHAKE_OPERATION,
				position);
			prefEditor.commit();

		} else if (parentView.getId() == R.id.powerBtnOpSpinner) {

			prefEditor.remove(
				ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_POWER_BTN_OPERATION);
			prefEditor.putInt(
				ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_POWER_BTN_OPERATION,
				position);
			prefEditor.commit();

		} else if (parentView.getId() == R.id.themeSpinner) {

			if (position !=
				sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_THEME,
					defaultTheme)) {

				prefEditor.remove(ConstantsAndStatics.SHARED_PREF_KEY_THEME);
				prefEditor.putInt(ConstantsAndStatics.SHARED_PREF_KEY_THEME, position);
				prefEditor.commit();
				applyTheme();
			}
		}
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) {
	}

	//-----------------------------------------------------------------------------------------------------

	/**
	 * Applies the appropriate theme. Gets the theme using
	 * {@link ConstantsAndStatics#getTheme(int)}.
	 */
	private void applyTheme() {
		AppCompatDelegate.setDefaultNightMode(ConstantsAndStatics.getTheme(
			sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_THEME,
				defaultTheme)));
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public void onCheckedChanged(CompoundButton compoundButton, boolean checkedState) {

		if (compoundButton.getId() == R.id.settingsSnoozeOnOffSwitch) {

			prefEditor.remove(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON);
			prefEditor.putBoolean(
				ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON, checkedState);
			prefEditor.commit();
			setSwitchText();
			activateOrDeactivateSnoozeOptions(checkedState);

		} else if (compoundButton.getId() == R.id.autoToneSelectionCheckBox) {

			prefEditor.remove(ConstantsAndStatics.SHARED_PREF_KEY_AUTO_SET_TONE);
			prefEditor.putBoolean(ConstantsAndStatics.SHARED_PREF_KEY_AUTO_SET_TONE,
				checkedState);
			prefEditor.commit();
		}
	}

	//-----------------------------------------------------------------------------------------------------

	/**
	 * Activates or deactivates the rest of the snooze options.
	 *
	 * @param isSnoozeOn {@code true} if the options are to be activated, otherwise
	 *    {@code false}.
	 */
	private void activateOrDeactivateSnoozeOptions(boolean isSnoozeOn) {

		if (isSnoozeOn) {

			snoozeFreqEditText.setEnabled(true);
			snoozeIntvEditText.setEnabled(true);
			snoozeIntvLabel.setEnabled(true);
			snoozeFreqLabel.setEnabled(true);

			snoozeIntvEditText.setTextColor(
				getResources().getColor(R.color.defaultLabelColor));
			snoozeFreqEditText.setTextColor(
				getResources().getColor(R.color.defaultLabelColor));
			snoozeFreqLabel.setTextColor(
				getResources().getColor(R.color.defaultLabelColor));
			snoozeIntvLabel.setTextColor(
				getResources().getColor(R.color.defaultLabelColor));

		} else {

			snoozeFreqEditText.setEnabled(false);
			snoozeIntvEditText.setEnabled(false);
			snoozeIntvLabel.setEnabled(false);
			snoozeFreqLabel.setEnabled(false);

			snoozeIntvEditText.setTextColor(
				getResources().getColor(R.color.disabledColor));
			snoozeFreqEditText.setTextColor(
				getResources().getColor(R.color.disabledColor));
			snoozeFreqLabel.setTextColor(getResources().getColor(R.color.disabledColor));
			snoozeIntvLabel.setTextColor(getResources().getColor(R.color.disabledColor));
		}
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public void onClick(View view) {

		if (view.getId() == R.id.settings_snoozeOptionsConstraintLayout) {

			if (snoozeOptionsExpandableLayout.isExpanded()) {
				snoozeOptionsExpandableLayout.collapse();
			} else {
				snoozeOptionsExpandableLayout.expand(true);
			}

		} else if (view.getId() == R.id.settings_toneConstraintLayout) {

			Intent intent = new Intent(this, Activity_RingtonePicker.class);
			intent.setAction(RingtoneManager.ACTION_RINGTONE_PICKER)
				.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
					RingtoneManager.TYPE_ALARM)
				.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select alarm tone:")
				.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
					getCurrentToneUri())
				.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
					Settings.System.DEFAULT_ALARM_ALERT_URI)
				.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
				.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
				.putExtra(ConstantsAndStatics.EXTRA_PLAY_RINGTONE, false);

			ringPickActLauncher.launch(intent);

		} else if (view.getId() == R.id.shakeSensitivityConstarintLayout) {

			shakeSensitivityExpandableLayout.toggle();

		} else if (view.getId() == R.id.testShakeSenstivityTextView) {

			sensorManager.registerListener(this, acclerometer,
				SensorManager.SENSOR_DELAY_UI, new Handler());
			lastShakeTime = System.currentTimeMillis();
			DialogFragment dialogFragment = new AlertDialog_TestShakeSensitivity();
			dialogFragment.setCancelable(false);
			dialogFragment.show(getSupportFragmentManager(), "");
			isShakeTestGoingOn = true;
		}
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

		if (seekBar.getId() == R.id.settings_volumeSeekbar) {
			setVolumeImageView(progress);
		}
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {

		if (seekBar.getId() == R.id.settings_volumeSeekbar) {

			prefEditor.remove(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_VOLUME)
				.putInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_VOLUME,
					seekBar.getProgress())
				.commit();

		} else if (seekBar.getId() == R.id.shakeSensitivitySeekBar) {

			Log.e(getClass().getSimpleName(),
				"Sensitivity value = " + getSensitivityValue(seekBar.getProgress()));
			prefEditor.remove(ConstantsAndStatics.SHARED_PREF_KEY_SHAKE_SENSITIVITY)
				.putFloat(ConstantsAndStatics.SHARED_PREF_KEY_SHAKE_SENSITIVITY,
					getSensitivityValue(seekBar.getProgress()))
				.commit();
		}
	}

	//-----------------------------------------------------------------------------------------------------

	/**
	 * Calculates the steps of seekbar from sensitivity value.
	 *
	 * @param sensitivity The value of sensitivity, i.e. the minimum gForce required to
	 * 	trigger the detector.
	 * @return The corresponding progress value that can be set in the seekbar.
	 */
	private int getStepValue(float sensitivity) {
		return (int) ((sensitivity - 1.5f) / .2f);
	}

	//-----------------------------------------------------------------------------------------------------

	/**
	 * Given a progress value from the seekbar, this function calculates the sensitivity
	 * of shake detector.
	 *
	 * @param step The value returned by {@link SeekBar#getProgress()}}.
	 * @return The sensitivity corresponding to the step.
	 */
	private float getSensitivityValue(int step) {
		BigDecimal bigDecimal = new BigDecimal(1.5f + step * .2f).setScale(1,
			RoundingMode.FLOOR);
		return bigDecimal.floatValue();
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public void onSensorChanged(SensorEvent event) {

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];

			float gX = x / SensorManager.GRAVITY_EARTH;
			float gY = y / SensorManager.GRAVITY_EARTH;
			float gZ = z / SensorManager.GRAVITY_EARTH;

			float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);
			// gForce will be close to 1 when there is no movement.

			if (gForce >= sharedPreferences.getFloat(
				ConstantsAndStatics.SHARED_PREF_KEY_SHAKE_SENSITIVITY,
				ConstantsAndStatics.DEFAULT_SHAKE_SENSITIVITY)) {
				long currTime = System.currentTimeMillis();
				if (Math.abs(currTime - lastShakeTime) > MINIMUM_MILLIS_BETWEEN_SHAKES) {
					lastShakeTime = currTime;
					shakeVibration();
				}
			}
		}
	}

	//-----------------------------------------------------------------------------------------------------

	/**
	 * Creates a vibration for a small period of time, indicating that the app has
	 * registered a shake event.
	 */
	private void shakeVibration() {
		Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		if (vibrator.hasVibrator()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				vibrator.vibrate(VibrationEffect.createOneShot(200,
					VibrationEffect.DEFAULT_AMPLITUDE));
			} else {
				vibrator.vibrate(200);
			}
		}
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public void onDialogNegativeClick(DialogFragment dialogFragment) {
		sensorManager.unregisterListener(this, acclerometer);
		isShakeTestGoingOn = false;
	}

	//-----------------------------------------------------------------------------------------------------

	private void initializeActLauncher() {

		ringPickActLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(), (result) -> {

				if (result.getResultCode() == RESULT_OK) {

					assert result.getData() != null;

					setDefaultTone(Objects.requireNonNull(result.getData()
						.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)));
					setToneTextView(Objects.requireNonNull(result.getData()
						.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)));

					autoSelectToneCheckBox.setChecked(false);
				}
			});
	}

}
