# 儿童桌面

给孩子用的安卓桌面。设成系统默认桌面后，开机进入这里，桌面上只出现白名单里的应用。换回其他桌面、修改白名单，都要先输入保护密码。

最低系统版本是安卓 6（API 23）。

## 直接下载安装

可安装的正式包在仓库里的 `kids-launcher/dist/kids-desktop.apk`。版本 1.0.0，已经签名，手机下载后就能安装。

在 GitHub 上打开这个文件，点 Download：

https://github.com/tenson2zhang/kids-desktop-androd/blob/cursor/kids-desktop-launcher-c301/kids-launcher/dist/kids-desktop.apk

手机浏览器也可以直接下载：

https://github.com/tenson2zhang/kids-desktop-androd/raw/cursor/kids-desktop-launcher-c301/kids-launcher/dist/kids-desktop.apk

SHA-256：`4e0fe8885bccd822782982e1fb11e43151ae20f83e5fc2dce363897d8b322b79`

安装时如果系统询问，允许浏览器「安装未知应用」。装好后，应用名称是「儿童桌面」。从原来的桌面打开它，按提示设成默认桌面。

## 能做什么

- 作为系统桌面安装。首次打开会一步步要求：设置保护密码、选择白名单、设为默认桌面、打开防逃离。
- 家长从桌面的「家长」进入。连续输错 5 次会锁 60 秒。
- 白名单可以搜索、勾选、清空后再保存。设置、浏览器、应用商店、其他桌面会被标成不建议加入，家长仍然可以勾选。
- 「关机」会先弹出确认。确认后向系统发起关机；如果系统不允许普通应用关机，会提示改用电源键。
- 设为默认桌面并且打开无障碍里的「儿童桌面」之后，最近任务、通知栏、应用选择器，以及不在白名单里的应用会被拉回这个桌面。已经启用的输入法、紧急呼叫和来电界面会放行。
- 家长选择「切换系统桌面」后，大约 90 秒内不再拦截，方便到系统设置里更换默认桌面。更换完成后，本桌面不再把人拉回来。回到本应用窗口会立刻结束这 90 秒。

## 防逃离做不到的部分

普通应用不能从系统里删掉电源键，也不能禁止安全模式。这是故意留的出口：忘记密码时，关机后长按电源键进入安全模式，再清除本应用数据或卸载。

无障碍防护能挡住最近任务和非白名单应用。更严的锁定需要把本应用设成设备所有者（手机会暂时取消锁屏，开机直接进入儿童桌面）：

```bash
adb shell dpm set-device-owner com.kidsdesktop.launcher/.KidsDeviceAdminReceiver
```

执行前，这台手机上不能已经登录账号。

## 构建

需要 Android SDK，`compileSdk` 为 34。

```bash
export ANDROID_HOME="$HOME/android-sdk"
cd kids-launcher
./gradlew :core:test :app:assembleDebug
```

调试包在 `app/build/outputs/apk/debug/app-debug.apk`。

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

安装后从原来的桌面打开「儿童桌面」，按提示完成设置。
