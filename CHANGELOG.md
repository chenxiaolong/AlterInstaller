<!--
    When adding new changelog entries, use [Issue #0] to link to issues and
    [PR #0] to link to pull requests. Then run:

        ./gradlew changelogUpdateLinks

    to update the actual links at the bottom of the file.
-->

### Unreleased

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
[PR #2]: https://github.com/chenxiaolong/AlterInstaller/pull/2
[PR #3]: https://github.com/chenxiaolong/AlterInstaller/pull/3
[PR #4]: https://github.com/chenxiaolong/AlterInstaller/pull/4
[PR #5]: https://github.com/chenxiaolong/AlterInstaller/pull/5
