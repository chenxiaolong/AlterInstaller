# AlterInstaller

[![latest release badge](https://img.shields.io/github/v/release/chenxiaolong/AlterInstaller?sort=semver)](https://github.com/chenxiaolong/AlterInstaller/releases/latest)
[![license badge](https://img.shields.io/github/license/chenxiaolong/AlterInstaller)](./LICENSE)

AlterInstaller is a simple Magisk/KernelSU module that changes apps' installer and initiating installer fields in the Android package manager database. This makes it possible to spoof where an app is installed from.

The module directly modifies `/data/system/packages.xml` before the package manager service starts and thus, does not require runtime code injection (eg. Zygisk). Because this state file is modified, the changes persist even if the module is uninstalled.

**Supports Android 12 and newer only.**

## Usage

1. Download the latest version from the [releases page](https://github.com/chenxiaolong/AlterInstaller/releases). To verify the digital signature, see the [verifying digital signatures](#verifying-digital-signatures) section.

2. Install the module from the Magisk/KernelSU app.

3. Create `/data/local/tmp/AlterInstaller.properties` listing the package IDs to modify:

    ```properties
    # Syntax: <Package> = <Installer>
    # For example, to mark VLC as being installed by the Play Store:
    org.videolan.vlc = com.android.vending
    ```

4. Reboot. The log file is written to `/data/local/tmp/AlterInstaller.log`.

    If an app is updated and it no longer reports as being installed by the specified installer, just reboot again.

## Verifying digital signatures

First save the public key to a file that lists which keys should be trusted.

```bash
echo 'AlterInstaller ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIDOe6/tBnO7xZhAWXRj3ApUYgn+XZ0wnQiXM8B7tPgv4' > AlterInstaller_trusted_keys
```

Then, verify the signature of the zip file using the list of trusted keys.

```bash
ssh-keygen -Y verify -f AlterInstaller_trusted_keys -I AlterInstaller -n file -s AlterInstaller-<version>-release.zip.sig < AlterInstaller-<version>-release.zip
```

If the file is successfully verified, the output will be:

```
Good "file" signature for AlterInstaller with ED25519 key SHA256:Ct0HoRyrFLrnF9W+A/BKEiJmwx7yWkgaW/JvghKrboA
```

## Building from source

AlterInstaller can be built like most other Android projects using Android Studio or the gradle command line.

To build the module zip:

```bash
./gradlew zipRelease
```

The output file is written to `app/build/distributions/release/`.

## License

AlterInstaller is licensed under GPLv3. Please see [`LICENSE`](./LICENSE) for the full license text.
