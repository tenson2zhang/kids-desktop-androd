package com.kidsdesktop.launcher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import androidx.core.content.ContextCompat;
import com.kidsdesktop.core.GuardPolicy;
import com.kidsdesktop.launcher.databinding.ItemCheckAppBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WhitelistAdapter extends BaseAdapter {
    interface Listener {
        void onWhitelistChanged();
    }

    private final Set<String> selected;
    private final Listener listener;
    private final List<AppEntry> apps = new ArrayList<AppEntry>();
    private LayoutInflater inflater;

    WhitelistAdapter(Set<String> selected, Listener listener) {
        this.selected = selected;
        this.listener = listener;
    }

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
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (inflater == null) {
            inflater = LayoutInflater.from(parent.getContext());
        }
        ItemCheckAppBinding binding;
        if (convertView == null) {
            binding = ItemCheckAppBinding.inflate(inflater, parent, false);
            convertView = binding.getRoot();
            convertView.setTag(binding);
        } else {
            binding = (ItemCheckAppBinding) convertView.getTag();
        }
        final AppEntry app = apps.get(position);
        binding.checkLabel.setText(app.label);
        boolean risky = GuardPolicy.isRisky(app.packageName, parent.getContext().getPackageName());
        binding.checkPackage.setText(risky ? "不建议加入  ·  " + app.packageName : app.packageName);
        binding.checkPackage.setTextColor(ContextCompat.getColor(
                parent.getContext(),
                risky ? R.color.coral : R.color.ink_soft));
        if (app.icon != null) {
            binding.checkIcon.setImageDrawable(app.icon);
        } else {
            binding.checkIcon.setImageResource(R.drawable.ic_app_fallback);
        }
        binding.checkBox.setChecked(selected.contains(app.packageName));
        binding.checkBox.setClickable(false);
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selected.contains(app.packageName)) {
                    selected.remove(app.packageName);
                } else {
                    selected.add(app.packageName);
                }
                binding.checkBox.setChecked(selected.contains(app.packageName));
                listener.onWhitelistChanged();
            }
        });
        return convertView;
    }
}
