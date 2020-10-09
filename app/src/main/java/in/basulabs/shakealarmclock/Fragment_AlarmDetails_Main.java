package in.basulabs.shakealarmclock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static android.app.Activity.RESULT_OK;

public class Fragment_AlarmDetails_Main extends Fragment
		implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, TimePicker.OnTimeChangedListener,
		AdapterView.OnItemSelectedListener {

	private static final int RINGTONE_REQUEST_CODE = 5280;

	private ViewModel_AlarmDetails viewModel;

	private FragmentGUIListener listener;
	private TextView currentRepeatOptionsTV, currentSnoozeOptionsTV, alarmDateTV, alarmToneTV;
	private ImageView alarmVolumeImageView;
	private boolean savedInstanceStateIsNull;

	//----------------------------------------------------------------------------------------------------

	public interface FragmentGUIListener {
		void onSaveButtonClick();

		void onRequestSnoozeFragCreation();

		void onRequestDatePickerFragCreation();

		void onRequestRepeatFragCreation();

		void onCancelButtonClick();
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		if (context instanceof FragmentGUIListener) {
			listener = (FragmentGUIListener) context;
		} else {
			throw new ClassCastException(context.getClass().getSimpleName() +
					" must implement Fragment_AlarmDetails_Main.FragmentGUIListener");
		}
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.e(this.getClass().getSimpleName(), "INSIDE ONCREATE()");
		savedInstanceStateIsNull = savedInstanceState == null;
		/*if (savedInstanceState == null) {
			//Log.e(this.getClass().getSimpleName(), "Saved instance is null.");
			savedInstanceStateIsNull = true;
		} else {
			//Log.e(this.getClass().getSimpleName(), "Saved instance NOT null.");
			savedInstanceStateIsNull = false;
		}*/
	}

	//--------------------------------------------------------------------------------------------------

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {

		//Log.e(this.getClass().getSimpleName(), "Inside onCreateView; setView = " + savedInstanceStateIsNull);

		View view = inflater.inflate(R.layout.frag_alarm_details_main, container, false);

		viewModel = new ViewModelProvider(requireActivity()).get(ViewModel_AlarmDetails.class);

		/////////////////////////////////////////////
		// Declare/initialise all variables
		/////////////////////////////////////////////
		TimePicker timePicker = view.findViewById(R.id.addAlarmTimePicker);
		ConstraintLayout repeatConsLayout = view.findViewById(R.id.repeatConstraintLayout);
		ConstraintLayout snoozeConsLayout = view.findViewById(R.id.snoozeConstraintLayout);
		ConstraintLayout alarmDateConstarintLayout = view.findViewById(R.id.alarmDateConstraintLayout);
		ConstraintLayout alarmToneConstraintLayout = view.findViewById(R.id.alarmToneConstraintLayout);
		currentRepeatOptionsTV = view.findViewById(R.id.currentRepeatOptionsTextView);
		currentSnoozeOptionsTV = view.findViewById(R.id.currentSnoozeOptionTextView);
		Spinner alarmTypeSpinner = view.findViewById(R.id.alarmTypeSpinner);
		SeekBar alarmVolumeSeekbar = view.findViewById(R.id.alarmVolumeSeekbar);
		alarmVolumeImageView = view.findViewById(R.id.alarmVolumeImageView);
		Button saveButton = view.findViewById(R.id.saveButton);
		Button cancelButton = view.findViewById(R.id.cancelButton);
		alarmDateTV = view.findViewById(R.id.alarmDateTextView);
		alarmToneTV = view.findViewById(R.id.alarmToneTextView);
		TextView alarmDateLabel = view.findViewById(R.id.alarmDateLabel);
		TextView alarmVolumeLabel = view.findViewById(R.id.alarmVolumeLabel);
		TextView alarmToneLabel = view.findViewById(R.id.alarmToneLabel);

		////////////////////////////////////////////
		// Initialise the GUI
		///////////////////////////////////////////
		timePicker.setIs24HourView(DateFormat.is24HourFormat(requireContext()));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			timePicker.setHour(viewModel.getAlarmDateTime().getHour());
			timePicker.setMinute(viewModel.getAlarmDateTime().getMinute());
		} else {
			timePicker.setCurrentHour(viewModel.getAlarmDateTime().getHour());
			timePicker.setCurrentMinute(viewModel.getAlarmDateTime().getMinute());
		}

		setDate();

		displayRepeatOptions();
		displaySnoozeOptions();
		displayAlarmTone();

		ArrayAdapter<CharSequence> alarmTypeAdapter = ArrayAdapter.createFromResource(requireContext(),
				R.array.alarmTypeSpinnerEntries, android.R.layout.simple_spinner_item);
		alarmTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		alarmTypeSpinner.setAdapter(alarmTypeAdapter);

		alarmTypeSpinner.setSelection(viewModel.getAlarmType());

		AudioManager audioManager = (AudioManager) requireContext()
				.getSystemService(Context.AUDIO_SERVICE);
		alarmVolumeSeekbar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM));

		alarmVolumeSeekbar.setProgress(viewModel.getAlarmVolume());
		if (viewModel.getAlarmVolume() == 0) {
			alarmVolumeImageView.setImageResource(R.drawable.ic_volume_mute);
		} else {
			alarmVolumeImageView.setImageResource(R.drawable.ic_volume_high);
		}


		//////////////////////////////////
		// Set the listeners
		//////////////////////////////////
		timePicker.setOnTimeChangedListener(this);

		// Workaround for AM-PM button not triggered bug:
		// SO link: https://stackoverflow.com/a/35786123/8387076
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			try {
				ViewGroup amPmView;
				ViewGroup v1 = (ViewGroup) timePicker.getChildAt(0);
				ViewGroup v2 = (ViewGroup) v1.getChildAt(0);
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
					ViewGroup v3 = (ViewGroup) v2.getChildAt(0);
					amPmView = (ViewGroup) v3.getChildAt(3);
				} else {
					amPmView = (ViewGroup) v2.getChildAt(3);
				}
				View.OnClickListener listener = v -> timePicker
						.setCurrentHour((timePicker.getCurrentHour() + 12) % 24);

				View am = amPmView.getChildAt(0);
				View pm = amPmView.getChildAt(1);

				am.setOnClickListener(listener);
				pm.setOnClickListener(listener);
			} catch (Exception e) {
				//Log.e(this.getClass().toString(), e.toString());
			}
		}

		saveButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);

		repeatConsLayout.setOnClickListener(this);
		snoozeConsLayout.setOnClickListener(this);
		alarmDateConstarintLayout.setOnClickListener(this);
		alarmToneConstraintLayout.setOnClickListener(this);

		alarmVolumeSeekbar.setOnSeekBarChangeListener(this);

		alarmTypeSpinner.setOnItemSelectedListener(this);

		//////////////////////////////////
		// Set viewModel observers
		//////////////////////////////////
		viewModel.getLiveAlarmVolume().observe(getViewLifecycleOwner(), volume -> {
			if (volume == 0) {
				alarmVolumeImageView.setImageResource(R.drawable.ic_volume_mute);
			} else {
				alarmVolumeImageView.setImageResource(R.drawable.ic_volume_high);
			}
		});

		viewModel.getLiveIsRepeatOn().observe(getViewLifecycleOwner(), isRepeatOn -> {
			if (isRepeatOn) {
				alarmDateConstarintLayout.setEnabled(false);

				alarmDateLabel.setTextColor(getResources().getColor(R.color.disabledColor));
				alarmDateLabel.setPaintFlags(alarmDateLabel.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

				alarmDateTV.setTextColor(getResources().getColor(R.color.disabledColor));
				alarmDateTV.setPaintFlags(alarmDateLabel.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
			} else {
				alarmDateConstarintLayout.setEnabled(true);

				alarmDateLabel.setTextColor(getResources().getColor(R.color.defaultLabelColor));
				alarmDateLabel
						.setPaintFlags(alarmDateLabel.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));

				alarmDateTV.setTextColor(getResources().getColor(R.color.defaultLabelColor));
				alarmDateTV.setPaintFlags(alarmDateLabel.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
			}
		});

		viewModel.getLiveAlarmType().observe(getViewLifecycleOwner(), alarmType -> {

			if (alarmType == ConstantsAndStatics.ALARM_TYPE_VIBRATE_ONLY) {

				alarmVolumeLabel.setTextColor(getResources().getColor(R.color.disabledColor));
				alarmVolumeLabel.setPaintFlags(alarmDateLabel.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

				alarmVolumeSeekbar.setEnabled(false);

				alarmToneLabel.setTextColor(getResources().getColor(R.color.disabledColor));
				alarmToneLabel.setPaintFlags(alarmDateLabel.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

				alarmToneTV.setTextColor(getResources().getColor(R.color.disabledColor));
				alarmToneTV.setPaintFlags(alarmDateLabel.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

				alarmToneConstraintLayout.setEnabled(false);

			} else {

				alarmVolumeLabel.setTextColor(getResources().getColor(R.color.defaultLabelColor));
				alarmVolumeLabel
						.setPaintFlags(alarmDateLabel.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));

				alarmVolumeSeekbar.setEnabled(true);

				alarmToneLabel.setTextColor(getResources().getColor(R.color.defaultLabelColor));
				alarmToneLabel
						.setPaintFlags(alarmDateLabel.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));

				alarmToneTV.setTextColor(getResources().getColor(R.color.defaultLabelColor));
				alarmToneTV.setPaintFlags(alarmDateLabel.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));

				alarmToneConstraintLayout.setEnabled(true);
			}
		});

		if (savedInstanceStateIsNull) {
			savedInstanceStateIsNull = false;
		}

		return view;
	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Updates {@link #currentSnoozeOptionsTV}.
	 */
	private void displaySnoozeOptions() {
		if (viewModel.getIsSnoozeOn()) {
			currentSnoozeOptionsTV.setText(requireContext().getResources()
					.getString(R.string.snoozeOptionsTV_snoozeOn,
							viewModel.getSnoozeIntervalInMins(), viewModel.getSnoozeFreq()));
		} else {
			currentSnoozeOptionsTV
					.setText(requireContext().getResources().getString(R.string.snoozeOffLabel));
		}
	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Updates {@link #currentRepeatOptionsTV}.
	 */
	private void displayRepeatOptions() {
		/*Log.e(this.getClass().getSimpleName(),
				"contents = " + Arrays.toString(viewModel.getRepeatDays().toArray()));*/
		if (viewModel.getIsRepeatOn()) {
			StringBuilder str = new StringBuilder();
			for (int i = 0; i < viewModel.getRepeatDays().size(); i++) {
				int day = (viewModel.getRepeatDays().get(i) + 1) > 7 ? 1 : (viewModel.getRepeatDays()
						.get(i) + 1);
				str.append(new DateFormatSymbols().getShortWeekdays()[day]);
				if (i < viewModel.getRepeatDays().size() - 1) {
					str.append(", ");
				}
			}
			currentRepeatOptionsTV.setText(str.toString());
		} else {
			currentRepeatOptionsTV.setText(requireContext().getResources().getString(R.string.repeatNone));
		}
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Displays the alarm tone in {@link #alarmToneTV}.
	 */
	private void displayAlarmTone() {
		assert viewModel.getAlarmToneUri() != null;

		try (Cursor cursor = requireContext().getContentResolver()
				.query(viewModel.getAlarmToneUri(), null, null, null, null)) {
			assert cursor != null;
			int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
			cursor.moveToFirst();

			String fileNameWithExt = cursor.getString(nameIndex);
			alarmToneTV.setText(fileNameWithExt.substring(0, fileNameWithExt.indexOf(".")));
		}
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Sets the date in {@link #alarmDateTV}. Uses {@link ViewModel_AlarmDetails#getAlarmDateTime()} to
	 * retrieve the date.
	 */
	private void setDate() {
		alarmDateTV
				.setText(viewModel.getAlarmDateTime().format(DateTimeFormatter.ofPattern("dd MMMM, yyyy")));
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.saveButton:
				//Log.e(this.getClass().toString(), "save button clicked.");
				saveButtonClicked();
				break;
			case R.id.cancelButton:
				listener.onCancelButtonClick();
				break;
			case R.id.repeatConstraintLayout:
				listener.onRequestRepeatFragCreation();
				break;
			case R.id.snoozeConstraintLayout:
				listener.onRequestSnoozeFragCreation();
				break;
			case R.id.alarmDateConstraintLayout:
				listener.onRequestDatePickerFragCreation();
				break;
			case R.id.alarmToneConstraintLayout:
				Intent intent = new Intent(requireContext(), Activity_RingtonePicker.class);
				intent.setAction(RingtoneManager.ACTION_RINGTONE_PICKER);
				//Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select alarm tone:");
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, viewModel.getAlarmToneUri());
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
				intent.putExtra(ConstantsAndStatics.EXTRA_PLAY_RINGTONE, false);
				startActivityForResult(intent, RINGTONE_REQUEST_CODE);
				break;

		}
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		//Log.e(this.getClass().getSimpleName(), "Inside onActivityResult(...)");
		if (requestCode == RINGTONE_REQUEST_CODE) {
			//Log.e(this.getClass().getSimpleName(), "Request code matched.");
			//Log.e(this.getClass().getSimpleName(), "result code: " + resultCode);
			if (resultCode == RESULT_OK) {
				//Log.e(this.getClass().getSimpleName(), "result OK.");
				assert data != null;

				/*Log.e(this.getClass().getSimpleName(), "Ringtone received: " + data
						.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI));*/

				Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
				viewModel.setAlarmToneUri(uri);

				//Set the default tone to the newly selected tone.
				SharedPreferences sharedPreferences = requireContext()
						.getSharedPreferences(ConstantsAndStatics.SHARED_PREF_FILE_NAME,
								Context.MODE_PRIVATE);
				if (sharedPreferences
						.getBoolean(ConstantsAndStatics.SHARED_PREF_KEY_AUTO_SET_TONE, true) && uri != null) {
					sharedPreferences.edit()
							.remove(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI)
							.putString(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI, uri.toString())
							.commit();
				}
			} /*else {
				//Log.e(this.getClass().getSimpleName(), "result NOT OK.");
			}*/
		}
		displayAlarmTone();
	}


	//----------------------------------------------------------------------------------------------------

	@Override
	public void onTimeChanged(TimePicker timePicker, int hourOfDay, int minute) {

		//Log.e(this.getClass().getSimpleName(), "Time changed." + hourOfDay + " " + minute);

		viewModel.setAlarmDateTime(viewModel.getAlarmDateTime().withHour(hourOfDay));
		viewModel.setAlarmDateTime(viewModel.getAlarmDateTime().withMinute(minute));

		if (viewModel.getIsChosenDateToday()) {
			///////////////////////////////////////////////////////////////////////////////////
			// Chosen date is today. We have to check if the alarm is possible today.
			// If not possible, the date is changed to tomorrow.
			//////////////////////////////////////////////////////////////////////////////////
			viewModel.setAlarmDateTime(LocalDateTime.of(LocalDate.now(),
					viewModel.getAlarmDateTime().toLocalTime()));

			if (! viewModel.getAlarmDateTime().toLocalTime().isAfter(LocalTime.now())) {
				//Date today NOT possible.
				viewModel.setAlarmDateTime(viewModel.getAlarmDateTime().plusDays(1));
				viewModel.setIsChosenDateToday(false);
				viewModel.setHasUserChosenDate(false);
			}

			// Set the minDate.
			viewModel.setMinDate(viewModel.getAlarmDateTime().toLocalDate());

		} else {
			/////////////////////////////////////////////////////////////////////////////////////
			// Chosen date is NOT today. If the user has not chosen a different date
			// deliberately, we check whether alarm today is possible. If possible, we
			// change the date to today, otherwise it will stay as it is.
			//
			// If the user has chosen a date deliberately, we do nothing.
			/////////////////////////////////////////////////////////////////////////////////////
			if (! viewModel.getHasUserChosenDate()){
				if (viewModel.getAlarmDateTime().toLocalTime().isAfter(LocalTime.now())){
					// Date today possible.
					viewModel.setAlarmDateTime(LocalDateTime.of(LocalDate.now(),
							viewModel.getAlarmDateTime().toLocalTime()));
					viewModel.setIsChosenDateToday(true);
				}
			}

			// Set the minDate.
			if (! viewModel.getAlarmDateTime().toLocalTime().isAfter(LocalTime.now())) {
				viewModel.setMinDate(LocalDate.now().plusDays(1));
			} else {
				viewModel.setMinDate(LocalDate.now());
			}
		}

		setDate();
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		viewModel.setAlarmVolume(progress);
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
		viewModel.setAlarmType(position);
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) {
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Handles {@code saveButton} clicks.
	 */
	private void saveButtonClicked() {
		listener.onSaveButtonClick();
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onDetach() {
		super.onDetach();
		listener = null;
	}

}
