/*
Copyright (C) 2024  Wrichik Basu (basulabs.developer@gmail.com)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package in.basulabs.shakealarmclock.backend;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import in.basulabs.shakealarmclock.R;

public class PermissionAdapter extends
	RecyclerView.Adapter<PermissionAdapter.ViewHolder> {

	private final ArrayList<Permission> permList;
	private final PermissionAdapter.EventListener listener;
	private final Context context;

	public PermissionAdapter(@NonNull ArrayList<Permission> permList,
		@NonNull EventListener listener, @NonNull Context context) {
		this.permList = permList;
		this.listener = listener;
		this.context = context;
	}

	public interface EventListener {

		void onGrantBtnClick(Permission permission);

		void onDenyBtnClick(Permission permission);

	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View listItem = LayoutInflater.from(parent.getContext())
			.inflate(R.layout.recyclerviewrow_reqperm, parent, false);
		return new PermissionAdapter.ViewHolder(listItem);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

		Permission permission = permList.get(position);

		holder.permTitleTV.setText(permission.displayNameID());
		holder.permExpTV.setText(permission.permExplanationID());

		switch (permission.permType()) {
			case ConstantsAndStatics.PERMISSION_LEVEL_ESSENTIAL -> {
				holder.permTypeTV.setText(
					context.getString(R.string.perm_type_essential));
				holder.permTypeTV.setTextColor(Color.RED);
				holder.denyAccessBtn.setVisibility(View.GONE);
			}
			case ConstantsAndStatics.PERMISSION_LEVEL_RECOMMENDED -> {
				holder.permTypeTV.setText(context.getString(R.string.perm_type_recom));
				holder.permTypeTV.setTextColor(
					ConstantsAndStatics.isNightModeActive(context) ?
						Color.parseColor("#c32b7a73") : Color.parseColor("#FFC869FF"));
				holder.denyAccessBtn.setVisibility(View.VISIBLE);
			}
			case ConstantsAndStatics.PERMISSION_LEVEL_OPTIONAL -> {
				holder.permTypeTV.setText(context.getString(R.string.perm_type_optional));
				holder.permTypeTV.setTextColor(
					ConstantsAndStatics.isNightModeActive(context) ?
						Color.parseColor("#FFB4FF90") : Color.parseColor("#FF282099"));
				holder.denyAccessBtn.setVisibility(View.VISIBLE);
			}
		}

		holder.grantAccessBtn.setOnClickListener(
			view -> listener.onGrantBtnClick(permission));

		if (holder.denyAccessBtn.getVisibility() == View.VISIBLE) {
			holder.denyAccessBtn.setOnClickListener(
				view -> listener.onDenyBtnClick(permission));
		}

	}

	@Override
	public int getItemCount() {
		return permList.size();
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {

		public Button grantAccessBtn, denyAccessBtn;
		public TextView permTitleTV, permExpTV, permTypeTV;

		public ViewHolder(View view) {
			super(view);
			grantAccessBtn = view.findViewById(R.id.button_perm_grant_access);
			denyAccessBtn = view.findViewById(R.id.button_perm_deny_access);
			permTitleTV = view.findViewById(R.id.perm_title_textview);
			permExpTV = view.findViewById(R.id.perm_exp_textview);
			permTypeTV = view.findViewById(R.id.perm_type_textview);
		}
	}

}
