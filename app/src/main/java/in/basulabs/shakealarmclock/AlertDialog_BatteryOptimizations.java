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
package in.basulabs.shakealarmclock;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class AlertDialog_BatteryOptimizations extends DialogFragment {

	private boolean attached;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		attached = true;
	}

	//-------------------------------------------------------------------------------------------------------------

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

		AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
				.setCancelable(false)
				.setTitle(R.string.batteryOptim_title)
				.setMessage(R.string.batteryOptim_message)
				.setPositiveButton(R.string.batteryOptim_positive, (dialog, which) -> {
					if (attached && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
						Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
						requireContext().startActivity(intent);
					}

				})
				.setNegativeButton(R.string.batteryOptim_negative, null)
				.setNeutralButton(R.string.batteryOptim_neutral, (dialog, which) -> {
					dialog.dismiss();
					requireContext().getSharedPreferences(ConstantsAndStatics.SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE)
							.edit()
							.remove(ConstantsAndStatics.SHARED_PREF_KEY_SHOW_BATTERY_OPTIM_DIALOG)
							.putBoolean(ConstantsAndStatics.SHARED_PREF_KEY_SHOW_BATTERY_OPTIM_DIALOG, false)
							.commit();
				});

		return builder.create();
	}

	//--------------------------------------------------------------------------------------------------------------------

	@Override
	public void onDetach() {
		super.onDetach();
		attached = false;
	}

}
