package com.epivitae.mia.core;

import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the heavy lifting of Bio-Formats parsing.
 * v0.9.0: HTML RICH TEXT & COLOR MAPPING.
 * Renders metadata as beautiful HTML with spectral colors.
 */
public class MetadataEngine {

    // 注意：这里参数类型变了，是 JTextPane
    public void parse(String filePath, JTextPane summaryPane, DefaultTableModel tableModel) throws Exception {
        ImageReader reader = new ImageReader();
        reader.setOriginalMetadataPopulated(true);
        reader.setGroupFiles(false);
        reader.setMetadataStore(MetadataTools.createOMEXMLMetadata());
        reader.setId(filePath);

        IMetadata meta = (IMetadata) reader.getMetadataStore();
        
        Hashtable<String, Object> allRawMeta = new Hashtable<>();
        if (reader.getGlobalMetadata() != null) allRawMeta.putAll(reader.getGlobalMetadata());
        if (reader.getSeriesMetadata() != null) allRawMeta.putAll(reader.getSeriesMetadata());
        Map<String, Object> sortedMeta = new TreeMap<>(allRawMeta);

        // 开始构建 HTML
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: sans-serif; font-size: 10px;'>");

        // =================================================================
        // SECTION 1: HEADER (Grid Layout using Table)
        // =================================================================
        html.append("<table width='100%' border='0' cellspacing='0' cellpadding='1'>");
        
        // Dimensions
        html.append("<tr>");
        html.append(String.format("<td><b>XYZCT:</b> %d, %d, %d, %d, %d</td>",
                reader.getSizeX(), reader.getSizeY(), reader.getSizeZ(), reader.getSizeC(), reader.getSizeT()));
        html.append("</tr>");

        // Physical
        html.append("<tr><td>");
        try {
            if (meta.getPixelsPhysicalSizeX(0) != null) {
                double px = meta.getPixelsPhysicalSizeX(0).value().doubleValue();
                String unit = meta.getPixelsPhysicalSizeX(0).unit().getSymbol();
                html.append(String.format("<b>Pixel Size:</b> %.4f %s", px, unit));
            } else {
                html.append("<b>Pixel Size:</b> N/A");
            }
        } catch (Exception e) { html.append("<b>Pixel Size:</b> N/A"); }

        // Interval
        if (reader.getSizeT() > 1) {
            html.append("&nbsp;&nbsp;<font color='#888888'>|</font>&nbsp;&nbsp;<b>Interval:</b> ");
            Double dt = null;
            try {
                if (meta.getPixelsTimeIncrement(0) != null) {
                    dt = meta.getPixelsTimeIncrement(0).value().doubleValue();
                }
            } catch (Exception e) {}

            if (dt == null || dt == 0) {
                String rawInterval = findValue(sortedMeta, "seriesInterval", "speedInformation");
                if (rawInterval != null) {
                    try { dt = Double.parseDouble(rawInterval) / 1000.0; } catch (Exception e) {}
                }
            }

            if (dt != null) html.append(String.format("%.2f s", dt));
            else html.append("N/A");
        }
        html.append("</td></tr></table>");
        
        html.append("<hr color='#eeeeee'>"); // 淡淡的分割线

        // =================================================================
        // SECTION 2: CHANNELS (HTML Table for Perfect Alignment)
        // =================================================================
        html.append("<table width='100%' border='0' cellspacing='0' cellpadding='2'>");

        int channelCount = reader.getSizeC();
        for (int c = 0; c < channelCount; c++) {
            int idx = c + 1;
            String tagShort = "#" + idx;
            String tagLong = String.format("#%02d", idx);

            // 1. Name Extraction (Logic kept same)
            String name = findValue(sortedMeta, "dyeName", tagShort);
            if (name == null) name = findValue(sortedMeta, "dyeId", tagShort);
            if (name == null) name = findValue(sortedMeta, "dyeName", tagLong);
            if (name == null) {
                try {
                    String omeName = meta.getChannelName(0, c);
                    if (omeName != null && !omeName.matches("(?i)Channel\\s*\\d+|CH\\d+|Ref")) name = omeName;
                } catch (Exception e) {}
            }
            if (name == null) name = "CH" + idx;

            // 2. Excitation Extraction
            String exStr = null;
            String laserId = findValue(sortedMeta, "laserDataId", tagShort);
            if (laserId != null) {
                Matcher m = Pattern.compile("LD(\\d+)").matcher(laserId);
                if (m.find()) exStr = m.group(1);
            }
            if (exStr == null) {
                String rawEx = findValue(sortedMeta, "excitationWavelength", tagShort);
                if (rawEx != null && rawEx.matches("^\\d+(\\.\\d+)?$")) exStr = rawEx.split("\\.")[0];
            }
            if (exStr == null) {
                try {
                    if (meta.getChannelExcitationWavelength(0, c) != null) 
                        exStr = String.valueOf(meta.getChannelExcitationWavelength(0, c).value().intValue());
                } catch (Exception e) {}
            }

            // 3. Emission Extraction
            String emStr = null;
            String startW = findValue(sortedMeta, "wavelengthRange", "start", tagLong);
            String endW = findValue(sortedMeta, "wavelengthRange", "end", tagLong);
            if (startW == null) startW = findValue(sortedMeta, "wavelengthRange", "start", tagShort);
            if (endW == null) endW = findValue(sortedMeta, "wavelengthRange", "end", tagShort);

            if (startW != null && endW != null) {
                emStr = startW.split("\\.")[0] + "-" + endW.split("\\.")[0];
            }
            // ... (Rest of Em logic similar to v0.8.0) ...
            if (emStr == null) {
                 String centerW = findValue(sortedMeta, "emissionWavelength", tagShort);
                 if (centerW != null) emStr = centerW.split("\\.")[0];
            }
            if (emStr == null) {
                 String filter = findValue(sortedMeta, "emission", tagShort);
                 if (filter != null && !filter.toUpperCase().contains("GROUP")) emStr = filter;
            }

            // --- COLOR CALCULATION ---
            // 算激发光颜色
            String exColor = "#000000"; // 默认黑
            if (exStr != null) {
                try {
                    double w = Double.parseDouble(exStr);
                    exColor = SpectrumUtils.wavelengthToHex(w);
                } catch (Exception e) {}
            }
            
            // 算发射光颜色 (取范围中值)
            String emColor = "#000000";
            if (emStr != null) {
                double w = SpectrumUtils.parseCenterWavelength(emStr);
                emColor = SpectrumUtils.wavelengthToHex(w);
            }

            // --- HTML ROW CONSTRUCTION ---
            html.append("<tr>");
            
            // Col 1: Name (Bold)
            html.append("<td width='30%'><b>Ch").append(idx).append(":</b> [").append(name.trim()).append("]</td>");
            
            // Col 2: Excitation (With colored dot and text)
            html.append("<td>");
            if (exStr != null) {
                html.append("<span style='color:").append(exColor).append(";'>● </span>");
                html.append("Ex ").append(exStr);
            }
            html.append("</td>");

            // Col 3: Emission (With colored dot and text)
            html.append("<td>");
            if (emStr != null) {
                html.append("<span style='color:").append(emColor).append(";'>● </span>");
                html.append("Em ").append(emStr);
            }
            html.append("</td>");
            
            html.append("</tr>");
        }
        
        html.append("</table></body></html>");

        // Update UI Summary (HTML)
        SwingUtilities.invokeLater(() -> {
            summaryPane.setText(html.toString());
            summaryPane.setCaretPosition(0);
        });

        // Update Raw Table (Standard)
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            for (Map.Entry<String, Object> entry : sortedMeta.entrySet()) {
                tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
        });

        reader.close();
    }
    
    // ... findValue 方法保持不变 ...
    private String findValue(Map<String, Object> map, String k1, String k2) {
        return findValue(map, k1, k2, null);
    }
    private String findValue(Map<String, Object> map, String k1, String k2, String k3) {
        String key1 = k1.toLowerCase();
        String key2 = k2.toLowerCase();
        String key3 = (k3 != null) ? k3.toLowerCase() : null;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (key.contains(key1) && key.contains(key2)) {
                if (key3 != null && !key.contains(key3)) continue;
                if (key1.contains("emission") && key.contains("group")) continue;
                Object val = entry.getValue();
                if (val != null && !val.toString().isEmpty()) return val.toString();
            }
        }
        return null;
    }
}