package com.kidsdesktop.launcher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.kidsdesktop.launcher.databinding.ActivityHomeBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeActivity extends AppCompatActivity {
    private static final int REQUEST_PARENT = 31;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActivityHomeBinding binding;
    private AppGridAdapter adapter;
    private int loadGeneration;
    private boolean awaitingShutdown;

    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            renderClock();
            handler.postDelayed(this, 30_000L);
        }
    };

    private final Runnable shutdownCheck = new Runnable() {
        @Override
        public void run() {
            if (awaitingShutdown && hasWindowFocus() && !isFinishing()) {
                awaitingShutdown = false;
                showShutdownFailed();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        adapter = new AppGridAdapter();
        binding.appGrid.setAdapter(adapter);
        binding.appGrid.setEmptyView(binding.emptyView);
        binding.appGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launchApp(adapter.getItem(position));
            }
        });
        binding.appGrid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return true;
            }
        });
        binding.shutdownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmShutdown();
            }
        });
        binding.parentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(PasswordActivity.intent(HomeActivity.this, PasswordActivity.MODE_UNLOCK), REQUEST_PARENT);
            }
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 桌面不退出。
            }
        });
        if (!Prefs.isSetupComplete(this)) {
            startActivity(new Intent(this, SetupActivity.class));
        } else {
            maybeRequestNotifications();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SystemBars.hide(this);
        LauncherRoles.invalidate();
        EscapeWindow.clearWhenFocused(this);
        renderClock();
        handler.removeCallbacks(clockTick);
        handler.postDelayed(clockTick, 30_000L);
        renderBanner();
        loadApps();
        if (Prefs.isSetupComplete(this)) {
            WatchdogService.start(this);
            LockTaskController.apply(this);
            LockTaskController.start(this);
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(clockTick);
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            SystemBars.hide(this);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PARENT && resultCode == RESULT_OK) {
            startActivity(new Intent(this, ParentActivity.class));
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void maybeRequestNotifications() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    private void renderClock() {
        Date now = new Date();
        binding.clock.setText(new SimpleDateFormat("H:mm", Locale.CHINA).format(now));
        binding.greeting.setText(greeting() + "  ·  " + new SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(now));
    }

    private static String greeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 5) {
            return "夜深了";
        }
        if (hour < 11) {
            return "早上好";
        }
        if (hour < 14) {
            return "中午好";
        }
        if (hour < 18) {
            return "下午好";
        }
        return "晚上好";
    }

    private void renderBanner() {
        if (!Prefs.isSetupComplete(this)) {
            binding.guardBannerText.setVisibility(View.GONE);
            return;
        }
        boolean home = LauncherRoles.isDefaultHome(this);
        boolean guard = Access.isGuardEnabled(this);
        if (home && guard) {
            binding.guardBannerText.setVisibility(View.GONE);
            return;
        }
        StringBuilder message = new StringBuilder();
        if (!home) {
            message.append("还没有设成默认桌面，开机不会自动进入这里。");
        }
        if (!guard) {
            if (message.length() > 0) {
                message.append(" ");
            }
            message.append("防逃离还没打开，最近任务仍可能切到别的应用。");
        }
        message.append(" 请家长进入设置处理。");
        binding.guardBannerText.setText(message.toString());
        binding.guardBannerText.setVisibility(View.VISIBLE);
    }

    private void loadApps() {
        if (!Prefs.isSetupComplete(this)) {
            adapter.setApps(new ArrayList<AppEntry>());
            return;
        }
        final int generation = ++loadGeneration;
        final PackageManager pm = getPackageManager();
        final String own = getPackageName();
        final Set<String> allow = Prefs.whitelist(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<AppEntry> installed = InstalledApps.launcherApps(pm, own);
                final List<AppEntry> shown = new ArrayList<AppEntry>();
                for (AppEntry app : installed) {
                    if (allow.contains(app.packageName)) {
                        shown.add(app);
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (generation != loadGeneration || isFinishing()) {
                            return;
                        }
                        adapter.setApps(shown);
                    }
                });
            }
        }).start();
    }

    private void launchApp(AppEntry app) {
        if (app == null) {
            return;
        }
        Intent intent = getPackageManager().getLaunchIntentForPackage(app.packageName);
        if (intent == null) {
            Toast.makeText(this, "这个应用已经不能打开了", Toast.LENGTH_SHORT).show();
            loadApps();
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (RuntimeException ex) {
            Toast.makeText(this, "打不开这个应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmShutdown() {
        new AlertDialog.Builder(this)
                .setTitle("关机")
                .setMessage("要现在关机吗？关机后需要手动再开机。")
                .setNegativeButton("取消", null)
                .setPositiveButton("关机", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        doShutdown();
                    }
                })
                .show();
    }

    private void doShutdown() {
        awaitingShutdown = true;
        boolean sent = ShutdownHelper.requestShutdown(this);
        if (!sent) {
            awaitingShutdown = false;
            showShutdownFailed();
            return;
        }
        Toast.makeText(this, "正在关机…", Toast.LENGTH_SHORT).show();
        handler.removeCallbacks(shutdownCheck);
        handler.postDelayed(shutdownCheck, 1800L);
    }

    private void showShutdownFailed() {
        new AlertDialog.Builder(this)
                .setTitle("系统没有允许直接关机")
                .setMessage("安卓通常只让系统应用关机。请长按电源键，在弹出的菜单里选择关机。")
                .setPositiveButton("知道了", null)
                .show();
    }
}
