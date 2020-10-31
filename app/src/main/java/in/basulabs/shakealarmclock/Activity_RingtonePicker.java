package in.basulabs.shakealarmclock;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.Objects;

import static android.media.RingtoneManager.ACTION_RINGTONE_PICKER;
import static android.media.RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI;
import static android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI;
import static android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI;
import static android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT;
import static android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT;
import static android.media.RingtoneManager.EXTRA_RINGTONE_TITLE;
import static android.media.RingtoneManager.EXTRA_RINGTONE_TYPE;
import static android.media.RingtoneManager.ID_COLUMN_INDEX;
import static android.media.RingtoneManager.TITLE_COLUMN_INDEX;
import static android.media.RingtoneManager.TYPE_ALL;
import static android.media.RingtoneManager.URI_COLUMN_INDEX;

public class Activity_RingtonePicker extends AppCompatActivity implements View.OnClickListener,
		AlertDialog_PermissionReason.DialogListener {

	private static Uri defaultUri, existingUri, pickedUri;
	private static boolean showDefault, showSilent, wasExistingUriGiven, playTone;
	private static CharSequence title;
	private static ArrayList<Uri> toneUriList;
	private static ArrayList<String> toneNameList;
	private static ArrayList<Integer> toneIdList;
	private AudioAttributes audioAttributes;

	private Bundle savedInstanceState;

	private MediaPlayer mediaPlayer;

	private RadioGroup radioGroup;

	private static final int DEFAULT_RADIO_BTN_ID = View.generateViewId(), SILENT_RADIO_BTN_ID = View
			.generateViewId();

	private static final int FILE_REQUEST_CODE = 4937, PERMISSIONS_REQUEST_CODE = 3720;

	private SharedPreferences sharedPreferences;

	//----------------------------------------------------------------------------------------------------

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ringtonepicker);

		this.savedInstanceState = savedInstanceState;

		setSupportActionBar(findViewById(R.id.toolbar4));
		Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

		radioGroup = findViewById(R.id.ringtonePickerRadioGroup);
		ConstraintLayout chooseToneLayout = findViewById(R.id.chooseCustomToneConstarintLayout);

		sharedPreferences = getSharedPreferences(ConstantsAndStatics.SHARED_PREF_FILE_NAME, MODE_PRIVATE);

		mediaPlayer = new MediaPlayer();
		audioAttributes = new AudioAttributes.Builder()
				.setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
				.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
				.build();

		if (savedInstanceState == null) {

			RingtoneManager ringtoneManager = new RingtoneManager(this);

			Intent intent = getIntent();

			Cursor allTonesCursor;

			toneNameList = new ArrayList<>();
			toneUriList = new ArrayList<>();
			toneIdList = new ArrayList<>();

			if (Objects.equals(getIntent().getAction(), ACTION_RINGTONE_PICKER)) {

				int type;
				if (intent.hasExtra(EXTRA_RINGTONE_TYPE)) {
					type = Objects.requireNonNull(intent.getExtras()).getInt(EXTRA_RINGTONE_TYPE);
				} else {
					type = TYPE_ALL;
				}
				ringtoneManager.setType(type);
				allTonesCursor = ringtoneManager.getCursor();

				Thread thread = new Thread(() -> {
					if (allTonesCursor.moveToFirst()) {
						do {
							int id = allTonesCursor.getInt(ID_COLUMN_INDEX);
							String uri = allTonesCursor.getString(URI_COLUMN_INDEX);

							toneUriList.add(Uri.parse(uri + "/" + id));
							toneNameList.add(allTonesCursor.getString(TITLE_COLUMN_INDEX));
							toneIdList.add(View.generateViewId());
						} while (allTonesCursor.moveToNext());
					}
				});
				thread.start();

				if (intent.hasExtra(EXTRA_RINGTONE_SHOW_DEFAULT)) {
					showDefault = Objects.requireNonNull(intent.getExtras()).getBoolean(EXTRA_RINGTONE_SHOW_DEFAULT);
				} else {
					showDefault = true;
				}

				if (intent.hasExtra(EXTRA_RINGTONE_SHOW_SILENT)) {
					showSilent = Objects.requireNonNull(intent.getExtras()).getBoolean(EXTRA_RINGTONE_SHOW_SILENT);
				} else {
					showSilent = false;
				}

				if (showDefault) {
					if (intent.hasExtra(EXTRA_RINGTONE_DEFAULT_URI)) {
						defaultUri = Objects.requireNonNull(intent.getExtras()).getParcelable(EXTRA_RINGTONE_DEFAULT_URI);
					} else {
						if (type == RingtoneManager.TYPE_ALARM) {
							defaultUri = Settings.System.DEFAULT_ALARM_ALERT_URI;
						} else if (type == RingtoneManager.TYPE_NOTIFICATION) {
							defaultUri = Settings.System.DEFAULT_NOTIFICATION_URI;
						} else if (type == RingtoneManager.TYPE_RINGTONE) {
							defaultUri = Settings.System.DEFAULT_RINGTONE_URI;
						} else {
							defaultUri = RingtoneManager.getActualDefaultRingtoneUri(this, type);
						}
					}
				} else {
					defaultUri = null;
				}

				if (intent.hasExtra(EXTRA_RINGTONE_EXISTING_URI)) {
					existingUri = Objects.requireNonNull(intent.getExtras()).getParcelable(EXTRA_RINGTONE_EXISTING_URI);
					wasExistingUriGiven = true;
				} else {
					existingUri = null;
					wasExistingUriGiven = false;
				}

				if (intent.hasExtra(EXTRA_RINGTONE_TITLE)) {
					title = (CharSequence) Objects.requireNonNull(intent.getExtras()).get(EXTRA_RINGTONE_TITLE);
				} else {
					title = "Select tone:";
				}

				if (intent.hasExtra(ConstantsAndStatics.EXTRA_PLAY_RINGTONE)) {
					playTone = Objects.requireNonNull(intent.getExtras())
							.getBoolean(ConstantsAndStatics.EXTRA_PLAY_RINGTONE);
				} else {
					playTone = true;
				}

				try {
					thread.join();
				} catch (InterruptedException ignored) {
				}

			}
		}
		populateRadioGroup();
		chooseToneLayout.setOnClickListener(this);
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_ringtonepicker_menu, menu);

		// Get the action view used in your playTone item
		MenuItem toggleservice = menu.findItem(R.id.playTone);
		SwitchCompat actionView = (SwitchCompat) toggleservice.getActionView();
		actionView.setChecked(playTone);
		actionView.setOnCheckedChangeListener((buttonView, isChecked) -> {
			playTone = isChecked;
			if (! isChecked) {
				try {
					mediaPlayer.stop();
				} catch (IllegalStateException ignored) {
				}
			}
		});
		return super.onCreateOptionsMenu(menu);
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		if (! isPermissionAvailable()) {
			checkAndRequestPermission();
		}
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	protected void onPause() {
		super.onPause();
		try {
			mediaPlayer.stop();
		} catch (Exception ignored) {
		}
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Populate {@link #radioGroup} by creating and adding appropriate {@link RadioButton}.
	 */
	private void populateRadioGroup() {

		Objects.requireNonNull(getSupportActionBar()).setTitle(title);

		if (showDefault) {
			createOneRadioButton(DEFAULT_RADIO_BTN_ID, getResources().getString(R.string.defaultTone));
		}

		if (showSilent) {
			createOneRadioButton(SILENT_RADIO_BTN_ID, getResources().getString(R.string.silentTone));
		}

		for (int i = 0; i < toneIdList.size(); i++) {
			createOneRadioButton(toneIdList.get(i), toneNameList.get(i));
		}

		if (existingUri != null) {

			////////////////////////////////////////////////////////////////////
			// As existingUri is not null, we are required to pre-select
			// a specific RadioButton.
			///////////////////////////////////////////////////////////////////

			if (showDefault && existingUri.equals(defaultUri)) {

				///////////////////////////////////////////////////////////////////////////
				// The existingUri is same as defaultUri, and showDefault is true.
				// So, we check the "Default" RadioButton.
				//////////////////////////////////////////////////////////////////////////
				((RadioButton) findViewById(DEFAULT_RADIO_BTN_ID)).setChecked(true);
				setPickedUri(defaultUri);

			} else {

				// Find index of existingUri in toneUriList
				int index = toneUriList.indexOf(existingUri);

				if (index != - 1) {

					// toneUriList has existingUri. Check the corresponding RadioButton.
					((RadioButton) findViewById(toneIdList.get(index))).setChecked(true);
					setPickedUri(existingUri);

				} else {
					///////////////////////////////////////////////////////////////////////
					// toneUriList does NOT have existingUri. It is a custom Uri
					// provided to us. We have to first check whether the file exists
					// or not. If it exists, we shall add that file to our RadioGroup.
					// If the file does not exist, we do not select any Radiogroup.
					///////////////////////////////////////////////////////////////////////
					try (Cursor cursor = getContentResolver()
							.query(existingUri, null, null, null, null)) {

						if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
							// existingUri is a valid Uri.

							String fileNameWithExt;
							int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
							if (columnIndex != - 1) {
								fileNameWithExt = cursor.getString(columnIndex);
							} else {
								fileNameWithExt = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
							}

							int toneId = View.generateViewId();

							toneNameList.add(fileNameWithExt);
							toneUriList.add(existingUri);
							toneIdList.add(toneId);

							createOneRadioButton(toneId, fileNameWithExt);

							((RadioButton) findViewById(toneId)).setChecked(true);

							setPickedUri(existingUri);

						}
					}
				}
			}
		} else {

			if (wasExistingUriGiven) {
				//////////////////////////////////////////////////////////////////////////
				// existingUri was specifically passed as a null value. If showSilent
				// is true, we pre-select the "Silent" RadioButton. Otherwise
				// we do not select any specific RadioButton.
				/////////////////////////////////////////////////////////////////////////
				if (showSilent) {
					((RadioButton) findViewById(SILENT_RADIO_BTN_ID)).setChecked(true);
				}
			}
			setPickedUri(null);
		}
	}

	//----------------------------------------------------------------------------------------------------

	private void setPickedUri(@Nullable Uri newUri) {
		if (savedInstanceState == null) {
			pickedUri = newUri;
		}
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Creates one {@link RadioButton} and adds it to {@link #radioGroup}.
	 *
	 * @param id The id to be assigned to the {@link RadioButton}.
	 * @param text The text to be set in the {@link RadioButton}.
	 */
	private void createOneRadioButton(int id, String text) {
		RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		params.setMargins(5, 24, 5, 24);

		RadioButton radioButton = new RadioButton(this);
		radioButton.setId(id);
		radioButton.setTextColor(getResources().getColor(R.color.defaultLabelColor));
		radioButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
		radioButton.setLayoutParams(params);
		radioButton.setText(text);
		radioButton.setOnClickListener(this);
		radioGroup.addView(radioButton);
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			this.onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onClick(View view) {
		if (view.getId() == DEFAULT_RADIO_BTN_ID) {
			pickedUri = defaultUri;
			playChosenTone();
		} else if (view.getId() == SILENT_RADIO_BTN_ID) {
			pickedUri = null;
		} else if (view.getId() == R.id.chooseCustomToneConstarintLayout) {
			openFileBrowser();
		} else {
			pickedUri = toneUriList.get(toneIdList.indexOf(view.getId()));
			playChosenTone();
		}
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {

		if (pickedUri == null) {
			if (showSilent) {
				Intent intent = new Intent().putExtra(EXTRA_RINGTONE_PICKED_URI, pickedUri);
				setResult(RESULT_OK, intent);
			} else {
				setResult(RESULT_CANCELED);
			}
		} else {
			Intent intent = new Intent().putExtra(EXTRA_RINGTONE_PICKED_URI, pickedUri);
			setResult(RESULT_OK, intent);
		}
		finish();
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Fires an implicit Intent to open a file browser and let the user choose an alarm tone.
	 */
	private void openFileBrowser() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
		intent.setType("*/*");
		String[] mimeTypes = new String[]{"audio/mpeg", "audio/ogg", "audio/aac", "audio/x-matroska"};
		intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
		startActivityForResult(intent, FILE_REQUEST_CODE);
	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Checks whether {@link Manifest.permission#READ_EXTERNAL_STORAGE} permission is available or not.
	 *
	 * @return {@code true} if the permission is available, otherwise {@code false}.
	 */
	private boolean isPermissionAvailable() {
		return ContextCompat.checkSelfPermission(this,
				Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
	}

	//--------------------------------------------------------------------------------------------------

	private void checkAndRequestPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
				/////////////////////////////////////////////////////////////////
				//User has denied the permission once or more than once, but
				// never clicked on "Don't ask again" before denying.
				////////////////////////////////////////////////////////////////
				showPermissionExplanationDialog();
			} else {
				////////////////////////////////////////////////////////////////
				// Two possibilities:
				// 1. We are asking for the permission the first time.
				// 2. User has clicked on "Don't ask again".
				///////////////////////////////////////////////////////////////
				if (! sharedPreferences.getBoolean(ConstantsAndStatics.SHARED_PREF_KEY_PERMISSION_WAS_ASKED_BEFORE, false)) {
					// Permission was never asked before.
					sharedPreferences.edit()
							.remove(ConstantsAndStatics.SHARED_PREF_KEY_PERMISSION_WAS_ASKED_BEFORE)
							.putBoolean(ConstantsAndStatics.SHARED_PREF_KEY_PERMISSION_WAS_ASKED_BEFORE, true)
							.commit();

					ActivityCompat.requestPermissions(this,
							new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);

				} else {
					////////////////////////////////////////////////////////////////////////////////
					// User had chosen "Don't ask again".
					////////////////////////////////////////////////////////////////////////////////
					showPermissionExplanationDialog();
				}
			}
		}
	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Shows an {@code AlertDialog} explaining why the permission is necessary.
	 */
	private void showPermissionExplanationDialog() {
		DialogFragment dialogPermissionReason =
				new AlertDialog_PermissionReason(
						getResources().getString(R.string.permissionReasonExp_ringtonePicker));
		dialogPermissionReason.setCancelable(false);
		dialogPermissionReason.show(getSupportFragmentManager(), "");
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
	                                       @NonNull int[] grantResults) {
		if (requestCode == PERMISSIONS_REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
			}
		}
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == FILE_REQUEST_CODE) {

			if (resultCode == RESULT_OK && data != null) {

				Uri toneUri = data.getData();
				assert toneUri != null;

				try (Cursor cursor = getContentResolver().query(toneUri, null, null, null, null)) {

					if (cursor != null) {

						if (toneUriList.contains(toneUri)) {

							int index = toneUriList.indexOf(toneUri);
							((RadioButton) findViewById(toneIdList.get(index))).setChecked(true);

						} else {

							int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
							cursor.moveToFirst();

							String fileNameWithExt = cursor.getString(nameIndex);
							String fileNameWithoutExt = fileNameWithExt.substring(0, fileNameWithExt.indexOf("."));
							int toneId = View.generateViewId();

							toneNameList.add(fileNameWithoutExt);
							toneUriList.add(toneUri);
							toneIdList.add(toneId);

							createOneRadioButton(toneId, fileNameWithoutExt);

							((RadioButton) findViewById(toneId)).setChecked(true);
						}
						pickedUri = toneUri;
						playChosenTone();
					}
				}
			}
		}
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onDialogPositiveClick(DialogFragment dialogFragment) {
		if (dialogFragment.getClass().equals(AlertDialog_PermissionReason.class)) {

			if ((! ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.READ_EXTERNAL_STORAGE)) && (sharedPreferences
					.getBoolean(ConstantsAndStatics.SHARED_PREF_KEY_PERMISSION_WAS_ASKED_BEFORE, false))) {

				////////////////////////////////////////////////////////////////////////////////
				// User had chosen "Don't ask again". We need to redirect the user to the
				// Settings app to obtain the permission.
				////////////////////////////////////////////////////////////////////////////////
				Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
				Uri uri = Uri.fromParts("package", getPackageName(), null);
				intent.setData(uri);
				startActivity(intent);

			} else {
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
			}
		}
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onDialogNegativeClick(DialogFragment dialogFragment) {
		if (dialogFragment.getClass().equals(AlertDialog_PermissionReason.class)) {
			Toast.makeText(this, "Operation not possible without the permission.", Toast.LENGTH_LONG).show();
			onBackPressed();
		}
	}

	//----------------------------------------------------------------------------------------------------

	private void playChosenTone() {
		if (pickedUri != null && playTone) {
			try {
				mediaPlayer.reset();
				mediaPlayer.setDataSource(this, pickedUri);
				mediaPlayer.setLooping(false);
				mediaPlayer.setAudioAttributes(audioAttributes);
				mediaPlayer.prepare();
				mediaPlayer.start();
			} catch (Exception ignored) {
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			mediaPlayer.release();
		} catch (Exception ignored) {
		}
	}

}
