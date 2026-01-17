package com.epivitae.mia.core;

import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class to read version info from maven-filtered properties.
 */
public class VersionUtils {

    private static final String VERSION_FILE = "/mia_version.properties";

    public static String getVersion() {
        try (InputStream stream = VersionUtils.class.getResourceAsStream(VERSION_FILE)) {
            if (stream == null) {
                return "Dev-Mode"; // 当直接在IDE运行且未经过Maven处理时显示
            }
            Properties props = new Properties();
            props.load(stream);
            return props.getProperty("version", "Unknown");
        } catch (Exception e) {
            e.printStackTrace();
            return "Error";
        }
    }
}