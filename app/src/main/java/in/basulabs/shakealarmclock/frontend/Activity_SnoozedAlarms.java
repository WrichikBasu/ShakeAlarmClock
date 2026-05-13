/*
 * Copyright (c) 2026. Wrichik Basu (basulabs.developer@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package in.basulabs.shakealarmclock.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Dictionary;

import in.basulabs.shakealarmclock.R;
import in.basulabs.shakealarmclock.backend.AlarmRingDS;
import in.basulabs.shakealarmclock.backend.ConstantsAndStatics;

public class Activity_SnoozedAlarms extends AppCompatActivity implements
		SnoozedAlarmAdapter.AdapterInterface {

	private RecyclerView snoozedAlarmsRecyclerView;
	private SnoozedAlarmAdapter snoozedAlarmAdapter;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_snoozed_alarms);

		Log.e(ConstantsAndStatics.DEBUG_TAG, "In onCreate()");

		snoozedAlarmsRecyclerView = findViewById(R.id.snoozed_alarms_recyclerView);
		snoozedAlarmsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		snoozedAlarmAdapter = new SnoozedAlarmAdapter(AlarmRingDS.getSnoozedAlarms(),this, this);
		snoozedAlarmsRecyclerView.setAdapter(snoozedAlarmAdapter);

		AlarmRingDS.getSnoozedAlarmsLiveData().observe(this, this::onSnoozedAlarmDictModified);
	}

	@Override
	public void onDismissed(int rowNumber, @NonNull Bundle alarmData) {

		Log.e(ConstantsAndStatics.DEBUG_TAG, "In onDismissed() rowNumber: " + rowNumber);

		Intent intent = new Intent(ConstantsAndStatics.ACTION_CANCEL_ALARM);
		intent.setPackage(getPackageName()).putExtras(alarmData);
		sendBroadcast(intent);

		Log.e(ConstantsAndStatics.DEBUG_TAG, "In onDismissed() after sending broadcast");

		if (AlarmRingDS.isSnoozedAlarmsEmpty()) {
			Log.e(ConstantsAndStatics.DEBUG_TAG, "No further alarms, finishing activity");
			finish();
		}
		else {
			snoozedAlarmAdapter = new SnoozedAlarmAdapter(AlarmRingDS.getSnoozedAlarms(),this, this);
			snoozedAlarmsRecyclerView.swapAdapter(snoozedAlarmAdapter, true);
			Log.e(ConstantsAndStatics.DEBUG_TAG, "Adapter updated");
		}
//		snoozedAlarmAdapter.notifyItemRemoved(rowNumber);
	}

	private void onSnoozedAlarmDictModified(@NonNull Dictionary<Integer, Bundle> integerBundleDictionary) {

		Log.e(ConstantsAndStatics.DEBUG_TAG, "In onSnoozedAlarmDictModified()");

		if (AlarmRingDS.isSnoozedAlarmsEmpty()) {
			Log.e(ConstantsAndStatics.DEBUG_TAG, "No further alarms, finishing activity");
			finish();
		} else {
			Log.e(ConstantsAndStatics.DEBUG_TAG, "In onSnoozedAlarmDictModified(), Adapter updated");
			snoozedAlarmAdapter = new SnoozedAlarmAdapter(AlarmRingDS.getSnoozedAlarms(),this, this);
			snoozedAlarmsRecyclerView.swapAdapter(snoozedAlarmAdapter, false);
		}
	}
}
