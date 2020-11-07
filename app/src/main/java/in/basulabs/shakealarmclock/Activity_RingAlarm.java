package in.basulabs.shakealarmclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalTime;
import java.util.Objects;

public class Activity_RingAlarm extends AppCompatActivity implements View.OnClickListener {

	private SharedPreferences sharedPreferences;

	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Objects.equals(intent.getAction(), ConstantsAndStatics.ACTION_DESTROY_RING_ALARM_ACTIVITY)) {
				finish();
			} else if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_OFF)) {
				DisplayManager displayManager = (DisplayManager) context.getSystemService(DISPLAY_SERVICE);
				for (Display display : displayManager.getDisplays()) {
					if (display.getState() == Display.STATE_OFF) {
						onPowerButtonPressed();
					}
				}
			}
		}
	};

	//-----------------------------------------------------------------------------------------------------

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

		sharedPreferences = getSharedPreferences(ConstantsAndStatics.SHARED_PREF_FILE_NAME, MODE_PRIVATE);

		TextView alarmTimeTextView = findViewById(R.id.alarmTimeTextView2);
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

		snoozeButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConstantsAndStatics.ACTION_DESTROY_RING_ALARM_ACTIVITY);
		intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		intentFilter.addAction(Intent.ACTION_SCREEN_ON);
		registerReceiver(broadcastReceiver, intentFilter);

	}

	//--------------------------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(broadcastReceiver);
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.snoozeButton:
				Intent intent = new Intent(ConstantsAndStatics.ACTION_SNOOZE_ALARM);
				sendBroadcast(intent);
				finish();
				break;
			case R.id.cancelButton:
				Intent intent1 = new Intent(ConstantsAndStatics.ACTION_CANCEL_ALARM);
				sendBroadcast(intent1);
				finish();
				break;
		}
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_POWER) {
			onPowerButtonPressed();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	//---------------------------------------------------------------------------------------------------

	private void onPowerButtonPressed() {
		Intent intent;
		int powerBtnAction = sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_POWER_BTN_OPERATION,
				ConstantsAndStatics.DISMISS);
		if (powerBtnAction == ConstantsAndStatics.DISMISS) {
			intent = new Intent(ConstantsAndStatics.ACTION_CANCEL_ALARM);
			sendBroadcast(intent);
			finish();
		} else if (powerBtnAction == ConstantsAndStatics.SNOOZE) {
			intent = new Intent(ConstantsAndStatics.ACTION_SNOOZE_ALARM);
			sendBroadcast(intent);
			finish();
		}

	}
}
