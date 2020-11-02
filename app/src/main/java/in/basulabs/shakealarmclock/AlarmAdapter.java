package in.basulabs.shakealarmclock;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormatSymbols;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Objects;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.ViewHolder> {

	private AlarmAdapter.AdapterInterface listener;

	private ArrayList<AlarmData> alarmDataArrayList;

	private Context context;

	//----------------------------------------------------------------------------------------------------

	public interface AdapterInterface {

		void onOnOffButtonClick(int rowNumber, int hour, int mins, int currentAlarmState);

		void onDeleteButtonClicked(int rowNumber, int hour, int mins);

		void onItemClicked(int rowNumber, int hour, int mins);
	}

	//----------------------------------------------------------------------------------------------------

	public static class ViewHolder extends RecyclerView.ViewHolder {

		public ImageButton alarmOnOffImgBtn, alarmDeleteBtn;
		public TextView alarmTimeTextView, alarmDateTextView, alarmTypeTextView;
		public CardView alarmCardView;

		public ViewHolder(View view) {
			super(view);
			alarmOnOffImgBtn = view.findViewById(R.id.alarmOnOffImgBtn);
			alarmTimeTextView = view.findViewById(R.id.alarmTimeTextView);
			alarmDateTextView = view.findViewById(R.id.alarmDateTextView);
			alarmTypeTextView = view.findViewById(R.id.alarmTypeTextView);
			alarmDeleteBtn = view.findViewById(R.id.alarmDeleteBtn);
			alarmCardView = view.findViewById(R.id.alarmCardView);
		}
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * A constructor.
	 *
	 * @param alarmDataArrayList The {@link ArrayList} containing {@link AlarmData} objects.
	 * @param listener An instance of  {@link AlarmAdapter.AdapterInterface} that will listen to click
	 * 		events.
	 * @param context The context.
	 */
	public AlarmAdapter(@NonNull ArrayList<AlarmData> alarmDataArrayList,
	                    @NonNull AlarmAdapter.AdapterInterface listener, @NonNull Context context) {
		this.alarmDataArrayList = alarmDataArrayList;
		this.listener = listener;
		this.context = context;
	}

	//----------------------------------------------------------------------------------------------------

	public void setListener(AdapterInterface listener) {
		this.listener = listener;
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

	@Override
	public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

		AlarmData alarmData = alarmDataArrayList.get(position);

		if (alarmData.isSwitchedOn()) {
			holder.alarmOnOffImgBtn.setImageResource(R.drawable.ic_alarm_on);
		} else {
			holder.alarmOnOffImgBtn.setImageResource(R.drawable.ic_alarm_off);
		}

		if (DateFormat.is24HourFormat(context)) {
			holder.alarmTimeTextView.setText(context.getResources().getString(R.string.time_24hour,
					alarmData.getAlarmTime().getHour(),
					alarmData.getAlarmTime().getMinute()));
		} else {
			String amPm = alarmData.getAlarmTime().getHour() < 12 ? "AM" : "PM";

			if ((alarmData.getAlarmTime().getHour() <= 12) && (alarmData.getAlarmTime().getHour() > 0)) {
				holder.alarmTimeTextView.setText(context.getResources().getString(R.string.time_12hour,
						alarmData.getAlarmTime().getHour(),
						alarmData.getAlarmTime().getMinute(), amPm));
			} else if (alarmData.getAlarmTime().getHour() > 12 && alarmData.getAlarmTime().getHour() <= 23) {
				holder.alarmTimeTextView.setText(context.getResources().getString(R.string.time_12hour,
						alarmData.getAlarmTime().getHour() - 12,
						alarmData.getAlarmTime().getMinute(), amPm));
			} else {
				holder.alarmTimeTextView.setText(context.getResources().getString(R.string.time_12hour,
						alarmData.getAlarmTime().getHour() + 12,
						alarmData.getAlarmTime().getMinute(), amPm));
			}
		}

		if (! alarmData.isRepeatOn()) {

			int day = (alarmData.getAlarmDateTime().getDayOfWeek().getValue() + 1) > 7 ? 1 :
					(alarmData.getAlarmDateTime().getDayOfWeek().getValue() + 1);

			holder.alarmDateTextView.setText(context.getResources().getString(R.string.date,
					new DateFormatSymbols().getShortWeekdays()[day],
					alarmData.getAlarmDateTime().getDayOfMonth(),
					new DateFormatSymbols().getShortMonths()[alarmData.getAlarmDateTime()
							.getMonthValue() - 1]));
		} else {
			StringBuilder str = new StringBuilder();
			for (int i = 0; i < alarmData.getRepeatDays().size(); i++) {
				int day = (alarmData.getRepeatDays().get(i) + 1) > 7 ? 1 : (alarmData.getRepeatDays()
						.get(i) + 1);
				str.append(new DateFormatSymbols().getShortWeekdays()[day].substring(0, 3));
				if (i < alarmData.getRepeatDays().size() - 1) {
					str.append(" ");
				}
			}
			holder.alarmDateTextView.setText(str.toString());
		}

		if (alarmData.getSoundVibrateSetting() == ConstantsAndStatics.ALARM_TYPE_SOUND_ONLY) {
			holder.alarmTypeTextView.setText(context.getResources().getString(R.string.sound));
		} else if (alarmData.getSoundVibrateSetting() == ConstantsAndStatics.ALARM_TYPE_VIBRATE_ONLY) {
			holder.alarmTypeTextView.setText(context.getResources().getString(R.string.vibrate));
		} else {
			holder.alarmTypeTextView.setText(context.getResources().getString(R.string.sound_and_vibrate));
		}

		holder.alarmOnOffImgBtn.setOnClickListener(view -> {
			int currentAlarmState;
			if (! alarmData.isSwitchedOn()) {
				currentAlarmState = 1;
				alarmData.setSwitchedOn(true);
				holder.alarmOnOffImgBtn.setImageResource(R.drawable.ic_alarm_on);
			} else {
				currentAlarmState = 0;
				alarmData.setSwitchedOn(false);
				holder.alarmOnOffImgBtn.setImageResource(R.drawable.ic_alarm_off);
			}
			listener.onOnOffButtonClick(holder.getLayoutPosition(),
					alarmData.getAlarmTime().getHour(),
					alarmData.getAlarmTime().getMinute(), currentAlarmState);
		});

		holder.alarmDeleteBtn
				.setOnClickListener(view -> listener.onDeleteButtonClicked(holder.getLayoutPosition(),
						alarmData.getAlarmTime().getHour(),
						alarmData.getAlarmTime().getMinute()));

		holder.alarmCardView.setOnClickListener(view -> listener.onItemClicked(holder.getLayoutPosition(),
				alarmData.getAlarmTime().getHour(),
				alarmData.getAlarmTime().getMinute()));
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Get the number of items in the current instance of the adapter.
	 *
	 * @return The number of items in the current instance of the adapter.
	 */
	@Override
	public int getItemCount() {
		return alarmDataArrayList.size();
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Remove an alarm from the current adapter instance.
	 *
	 * @param hour The alarm hour.
	 * @param minute The alarm minute.
	 */
	@Deprecated
	public void remove(int hour, int minute) {
		for (int i = 0; i < alarmDataArrayList.size(); i++) {
			AlarmData alarmData = alarmDataArrayList.get(i);
			if ((alarmData.getAlarmDateTime().getHour() == hour) && (alarmData.getAlarmDateTime()
					.getMinute() == minute)) {
				alarmDataArrayList.remove(i);
				notifyDataSetChanged();
				notifyItemRemoved(i);
				notifyItemRangeChanged(i, alarmDataArrayList.size());
				break;
			}
		}
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Adds an alarm to the current adapter instance.
	 *
	 * @param alarmDateTime The alarm date and time.
	 * @param isRepeatOn Whether alarm repeat is ON or OFF.
	 * @param repeatDays If repeat is ON, put the {@link ArrayList} containing the repeat days, otherwise
	 * 		put {@code null}.
	 * @param alarmType The alarm type.
	 */
	@Deprecated
	public void add(@NonNull LocalDateTime alarmDateTime, boolean isRepeatOn,
	                @Nullable ArrayList<Integer> repeatDays, int alarmType) {

		AlarmData newAlarmData;

		if (isRepeatOn && repeatDays != null) {
			newAlarmData = new AlarmData(true, alarmDateTime.toLocalTime(), alarmType, repeatDays);
		} else {
			newAlarmData = new AlarmData(true, alarmDateTime, alarmType);
		}

		int position = 0;

		if (alarmDataArrayList.size() == 0) {

			alarmDataArrayList = new ArrayList<>();
			Objects.requireNonNull(alarmDataArrayList).add(newAlarmData);

		} else {

			for (int i = 0; i < Objects.requireNonNull(alarmDataArrayList).size(); i++) {

				if (alarmDataArrayList.get(i).getAlarmDateTime().toLocalTime().isBefore(alarmDateTime.toLocalTime())) {

					if ((i + 1) < alarmDataArrayList.size()) {

						if (alarmDataArrayList.get(i + 1).getAlarmDateTime().toLocalTime()
								.isAfter(alarmDateTime.toLocalTime())) {

							alarmDataArrayList.add(i + 1, newAlarmData);
							position = i + 1;
							break;
						}
					} else {
						alarmDataArrayList.add(newAlarmData);
						position = alarmDataArrayList.size() - 1;
						break;
					}
				}

				if (i == alarmDataArrayList.size() - 1) {
					alarmDataArrayList.add(0, newAlarmData);
					break;
				}
			}
		}

		notifyDataSetChanged();
		notifyItemInserted(position);
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Toggle the alarm ON/OFF state.
	 *
	 * @param hour The alarm hour.
	 * @param mins The alarm minute.
	 * @param newAlarmState The new alarm state. 0 means alarm is OFF, 1 means ON.
	 */
	@Deprecated
	public void toggleAlarmState(int hour, int mins, int newAlarmState) {

		int position = 0;

		for (int i = 0; i < alarmDataArrayList.size(); i++) {
			AlarmData alarmData = alarmDataArrayList.get(i);

			if (alarmData.getAlarmTime().equals(LocalTime.of(hour, mins))) {
				alarmData.setSwitchedOn(newAlarmState == 1);
				position = i;
				break;
			}
		}

		notifyDataSetChanged();
		notifyItemChanged(position);
	}

}
