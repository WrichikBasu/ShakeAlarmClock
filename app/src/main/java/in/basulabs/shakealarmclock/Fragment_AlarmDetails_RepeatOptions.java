package in.basulabs.shakealarmclock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Fragment_AlarmDetails_RepeatOptions extends Fragment implements CompoundButton.OnCheckedChangeListener {

	private List<CheckBox> checkBoxArrayList;

	private ViewModel_AlarmDetails viewModel;

	//----------------------------------------------------------------------------------------------------

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.frag_alarmdetails_repeatoptions, container, false);

		viewModel = new ViewModelProvider(requireActivity()).get(ViewModel_AlarmDetails.class);

		checkBoxArrayList = new ArrayList<>();
		checkBoxArrayList.add(view.findViewById(R.id.checkBox_mon));
		checkBoxArrayList.add(view.findViewById(R.id.checkBox_tue));
		checkBoxArrayList.add(view.findViewById(R.id.checkBox_wed));
		checkBoxArrayList.add(view.findViewById(R.id.checkBox_thu));
		checkBoxArrayList.add(view.findViewById(R.id.checkBox_fri));
		checkBoxArrayList.add(view.findViewById(R.id.checkBox_sat));
		checkBoxArrayList.add(view.findViewById(R.id.checkBox_sun));

		for (CheckBox checkBox : checkBoxArrayList) {
			checkBox.setChecked(false);
		}

		if (viewModel.getRepeatDays() != null && viewModel.getRepeatDays().size() > 0) {
			for (int i : viewModel.getRepeatDays()) {
				// The checkBoxArrayList starts from index 0 (for monday), while the DayOfWeek enum assigns monday to 1.
				checkBoxArrayList.get(i - 1).setChecked(true);
			}
		}

		for (CheckBox checkBox : checkBoxArrayList) {
			checkBox.setOnCheckedChangeListener(this);
		}

		return view;
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

		CheckBox checkBox = (CheckBox) compoundButton;

		if (isChecked) {

			// Initialize the ArrayList if it is null.
			if (viewModel.getRepeatDays() == null) {
				viewModel.setRepeatDays(new ArrayList<>());
			}

			// Add the new day to the ArrayList:
			viewModel.getRepeatDays().add(checkBoxArrayList.indexOf(checkBox) + 1);
			viewModel.setIsRepeatOn(true);
		} else {

			// Remove the dpecific day from the View Model:
			int index = Objects.requireNonNull(viewModel.getRepeatDays(), "Repeat days array list was null!")
					.indexOf(checkBoxArrayList.indexOf(checkBox) + 1);
			viewModel.getRepeatDays().remove(index);

			// Make the ArrayList of repeatDays null if it is empty.
			if (viewModel.getRepeatDays() != null && viewModel.getRepeatDays().size() == 0) {
				viewModel.setRepeatDays(null);
				viewModel.setIsRepeatOn(false);
			}
		}

		if (viewModel.getRepeatDays() != null) {
			viewModel.setIsRepeatOn(viewModel.getRepeatDays().size() > 0);
			Collections.sort(viewModel.getRepeatDays());
		}
	}

}
