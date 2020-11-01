package in.basulabs.shakealarmclock;

import android.net.Uri;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

public class ViewModel_RingtonePicker extends ViewModel {

	private MutableLiveData<Uri> defaultUri = new MutableLiveData<>();
	private MutableLiveData<Uri> existingUri = new MutableLiveData<>();
	private MutableLiveData<Uri> pickedUri = new MutableLiveData<>();

	private MutableLiveData<Boolean> showDefault = new MutableLiveData<>();
	private MutableLiveData<Boolean> showSilent = new MutableLiveData<>();
	private MutableLiveData<Boolean> wasExistingUriGiven = new MutableLiveData<>();
	private MutableLiveData<Boolean> playTone = new MutableLiveData<>();

	private MutableLiveData<CharSequence> title = new MutableLiveData<>();

	private MutableLiveData<ArrayList<Uri>> toneUriList = new MutableLiveData<>(new ArrayList<>());
	private MutableLiveData<ArrayList<String>> toneNameList = new MutableLiveData<>(new ArrayList<>());
	private MutableLiveData<ArrayList<Integer>> toneIdList = new MutableLiveData<>(new ArrayList<>());

	public Uri getDefaultUri() {
		return defaultUri.getValue();
	}

	public void setDefaultUri(Uri defaultUri) {
		this.defaultUri.setValue(defaultUri);
	}

	public Uri getExistingUri() {
		return existingUri.getValue();
	}

	public void setExistingUri(Uri existingUri) {
		this.existingUri.setValue(existingUri);
	}

	public Uri getPickedUri() {
		return pickedUri.getValue();
	}

	public void setPickedUri(Uri pickedUri) {
		this.pickedUri.setValue(pickedUri);
	}

	public Boolean getShowDefault() {
		return showDefault.getValue();
	}

	public void setShowDefault(boolean showDefault) {
		this.showDefault.setValue(showDefault);
	}

	public Boolean getShowSilent() {
		return showSilent.getValue();
	}

	public void setShowSilent(boolean showSilent) {
		this.showSilent.setValue(showSilent);
	}

	public Boolean getWasExistingUriGiven() {
		return wasExistingUriGiven.getValue();
	}

	public void setWasExistingUriGiven(boolean wasExistingUriGiven) {
		this.wasExistingUriGiven.setValue(wasExistingUriGiven);
	}

	public Boolean getPlayTone() {
		return playTone.getValue();
	}

	public void setPlayTone(boolean playTone) {
		this.playTone.setValue(playTone);
	}

	public CharSequence getTitle() {
		return title.getValue();
	}

	public void setTitle(CharSequence title) {
		this.title.setValue(title);
	}

	public ArrayList<Uri> getToneUriList() {
		return toneUriList.getValue();
	}

	public void setToneUriList(ArrayList<Uri> toneUriList) {
		this.toneUriList.setValue(toneUriList);
	}

	public ArrayList<String> getToneNameList() {
		return toneNameList.getValue();
	}

	public void setToneNameList(ArrayList<String> toneNameList) {
		this.toneNameList.setValue(toneNameList);
	}

	public ArrayList<Integer> getToneIdList() {
		return toneIdList.getValue();
	}

	public void setToneIdList(ArrayList<Integer> toneIdList) {
		this.toneIdList.setValue(toneIdList);
	}

}
