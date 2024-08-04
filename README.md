# AlterInstaller

[![latest release badge](https://img.shields.io/github/v/release/chenxiaolong/AlterInstaller?sort=semver)](https://github.com/chenxiaolong/AlterInstaller/releases/latest)
[![license badge](https://img.shields.io/github/license/chenxiaolong/AlterInstaller)](./LICENSE)

AlterInstaller is a simple Magisk/KernelSU module that changes apps' installer, initiating installer, and update owner fields in the Android package manager database. This makes it possible to spoof where an app is installed from and control which app store is allowed to update it.

The module directly modifies `/data/system/packages.xml` before the package manager service starts and thus, does not require runtime code injection (eg. Zygisk). Because this state file is modified, the changes persist even if the module is uninstalled.

**Supports Android 12 and newer only.**

## Usage

1. Download the latest version from the [releases page](https://github.com/chenxiaolong/AlterInstaller/releases). To verify the digital signature, see the [verifying digital signatures](#verifying-digital-signatures) section.

2. Install the module from the Magisk/KernelSU app.

3. Create `/data/local/tmp/AlterInstaller.json` listing the package IDs to modify:

    ```jsonc
    {
        // The top level key is the package to modify. The values below can be
        // omitted or set to null to leave the fields unchanged.
        "org.videolan.vlc": {
            // Mark VLC as being installed by the Play Store.
            "installer": "com.android.vending",
            // Mark VLC as being only updatable by Droid-ify.
            "updateOwner": "com.looker.droidify"
        }
    }
    ```

4. Reboot. The log file is written to `/data/local/tmp/AlterInstaller.log`.

    If an app is updated and it no longer reports as being installed by the specified installer, just reboot again.

## Verifying digital signatures

To verify the digital signatures of the downloads, follow [the steps here](https://github.com/chenxiaolong/chenxiaolong/blob/master/VERIFY_SSH_SIGNATURES.md).

## Building from source

AlterInstaller can be built like most other Android projects using Android Studio or the gradle command line.

To build the module zip:

```bash
./gradlew zipRelease
```

The output file is written to `app/build/distributions/release/`.

## License

AlterInstaller is licensed under GPLv3. Please see [`LICENSE`](./LICENSE) for the full license text.
