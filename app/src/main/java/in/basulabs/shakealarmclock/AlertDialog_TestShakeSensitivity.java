package in.basulabs.shakealarmclock;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class AlertDialog_TestShakeSensitivity extends DialogFragment {

	private AlertDialog_TestShakeSensitivity.DialogListener listener;

	//---------------------------------------------------------------------------------------------------

	public interface DialogListener {

		void onDialogNegativeClick(DialogFragment dialogFragment);

	}

	//---------------------------------------------------------------------------------------------------

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		if (context instanceof AlertDialog_TestShakeSensitivity.DialogListener) {
			listener = (AlertDialog_TestShakeSensitivity.DialogListener) context;
		} else {
			throw new ClassCastException(context.getClass().getSimpleName() + " must implement AlertDialog_TestShakeSensitivity.DialogListener");
		}
	}

	//---------------------------------------------------------------------------------------------------

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

		AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
		builder.setTitle(R.string.title_shakeDetectionTest)
				.setMessage(getResources().getString(R.string.message_shakeDetectionTest))
				.setNegativeButton(getResources().getString(R.string.negative_shakeDetectionTest), (dialogInterface, i) -> {
					listener.onDialogNegativeClick(AlertDialog_TestShakeSensitivity.this);
					dismiss();
				})
				.setCancelable(false);

		return builder.create();
	}

	//---------------------------------------------------------------------------------------------------

	@Override
	public void onDetach() {
		super.onDetach();
		listener = null;
	}

}
