exec >/data/local/tmp/AlterInstaller.log 2>&1

mod_dir=${0%/*}

header() {
    echo "----- ${*} -----"
}

run_cli_apk() {
    CLASSPATH=$(echo "${mod_dir}"/app-*.apk) \
        app_process / @NAMESPACE@.Main "${@}" &
    pid=${!}
    wait "${pid}"
    echo "Exit status: ${?}"
    echo "Logcat:"
    logcat -d --pid "${pid}"
}

header Environment
echo "Timestamp: $(date)"
echo "Script: ${0}"
echo "UID/GID/Context: $(id)"

header Backing up PackageManager state
cp -v /data/system/packages.xml \
    /data/local/tmp/AlterInstaller.backup.xml \
    || exit 1

if [ -f /data/local/tmp/AlterInstaller.properties ] \
    && [ ! -f /data/local/tmp/AlterInstaller.json ]; then
    header Migrating legacy properties config to JSON
    run_cli_apk \
        migrate-config \
        /data/local/tmp/AlterInstaller.properties \
        /data/local/tmp/AlterInstaller.json \
        || exit 1
fi

header Altering PackageManager installer fields
run_cli_apk \
    apply \
    /data/local/tmp/AlterInstaller.json \
    /data/system/packages.xml \
    /data/system/packages.xml
