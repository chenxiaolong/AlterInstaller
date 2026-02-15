<!--
    When adding new changelog entries, use [Issue #0] to link to issues and
    [PR #0] to link to pull requests. Then run:

        ./gradlew changelogUpdateLinks

    to update the actual links at the bottom of the file.
-->

### Unreleased

* Update AGP to 9.0.0 ([PR #9])
* Reenable default proguard optimizations ([PR #19])
  * For folks who want to decode stack traces from log files, the mapping files are now included with the official releases in `mappings.tar.zst`

### Version 2.3

* Fix compatibility with Android 12 through 14 ([Issue #15], [PR #16])
* Update dependencies ([PR #17])

### Version 2.2

* Remove dependency info block from APK ([PR #11])
* Update dependencies ([PR #12])

### Version 2.1

* Update build script dependencies and target API 36 ([PR #9])

### Version 2.0

* Update checksum for `tensorflow-lite-metadata-0.1.0-rc2.pom` dependency ([PR #3])
* Update dependencies ([PR #4])
* Add support for changing the update owner field ([PR #5])
* Switch to using JSON for the configuration file ([PR #5])
  * The old properties config file will be automatically migrated to the new JSON format.

### Version 1.1

* Log boot script output to logcat ([PR #2])

### Version 1.0

* Initial release

<!-- Do not manually edit the lines below. Use `./gradlew changelogUpdateLinks` to regenerate. -->
[Issue #15]: https://github.com/chenxiaolong/AlterInstaller/issues/15
[PR #2]: https://github.com/chenxiaolong/AlterInstaller/pull/2
[PR #3]: https://github.com/chenxiaolong/AlterInstaller/pull/3
[PR #4]: https://github.com/chenxiaolong/AlterInstaller/pull/4
[PR #5]: https://github.com/chenxiaolong/AlterInstaller/pull/5
[PR #9]: https://github.com/chenxiaolong/AlterInstaller/pull/9
[PR #11]: https://github.com/chenxiaolong/AlterInstaller/pull/11
[PR #12]: https://github.com/chenxiaolong/AlterInstaller/pull/12
[PR #16]: https://github.com/chenxiaolong/AlterInstaller/pull/16
[PR #17]: https://github.com/chenxiaolong/AlterInstaller/pull/17
[PR #19]: https://github.com/chenxiaolong/AlterInstaller/pull/19
