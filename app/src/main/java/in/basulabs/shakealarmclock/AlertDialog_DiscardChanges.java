package in.basulabs.shakealarmclock;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

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
			throw new ClassCastException(context.getClass() + " must implement AlertDialog_DiscardChanges.DialogListener");
		}
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

		AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
		builder.setMessage(getResources().getString(R.string.cancelDialogMessage))
				.setPositiveButton(getResources().getString(R.string.cancelDialog_positive),
						(dialogInterface, i) -> listener.onDialogPositiveClick(AlertDialog_DiscardChanges.this))
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
