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

import in.basulabs.shakealarmclock.R;

public class Fragment_AlarmDetails_SnoozeOptions extends Fragment
	implements RadioGroup.OnCheckedChangeListener,
	CompoundButton.OnCheckedChangeListener {

	private View view;
	private RadioGroup freqRadioGroup, intervalRadioGroup;
	private SwitchCompat onOffSwitch;
	private EditText snoozeFreqEditText, snoozeIntervalEditText;

	private ViewModel_AlarmDetails viewModel;

	//----------------------------------------------------------------------------------------------------

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
		@Nullable ViewGroup container,
		@Nullable Bundle savedInstanceState) {

		view = inflater.inflate(R.layout.frag_alarmdetails_snooze, container, false);

		viewModel = new ViewModelProvider(requireActivity()).get(
			ViewModel_AlarmDetails.class);

		onOffSwitch = view.findViewById(R.id.snoozeOnOffSwitch);
		freqRadioGroup = view.findViewById(R.id.snoozeFreqRadioGroup);
		intervalRadioGroup = view.findViewById(R.id.snoozeIntervalRadioGroup);
		snoozeFreqEditText = view.findViewById(R.id.snoozeFreqEditText);
		snoozeIntervalEditText = view.findViewById(R.id.snoozeIntervalEditText);

		onOffSwitch.setChecked(viewModel.getIsSnoozeOn());
		onSwitchCheckedChanged();
		onOffSwitch.setOnCheckedChangeListener(this);

		switch (viewModel.getSnoozeFreq()) {
			case 3 -> {
				freqRadioGroup.check(R.id.freqRadioButton_three);
				snoozeFreqEditText.setEnabled(false);
			}
			case 5 -> {
				freqRadioGroup.check(R.id.freqRadioButton_five);
				snoozeFreqEditText.setEnabled(false);
			}
			case 10 -> {
				freqRadioGroup.check(R.id.freqRadioButton_ten);
				snoozeFreqEditText.setEnabled(false);
			}
			default -> {
				freqRadioGroup.check(R.id.freqRadioButton_custom);
				snoozeFreqEditText.setEnabled(true);
				snoozeFreqEditText.setText(String.valueOf(viewModel.getSnoozeFreq()));
			}
		}

		switch (viewModel.getSnoozeIntervalInMins()) {
			case 5 -> {
				intervalRadioGroup.check(R.id.intervalRadioButton_five);
				snoozeIntervalEditText.setEnabled(false);
			}
			case 10 -> {
				intervalRadioGroup.check(R.id.intervalRadioButton_ten);
				snoozeIntervalEditText.setEnabled(false);
			}
			case 15 -> {
				intervalRadioGroup.check(R.id.intervalRadioButton_fifteen);
				snoozeIntervalEditText.setEnabled(false);
			}
			default -> {
				intervalRadioGroup.check(R.id.intervalRadioButton_custom);
				snoozeIntervalEditText.setEnabled(true);
				snoozeIntervalEditText.setText(
					String.valueOf(viewModel.getSnoozeIntervalInMins()));
			}
		}

		onSwitchCheckedChanged();

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
					viewModel.setSnoozeFreq(Integer.parseInt("" + charSequence));
				}
			}

			@Override
			public void afterTextChanged(Editable editable) {
			}
		});

		snoozeIntervalEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int start,
				int count,
				int after) {
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int start, int before,
				int count) {
				if (charSequence.length() != 0) {
					viewModel.setSnoozeIntervalInMins(
						Integer.parseInt("" + charSequence));
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

	/**
	 * Makes necessary changes in UI when the snooze switch is turned on or off.
	 */
	private void onSwitchCheckedChanged() {
		if (viewModel.getIsSnoozeOn()) {

			onOffSwitch.setText(
				requireContext().getResources().getString(R.string.switchOn));

			view.findViewById(R.id.freqRadioButton_three).setEnabled(true);
			view.findViewById(R.id.freqRadioButton_five).setEnabled(true);
			view.findViewById(R.id.freqRadioButton_ten).setEnabled(true);
			view.findViewById(R.id.freqRadioButton_custom).setEnabled(true);

			view.findViewById(R.id.intervalRadioButton_five).setEnabled(true);
			view.findViewById(R.id.intervalRadioButton_ten).setEnabled(true);
			view.findViewById(R.id.intervalRadioButton_fifteen).setEnabled(true);
			view.findViewById(R.id.intervalRadioButton_custom).setEnabled(true);

			snoozeFreqEditText.setEnabled(freqRadioGroup.getCheckedRadioButtonId()
				== R.id.freqRadioButton_custom);
			snoozeIntervalEditText.setEnabled(intervalRadioGroup.getCheckedRadioButtonId()
				== R.id.intervalRadioButton_custom);
		} else {

			onOffSwitch.setText(
				requireContext().getResources().getString(R.string.switchOff));

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

			if (checkedId == R.id.freqRadioButton_three) {
				viewModel.setSnoozeFreq(3);
				snoozeFreqEditText.setEnabled(false);
			} else if (checkedId == R.id.freqRadioButton_five) {
				viewModel.setSnoozeFreq(5);
				snoozeFreqEditText.setEnabled(false);
			} else if (checkedId == R.id.freqRadioButton_ten) {
				viewModel.setSnoozeFreq(10);
				snoozeFreqEditText.setEnabled(false);
			} else {
				snoozeFreqEditText.setEnabled(true);
			}

		} else if (radioGroup.getId() == R.id.snoozeIntervalRadioGroup) {

			if (checkedId == R.id.intervalRadioButton_five) {
				viewModel.setSnoozeIntervalInMins(5);
				snoozeIntervalEditText.setEnabled(false);
			} else if (checkedId == R.id.intervalRadioButton_ten) {
				viewModel.setSnoozeIntervalInMins(10);
				snoozeIntervalEditText.setEnabled(false);
			} else if (checkedId == R.id.intervalRadioButton_fifteen) {
				viewModel.setSnoozeIntervalInMins(15);
				snoozeIntervalEditText.setEnabled(false);
			} else {
				snoozeIntervalEditText.setEnabled(true);
			}
		}
	}

}
