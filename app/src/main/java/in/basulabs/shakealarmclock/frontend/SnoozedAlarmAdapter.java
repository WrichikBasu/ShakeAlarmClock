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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;

import in.basulabs.shakealarmclock.R;
import in.basulabs.shakealarmclock.backend.AlarmData;
import in.basulabs.shakealarmclock.backend.ConstantsAndStatics;

public class SnoozedAlarmAdapter extends RecyclerView.Adapter<SnoozedAlarmAdapter.ViewHolder> {

	private final SnoozedAlarmAdapter.AdapterInterface listener;

	private final ArrayList<Bundle> alarmBundles;

	private Context context;

	//---------------------------------------------------------------------------------------------

	/**
	 * An interface to listen to user action on each item of the RecyclerView.
	 */
	public interface AdapterInterface {

		/**
		 * The user has clicked the delete button.
		 *
		 * @param rowNumber The row of the RecyclerView where the action took place.
		 * @param alarmData The {@link Bundle} containing the alarm data.
		 */
		void onDismissed(int rowNumber, @NonNull Bundle alarmData);
	}

	//---------------------------------------------------------------------------------------------

	public static class ViewHolder extends RecyclerView.ViewHolder {

		public TextView originalTimeTextView, snoozedTimeTextView, durationTextView,
				alarmMessageTextView;

		public LinearLayout alarmMsgContainer, repeatDaysContainer;
		public CardView snoozedAlarmCardView;

		public ChipGroup repeatDaysChipGroup;

		public Button dismissButton;

		public View divider1, divider2;

		public ViewHolder(View view) {
			super(view);
			originalTimeTextView = view.findViewById(R.id.txt_original_time);
			snoozedTimeTextView = view.findViewById(R.id.txt_next_ring);
			durationTextView = view.findViewById(R.id.txt_remaining_time);
			alarmMessageTextView = view.findViewById(R.id.txt_alarm_message);
			alarmMsgContainer = view.findViewById(R.id.layout_alarm_message);
			repeatDaysContainer = view.findViewById(R.id.layout_repeat_days);
			snoozedAlarmCardView = view.findViewById(R.id.card_snoozed_alarm);
			repeatDaysChipGroup = view.findViewById(R.id.chip_group_repeat_days);
			dismissButton = view.findViewById(R.id.dismiss_button);
			divider1 = view.findViewById(R.id.divider1);
			divider2 = view.findViewById(R.id.divider2);
		}
	}

	//---------------------------------------------------------------------------------------------

	/**
	 * A constructor.
	 *
	 * @param alarmBundles The {@link ArrayList} containing {@link AlarmData} objects.
	 * @param listener An instance of  {@link AlarmAdapter.AdapterInterface} that will
	 * listen to click events.
	 * @param context The context.
	 */
	public SnoozedAlarmAdapter(@NonNull ArrayList<Bundle> alarmBundles,
	                           @NonNull SnoozedAlarmAdapter.AdapterInterface listener,
	                           @NonNull Context context) {
		this.alarmBundles = alarmBundles;
		this.listener = listener;
		this.context = context;
	}

	//---------------------------------------------------------------------------------------------

	public void setContext(Context context) {
		this.context = context;
	}

	//---------------------------------------------------------------------------------------------

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View listItem = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.card_snoozed_alarm, parent, false);
		return new ViewHolder(listItem);
	}

	//---------------------------------------------------------------------------------------------

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

		Bundle alarmData = alarmBundles.get(position);

		final int alarmHour = alarmData.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR);
		final int alarmMinute = alarmData.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE);

		if (DateFormat.is24HourFormat(context)) {
			holder.originalTimeTextView.setText(
					context.getResources().getString(R.string.time_24hour, alarmHour, alarmMinute));
		} else {
			String amPm = alarmHour < 12 ? "AM" : "PM";

			int displayHour;

			if ((alarmHour > 0) && (alarmHour <= 12)) {
				displayHour = alarmHour;
			} else if (alarmHour > 12 && alarmHour <= 23) {
				displayHour = alarmHour - 12;
			} else {
				displayHour = alarmHour + 12;
			}

			holder.originalTimeTextView.setText(
					context.getResources().getString(R.string.time_12hour, displayHour,
							alarmMinute, amPm));
		}

		ArrayList<Integer> repeatDays;

		if (alarmData.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_REPEAT_ON)
				&& (repeatDays = alarmData.getIntegerArrayList(
				ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS)) != null) {

			holder.repeatDaysChipGroup.removeAllViews();
			holder.repeatDaysContainer.setVisibility(View.VISIBLE);

			for (int i : repeatDays) {
				Chip chip = new Chip(context);
				chip.setText(getChipTextID(i + 1));
				chip.setCheckable(false);
				chip.setClickable(false);
				holder.repeatDaysChipGroup.addView(chip);
			}
			holder.divider1.setVisibility(View.VISIBLE);
		} else {
			holder.repeatDaysContainer.setVisibility(View.GONE);
			holder.divider1.setVisibility(View.GONE);
		}

		String alarmMessage = alarmData.getString(ConstantsAndStatics.BUNDLE_KEY_ALARM_MESSAGE);

		if (alarmMessage == null || alarmMessage.isEmpty()) {
			holder.alarmMsgContainer.setVisibility(View.GONE);
			holder.divider2.setVisibility(View.GONE);
		} else {
			holder.alarmMsgContainer.setVisibility(View.VISIBLE);
			holder.alarmMessageTextView.setText(alarmMessage);
			holder.divider2.setVisibility(View.VISIBLE);
		}

		holder.dismissButton.setOnClickListener(
				view -> listener.onDismissed(holder.getLayoutPosition(), alarmData));
	}

	//---------------------------------------------------------------------------------------------

	private int getChipTextID(int day) {
		return day == 1 ? R.string.sunday : day == 2 ? R.string.monday :
				day == 3 ? R.string.tuesday : day == 4 ? R.string.wednesday :
						day == 5 ? R.string.thursday :
								day == 6 ? R.string.friday : R.string.saturday;
	}

	//---------------------------------------------------------------------------------------------

	/**
	 * Get the number of items in the current instance of the adapter.
	 *
	 * @return Same as in description.
	 */
	@Override
	public int getItemCount() {
		return alarmBundles.size();
	}

}
