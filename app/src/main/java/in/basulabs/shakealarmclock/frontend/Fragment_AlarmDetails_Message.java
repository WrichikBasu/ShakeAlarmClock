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
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import in.basulabs.shakealarmclock.R;

public class Fragment_AlarmDetails_Message extends Fragment {

	private ViewModel_AlarmDetails viewModel;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
		@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.frag_alarmdetails_message, container,
			false);

		viewModel = new ViewModelProvider(requireActivity()).get(
			ViewModel_AlarmDetails.class);

		EditText messageEditText = view.findViewById(R.id.alarmMessageEditText);

		if (viewModel.getAlarmMessage() == null) {
			messageEditText.setText("");
		} else {
			messageEditText.setText(viewModel.getAlarmMessage());
		}

		messageEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s != null && s.length() == 0) {
					viewModel.setAlarmMessage(null);
				} else {
					viewModel.setAlarmMessage(s == null ? null : s.toString());
				}
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		return view;
	}
}
