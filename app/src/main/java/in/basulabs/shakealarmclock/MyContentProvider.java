package in.basulabs.shakealarmclock;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class MyContentProvider extends ContentProvider {

	@Override
	public boolean onCreate() {
		return true;
	}

	@Nullable
	@Override
	public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
		return null;
	}

	@Nullable
	@Override
	public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
	                    @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		return null;
	}

	@Override
	public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
	                  @Nullable String[] selectionArgs) {
		return 0;
	}

	@Override
	public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
		return 0;
	}

	@Nullable
	@Override
	public String getType(@NonNull Uri uri) {
		return null;
	}
}
