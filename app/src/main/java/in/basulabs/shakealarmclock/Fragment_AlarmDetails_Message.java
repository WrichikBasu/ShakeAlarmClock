package in.basulabs.shakealarmclock;

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

public class Fragment_AlarmDetails_Message extends Fragment {

	private ViewModel_AlarmDetails viewModel;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.frag_alarmdetails_message, container, false);

		viewModel = new ViewModelProvider(requireActivity()).get(ViewModel_AlarmDetails.class);

		EditText messageEditText = view.findViewById(R.id.alarmMessageEditText);

		if (viewModel.getAlarmMessage() == null) {
			messageEditText.setText("");
		} else {
			messageEditText.setText(viewModel.getAlarmMessage());
		}

		messageEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
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
