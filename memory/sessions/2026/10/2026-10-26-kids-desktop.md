---
date: 2026-10-26
title: 开发可设为默认桌面的儿童桌面
session: 2026-10-26-kids-desktop
agents:
  - cursor
models:
  - grok-4.7
---

## 2026-10-26 05:40:14.110 UTC

### 用户

帮我开发一个儿童桌面app，要求如下：
1. 支持安卓6及以上。
2. 是一个系统桌面，可用被安卓系统设置为默认桌面，安装开机后自动进入默认桌面。
3. 可以从这个桌面切换到其他系统桌面，但要输入保护密码才能切换，这个桌面有地方设置这个保护密码。
4. 这个桌面可以舔加应用白名单，只要被调加了白名单的应用才会在这个桌面上显示（目的是限制儿童可以使用的app范围）。白名单可以修改。
5. 这个桌面有直接关机功能（需要用户再次确认）。
6.进行这个桌面后，用户无法通过其他途径逃离这个桌面切换到非白名单应用。（例如：切换任务切换到其他应用）。

## 2026-10-26 05:40:14.110 UTC

### 思考

这是家长自己安装的儿童桌面，用来限制孩子能打开的应用。按安卓桌面来做：minSdk 23，HomeActivity 同时响应 HOME 和 LAUNCHER。开机进入默认桌面靠系统的 HOME 启动，再加 BOOT_COMPLETED。保护密码只存盐和 SHA-256 摘要，连续错 5 次锁 60 秒。白名单存在 SharedPreferences，桌面只显示勾选的可启动应用。关机先确认，再尝试系统关机接口，失败就说明改用电源键，不走 root。防逃离在本应用是默认桌面时，用无障碍看前台包名和窗口类名，把最近任务、通知栏、选择器和非白名单应用拉回桌面；输入法、紧急呼叫和来电放行。家长切换桌面时给大约 90 秒放行，并打开系统桌面设置；换掉默认桌面后不再拉回。设备所有者锁定任务作为可选的更严模式。策略放在 core 模块里用 JUnit 测。模拟器若起不来，就用测试和 APK 清单证明。

## 2026-10-26 06:03:04.115 UTC

### 工具

名称：Shell

参数：export ANDROID_HOME="$HOME/android-sdk"; cd /workspace/kids-launcher; ./gradlew :core:test :app:assembleDebug --no-daemon

结果：BUILD SUCCESSFUL。AttemptLimiterTest 3 项、GuardPolicyTest 14 项、PasswordHasherTest 4 项全部通过，失败 0。调试包 app/build/outputs/apk/debug/app-debug.apk。

## 2026-10-26 06:03:04.115 UTC

### 工具

名称：Shell

参数：aapt dump badging 与 dump xmltree，检查已安装的调试包

结果：package name=com.kidsdesktop.launcher versionName=1.0.0，sdkVersion 23，targetSdkVersion 34，application-label 儿童桌面。清单里有 android.intent.category.HOME、android.intent.category.LAUNCHER、无障碍服务和 BOOT_COMPLETED。minSdkVersion=0x17。模拟器 qemu 进程监听 5555，但 adb 一直显示 emulator-5554 offline，sys.boot_completed 读不到，界面点击没有做成。

## 2026-10-26 06:03:04.115 UTC

### 代理

儿童桌面已经放在 `kids-launcher/`，最低支持安卓 6（API 23），可以让系统把它设成默认桌面。

桌面本身：`HomeActivity` 同时响应 HOME 和 LAUNCHER。第一次打开会设置保护密码、白名单、默认桌面和无障碍防逃离。设成默认桌面后，开机由系统进入这里，开机广播会再拉起一次。

保护密码：从桌面的「家长」进入。密码只存盐和摘要。连续错 5 次锁 60 秒。修改白名单和切换到其他系统桌面都要先通过。切换时会再确认，然后打开系统桌面设置，大约 90 秒内不再拦截。换掉默认桌面后不再拉回。回到本应用会立刻结束这 90 秒。

白名单：只有勾上的应用出现在桌面上。设置、浏览器、商店和其他桌面会标成不建议加入，家长仍然可以勾。可以搜索、清空后再保存。

关机：「关机」会先确认。确认后向系统发起关机。如果系统不允许普通应用关机，会提示改用电源键。

防逃离：设为默认桌面并打开无障碍里的「儿童桌面」之后，最近任务、通知栏、应用选择器和非白名单应用会被拉回桌面。输入法、紧急呼叫和来电界面会放行。使用情况权限是补充。用电脑设成设备所有者后，还会启用系统锁定任务。忘记密码可以进安全模式再清除数据。普通应用无法禁止安全模式。

验证：策略单元测试 21 项通过。调试包已编出，最低版本 API 23，并注册了桌面和启动入口。这台环境的模拟器一直离线，没有在界面上点过。

构建：export ANDROID_HOME="$HOME/android-sdk"；cd kids-launcher；./gradlew :core:test :app:assembleDebug；adb install -r app/build/outputs/apk/debug/app-debug.apk。安装后从原来的桌面打开「儿童桌面」。
