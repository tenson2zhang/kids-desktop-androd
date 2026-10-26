# 儿童桌面

给孩子用的安卓桌面。设成系统默认桌面后，开机进入这里，桌面上只出现白名单里的应用。换回其他桌面、修改白名单，都要先输入保护密码。

最低系统版本是安卓 6（API 23）。

## 在 GitHub 上一键生成安装包

Actions 工作流「构建安装包」会按当时的源代码打出已签名的 APK，并发布到 Releases。推送到 `main` 且改动了 `kids-launcher/` 时会自动跑；也可以随时手动点一次。

第一次使用前，到仓库的 Settings → Secrets and variables → Actions，新增两个 Secret。名字必须一致，值不要写进 Git：

- `ANDROID_KEYSTORE_BASE64`：签名库 `kids-desktop.jks` 的 Base64
- `ANDROID_KEYSTORE_PASSWORD`：签名库口令

别名默认是 `kidsdesktop`。密钥口令和库口令不同时，再加 `ANDROID_KEY_PASSWORD`；别名不是 `kidsdesktop` 时，再加 `ANDROID_KEY_ALIAS`。

然后：

1. 打开仓库的 Actions。
2. 左侧选择「构建安装包」。
3. 点 Run workflow，分支选 `main`，再点 Run workflow。
4. 绿色勾出现后，用手机浏览器打开固定地址下载。这个地址每次都会换成刚构建的包：

https://github.com/tenson2zhang/kids-desktop-androd/releases/download/apk/kids-desktop.apk

同一次运行的 Artifacts 里也有 `kids-desktop.apk`。安装时如果系统询问，允许浏览器「安装未知应用」。装好后应用名称是「儿童桌面」。

仓库里还留着一份已经打好的 `kids-launcher/dist/kids-desktop.apk`。Actions 第一次成功之前可以用它；之后以 Releases 里的新包为准。

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
