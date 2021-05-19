package in.basulabs.shakealarmclock;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.Objects;

public class AlertDialog_PermissionReason extends DialogFragment {

	private DialogListener listener;

	//----------------------------------------------------------------------------------------------------

	public static AlertDialog_PermissionReason getInstance(String message) {
		Bundle args = new Bundle();
		args.putString("message", message);
		AlertDialog_PermissionReason frag = new AlertDialog_PermissionReason();
		frag.setArguments(args);
		return frag;
	}

	//----------------------------------------------------------------------------------------------------

	public interface DialogListener {
		void onDialogPositiveClick(DialogFragment dialogFragment);

		void onDialogNegativeClick(DialogFragment dialogFragment);
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		if (context instanceof DialogListener) {
			listener = (DialogListener) context;
		} else {
			throw new ClassCastException(context.getClass() + " must implement AlertDialog_PermissionReason.DialogListener");
		}
	}

	//----------------------------------------------------------------------------------------------------

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

		AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
		builder.setMessage(Objects.requireNonNull(getArguments()).getString("message"))
		       .setPositiveButton(getResources().getString(R.string.cancelDialog_positive), (dialogInterface, i)
				       -> listener.onDialogPositiveClick(AlertDialog_PermissionReason.this))
		       .setNegativeButton(getResources().getString(R.string.cancelDialog_negative), (dialogInterface, i) -> {
			       listener.onDialogNegativeClick(AlertDialog_PermissionReason.this);
			       dismiss();
		       })
		       .setCancelable(false);

		return builder.create();
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onDetach() {
		super.onDetach();
		listener = null;
	}


}
