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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import in.basulabs.shakealarmclock.R;

public class AlertDialog_DiscardChanges extends DialogFragment {

	private DialogListener listener;

	public interface DialogListener {
		void onDialogPositiveClick(DialogFragment dialogFragment);
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		if (context instanceof DialogListener) {
			listener = (DialogListener) context;
		} else {
			throw new ClassCastException(context.getClass() +
				" must implement AlertDialog_DiscardChanges.DialogListener");
		}
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

		AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
		builder.setMessage(getResources().getString(R.string.cancelDialogMessage))
			.setPositiveButton(getResources().getString(R.string.cancelDialog_positive),
				(dialogInterface, i) -> listener.onDialogPositiveClick(
					AlertDialog_DiscardChanges.this))
			.setNegativeButton(getResources().getString(R.string.cancelDialog_negative),
				(dialogInterface, i) -> dismiss())
			.setCancelable(false);

		return builder.create();
	}

	@Override
	public void onDetach() {
		super.onDetach();
		listener = null;
	}
}
