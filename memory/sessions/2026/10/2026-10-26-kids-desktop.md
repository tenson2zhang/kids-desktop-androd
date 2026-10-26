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

## 2026-10-26 06:17:27.904 UTC

### 用户

我不但需要你帮我生成代码，还需要你帮我弄成可以直接在这个github仓库里面下载的可以安装的最终apk包，这样我直接下载这个apk安装包就可以用了。

## 2026-10-26 06:17:27.904 UTC

### 思考

正式包必须签名，否则手机不能安装。用发布签名打出 release APK，放到 kids-launcher/dist/kids-desktop.apk，并让 git 忽略签名密钥、只提交这个安装包。v1 和 v2 签名都要成立，安卓 6 靠 v1。下载地址写进 README。

## 2026-10-26 06:18:54.701 UTC

### 工具

名称：Shell

参数：生成发布密钥库（口令已打码，未写入 Git），执行 ./gradlew :app:assembleRelease，把 app-release.apk 复制为 dist/kids-desktop.apk，再用 apksigner verify 和 aapt dump badging 检查。

结果：BUILD SUCCESSFUL。apksigner：Verified using v1 scheme true，v2 scheme true，signers 1。badging：package com.kidsdesktop.launcher versionName 1.0.0，sdkVersion 23，targetSdkVersion 34，application-label 儿童桌面，没有 debuggable。文件 2.5M。SHA-256 4e0fe8885bccd822782982e1fb11e43151ae20f83e5fc2dce363897d8b322b79。keystore.properties 与 jks 被 gitignore 排除。dist/kids-desktop.apk 可以加入 Git。

## 2026-10-26 06:18:54.701 UTC

### 代理

可安装的正式包已经放进仓库：kids-launcher/dist/kids-desktop.apk。版本 1.0.0，已用 v1 和 v2 签名，安卓 6 及以上可以安装。手机浏览器打开 https://github.com/tenson2zhang/kids-desktop-androd/raw/cursor/kids-desktop-launcher-c301/kids-launcher/dist/kids-desktop.apk 即可下载。安装时允许浏览器安装未知应用。装好后应用名是「儿童桌面」。SHA-256 是 4e0fe8885bccd822782982e1fb11e43151ae20f83e5fc2dce363897d8b322b79。


## 2026-10-26 06:34:07.700 UTC

### 用户

我不希望每次都让你帮我生成apk包，而是希望你帮我利用github的功能（例如：github actions之类）。让我在github上一键就可以根据最新源代码生成最新的apk安装包。

## 2026-10-26 06:34:07.700 UTC

### 思考

用户要自己在 GitHub 上按最新源代码出安装包。用 workflow_dispatch 做一键，main 上改了 kids-launcher 时也自动构建。签名密钥放在 Actions Secrets，不进 Git。APK 发到固定 Release 标签 apk，下载地址不变。继续用已经发布的那把密钥，否则覆盖安装会失败。本环境不能替用户写入仓库 Secret。

## 2026-10-26 06:36:29.869 UTC

### 工具

名称：Write、StrReplace

参数：新增 .github/workflows/build-apk.yml。更新 kids-launcher/README.md 和 README.md。在 kids-launcher/app/build.gradle 的 release 签名里显式打开 v1SigningEnabled 和 v2SigningEnabled。

结果：工作流名称是「构建安装包」。可以手动 Run workflow；推送到 main 且改动 kids-launcher/** 或该工作流文件时也会运行。缺少 ANDROID_KEYSTORE_BASE64 或 ANDROID_KEYSTORE_PASSWORD 时失败，并提示到 Settings → Secrets and variables → Actions 添加。成功后把 kids-desktop.apk 发到标签 apk。密钥文件仍被 gitignore。

## 2026-10-26 06:36:29.869 UTC

### 工具

名称：Shell

参数：查看分支与工作区；用 PyYAML 解析工作流；用本地密钥库做 Base64 往返（口令和 Base64 已打码）；keytool -list。

结果：当前分支 cursor/actions-apk-c301，基于 origin/main 的 d3332a4。工作流有 8 个步骤，Java 17，contents 为 write。密钥别名 kidsdesktop，库口令与密钥口令相同。Base64 往返后字节一致。缺少 Secret 时脚本退出码 1。

## 2026-10-26 06:38:59.340 UTC

### 工具

名称：Shell

参数：在 kids-launcher 执行 ./gradlew :core:test :app:assembleRelease --no-daemon，再用 apksigner verify --verbose 检查 app-release.apk。

结果：BUILD SUCCESSFUL。apksigner：v1 scheme true，v2 scheme true，v3 与 v4 false，signers 1。证书 SHA-256 为 e302a16f288a6f396d45d50f3c562d75cc58a519ea05e15e8d2620c936901b14。keystore.properties 与 jks 仍被 gitignore，没有进入待提交列表。

## 2026-10-26 06:38:59.340 UTC

### 代理

可以在 GitHub 上自己生成安装包。合并到 main 之后，打开仓库的 Actions，左侧选「构建安装包」，点 Run workflow，分支选 main，再点一次 Run workflow。改动 kids-launcher 并推到 main 时也会自动构建。

第一次运行前，到 Settings → Secrets and variables → Actions 添加两个 Secret，不要写进 Git：

- ANDROID_KEYSTORE_BASE64：***
- ANDROID_KEYSTORE_PASSWORD：***

别名是 kidsdesktop，库口令和密钥口令相同，不必再加 ANDROID_KEY_ALIAS 或 ANDROID_KEY_PASSWORD。必须用这一把已经发布过的密钥，才能覆盖安装现有的「儿童桌面」。换一把密钥就只能卸载重装，保护密码会一起清掉。

成功后用手机浏览器打开 https://github.com/tenson2zhang/kids-desktop-androd/releases/download/apk/kids-desktop.apk 。同一次运行的 Artifacts 里也有 kids-desktop.apk。第一次成功之前，这个地址还没有文件，可以先装仓库里的 kids-launcher/dist/kids-desktop.apk。

本地已跑通 :core:test 和 :app:assembleRelease，正式包 v1、v2 签名都成立。GitHub 上的第一次运行要等 Secret 配好才会成功。

聊天回复里另给了 ANDROID_KEYSTORE_BASE64 和 ANDROID_KEYSTORE_PASSWORD 的实际值，此处打码，没有写入 Git。

## 2026-10-26 06:40:15.796 UTC

### 工具

名称：Shell

参数：git add 工作流、两份 README、app/build.gradle 和本会话记录；git commit；git push -u origin cursor/actions-apk-c301。

结果：提交 6ef1b3d，标题「[Cursor/grok-4.7] 用 GitHub Actions 一键构建安装包」。远程新建分支 cursor/actions-apk-c301。keystore 与 keystore.properties 未纳入提交。

## 2026-10-26 06:40:15.796 UTC

### 工具

名称：ManagePullRequest

参数：action create_pr，branch_name cursor/actions-apk-c301，base_branch main，title 用 GitHub Actions 一键构建安装包。

结果：已创建 https://github.com/tenson2zhang/kids-desktop-androd/pull/3 。
