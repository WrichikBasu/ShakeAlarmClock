package in.basulabs.shakealarmclock;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public class MyApplication extends Application {

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Objects.equals(intent.getAction(), Intent.ACTION_DATE_CHANGED)) {
				Activity_AlarmsList.onDateChanged();
			}
		}
	};

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onCreate() {
		super.onCreate();

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_DATE_CHANGED);

		registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
			@Override
			public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
				if (activity.getClass().equals(Activity_AlarmsList.class)) {
					registerReceiver(broadcastReceiver, intentFilter);
				}
			}

			@Override
			public void onActivityStarted(@NonNull Activity activity) {

			}

			@Override
			public void onActivityResumed(@NonNull Activity activity) {

			}

			@Override
			public void onActivityPaused(@NonNull Activity activity) {

			}

			@Override
			public void onActivityStopped(@NonNull Activity activity) {

			}

			@Override
			public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

			}

			@Override
			public void onActivityDestroyed(@NonNull Activity activity) {
				if (activity.getClass().equals(Activity_AlarmsList.class)) {
					unregisterReceiver(broadcastReceiver);
				}
			}
		});

	}
}
