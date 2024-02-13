package com.chiller3.alterinstaller;

import android.annotation.SuppressLint;
import android.os.Build;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import android.util.Xml;

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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @noinspection JavaReflectionMemberAccess
 */
@SuppressLint({"BlockedPrivateApi", "SoonBlockedPrivateApi"})
public class Main {
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
                                 Map<String, String> packageToInstaller) throws Exception {
        XmlPullParser input = resolvePullParser(inputStream);
        XmlSerializer output = new AlterInstallerSerializer(
                resolveSerializer(outputStream), packageToInstaller,
                (packageName, field, oldValue, newValue) ->
                        System.err.printf("[%s] Changing %s: %s -> %s%n", packageName, field, oldValue, newValue));

        // This will make the output file size potentially up to 2 times the input size. There is no
        // way to easily extend TypedXmlPullParser via reflection, so binary attributes that were
        // efficiently stored as binary data in the input file will be stored as hex output file.
        // The difference is only in representation, not semantics. It's not worth try to improve
        // this situation because the package manager will rewrite the file as soon as any package
        // operation occurs (app update, etc.).
        copy(input, output);
    }

    private static void alterXml(String inputPath, String outputPath,
                                 Map<String, String> packageToInstaller) throws Exception {
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
                    alterXml(inputStream, outputStream, packageToInstaller);
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

    private static HashMap<String, String> parseConfig(String path) throws IOException {
        Properties properties = new Properties();

        try (FileReader reader = new FileReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        HashMap<String, String> result = new HashMap<>();

        for (String name : properties.stringPropertyNames()) {
            String value = properties.getProperty(name);
            result.put(name, value);
        }

        return result;
    }

    @SuppressLint("ObsoleteSdkInt")
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: " + Main.class.getSimpleName() + " <config> <input> <output>");
            System.exit(1);
        }

        String configPath = args[0];
        String inputPath = args[1];
        String outputPath = args[2];

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                throw new Exception("Android <12 is not supported");
            }

            HashMap<String, String> config = parseConfig(configPath);
            System.err.println("Loaded config: " + config);

            alterXml(inputPath, outputPath, config);
        } catch (Exception e) {
            System.err.println("Failed to alter package manager state");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
