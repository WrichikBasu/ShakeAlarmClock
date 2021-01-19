package in.basulabs.shakealarmclock;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

public class ViewModel_RingtonePicker extends ViewModel {

	private final MutableLiveData<Uri> defaultUri = new MutableLiveData<>();
	private final MutableLiveData<Uri> existingUri = new MutableLiveData<>();
	private final MutableLiveData<Uri> pickedUri = new MutableLiveData<>();

	private final MutableLiveData<Boolean> showDefault = new MutableLiveData<>();
	private final MutableLiveData<Boolean> showSilent = new MutableLiveData<>();
	private final MutableLiveData<Boolean> wasExistingUriGiven = new MutableLiveData<>();
	private final MutableLiveData<Boolean> playTone = new MutableLiveData<>();

	private final MutableLiveData<CharSequence> title = new MutableLiveData<>();

	private final MutableLiveData<ArrayList<Uri>> toneUriList = new MutableLiveData<>(new ArrayList<>());
	private final MutableLiveData<ArrayList<String>> toneNameList = new MutableLiveData<>(new ArrayList<>());
	private final MutableLiveData<ArrayList<Integer>> toneIdList = new MutableLiveData<>(new ArrayList<>());

	@Nullable
	public Uri getDefaultUri() {
		return defaultUri.getValue();
	}

	public void setDefaultUri(@Nullable Uri defaultUri) {
		this.defaultUri.setValue(defaultUri);
	}

	@Nullable
	public Uri getExistingUri() {
		return existingUri.getValue();
	}

	public void setExistingUri(@Nullable Uri existingUri) {
		this.existingUri.setValue(existingUri);
	}

	@Nullable
	public Uri getPickedUri() {
		return pickedUri.getValue();
	}

	public void setPickedUri(@Nullable Uri pickedUri) {
		this.pickedUri.setValue(pickedUri);
	}

	public boolean getShowDefault() {
		return showDefault.getValue();
	}

	public void setShowDefault(boolean showDefault) {
		this.showDefault.setValue(showDefault);
	}

	public boolean getShowSilent() {
		return showSilent.getValue();
	}

	public void setShowSilent(boolean showSilent) {
		this.showSilent.setValue(showSilent);
	}

	public boolean getWasExistingUriGiven() {
		return wasExistingUriGiven.getValue();
	}

	public void setWasExistingUriGiven(boolean wasExistingUriGiven) {
		this.wasExistingUriGiven.setValue(wasExistingUriGiven);
	}

	public boolean getPlayTone() {
		return playTone.getValue();
	}

	public void setPlayTone(boolean playTone) {
		this.playTone.setValue(playTone);
	}

	@NonNull
	public CharSequence getTitle() {
		return title.getValue();
	}

	public void setTitle(@NonNull CharSequence title) {
		this.title.setValue(title);
	}

	@NonNull
	public ArrayList<Uri> getToneUriList() {
		return toneUriList.getValue();
	}

	@NonNull
	public ArrayList<String> getToneNameList() {
		return toneNameList.getValue();
	}

	@NonNull
	public ArrayList<Integer> getToneIdList() {
		return toneIdList.getValue();
	}

}
