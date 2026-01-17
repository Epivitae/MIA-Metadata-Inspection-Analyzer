package com.epivitae.mia.core;

import java.awt.Color;

public class SpectrumUtils {

    /**
     * 将波长 (nm) 转换为 HTML Hex 颜色代码 (e.g., "#00FF00")
     */
    public static String wavelengthToHex(double nm) {
        Color color = wavelengthToColor(nm);
        // 格式化为 Hex，并稍微加深一点以便在白底上阅读
        return String.format("#%02x%02x%02x", 
            color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * 简单的光谱估算算法 (Dan Bruton's Algorithm 简化版)
     */
    private static Color wavelengthToColor(double lambda) {
        double r = 0, g = 0, b = 0;

        if (lambda >= 380 && lambda < 440) {
            r = -(lambda - 440) / (440 - 380);
            b = 1.0;
        } else if (lambda >= 440 && lambda < 490) {
            g = (lambda - 440) / (490 - 440);
            b = 1.0;
        } else if (lambda >= 490 && lambda < 510) {
            g = 1.0;
            b = -(lambda - 510) / (510 - 490);
        } else if (lambda >= 510 && lambda < 580) {
            r = (lambda - 510) / (580 - 510);
            g = 1.0;
        } else if (lambda >= 580 && lambda < 645) {
            r = 1.0;
            g = -(lambda - 645) / (645 - 580);
        } else if (lambda >= 645 && lambda <= 780) {
            r = 1.0;
        } else {
            // 不可见光显示为灰色
            return Color.GRAY;
        }

        // 强度修正 (让两端的光变暗)
        double factor;
        if (lambda >= 380 && lambda < 420) {
            factor = 0.3 + 0.7 * (lambda - 380) / (420 - 380);
        } else if (lambda >= 420 && lambda < 700) {
            factor = 1.0;
        } else if (lambda >= 700 && lambda <= 780) {
            factor = 0.3 + 0.7 * (780 - lambda) / (780 - 700);
        } else {
            factor = 0;
        }

        // 转为 0-255，并为了UI显示做Gamma校正
        int R = (int) (adjust(r, factor) * 255);
        int G = (int) (adjust(g, factor) * 255);
        int B = (int) (adjust(b, factor) * 255);

        return new Color(R, G, B);
    }

    private static double adjust(double color, double factor) {
        if (color == 0.0) return 0;
        else return Math.pow(color * factor, 0.80);
    }
    
    /**
     * 辅助：从 "500-600" 这种字符串中提取中心波长用于取色
     */
    public static double parseCenterWavelength(String str) {
        try {
            if (str == null) return 550; // 默认绿色
            String clean = str.replaceAll("[^0-9.\\-]", "");
            if (clean.contains("-")) {
                String[] parts = clean.split("-");
                double start = Double.parseDouble(parts[0]);
                double end = Double.parseDouble(parts[1]);
                return (start + end) / 2.0;
            } else {
                return Double.parseDouble(clean);
            }
        } catch (Exception e) {
            return 550;
        }
    }
}