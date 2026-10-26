package com.kidsdesktop.launcher;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.kidsdesktop.launcher.databinding.ActivityWhitelistBinding;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class WhitelistActivity extends AppCompatActivity implements WhitelistAdapter.Listener {
    private ActivityWhitelistBinding binding;
    private final Set<String> selected = new HashSet<String>();
    private final List<AppEntry> all = new ArrayList<AppEntry>();
    private WhitelistAdapter adapter;
    private int generation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Prefs.isSetupComplete(this) && !Prefs.isUnlocked(this)) {
            finish();
            return;
        }
        binding = ActivityWhitelistBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        adapter = new WhitelistAdapter(selected, this);
        binding.appList.setAdapter(adapter);
        selected.addAll(Prefs.whitelist(this));
        binding.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmClear();
            }
        });
        binding.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
        load();
        renderCount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding == null) {
            return;
        }
        if (Prefs.isSetupComplete(this) && !Prefs.isUnlocked(this)) {
            finish();
            return;
        }
        if (Prefs.isSetupComplete(this)) {
            Prefs.unlock(this, Prefs.PARENT_UNLOCK_MS);
        }
        EscapeWindow.clearWhenFocused(this);
    }

    private void load() {
        final int ticket = ++generation;
        final PackageManager pm = getPackageManager();
        final String own = getPackageName();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<AppEntry> apps = InstalledApps.launcherApps(pm, own);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (ticket != generation || isFinishing()) {
                            return;
                        }
                        all.clear();
                        all.addAll(apps);
                        applyFilter(binding.search.getText().toString());
                    }
                });
            }
        }).start();
    }

    private void applyFilter(String query) {
        String keyword = query == null ? "" : query.trim().toLowerCase(Locale.CHINA);
        List<AppEntry> shown = new ArrayList<AppEntry>();
        for (AppEntry app : all) {
            if (keyword.length() == 0
                    || app.label.toLowerCase(Locale.CHINA).contains(keyword)
                    || app.packageName.toLowerCase(Locale.US).contains(keyword)) {
                shown.add(app);
            }
        }
        adapter.setApps(shown);
        renderCount();
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("清空白名单")
                .setMessage("清空后，儿童桌面上不会留下任何应用。确定后还要再按保存才会生效。")
                .setNegativeButton("取消", null)
                .setPositiveButton("清空", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selected.clear();
                        adapter.notifyDataSetChanged();
                        renderCount();
                    }
                })
                .show();
    }

    private void save() {
        Prefs.setWhitelist(this, selected);
        LockTaskController.apply(this);
        Toast.makeText(this, "白名单已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onWhitelistChanged() {
        renderCount();
    }

    private void renderCount() {
        binding.selectedCount.setText("已选择 " + selected.size() + " 个");
    }
}
