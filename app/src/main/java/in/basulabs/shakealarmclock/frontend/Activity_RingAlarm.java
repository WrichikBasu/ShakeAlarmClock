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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.time.LocalTime;
import java.util.Objects;

import in.basulabs.shakealarmclock.R;
import in.basulabs.shakealarmclock.backend.ConstantsAndStatics;

public class Activity_RingAlarm extends AppCompatActivity implements
	View.OnClickListener {

	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(@NonNull Context context, @NonNull Intent intent) {
			if (Objects.equals(intent.getAction(),
				ConstantsAndStatics.ACTION_DESTROY_RING_ALARM_ACTIVITY)) {
				finish();
			}
		}
	};

	//---------------------------------------------------------------------------------

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
			WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
			WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
			WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
			setTurnScreenOn(true);
			setShowWhenLocked(true);
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ringalarm);

		TextView alarmTimeTextView = findViewById(R.id.alarmTimeTextView2);
		TextView alarmMessageTextView = findViewById(R.id.alarmmessageTextView);
		Button snoozeButton = findViewById(R.id.snoozeButton);
		ImageButton cancelButton = findViewById(R.id.cancelButton);

		LocalTime localTime = LocalTime.now();

		if (DateFormat.is24HourFormat(this)) {
			alarmTimeTextView.setText(getResources().getString(R.string.time_24hour,
				localTime.getHour(), localTime.getMinute()));
		} else {
			String amPm = localTime.getHour() < 12 ? "AM" : "PM";

			if ((localTime.getHour() <= 12) && (localTime.getHour() > 0)) {

				alarmTimeTextView.setText(getResources().getString(R.string.time_12hour,
					localTime.getHour(), localTime.getMinute(), amPm));

			} else if (localTime.getHour() > 12 && localTime.getHour() <= 23) {

				alarmTimeTextView.setText(getResources().getString(R.string.time_12hour,
					localTime.getHour() - 12, localTime.getMinute(), amPm));

			} else {
				alarmTimeTextView.setText(getResources().getString(R.string.time_12hour,
					localTime.getHour() + 12, localTime.getMinute(), amPm));
			}
		}

		// Display the alarm message. Additionally, if the screen size is small, change
		// the text size to 15sp.
		if (getIntent().getExtras() != null) {
			String message = getIntent().getExtras()
				.getString(ConstantsAndStatics.BUNDLE_KEY_ALARM_MESSAGE, null);
			if (message != null) {
				int screenSize = getResources().getConfiguration().screenLayout &
					Configuration.SCREENLAYOUT_SIZE_MASK;
				if (screenSize == Configuration.SCREENLAYOUT_SIZE_SMALL) {
					alarmMessageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
				}
			}
			alarmMessageTextView.setText(
				message != null ? message : getString(R.string.alarmMessage));
		}

		snoozeButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConstantsAndStatics.ACTION_DESTROY_RING_ALARM_ACTIVITY);
		ContextCompat.registerReceiver(this, broadcastReceiver, intentFilter,
			ContextCompat.RECEIVER_NOT_EXPORTED);

	}

	//---------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(broadcastReceiver);
	}

	//----------------------------------------------------------------------------------

	@Override
	public void onClick(@NonNull View view) {
		if (view.getId() == R.id.snoozeButton) {
			Intent intent = new Intent(ConstantsAndStatics.ACTION_SNOOZE_ALARM);
			intent.setPackage(getPackageName());
			sendBroadcast(intent);
			finish();
		} else if (view.getId() == R.id.cancelButton) {
			Intent intent1 = new Intent(ConstantsAndStatics.ACTION_CANCEL_ALARM);
			intent1.setPackage(getPackageName());
			sendBroadcast(intent1);
			finish();
		}
	}

}
