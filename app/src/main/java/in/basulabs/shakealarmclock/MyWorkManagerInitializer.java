package in.basulabs.shakealarmclock;

import android.util.Log;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import java.util.Objects;

public class MyWorkManagerInitializer extends MyContentProvider {

	@Override
	public boolean onCreate() {
		WorkManager.initialize(Objects.requireNonNull(getContext()),
				new Configuration.Builder()
						.setMinimumLoggingLevel(Log.DEBUG)
						.build());
		return true;
	}
}
