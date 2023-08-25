package in.basulabs.shakealarmclock.backend;

import androidx.annotation.NonNull;

public record Permission(String androidString, int displayNameID, int permExplanationID,
                         int permType, int numberOfTimesRequested) {

	@NonNull
	@Override
	public String toString() {
		return "Permission{androidString='" + androidString + '\'' + '}';
	}
}
