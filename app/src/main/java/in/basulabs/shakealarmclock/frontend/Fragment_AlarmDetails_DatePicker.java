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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.time.LocalDate;
import java.util.Calendar;

import in.basulabs.shakealarmclock.R;

public class Fragment_AlarmDetails_DatePicker extends Fragment {

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
		@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.frag_alarmdetails_datepicker, container,
			false);

		ViewModel_AlarmDetails viewModel = new ViewModelProvider(requireActivity()).get(
			ViewModel_AlarmDetails.class);

		Calendar minCalendar = Calendar.getInstance();
		minCalendar.set(Calendar.DAY_OF_MONTH, viewModel.getMinDate().getDayOfMonth());
		minCalendar.set(Calendar.MONTH, viewModel.getMinDate().getMonthValue() - 1);
		minCalendar.set(Calendar.YEAR, viewModel.getMinDate().getYear());
		minCalendar.set(Calendar.HOUR_OF_DAY, 1);
		minCalendar.set(Calendar.MINUTE, 0);
		minCalendar.set(Calendar.SECOND, 0);

		DatePicker datePicker = view.findViewById(R.id.datePicker);
		datePicker.setMinDate(minCalendar.getTimeInMillis());

		datePicker.init(viewModel.getAlarmDateTime().getYear(),
			viewModel.getAlarmDateTime().getMonthValue() - 1,
			viewModel.getAlarmDateTime().getDayOfMonth(),
			(datePicker1, newYear, newMonthOfYear, newDayOfMonth) -> {

				viewModel.setAlarmDateTime(
					viewModel.getAlarmDateTime().withDayOfMonth(newDayOfMonth));
				viewModel.setAlarmDateTime(
					viewModel.getAlarmDateTime().withMonth(newMonthOfYear + 1));
				viewModel.setAlarmDateTime(
					viewModel.getAlarmDateTime().withYear(newYear));

				viewModel.setIsChosenDateToday(
					viewModel.getAlarmDateTime().toLocalDate().equals(LocalDate.now()));
				viewModel.setHasUserChosenDate(true);
			});

		return view;
	}

}
