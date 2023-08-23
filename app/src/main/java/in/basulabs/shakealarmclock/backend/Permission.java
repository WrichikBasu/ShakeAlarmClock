package in.basulabs.shakealarmclock.backend;

import android.content.Context;

import androidx.annotation.NonNull;

public record Permission(String androidString, int displayNameID, int permExplanationID,
                         int permType, int numberOfTimesRequested) {

	public String getDisplayName(@NonNull Context context) {
		return context.getString(displayNameID);
	}

	public String getExplanation(@NonNull Context context) {
		return context.getString(permExplanationID);
	}

	@Override
	public String toString() {
		return "Permission{" +
				"androidString='" + androidString + '\'' +
				'}';
	}
}
