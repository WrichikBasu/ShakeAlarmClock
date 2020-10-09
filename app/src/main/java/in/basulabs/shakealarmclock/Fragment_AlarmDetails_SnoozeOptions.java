package in.basulabs.shakealarmclock;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class Fragment_AlarmDetails_SnoozeOptions extends Fragment
		implements RadioGroup.OnCheckedChangeListener, CompoundButton.OnCheckedChangeListener {

	private SwitchCompat onOffSwitch;
	private EditText snoozeFreqEditText, snoozeIntervalEditText;

	private ViewModel_AlarmDetails viewModel;

	private View view;
	private RadioGroup freqRadioGroup, intervalRadioGroup;

	//----------------------------------------------------------------------------------------------------

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {

		view = inflater.inflate(R.layout.frag_alarmdetails_snooze, container, false);

		viewModel = new ViewModelProvider(requireActivity()).get(ViewModel_AlarmDetails.class);


		onOffSwitch = view.findViewById(R.id.snoozeOnOffSwitch);
		freqRadioGroup = view.findViewById(R.id.snoozeFreqRadioGroup);
		intervalRadioGroup = view.findViewById(R.id.snoozeIntervalRadioGroup);
		snoozeFreqEditText = view.findViewById(R.id.snoozeFreqEditText);
		snoozeIntervalEditText = view.findViewById(R.id.snoozeIntervalEditText);

		onOffSwitch.setChecked(viewModel.getIsSnoozeOn());
		onSwitchCheckedChanged();
		onOffSwitch.setOnCheckedChangeListener(this);

		switch (viewModel.getSnoozeFreq()) {
			case 3:
				freqRadioGroup.check(R.id.freqRadioButton_three);
				snoozeFreqEditText.setEnabled(false);
				break;
			case 5:
				freqRadioGroup.check(R.id.freqRadioButton_five);
				snoozeFreqEditText.setEnabled(false);
				break;
			case 10:
				freqRadioGroup.check(R.id.freqRadioButton_ten);
				snoozeFreqEditText.setEnabled(false);
				break;
			default:
				freqRadioGroup.check(R.id.freqRadioButton_custom);
				snoozeFreqEditText.setEnabled(true);
				snoozeFreqEditText.setText(String.valueOf(viewModel.getSnoozeFreq()));
				break;
		}

		switch (viewModel.getSnoozeIntervalInMins()) {
			case 5:
				intervalRadioGroup.check(R.id.intervalRadioButton_five);
				snoozeIntervalEditText.setEnabled(false);
				break;
			case 10:
				intervalRadioGroup.check(R.id.intervalRadioButton_ten);
				snoozeIntervalEditText.setEnabled(false);
				break;
			case 15:
				intervalRadioGroup.check(R.id.intervalRadioButton_fifteen);
				snoozeIntervalEditText.setEnabled(false);
				break;
			default:
				intervalRadioGroup.check(R.id.freqRadioButton_custom);
				snoozeIntervalEditText.setEnabled(true);
				snoozeIntervalEditText
						.setText(String.valueOf(viewModel.getSnoozeIntervalInMins()));
				break;
		}

		onSwitchCheckedChanged();

		snoozeFreqEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
				if (charSequence.length() != 0) {
					viewModel.setSnoozeFreq(Integer.parseInt("" + charSequence));
				}
			}

			@Override
			public void afterTextChanged(Editable editable) {
			}
		});

		snoozeIntervalEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
				if (charSequence.length() != 0) {
					viewModel.setSnoozeIntervalInMins(Integer.parseInt("" + charSequence));
				}
			}

			@Override
			public void afterTextChanged(Editable editable) {
			}
		});

		freqRadioGroup.setOnCheckedChangeListener(this);
		intervalRadioGroup.setOnCheckedChangeListener(this);

		return view;
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
		if (compoundButton.getId() == R.id.snoozeOnOffSwitch) {
			viewModel.setIsSnoozeOn(isChecked);
			onSwitchCheckedChanged();
		}
	}

	//----------------------------------------------------------------------------------------------------

	private void onSwitchCheckedChanged() {
		if (viewModel.getIsSnoozeOn()) {
			onOffSwitch.setText(requireContext().getResources().getString(R.string.switchOn));

			view.findViewById(R.id.freqRadioButton_three).setEnabled(true);
			view.findViewById(R.id.freqRadioButton_five).setEnabled(true);
			view.findViewById(R.id.freqRadioButton_ten).setEnabled(true);
			view.findViewById(R.id.freqRadioButton_custom).setEnabled(true);

			view.findViewById(R.id.intervalRadioButton_five).setEnabled(true);
			view.findViewById(R.id.intervalRadioButton_ten).setEnabled(true);
			view.findViewById(R.id.intervalRadioButton_fifteen).setEnabled(true);
			view.findViewById(R.id.intervalRadioButton_custom).setEnabled(true);

			snoozeFreqEditText
					.setEnabled(freqRadioGroup.getCheckedRadioButtonId() == R.id.freqRadioButton_custom);
			snoozeIntervalEditText.setEnabled(
					intervalRadioGroup.getCheckedRadioButtonId() == R.id.intervalRadioButton_custom);
		} else {
			onOffSwitch.setText(requireContext().getResources().getString(R.string.switchOff));

			view.findViewById(R.id.freqRadioButton_three).setEnabled(false);
			view.findViewById(R.id.freqRadioButton_five).setEnabled(false);
			view.findViewById(R.id.freqRadioButton_ten).setEnabled(false);
			view.findViewById(R.id.freqRadioButton_custom).setEnabled(false);

			view.findViewById(R.id.intervalRadioButton_five).setEnabled(false);
			view.findViewById(R.id.intervalRadioButton_ten).setEnabled(false);
			view.findViewById(R.id.intervalRadioButton_fifteen).setEnabled(false);
			view.findViewById(R.id.intervalRadioButton_custom).setEnabled(false);

			snoozeFreqEditText.setEnabled(false);
			snoozeIntervalEditText.setEnabled(false);
		}

	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
		if (radioGroup.getId() == R.id.snoozeFreqRadioGroup) {
			//Log.e(this.getClass().getSimpleName(), "Frequency readio group registered callback.");
			switch (checkedId) {
				case R.id.freqRadioButton_three:
					viewModel.setSnoozeFreq(3);
					snoozeFreqEditText.setEnabled(false);
					break;
				case R.id.freqRadioButton_five:
					viewModel.setSnoozeFreq(5);
					snoozeFreqEditText.setEnabled(false);
					break;
				case R.id.freqRadioButton_ten:
					viewModel.setSnoozeFreq(10);
					snoozeFreqEditText.setEnabled(false);
					break;
				default:
					snoozeFreqEditText.setEnabled(true);
					break;
			}
		} else if (radioGroup.getId() == R.id.snoozeIntervalRadioGroup) {
			//Log.e(this.getClass().getSimpleName(), "Interval radio group registered callback.");
			switch (checkedId) {
				case R.id.intervalRadioButton_five:
					viewModel.setSnoozeIntervalInMins(5);
					snoozeIntervalEditText.setEnabled(false);
					break;
				case R.id.intervalRadioButton_ten:
					viewModel.setSnoozeIntervalInMins(10);
					snoozeIntervalEditText.setEnabled(false);
					break;
				case R.id.intervalRadioButton_fifteen:
					viewModel.setSnoozeIntervalInMins(15);
					snoozeIntervalEditText.setEnabled(false);
					break;
				default:
					snoozeIntervalEditText.setEnabled(true);
					break;
			}
		}
	}

}
