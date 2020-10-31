package in.basulabs.shakealarmclock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.util.Objects;

public class Activity_Settings extends AppCompatActivity implements AdapterView.OnItemSelectedListener,
		ExpandableLayout.OnExpansionUpdateListener, CompoundButton.OnCheckedChangeListener,
		View.OnClickListener, SeekBar.OnSeekBarChangeListener {

	private SharedPreferences.Editor prefEditor;
	private SharedPreferences sharedPreferences;

	private ExpandableLayout snoozeOptionsExpandableLayout;
	private TextView snoozeIntvLabel, snoozeFreqLabel, toneTextView;
	private EditText snoozeIntvEditText, snoozeFreqEditText;
	private ImageView snoozeImageView, volumeImageView;
	private SwitchCompat snoozeStateSwitch;
	private CheckBox autoSelectToneCheckBox;

	private static final String SAVE_INSTANCE_KEY_LAYOUT_EXPANDED =
			"in.basulabs.shakealarmclock.Activity_Settings.LAYOUT_EXPANDED";

	private int defaultTheme;

	private static final int RINGTONE_REQUEST_CODE = 834;

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
		getSupportActionBar().setTitle("Settings");

		//////////////////////////////////////////////////
		// Get SharedPreferences and its editor:
		/////////////////////////////////////////////////
		sharedPreferences = getSharedPreferences(ConstantsAndStatics.SHARED_PREF_FILE_NAME, MODE_PRIVATE);
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
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		shakeOpSpinner.setAdapter(arrayAdapter);
		shakeOpSpinner.setSelection(sharedPreferences
				.getInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SHAKE_OPERATION, ConstantsAndStatics.SNOOZE));
		shakeOpSpinner.setOnItemSelectedListener(this);

		/////////////////////////////////////////////////////////
		// Initialise spinner for power button operation:
		////////////////////////////////////////////////////////
		Spinner powerBtnOpSpinner = findViewById(R.id.powerBtnOpSpinner);
		ArrayAdapter<CharSequence> arrayAdapter2 = ArrayAdapter.createFromResource(this,
				R.array.shakeAndPowerOptions, android.R.layout.simple_spinner_item);
		arrayAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		powerBtnOpSpinner.setAdapter(arrayAdapter2);
		powerBtnOpSpinner.setSelection(sharedPreferences.getInt(
				ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_POWER_BTN_OPERATION, ConstantsAndStatics.DISMISS));
		powerBtnOpSpinner.setOnItemSelectedListener(this);

		////////////////////////////////////////////////////
		// Initialise spinner for theme:
		///////////////////////////////////////////////////
		Spinner themeSpinner = findViewById(R.id.themeSpinner);
		ArrayAdapter<CharSequence> arrayAdapter3;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			arrayAdapter3 = ArrayAdapter.createFromResource(this, R.array.themeOptions_all,
					android.R.layout.simple_spinner_item);
		} else {
			arrayAdapter3 = ArrayAdapter.createFromResource(this, R.array.themeOptions_noSystem,
					android.R.layout.simple_spinner_item);
		}
		arrayAdapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		themeSpinner.setAdapter(arrayAdapter3);
		themeSpinner.setSelection(sharedPreferences
				.getInt(ConstantsAndStatics.SHARED_PREF_KEY_THEME, ConstantsAndStatics.THEME_SYSTEM));
		themeSpinner.setSelection(sharedPreferences
				.getInt(ConstantsAndStatics.SHARED_PREF_KEY_THEME, defaultTheme));
		themeSpinner.setOnItemSelectedListener(this);

		///////////////////////////////////////////////////////////
		// Initialise expandable layout for snooze options:
		//////////////////////////////////////////////////////////
		snoozeOptionsExpandableLayout = findViewById(R.id.expandable_layout);
		if (savedInstanceState == null) {
			snoozeOptionsExpandableLayout.setExpanded(false);
		} else {
			snoozeOptionsExpandableLayout.setExpanded(savedInstanceState.getBoolean(SAVE_INSTANCE_KEY_LAYOUT_EXPANDED));
		}
		snoozeOptionsExpandableLayout.setOnExpansionUpdateListener(this);

		ConstraintLayout snoozeOptionsConstraintLayout = findViewById(R.id.settings_snoozeOptionsConstraintLayout);
		snoozeOptionsConstraintLayout.setOnClickListener(this);

		//////////////////////////////////////////////
		// Initialise snooze ON/OFF switch:
		/////////////////////////////////////////////
		snoozeStateSwitch = findViewById(R.id.settingsSnoozeOnOffSwitch);
		snoozeStateSwitch.setChecked(
				sharedPreferences.getBoolean(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON, true));
		setSwitchText();
		snoozeStateSwitch.setOnCheckedChangeListener(this);

		snoozeIntvLabel = findViewById(R.id.settings_snoozeIntervalLabel);
		snoozeFreqLabel = findViewById(R.id.settings_snoozeFreqLabel);
		snoozeIntvEditText = findViewById(R.id.settings_snoozeIntervalEditText);
		snoozeFreqEditText = findViewById(R.id.settings_snoozeFreqEditText2);

		snoozeIntvEditText.setText(String.valueOf(
				sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL, 5)));
		snoozeFreqEditText.setText(String.valueOf(
				sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ, 3)));

		activateOrDeactivateSnoozeOptions(
				sharedPreferences.getBoolean(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON, true));

		snoozeImageView = findViewById(R.id.snoozeExpColapImage);
		if (snoozeOptionsExpandableLayout.isExpanded()) {
			snoozeImageView.setImageResource(R.drawable.ic_collapse);
		} else {
			snoozeImageView.setImageResource(R.drawable.ic_expand);
		}

		snoozeFreqEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
				if (charSequence.length() != 0) {
					prefEditor.remove(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ);
					prefEditor.putInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ,
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
			public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
				if (charSequence.length() != 0) {
					prefEditor.remove(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL);
					prefEditor.putInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL,
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
		volumeSeekbar.setProgress(sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_VOLUME,
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
		ConstraintLayout toneConstraintLayout = findViewById(R.id.settings_toneConstraintLayout);
		toneConstraintLayout.setOnClickListener(this);

		toneTextView = findViewById(R.id.settings_toneTextView);
		setToneTextView(getCurrentToneUri());

		///////////////////////////////////////////
		// Auto select tone enable/disable:
		//////////////////////////////////////////
		autoSelectToneCheckBox = findViewById(R.id.autoToneSelectionCheckBox);
		autoSelectToneCheckBox.setChecked(
				sharedPreferences.getBoolean(ConstantsAndStatics.SHARED_PREF_KEY_AUTO_SET_TONE, true));
		autoSelectToneCheckBox.setOnCheckedChangeListener(this);

	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SAVE_INSTANCE_KEY_LAYOUT_EXPANDED, snoozeOptionsExpandableLayout.isExpanded());
	}


	//-----------------------------------------------------------------------------------------------------

	/**
	 * Finds whether a file exists or not.
	 *
	 * @param uri The Uri of the file.
	 *
	 * @return {@code true} if the file exists, otherwise {@code false}.
	 */
	private boolean doesFileExist(Uri uri) {
		try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
			return cursor != null;
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

			String fileNameWithExt = null;

			try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {

				try {
					if (cursor != null && cursor.moveToFirst()) {

						int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
						if (index != - 1) {
							fileNameWithExt = cursor.getString(index);
						} else {
							fileNameWithExt = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
						}
					}
				} catch (Exception ignored) {
				}
			}

			if (fileNameWithExt != null) {
				toneTextView.setText(fileNameWithExt);
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
		String tone = sharedPreferences.getString(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI, null);

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
				.putString(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI, uri.toString())
				.commit();
	}

	//-----------------------------------------------------------------------------------------------------

	/**
	 * Sets the volume in the {@link #volumeImageView} as per the progress in the volume seekbar.
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
	public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
		if (parentView.getId() == R.id.shakeOperationSpinner) {
			prefEditor.remove(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SHAKE_OPERATION);
			prefEditor.putInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SHAKE_OPERATION, position);
			prefEditor.commit();
		} else if (parentView.getId() == R.id.powerBtnOpSpinner) {
			prefEditor.remove(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_POWER_BTN_OPERATION);
			prefEditor.putInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_POWER_BTN_OPERATION, position);
			prefEditor.commit();
		} else if (parentView.getId() == R.id.themeSpinner) {
			if (position != sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_THEME, defaultTheme)) {
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
	 * Applies the appropriate theme. Gets the theme using {@link ConstantsAndStatics#getTheme(int)}.
	 */
	private void applyTheme() {
		AppCompatDelegate.setDefaultNightMode(ConstantsAndStatics.getTheme(
				sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_THEME, defaultTheme)));
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public void onExpansionUpdate(float expansionFraction, int state) {
		if (state == ExpandableLayout.State.EXPANDED) {
			snoozeImageView.setImageResource(R.drawable.ic_collapse);
		} else if (state == ExpandableLayout.State.COLLAPSED) {
			snoozeImageView.setImageResource(R.drawable.ic_expand);
		}
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public void onCheckedChanged(CompoundButton compoundButton, boolean checkedState) {
		if (compoundButton.getId() == R.id.settingsSnoozeOnOffSwitch) {
			prefEditor.remove(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON);
			prefEditor.putBoolean(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON, checkedState);
			prefEditor.commit();
			setSwitchText();
			activateOrDeactivateSnoozeOptions(checkedState);
		} else if (compoundButton.getId() == R.id.autoToneSelectionCheckBox) {
			prefEditor.remove(ConstantsAndStatics.SHARED_PREF_KEY_AUTO_SET_TONE);
			prefEditor.putBoolean(ConstantsAndStatics.SHARED_PREF_KEY_AUTO_SET_TONE, checkedState);
			prefEditor.commit();
		}
	}

	//-----------------------------------------------------------------------------------------------------

	/**
	 * Activates or deactivates the rest of the snooze options.
	 *
	 * @param isSnoozeOn {@code true} if the options are to be activated, otherwise {@code false}.
	 */
	private void activateOrDeactivateSnoozeOptions(boolean isSnoozeOn) {
		if (isSnoozeOn) {
			snoozeFreqEditText.setEnabled(true);
			snoozeIntvEditText.setEnabled(true);
			snoozeIntvLabel.setEnabled(true);
			snoozeFreqLabel.setEnabled(true);

			snoozeIntvEditText.setTextColor(getResources().getColor(R.color.defaultLabelColor));
			snoozeFreqEditText.setTextColor(getResources().getColor(R.color.defaultLabelColor));
			snoozeFreqLabel.setTextColor(getResources().getColor(R.color.defaultLabelColor));
			snoozeIntvLabel.setTextColor(getResources().getColor(R.color.defaultLabelColor));
		} else {
			snoozeFreqEditText.setEnabled(false);
			snoozeIntvEditText.setEnabled(false);
			snoozeIntvLabel.setEnabled(false);
			snoozeFreqLabel.setEnabled(false);

			snoozeIntvEditText.setTextColor(getResources().getColor(R.color.disabledColor));
			snoozeFreqEditText.setTextColor(getResources().getColor(R.color.disabledColor));
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
					.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
					.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select alarm tone:")
					.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, getCurrentToneUri())
					.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_ALARM_ALERT_URI)
					.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
					.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
					.putExtra(ConstantsAndStatics.EXTRA_PLAY_RINGTONE, false);
			startActivityForResult(intent, RINGTONE_REQUEST_CODE);
		}
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == RINGTONE_REQUEST_CODE) {

			if (resultCode == RESULT_OK) {

				assert data != null;

				setDefaultTone(Objects.requireNonNull(data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)));
				setToneTextView(Objects.requireNonNull(data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)));

				autoSelectToneCheckBox.setChecked(false);
			}
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
		prefEditor.remove(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_VOLUME)
				.putInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_VOLUME, seekBar.getProgress())
				.commit();
	}

}
