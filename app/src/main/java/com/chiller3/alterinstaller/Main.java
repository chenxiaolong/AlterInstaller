/*
 * SPDX-FileCopyrightText: 2024 Andrew Gunnerson
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.chiller3.alterinstaller;

import android.annotation.SuppressLint;
import android.os.Build;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * @noinspection JavaReflectionMemberAccess
 */
@SuppressLint({"BlockedPrivateApi", "SoonBlockedPrivateApi"})
public class Main {
    private static final String TAG = "AlterInstaller";

    private static XmlPullParser resolvePullParser(InputStream is) throws Exception {
        Method method = Xml.class.getDeclaredMethod("resolvePullParser", InputStream.class);
        return (XmlPullParser) method.invoke(null, is);
    }

    private static XmlSerializer resolveSerializer(OutputStream os) throws Exception {
        Method method = Xml.class.getDeclaredMethod("resolveSerializer", OutputStream.class);
        return (XmlSerializer) method.invoke(null, os);
    }

    private static void copy(XmlPullParser in, XmlSerializer out) throws Exception {
        Method method = Xml.class.getDeclaredMethod("copy", XmlPullParser.class, XmlSerializer.class);
        method.invoke(null, in, out);
    }

    private static void alterXml(InputStream inputStream, OutputStream outputStream,
                                 Map<String, PackageConfig> packageToConfig) throws Exception {
        XmlPullParser input = resolvePullParser(inputStream);
        XmlSerializer output = new AlterInstallerSerializer(
                resolveSerializer(outputStream), packageToConfig,
                (packageName, field, oldValue, newValue) -> {
                    if (TextUtils.equals(oldValue, newValue)) {
                        Log.i(TAG, "[" + packageName + "] Keeping: " + field + ": " +
                                oldValue);
                    } else if (oldValue == null) {
                        Log.i(TAG, "[" + packageName + "] Adding: " + field + ": " + newValue);
                    } else {
                        Log.i(TAG, "[" + packageName + "] Updating: " + field + ": " +
                                oldValue + " -> " + newValue);
                    }
                });

        // This will make the output file size potentially up to 2 times the input size. There is no
        // way to easily extend TypedXmlPullParser via reflection, so binary attributes that were
        // efficiently stored as binary data in the input file will be stored as hex strings in the
        // output file. The difference is only in representation, not semantics. It's not worth
        // trying to improve this situation because the package manager will rewrite the file as
        // soon as any package operation occurs (app update, etc.).
        copy(input, output);
    }

    private static void alterXml(String inputPath, String outputPath,
                                 Map<String, PackageConfig> packageToConfig) throws Exception {
        FileDescriptor inputFd = Os.open(inputPath, OsConstants.O_RDONLY, 0);

        try {
            StructStat inputStat = Os.fstat(inputFd);

            String tempOutputPath = outputPath + ".tmp";
            FileDescriptor outputFd = Os.open(tempOutputPath,
                    OsConstants.O_CREAT | OsConstants.O_TRUNC | OsConstants.O_WRONLY,
                    inputStat.st_mode & ~OsConstants.S_IFMT);

            try {
                Os.fchmod(outputFd, inputStat.st_mode & ~OsConstants.S_IFMT);
                Os.fchown(outputFd, inputStat.st_uid, inputStat.st_gid);

                // Android's FileInputStream is documented to not take ownership of the fd.
                try (FileInputStream inputStream = new FileInputStream(inputFd);
                     FileOutputStream outputStream = new FileOutputStream(outputFd)) {
                    alterXml(inputStream, outputStream, packageToConfig);
                }

                // Atomically replace original output file.
                Os.fsync(outputFd);
                Os.rename(tempOutputPath, outputPath);
            } catch (Exception e) {
                Os.remove(tempOutputPath);
                throw e;
            } finally {
                Os.close(outputFd);
            }
        } finally {
            Os.close(inputFd);
        }
    }

    private static HashMap<String, PackageConfig> parsePropConfig(String path) throws IOException {
        Properties properties = new Properties();

        try (FileReader reader = new FileReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        HashMap<String, PackageConfig> result = new HashMap<>();

        for (String name : properties.stringPropertyNames()) {
            String value = properties.getProperty(name);
            result.put(name, new PackageConfig(value, null));
        }

        return result;
    }

    private static HashMap<String, PackageConfig> parseJsonConfig(String path) throws IOException {
        // Files.readString() doesn't exist on Android.
        //noinspection ReadWriteStringCanBeUsed
        String contents = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);

        HashMap<String, PackageConfig> result = new HashMap<>();

        try {
            JSONObject data = new JSONObject(contents);

            for (Iterator<String> dataIt = data.keys(); dataIt.hasNext(); ) {
                String name = dataIt.next();
                JSONObject config = data.getJSONObject(name);
                String installer = null;
                String updateOwner = null;

                for (Iterator<String> configIt = config.keys(); configIt.hasNext(); ) {
                    String key = configIt.next();

                    switch (key) {
                        case "installer" -> installer = config.optString(key, null);
                        case "updateOwner" -> updateOwner = config.optString(key, null);
                        default -> Log.w(TAG, "[" + name + "] Unsupported JSON key: " + key);
                    }
                }

                result.put(name, new PackageConfig(installer, updateOwner));
            }
        } catch (JSONException e) {
            throw new IOException("Failed to load JSON config: " + path, e);
        }

        return result;
    }

    private static void migrateConfig(String oldPath, String newPath) throws Exception {
        HashMap<String, PackageConfig> config = parsePropConfig(oldPath);
        Log.i(TAG, "Loaded config: " + config);

        JSONObject data = new JSONObject();

        for (Map.Entry<String, PackageConfig> entry : config.entrySet()) {
            data.put(entry.getKey(), new JSONObject()
                    .putOpt("installer", entry.getValue().installer())
                    .putOpt("updateOwner", entry.getValue().updateOwner()));
        }

        // Files.writeString() doesn't exist on Android.
        //noinspection ReadWriteStringCanBeUsed
        Files.write(Paths.get(newPath), data.toString(4).getBytes(StandardCharsets.UTF_8));
    }

    @SuppressLint("ObsoleteSdkInt")
    private static void apply(String configPath, String inputPath, String outputPath)
            throws Exception {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            throw new Exception("Android <12 is not supported");
        }

        HashMap<String, PackageConfig> config = parseJsonConfig(configPath);
        Log.i(TAG, "Loaded config: " + config);

        alterXml(inputPath, outputPath, config);
    }

    private static void printUsageAndExit() {
        System.err.println("Usage:");
        System.err.println("  " + Main.class.getSimpleName() + " migrate-config <old> <new>");
        System.err.println("  " + Main.class.getSimpleName() + " apply <config> <input> <output>");
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsageAndExit();
        }

        switch (args[0]) {
            case "migrate-config" -> {
                if (args.length != 3) {
                    printUsageAndExit();
                }

                try {
                    migrateConfig(args[1], args[2]);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to migrate legacy config", e);
                    System.exit(1);
                }
            }
            case "apply" -> {
                if (args.length != 4) {
                    printUsageAndExit();
                }

                try {
                    apply(args[1], args[2], args[3]);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to alter package manager state", e);
                    System.exit(1);
                }
            }
            default -> printUsageAndExit();
        }
    }
}
