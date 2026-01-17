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

/**
 * Handles the heavy lifting of Bio-Formats parsing.
 */
public class MetadataEngine {

    public void parse(String filePath, JTextArea summaryArea, DefaultTableModel tableModel) throws Exception {
        IFormatReader reader = new ImageReader();
        // Crucial: Only read metadata, do not load pixels
        reader.setMetadataStore(MetadataTools.createOMEXMLMetadata());
        reader.setId(filePath);

        IMetadata meta = (IMetadata) reader.getMetadataStore();

        // 1. Build Summary
        StringBuilder sb = new StringBuilder();
        sb.append("File: ").append(filePath).append("\n");
        sb.append("Format: ").append(reader.getFormat()).append("\n");
        sb.append("Dimensions (XYZCT): ").append(reader.getSizeX()).append(", ")
          .append(reader.getSizeY()).append(", ")
          .append(reader.getSizeZ()).append(", ")
          .append(reader.getSizeC()).append(", ")
          .append(reader.getSizeT()).append("\n");

        try {
            if (meta.getPixelsPhysicalSizeX(0) != null) {
                 double px = meta.getPixelsPhysicalSizeX(0).value().doubleValue();
                 String unit = meta.getPixelsPhysicalSizeX(0).unit().getSymbol();
                 sb.append(String.format("Pixel Size: %.4f %s\n", px, unit));
            }
        } catch (Exception e) { sb.append("Pixel Size: Unknown\n"); }

        SwingUtilities.invokeLater(() -> summaryArea.setText(sb.toString()));

        // 2. Dump Raw Data
        Hashtable<String, Object> globalMeta = reader.getGlobalMetadata();
        Map<String, Object> sortedMeta = new TreeMap<>(globalMeta);
        
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            for (Map.Entry<String, Object> entry : sortedMeta.entrySet()) {
                tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
        });
        
        reader.close();
    }
}
