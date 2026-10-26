package com.kidsdesktop.launcher;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.kidsdesktop.launcher.databinding.ActivitySetupBinding;

public class SetupActivity extends AppCompatActivity {
    private static final int STEP_WELCOME = 0;
    private static final int STEP_PASSWORD = 1;
    private static final int STEP_APPS = 2;
    private static final int STEP_HOME = 3;
    private static final int STEP_GUARD = 4;

    private ActivitySetupBinding binding;
    private int step;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Prefs.isSetupComplete(this)) {
            finish();
            return;
        }
        binding = ActivitySetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (savedInstanceState != null) {
            step = savedInstanceState.getInt("step");
        }
        binding.primaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPrimary();
            }
        });
        binding.secondaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (step > 0) {
                    step--;
                }
                render();
            }
        });
        binding.extraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onExtra();
            }
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (step == 0) {
                    return;
                }
                step--;
                render();
            }
        });
        render();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("step", step);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LauncherRoles.invalidate();
        if (Prefs.isSetupComplete(this)) {
            finish();
            return;
        }
        if (binding != null) {
            render();
        }
    }

    private void onPrimary() {
        switch (step) {
            case STEP_WELCOME:
                step = STEP_PASSWORD;
                break;
            case STEP_PASSWORD:
                if (!Prefs.hasPassword(this)) {
                    startActivity(PasswordActivity.intent(this, PasswordActivity.MODE_SET));
                    return;
                }
                step = STEP_APPS;
                break;
            case STEP_APPS:
                step = STEP_HOME;
                break;
            case STEP_HOME:
                if (!LauncherRoles.isDefaultHome(this)) {
                    LauncherRoles.requestAsDefault(this);
                    return;
                }
                step = STEP_GUARD;
                break;
            case STEP_GUARD:
                if (!LauncherRoles.isDefaultHome(this)) {
                    LauncherRoles.requestAsDefault(this);
                    return;
                }
                if (!Access.isGuardEnabled(this)) {
                    Access.openAccessibilitySettings(this);
                    return;
                }
                Prefs.setSetupComplete(this, true);
                WatchdogService.start(this);
                finish();
                return;
            default:
                break;
        }
        render();
    }

    private void onExtra() {
        if (step == STEP_PASSWORD) {
            startActivity(PasswordActivity.intent(this, PasswordActivity.MODE_SET));
        } else if (step == STEP_APPS) {
            startActivity(new Intent(this, WhitelistActivity.class));
        } else if (step == STEP_GUARD) {
            Access.openUsageAccess(this);
        }
    }

    private void render() {
        boolean hasPassword = Prefs.hasPassword(this);
        int count = Prefs.whitelist(this).size();
        boolean isHome = LauncherRoles.isDefaultHome(this);
        boolean guard = Access.isGuardEnabled(this);
        boolean usage = Access.hasUsageAccess(this);
        binding.secondaryButton.setVisibility(step == STEP_WELCOME ? View.GONE : View.VISIBLE);
        binding.extraButton.setVisibility(View.GONE);
        binding.extraButton.setEnabled(true);
        switch (step) {
            case STEP_WELCOME:
                binding.progress.setText("开始之前");
                binding.title.setText("给孩子一个只剩允许应用的桌面");
                binding.body.setText("把它设成系统默认桌面后，开机就会进入这里。桌面上只显示白名单里的应用。要换回别的桌面，必须先输入保护密码。");
                binding.status.setText("适用于安卓 6 及以上。设置完成后，最近任务和非白名单应用会被拉回这个桌面。");
                binding.primaryButton.setText("开始设置");
                break;
            case STEP_PASSWORD:
                binding.progress.setText("第 1 步，共 4 步");
                binding.title.setText("先设置保护密码");
                binding.body.setText("这个密码用来修改白名单，以及切换到其他系统桌面。请家长自己记住。");
                binding.status.setText(hasPassword ? "保护密码已设置。" : "还没有设置保护密码。");
                binding.primaryButton.setText(hasPassword ? "下一步" : "设置保护密码");
                if (hasPassword) {
                    binding.extraButton.setVisibility(View.VISIBLE);
                    binding.extraButton.setText("重新设置密码");
                }
                break;
            case STEP_APPS:
                binding.progress.setText("第 2 步，共 4 步");
                binding.title.setText("选择孩子可以用的应用");
                binding.body.setText("只有勾选的应用会出现在儿童桌面上。设置、应用商店、浏览器和其他桌面不建议勾选。");
                binding.status.setText(count == 0
                        ? "当前白名单是空的，桌面上还不会出现应用。"
                        : "已加入 " + count + " 个应用。");
                binding.primaryButton.setText("下一步");
                binding.extraButton.setVisibility(View.VISIBLE);
                binding.extraButton.setText("编辑白名单");
                break;
            case STEP_HOME:
                binding.progress.setText("第 3 步，共 4 步");
                binding.title.setText("设为默认桌面");
                binding.body.setText("在系统弹出的列表里选择「儿童桌面」，并设为始终使用。这样开机后会自动进入。");
                binding.status.setText(isHome ? "已经是默认桌面。" : "还不是默认桌面。");
                binding.primaryButton.setText(isHome ? "下一步" : "去系统里设置");
                break;
            default:
                binding.progress.setText("第 4 步，共 4 步");
                binding.title.setText("打开防逃离");
                binding.body.setText("请在无障碍设置里打开「儿童桌面」。它只看前台窗口的应用包名，用来把最近任务和非白名单应用拉回来，不读取屏幕上的文字。");
                StringBuilder status = new StringBuilder();
                status.append(guard ? "防逃离已打开。" : "防逃离还没打开。");
                status.append(usage ? " 使用情况权限已允许，可作为补充。" : " 使用情况权限还没允许，可以稍后在家长设置里补开。");
                if (!isHome) {
                    status.append(" 默认桌面也还没设好。");
                }
                binding.status.setText(status.toString());
                if (!isHome) {
                    binding.primaryButton.setText("先设为默认桌面");
                } else if (!guard) {
                    binding.primaryButton.setText("打开无障碍设置");
                } else {
                    binding.primaryButton.setText("进入儿童桌面");
                }
                binding.extraButton.setVisibility(View.VISIBLE);
                binding.extraButton.setText(usage ? "使用情况已允许" : "允许查看使用情况");
                binding.extraButton.setEnabled(!usage);
                break;
        }
    }
}
