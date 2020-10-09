package in.basulabs.shakealarmclock;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

public class Convertors {

	@TypeConverter
	public static Uri stringToUri(@NonNull String str){
		return Uri.parse(str);
	}

	@TypeConverter
	public static String uriToString(@NonNull Uri uri){
		return uri.toString();
	}


}
