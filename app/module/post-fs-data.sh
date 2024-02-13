exec >/data/local/tmp/AlterInstaller.log 2>&1

mod_dir=${0%/*}

header() {
    echo "----- ${*} -----"
}

header Environment
echo "Timestamp: $(date)"
echo "Script: ${0}"
echo "UID/GID/Context: $(id)"

header Backing up PackageManager state
cp -v /data/system/packages.xml \
    /data/local/tmp/AlterInstaller.backup.xml \
    || exit 1

header Altering PackageManager installer fields
CLASSPATH=$(echo "${mod_dir}/"app-*.apk) app_process / @NAMESPACE@.Main \
    /data/local/tmp/AlterInstaller.properties \
    /data/system/packages.xml \
    /data/system/packages.xml \
    &
pid=${!}
wait "${pid}"
echo "Exit status: ${?}"
echo "Logcat:"
logcat -d --pid "${pid}"
