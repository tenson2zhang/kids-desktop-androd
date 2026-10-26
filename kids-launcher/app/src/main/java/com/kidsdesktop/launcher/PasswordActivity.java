package com.kidsdesktop.launcher;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.kidsdesktop.core.AttemptLimiter;
import com.kidsdesktop.core.PasswordHasher;
import com.kidsdesktop.launcher.databinding.ActivityPasswordBinding;

public class PasswordActivity extends AppCompatActivity {
    public static final String MODE_UNLOCK = "unlock";
    public static final String MODE_SET = "set";
    public static final String MODE_CHANGE = "change";
    public static final String EXTRA_MODE = "mode";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActivityPasswordBinding binding;
    private String mode = MODE_UNLOCK;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            renderLock();
            handler.postDelayed(this, 500L);
        }
    };

    public static Intent intent(Context context, String mode) {
        return new Intent(context, PasswordActivity.class).putExtra(EXTRA_MODE, mode);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) {
            mode = MODE_UNLOCK;
        }
        if (MODE_SET.equals(mode) && Prefs.isSetupComplete(this)) {
            finish();
            return;
        }
        if (MODE_CHANGE.equals(mode) && !Prefs.isUnlocked(this)) {
            finish();
            return;
        }
        binding = ActivityPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.confirmPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submit();
                }
                return true;
            }
        });
        binding.password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE && binding.confirmPassword.getVisibility() != View.VISIBLE) {
                    submit();
                    return true;
                }
                return false;
            }
        });
        binding.submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
        renderMode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tick);
        EscapeWindow.clearWhenFocused(this);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(tick);
        super.onPause();
    }

    private void renderMode() {
        if (MODE_SET.equals(mode)) {
            binding.title.setText("设置保护密码");
            binding.message.setText("离开儿童桌面、修改白名单都要输入它。请至少 4 位，字母或数字都可以。");
            binding.oldPassword.setVisibility(View.GONE);
            binding.confirmPassword.setVisibility(View.VISIBLE);
            binding.submitButton.setText("保存密码");
            binding.password.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        } else if (MODE_CHANGE.equals(mode)) {
            binding.title.setText("修改保护密码");
            binding.message.setText("先输入现在的密码，再设置新密码。");
            binding.oldPassword.setVisibility(View.VISIBLE);
            binding.confirmPassword.setVisibility(View.VISIBLE);
            binding.submitButton.setText("保存新密码");
            binding.password.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        } else {
            binding.title.setText("家长验证");
            binding.message.setText("输入保护密码后才能改白名单，或切换到其他桌面。");
            binding.oldPassword.setVisibility(View.GONE);
            binding.confirmPassword.setVisibility(View.GONE);
            binding.submitButton.setText("进入");
            binding.password.setImeOptions(EditorInfo.IME_ACTION_DONE);
        }
    }

    private void renderLock() {
        if (binding == null) {
            return;
        }
        AttemptLimiter.State state = Prefs.attempts(this);
        long now = SystemClock.elapsedRealtime();
        if (!AttemptLimiter.isLocked(state, now)) {
            CharSequence current = binding.error.getText();
            if (current != null && current.toString().startsWith("试错次数太多")) {
                binding.error.setText("");
            }
            binding.submitButton.setEnabled(true);
            return;
        }
        long seconds = Math.max(1L, (state.lockUntilMillis - now + 999L) / 1000L);
        binding.error.setText("试错次数太多，请 " + seconds + " 秒后再试");
        binding.submitButton.setEnabled(false);
    }

    private void submit() {
        long now = SystemClock.elapsedRealtime();
        AttemptLimiter.State state = Prefs.attempts(this);
        if (AttemptLimiter.isLocked(state, now)) {
            renderLock();
            return;
        }
        if (MODE_SET.equals(mode)) {
            saveNew(binding.password.getText().toString(), binding.confirmPassword.getText().toString());
            return;
        }
        if (MODE_CHANGE.equals(mode)) {
            if (!Prefs.isUnlocked(this)) {
                finish();
                return;
            }
            if (!Prefs.verifyPassword(this, binding.oldPassword.getText().toString())) {
                fail(state, now, "现在的密码不对");
                return;
            }
            if (saveNew(binding.password.getText().toString(), binding.confirmPassword.getText().toString())) {
                Prefs.saveAttempts(this, AttemptLimiter.onSuccess());
                Toast.makeText(this, "保护密码已更新", Toast.LENGTH_SHORT).show();
                finish();
            }
            return;
        }
        if (Prefs.verifyPassword(this, binding.password.getText().toString())) {
            Prefs.saveAttempts(this, AttemptLimiter.onSuccess());
            Prefs.unlock(this, Prefs.PARENT_UNLOCK_MS);
            setResult(RESULT_OK);
            finish();
            return;
        }
        fail(state, now, "密码不正确");
    }

    private boolean saveNew(String password, String confirm) {
        if (!PasswordHasher.isAcceptable(password)) {
            binding.error.setText("密码至少 4 位，最多 32 位");
            return false;
        }
        if (!password.trim().equals(confirm.trim())) {
            binding.error.setText("两次输入不一致");
            return false;
        }
        Prefs.setPassword(this, password);
        binding.error.setText("");
        if (MODE_SET.equals(mode)) {
            setResult(RESULT_OK);
            finish();
        }
        return true;
    }

    private void fail(AttemptLimiter.State state, long now, String message) {
        AttemptLimiter.State next = AttemptLimiter.onFailure(state, now);
        Prefs.saveAttempts(this, next);
        if (AttemptLimiter.isLocked(next, now)) {
            renderLock();
            return;
        }
        int left = AttemptLimiter.MAX_FAILURES - next.failures;
        binding.error.setText(message + "，还可以试 " + left + " 次");
    }
}
