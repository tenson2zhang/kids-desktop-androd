package com.kidsdesktop.launcher;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.kidsdesktop.launcher.databinding.ActivityParentBinding;

public class ParentActivity extends AppCompatActivity {
    private ActivityParentBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Prefs.isUnlocked(this)) {
            finish();
            return;
        }
        binding = ActivityParentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(PasswordActivity.intent(ParentActivity.this, PasswordActivity.MODE_CHANGE));
            }
        });
        binding.whitelistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ParentActivity.this, WhitelistActivity.class));
            }
        });
        binding.switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmSwitch();
            }
        });
        binding.accessibilityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                leave(new Runnable() {
                    @Override
                    public void run() {
                        Access.openAccessibilitySettings(ParentActivity.this);
                    }
                });
            }
        });
        binding.usageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                leave(new Runnable() {
                    @Override
                    public void run() {
                        Access.openUsageAccess(ParentActivity.this);
                    }
                });
            }
        });
        binding.relockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LockTaskController.apply(ParentActivity.this);
                LockTaskController.start(ParentActivity.this);
                Toast.makeText(ParentActivity.this, "已重新锁定", Toast.LENGTH_SHORT).show();
            }
        });
        binding.doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.lock(ParentActivity.this);
                finish();
            }
        });
        binding.adbCommand.setText("adb shell dpm set-device-owner com.kidsdesktop.launcher/.KidsDeviceAdminReceiver");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding == null) {
            return;
        }
        if (!Prefs.isUnlocked(this)) {
            finish();
            return;
        }
        Prefs.unlock(this, Prefs.PARENT_UNLOCK_MS);
        EscapeWindow.clearWhenFocused(this);
        LauncherRoles.invalidate();
        render();
    }

    private void leave(Runnable action) {
        EscapeWindow.arm(this, Prefs.ESCAPE_MS);
        Prefs.unlock(this, Prefs.PARENT_UNLOCK_MS);
        LockTaskController.suspendForEscape(this);
        action.run();
    }

    private void confirmSwitch() {
        new AlertDialog.Builder(this)
                .setTitle("切换到其他桌面")
                .setMessage("接下来大约 90 秒内，儿童桌面不再拦截其他应用。请到系统设置里把默认桌面换成别的。换好之后，这个桌面就不会再把你拉回来。这段时间请不要把手机交给孩子。")
                .setNegativeButton("取消", null)
                .setPositiveButton("去切换", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        leave(new Runnable() {
                            @Override
                            public void run() {
                                LauncherRoles.openHomeSettings(ParentActivity.this);
                            }
                        });
                    }
                })
                .show();
    }

    private void render() {
        boolean home = LauncherRoles.isDefaultHome(this);
        boolean guard = Access.isGuardEnabled(this);
        boolean usage = Access.hasUsageAccess(this);
        boolean owner = LockTaskController.isDeviceOwner(this);
        binding.homeStatus.setText(home ? "默认桌面：已设置，开机后会进入这里" : "默认桌面：还不是，开机不会自动进入");
        binding.guardStatus.setText(guard ? "防逃离：已打开" : "防逃离：未打开，最近任务仍可能离开");
        binding.usageStatus.setText(usage ? "使用情况：已允许，用来补漏" : "使用情况：未允许，建议打开");
        binding.ownerStatus.setText(owner
                ? "设备所有者：已开启。这是最严的锁定，状态栏和最近任务会被系统禁用。锁屏也会暂时关掉，开机直接进入儿童桌面。"
                : "设备所有者：未开启。无障碍防护可以挡住最近任务。若要系统级锁定，用电脑连接手机后执行下面的命令。手机上不能已登录账号。");
        binding.whitelistCount.setText("白名单里有 " + Prefs.whitelist(this).size() + " 个应用");
        binding.relockButton.setVisibility(owner ? View.VISIBLE : View.GONE);
        binding.accessibilityButton.setText(guard ? "无障碍已打开" : "打开无障碍设置");
    }
}
