package in.basulabs.shakealarmclock;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class Activity_Plugin extends AppCompatActivity implements View.OnClickListener {

	private TextView pluginStatus;
	private static boolean isPluginAvailable = false;

	//----------------------------------------------------------------------------------------------------

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_plugin);

		setSupportActionBar(findViewById(R.id.toolbar3));
		Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.actionBarTitle_pluginAct);

		findViewById(R.id.pluginDetectButton).setOnClickListener(this);
		findViewById(R.id.pluginDownloadButton).setOnClickListener(this);
		pluginStatus = findViewById(R.id.pluginDetectionResultsTextView);
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.pluginDetectButton:
				try {
					getPackageManager().getApplicationInfo("in.basulabs.shakealarmclockplugin", 0);

					// No exception thrown; plugin is available.
					Intent intent = new Intent();
					intent.setAction("in.basulabs.shakealarmclock.PLUGIN_ACTIVITY");
					startActivity(intent);

					pluginStatus.setText(R.string.plugin_available);
					isPluginAvailable = true;
				} catch (PackageManager.NameNotFoundException e) {
					// Exception thrown; plugin is not available.
					pluginStatus.setText(R.string.plugin_unavailable);
					isPluginAvailable = false;
				}
				break;
			case R.id.pluginDownloadButton:
				break;
		}
	}

	//-----------------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		if (isPluginAvailable){
			Intent intent = new Intent(this, Activity_AlarmsList.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(intent);
			finish();
		}
	}
}