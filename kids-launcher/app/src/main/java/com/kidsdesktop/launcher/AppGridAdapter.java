package com.kidsdesktop.launcher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.kidsdesktop.launcher.databinding.ItemAppBinding;
import java.util.ArrayList;
import java.util.List;

public class AppGridAdapter extends BaseAdapter {
    private final List<AppEntry> apps = new ArrayList<AppEntry>();
    private LayoutInflater inflater;

    void setApps(List<AppEntry> next) {
        apps.clear();
        apps.addAll(next);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return apps.size();
    }

    @Override
    public AppEntry getItem(int position) {
        return apps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return apps.get(position).packageName.hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (inflater == null) {
            inflater = LayoutInflater.from(parent.getContext());
        }
        ItemAppBinding binding;
        if (convertView == null) {
            binding = ItemAppBinding.inflate(inflater, parent, false);
            convertView = binding.getRoot();
            convertView.setTag(binding);
        } else {
            binding = (ItemAppBinding) convertView.getTag();
        }
        AppEntry app = apps.get(position);
        binding.appLabel.setText(app.label);
        if (app.icon != null) {
            binding.appIcon.setImageDrawable(app.icon);
        } else {
            binding.appIcon.setImageResource(R.drawable.ic_app_fallback);
        }
        return convertView;
    }
}
