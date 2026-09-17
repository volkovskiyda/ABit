# Installing ABit

Every build comes from one place: the [Releases page](https://github.com/volkovskiyda/ABit/releases).
One release carries all four apps, built from the same commit, so a phone and a watch taken from the
same release are guaranteed to agree about schedules and chime boundaries.

Asset names carry the version `V` — the tag without its `v`, plus the commit count, so `v1.0` cut at
commit 68 ships as `1.0.68`. Pick the assets for the platforms you want:

| You have | Download |
|---|---|
| An Android phone | `abit-V.apk` |
| A Wear OS watch | `abit-wear-V.apk` |
| A Mac (Apple Silicon) | `ABit-V.dmg` |
| A browser | nothing — <https://abit-kmp.web.app> |

**None of the installed apps update themselves.** ABit is on no app store, so an upgrade is you
downloading the newer asset and installing it over the old one. (The web app is the exception — a
reload is its update.) Every app says which build it is, so you can tell whether you are behind:
under **ABOUT** at the foot of Settings on the phone and in the browser, in the popover's footer on
the Mac, and under the **Schedules** button on the watch.

## Android

1. Download `abit-V.apk` on the phone.
2. Open it. Android asks once whether the browser or file manager may install apps; allow it, then
   confirm the install.
3. Play Protect may warn that the app was not scanned. It is a self-signed build outside the Play
   Store — that warning is what that means, not a finding about the app.

Debug builds carry the `.debug` application id, so a build you compiled yourself and a release from
here can sit side by side without uninstalling either.

## Wear OS

A Wear OS watch has no file manager, no browser download, and no "install unknown apps" switch. The
only way to put an app on one without Google Play is `adb`, over Wi-Fi, from a computer. There is no
shortcut being withheld here; that is the whole list.

You need the Android SDK platform tools on the computer. With Android Studio installed, `adb` is at
`~/Library/Android/sdk/platform-tools/adb` on a Mac. Without it:

```sh
brew install --cask android-platform-tools
```

### Turn on wireless debugging

1. On the watch, open **Settings** and find the build number:
   - Pixel Watch and most others: **System → About → Versions → Build number**.
   - Galaxy Watch: **About watch → Software information → Software version**.
2. Tap it **seven times**, until the watch says developer mode is on.
3. Go back to **Settings → Developer options** and turn on **ADB debugging**, then **Wireless
   debugging**.
4. The watch must be on the same Wi-Fi network as the computer. A watch paired to a phone over
   Bluetooth often parks its Wi-Fi radio to save battery; opening the Wireless debugging screen is
   what wakes it, so leave that screen open while you work.

### Connect

Which of these two the watch offers depends on its Wear OS version, so try them in this order.

The Wireless debugging screen shows an address:

```sh
adb connect 192.168.1.42:5555          # the address the watch shows
```

On Wear OS 4 and newer, and on One UI 5 watches, that address is not enough — the watch must pair
first. Tap **Pair new device** on the watch, which shows a six-digit code and a *different*,
randomised port:

```sh
adb pair 192.168.1.42:37129            # the pairing port, then the six-digit code
adb connect 192.168.1.42:5555          # then the debugging address
```

Confirm it landed:

```sh
adb devices
# 192.168.1.42:5555     device
```

### Install

```sh
adb -s 192.168.1.42:5555 install -r abit-wear-1.0.68.apk
```

Both flags earn their place. `-s` names the watch: `adb` does not filter an APK by the hardware it
declares it needs, so with a phone also plugged in the watch app will cheerfully install on the
phone instead. `-r` replaces an existing install, which is what makes the same command work for an
upgrade as for a first install.

The app appears in the watch's app list as **ABit**. To add its tile ("Next chime"), long-press any
tile to the left of the watch face, then pick ABit from the **+**.

### Turn debugging back off

Go back to **Settings → Developer options** and turn off **Wireless debugging** and **ADB
debugging**. Leaving them on drains the battery and leaves the watch accepting installs from anything
on the network.

### When it goes wrong

- **`INSTALL_FAILED_UPDATE_INCOMPATIBLE`** — a build signed with a different key is already there,
  usually one you compiled yourself. Remove it first:
  `adb -s <serial> uninstall com.gmail.volkovskiyda.abit`. That deletes the watch's local schedules;
  if you are signed in with Google they come back from sync.
- **`INSTALL_FAILED_VERSION_DOWNGRADE`** — you are going back to an older release. Add `-d`.
- **`failed to connect` after it worked a minute ago** — the watch slept and re-randomised the port.
  Re-open the Wireless debugging screen and `adb connect` again.
- **`device unauthorized`** — the watch is showing a confirmation dialog. Scroll it on the watch and
  accept.
- **`adb devices` is empty and `adb connect` hangs** — the watch is on a different network. Phone
  hotspots and guest networks that isolate clients will never work; both devices need to be on the
  same LAN.

## macOS

`ABit-V.dmg` is an Apple Silicon build. Open it and drag **ABit** to Applications.

The app is **not signed or notarized yet**, so macOS refuses to open it on a double click. Right-click
it in Applications and choose **Open**, once — after that it launches normally. On macOS 15 and newer,
where that bypass is gone, the route is **System Settings → Privacy & Security**, scroll to the notice
naming ABit, and **Open Anyway**. The reason it is unsigned is in
[RELEASING.md](RELEASING.md#the-unsigned-dmg).

ABit on the Mac is a menu-bar app with no Dock icon and no window: it lives in the menu bar, and
clicking the icon opens its popover.

## Web

<https://abit-kmp.web.app> — nothing to install, and the current build is always deployed there.
Schedules live in the browser tab's memory and in Firestore, so sign in with Google if you want them
to survive a reload.

## Not for installing

`mapping-android-V.txt` and `mapping-wear-V.txt` are also attached to every release. They are R8
mappings — what makes a crash report from that exact build readable. They are for anyone reading a
stack trace out of a GitHub issue, and there is nothing to install in them.
