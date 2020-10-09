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

public class Fragment_AlarmDetails_RepeatOptions extends Fragment
		implements CompoundButton.OnCheckedChangeListener {

	private List<CheckBox> checkBoxArrayList;

	private ViewModel_AlarmDetails viewModel;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {

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
				///////////////////////////////////////////////////////////
				// The ArrayList starts from index 0 (for monday),
				// while the DayOfWeek enum assigns monday to 1.
				//////////////////////////////////////////////////////////
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

		if (isChecked) { //TODO Check
			viewModel.getRepeatDays().add(checkBoxArrayList.indexOf(checkBox) + 1);
		} else {
			int index = viewModel.getRepeatDays().indexOf(checkBoxArrayList.indexOf(checkBox) + 1);
			//noinspection RedundantCollectionOperation
			viewModel.getRepeatDays().remove(index);
		}
		viewModel.setIsRepeatOn(viewModel.getRepeatDays().size() > 0);

		Collections.sort(viewModel.getRepeatDays());

		/*Log.e(this.getClass().getSimpleName(),
				"contents = " + Arrays.toString(viewModel.getRepeatDays().toArray()));*/
	}
}
