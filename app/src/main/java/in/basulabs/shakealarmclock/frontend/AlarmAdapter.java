/*
Copyright (C) 2022  Wrichik Basu (basulabs.developer@gmail.com)

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

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Objects;

import in.basulabs.shakealarmclock.R;
import in.basulabs.shakealarmclock.backend.AlarmData;
import in.basulabs.shakealarmclock.backend.ConstantsAndStatics;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.ViewHolder> {

	private final AlarmAdapter.AdapterInterface listener;

	private final ArrayList<AlarmData> alarmDataArrayList;

	private Context context;

	//----------------------------------------------------------------------------------------------------

	/**
	 * An interface to listen to user action on each item of the RecyclerView.
	 */
	public interface AdapterInterface {

		/**
		 * The user has clicked the ON/OFF button.
		 *
		 * @param rowNumber The row of the RecyclerView where the action took place.
		 * @param hour The alarm hour.
		 * @param mins The alarm minutes.
		 * @param newAlarmState The new alarm state -- 0 means OFF anf 1 means ON.
		 */
		void onOnOffButtonClick(int rowNumber, int hour, int mins, int newAlarmState);

		/**
		 * The user has clicked the delete button.
		 *
		 * @param rowNumber The row of the RecyclerView where the action took place.
		 * @param hour The alarm hour.
		 * @param mins The alarm minutes.
		 */
		void onDeleteButtonClicked(int rowNumber, int hour, int mins);

		/**
		 * The user has requested to see the details of an alarm.
		 *
		 * @param rowNumber The row of the RecyclerView where the action took place.
		 * @param hour The alarm hour.
		 * @param mins The alarm minutes.
		 */
		void onItemClicked(int rowNumber, int hour, int mins);
	}

	//----------------------------------------------------------------------------------------------------

	public static class ViewHolder extends RecyclerView.ViewHolder {

		public ImageButton alarmOnOffImgBtn, alarmDeleteBtn;
		public TextView alarmTimeTextView, alarmDateTextView, alarmTypeTextView,
			alarmMessageTextView;
		public CardView alarmCardView;

		public ViewHolder(View view) {
			super(view);
			alarmOnOffImgBtn = view.findViewById(R.id.alarmOnOffImgBtn);
			alarmTimeTextView = view.findViewById(R.id.alarmTimeTextView);
			alarmDateTextView = view.findViewById(R.id.alarmDateTextView);
			alarmTypeTextView = view.findViewById(R.id.alarmTypeTextView);
			alarmMessageTextView = view.findViewById(
				R.id.recyclerView_alarmMessageTextView);
			alarmDeleteBtn = view.findViewById(R.id.alarmDeleteBtn);
			alarmCardView = view.findViewById(R.id.alarmCardView);
		}
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * A constructor.
	 *
	 * @param alarmDataArrayList The {@link ArrayList} containing {@link AlarmData}
	 * 	objects.
	 * @param listener An instance of  {@link AlarmAdapter.AdapterInterface} that will
	 * 	listen to click events.
	 * @param context The context.
	 */
	public AlarmAdapter(@NonNull ArrayList<AlarmData> alarmDataArrayList,
		@NonNull AlarmAdapter.AdapterInterface listener,
		@NonNull Context context) {
		this.alarmDataArrayList = alarmDataArrayList;
		this.listener = listener;
		this.context = context;
	}

	//----------------------------------------------------------------------------------------------------

	public void setContext(Context context) {
		this.context = context;
	}

	//----------------------------------------------------------------------------------------------------

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View listItem = LayoutInflater.from(parent.getContext())
			.inflate(R.layout.recyclerviewrow_alarmslist, parent, false);
		return new ViewHolder(listItem);
	}

	//----------------------------------------------------------------------------------------------------

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

		AlarmData alarmData = alarmDataArrayList.get(position);

		if (alarmData.isSwitchedOn()) {
			holder.alarmOnOffImgBtn.setImageResource(R.drawable.ic_alarm_on);
		} else {
			holder.alarmOnOffImgBtn.setImageResource(R.drawable.ic_alarm_off);
		}

		if (DateFormat.is24HourFormat(context)) {
			holder.alarmTimeTextView.setText(
				context.getResources().getString(R.string.time_24hour,
					alarmData.getAlarmTime().getHour(),
					alarmData.getAlarmTime().getMinute()));
		} else {
			String amPm = alarmData.getAlarmTime().getHour() < 12 ? "AM" : "PM";

			int alarmHour;

			if ((alarmData.getAlarmTime().getHour() > 0) &&
				(alarmData.getAlarmTime().getHour() <= 12)) {
				alarmHour = alarmData.getAlarmTime().getHour();
			} else if (alarmData.getAlarmTime().getHour() > 12 &&
				alarmData.getAlarmTime().getHour() <= 23) {
				alarmHour = alarmData.getAlarmTime().getHour() - 12;
			} else {
				alarmHour = alarmData.getAlarmTime().getHour() + 12;
			}

			holder.alarmTimeTextView.setText(
				context.getResources().getString(R.string.time_12hour, alarmHour,
					alarmData.getAlarmTime().getMinute(), amPm));
		}

		if (!alarmData.isRepeatOn()) {

			String nullMessage
				= "AlarmAdapter: alarmDateTime was null for a non-repetitive alarm.";

			int day = (Objects.requireNonNull(alarmData.getAlarmDateTime(), nullMessage)
				.getDayOfWeek()
				.getValue() + 1) > 7 ? 1 :
				(alarmData.getAlarmDateTime().getDayOfWeek().getValue() + 1);

			holder.alarmDateTextView.setText(
				context.getResources().getString(R.string.date,
					new DateFormatSymbols().getShortWeekdays()[day],
					alarmData.getAlarmDateTime().getDayOfMonth(),
					new DateFormatSymbols().getShortMonths()[
						alarmData.getAlarmDateTime().getMonthValue() - 1]));
		} else {
			StringBuilder str = new StringBuilder();
			for (int i = 0; i < Objects.requireNonNull(alarmData.getRepeatDays(),
				"AlarmAdapter: repeatDays was null.").size(); i++) {
				int day = (alarmData.getRepeatDays().get(i) + 1) > 7
					? 1
					: (alarmData.getRepeatDays().get(i) + 1);
				str.append(
					new DateFormatSymbols().getShortWeekdays()[day]);
				if (i < alarmData.getRepeatDays().size() - 1) {
					str.append(" ");
				}
			}
			holder.alarmDateTextView.setText(str.toString());
		}

		if (alarmData.getAlarmType() == ConstantsAndStatics.ALARM_TYPE_SOUND_ONLY) {
			holder.alarmTypeTextView.setText(
				context.getResources().getString(R.string.sound));
		} else if (alarmData.getAlarmType() ==
			ConstantsAndStatics.ALARM_TYPE_VIBRATE_ONLY) {
			holder.alarmTypeTextView.setText(
				context.getResources().getString(R.string.vibrate));
		} else {
			holder.alarmTypeTextView.setText(
				context.getResources().getString(R.string.sound_and_vibrate));
		}

		String alarmMessage = alarmData.getAlarmMessage();
		holder.alarmMessageTextView.setText(alarmMessage == null ? "" : alarmMessage);

		holder.alarmOnOffImgBtn.setOnClickListener(view -> {
			int newAlarmState;
			if (!alarmData.isSwitchedOn()) {
				newAlarmState = 1;
			} else {
				newAlarmState = 0;
			}
			listener.onOnOffButtonClick(holder.getLayoutPosition(),
				alarmData.getAlarmTime().getHour(), alarmData.getAlarmTime().getMinute(),
				newAlarmState);
		});

		holder.alarmDeleteBtn.setOnClickListener(
			view -> listener.onDeleteButtonClicked(holder.getLayoutPosition(),
				alarmData.getAlarmTime().getHour(),
				alarmData.getAlarmTime().getMinute()));

		holder.alarmCardView.setOnClickListener(
			view -> listener.onItemClicked(holder.getLayoutPosition(),
				alarmData.getAlarmTime().getHour(),
				alarmData.getAlarmTime().getMinute()));
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Get the number of items in the current instance of the adapter.
	 *
	 * @return Same as in description.
	 */
	@Override
	public int getItemCount() {
		return alarmDataArrayList.size();
	}

}
