import os
import sys

# ================= Configuration =================
PROJECT_NAME = "MIA-Metadata-Inspection-Analyzer"
GROUP_ID = "com.epivitae"
ARTIFACT_ID = "MIA-Metadata-Inspection-Analyzer"
VERSION = "0.1.0-SNAPSHOT"
MAIN_PACKAGE = "com/epivitae/mia"

# ================= File Contents =================

# 1. POM.XML (Maven Configuration)
CONTENT_POM = f"""<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>{GROUP_ID}</groupId>
    <artifactId>{ARTIFACT_ID}</artifactId>
    <version>{VERSION}</version>
    <packaging>jar</packaging>

    <name>MIA: Metadata Inspection Analyzer</name>
    <description>A standalone GUI tool for instant extraction and batch inspection of microscopy metadata.</description>
    <url>https://github.com/Epivitae/{PROJECT_NAME}</url>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <bioformats.version>6.14.0</bioformats.version>
    </properties>

    <repositories>
        <repository>
            <id>ome.maven</id>
            <name>OME Maven Repository</name>
            <url>https://artifacts.openmicroscopy.org/artifactory/maven/</url>
        </repository>
        <repository>
            <id>scijava.public</id>
            <url>https://maven.scijava.org/content/groups/public</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>net.imagej</groupId>
            <artifactId>ij</artifactId>
            <version>1.54f</version>
        </dependency>

        <dependency>
            <groupId>ome</groupId>
            <artifactId>bioformats_package</artifactId>
            <version>${{bioformats.version}}</version>
        </dependency>
        
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.2.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>{GROUP_ID}.mia.MIA_Main</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
"""

# 2. README.MD (Epivitae Style)
CONTENT_README = f"""# MIA: Metadata Inspection Analyzer

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**MIA** is a lightweight, "idiot-proof" tool designed for biologists to inspect and extract hidden metadata from microscopy files (Olympus .oir, Zeiss .czi, Leica .lif, etc.) without writing a single line of code.

## Key Features
* **Instant Inspection**: Drag and drop a file to see dimensions, pixel size, and laser power.
* **Batch Extraction**: (Coming Soon) Process entire folders and export metadata to Excel/CSV.
* **Bio-Formats Powered**: Supports 150+ microscopy formats.

## Part of the Epivitae Biosensor Tools suite:
* [**FIA**] (Fluorescence Image Aligner)
* [**RIA**] (Ratio Imaging Analyzer)
* [**NIA**] (Neural Inference Assistant)
* [**MIA**] (Metadata Inspection Analyzer) - *You are here*
* [**WinMan**] (Windows Manager)

## Installation
1.  Download the `MIA-{VERSION}.jar` from Releases.
2.  Drag it into your ImageJ/Fiji `plugins` folder.
3.  Restart Fiji.
4.  Find it under `Plugins > Epivitae > MIA`.

## Developer Setup
1.  Clone this repository.
2.  Open as Maven Project in IntelliJ IDEA.
3.  Run `MIA_Main.java`.

## License
MIT License. Copyright (c) 2026 Epivitae.
"""

# 3. plugins.config (Vital for ImageJ)
CONTENT_PLUGINS_CONFIG = f"""# Epivitae MIA Plugin Configuration
# Format: Menu_Path, "Menu Item Label", Class_Name

Plugins>Epivitae, "MIA - Metadata Inspector", {GROUP_ID}.mia.MIA_Main
"""

# 4. .gitignore
CONTENT_GITIGNORE = """target/
*.class
.idea/
*.iml
.DS_Store
*.log
"""

# 5. Java Code: MIA_Main.java (Entry Point)
CONTENT_JAVA_MAIN = f"""package {GROUP_ID}.mia;

import ij.plugin.PlugIn;
import {GROUP_ID}.mia.gui.InspectorFrame;

/**
 * Plugin entry point for ImageJ.
 */
public class MIA_Main implements PlugIn {{

    @Override
    public void run(String arg) {{
        // Launch the GUI
        InspectorFrame frame = new InspectorFrame();
        frame.setVisible(true);
    }}
    
    /**
     * Main method for testing/debugging outside of ImageJ.
     */
    public static void main(String[] args) {{
        new ij.ImageJ(); // Open ImageJ console
        new MIA_Main().run("");
    }}
}}
"""

# 6. Java Code: InspectorFrame.java (GUI Stub)
CONTENT_JAVA_GUI = f"""package {GROUP_ID}.mia.gui;

import ij.IJ;
import ij.io.OpenDialog;
import {GROUP_ID}.mia.core.MetadataEngine;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;

public class InspectorFrame extends JFrame {{

    private final JTextArea summaryArea;
    private final JTable rawTable;
    private final DefaultTableModel tableModel;
    private final JTextField statusField;

    public InspectorFrame() {{
        super("MIA: Metadata Inspection Analyzer");
        setSize(900, 600);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // --- Top Panel ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton loadBtn = new JButton("Load File (.oir, .czi, .lif)");
        loadBtn.addActionListener(this::onLoadClicked);
        topPanel.add(loadBtn);
        
        statusField = new JTextField("Ready to inspect.", 40);
        statusField.setEditable(false);
        topPanel.add(statusField);
        
        add(topPanel, BorderLayout.NORTH);

        // --- Split Pane ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        
        // Summary Area
        summaryArea = new JTextArea("Key metadata will appear here...");
        summaryArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        summaryArea.setEditable(false);
        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        summaryScroll.setBorder(BorderFactory.createTitledBorder("Key Parameters (Summary)"));
        splitPane.setTopComponent(summaryScroll);

        // Raw Table
        String[] cols = {{"Key", "Value"}};
        tableModel = new DefaultTableModel(cols, 0);
        rawTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(rawTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Raw Metadata Dump"));
        splitPane.setBottomComponent(tableScroll);
        
        splitPane.setDividerLocation(200);
        add(splitPane, BorderLayout.CENTER);
    }}

    private void onLoadClicked(ActionEvent e) {{
        OpenDialog od = new OpenDialog("Choose Microscopy File", "");
        String dir = od.getDirectory();
        String name = od.getFileName();
        if (dir == null || name == null) return;

        String path = dir + name;
        statusField.setText("Processing: " + name + "...");
        
        // Run in background to avoid freezing UI
        new Thread(() -> {{
            try {{
                MetadataEngine engine = new MetadataEngine();
                engine.parse(path, summaryArea, tableModel);
                statusField.setText("Done: " + name);
            }} catch (Exception ex) {{
                IJ.log("Error: " + ex.getMessage());
                statusField.setText("Error occurred.");
                ex.printStackTrace();
            }}
        }}).start();
    }}
}}
"""

# 7. Java Code: MetadataEngine.java (Bio-Formats Logic)
CONTENT_JAVA_ENGINE = f"""package {GROUP_ID}.mia.core;

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
public class MetadataEngine {{

    public void parse(String filePath, JTextArea summaryArea, DefaultTableModel tableModel) throws Exception {{
        IFormatReader reader = new ImageReader();
        // Crucial: Only read metadata, do not load pixels
        reader.setMetadataStore(MetadataTools.createOMEXMLMetadata());
        reader.setId(filePath);

        IMetadata meta = (IMetadata) reader.getMetadataStore();

        // 1. Build Summary
        StringBuilder sb = new StringBuilder();
        sb.append("File: ").append(filePath).append("\\n");
        sb.append("Format: ").append(reader.getFormat()).append("\\n");
        sb.append("Dimensions (XYZCT): ").append(reader.getSizeX()).append(", ")
          .append(reader.getSizeY()).append(", ")
          .append(reader.getSizeZ()).append(", ")
          .append(reader.getSizeC()).append(", ")
          .append(reader.getSizeT()).append("\\n");

        try {{
            if (meta.getPixelsPhysicalSizeX(0) != null) {{
                 double px = meta.getPixelsPhysicalSizeX(0).value().doubleValue();
                 String unit = meta.getPixelsPhysicalSizeX(0).unit().getSymbol();
                 sb.append(String.format("Pixel Size: %.4f %s\\n", px, unit));
            }}
        }} catch (Exception e) {{ sb.append("Pixel Size: Unknown\\n"); }}

        SwingUtilities.invokeLater(() -> summaryArea.setText(sb.toString()));

        // 2. Dump Raw Data
        Hashtable<String, Object> globalMeta = reader.getGlobalMetadata();
        Map<String, Object> sortedMeta = new TreeMap<>(globalMeta);
        
        SwingUtilities.invokeLater(() -> {{
            tableModel.setRowCount(0);
            for (Map.Entry<String, Object> entry : sortedMeta.entrySet()) {{
                tableModel.addRow(new Object[]{{entry.getKey(), entry.getValue()}});
            }}
        }});
        
        reader.close();
    }}
}}
"""

# ================= Generator Logic =================

def create_project_structure():
    root = PROJECT_NAME
    
    # Define folder structure
    dirs = [
        f"{root}/src/main/java/{MAIN_PACKAGE}/gui",
        f"{root}/src/main/java/{MAIN_PACKAGE}/core",
        f"{root}/src/main/resources",
        f"{root}/src/test/java"
    ]

    # Create directories
    print(f"Creating project root: {root}")
    for d in dirs:
        os.makedirs(d, exist_ok=True)
        print(f"  Created: {d}")

    # Map content to file paths
    file_map = {
        f"{root}/pom.xml": CONTENT_POM,
        f"{root}/README.md": CONTENT_README,
        f"{root}/.gitignore": CONTENT_GITIGNORE,
        f"{root}/src/main/resources/plugins.config": CONTENT_PLUGINS_CONFIG,
        f"{root}/src/main/java/{MAIN_PACKAGE}/MIA_Main.java": CONTENT_JAVA_MAIN,
        f"{root}/src/main/java/{MAIN_PACKAGE}/gui/InspectorFrame.java": CONTENT_JAVA_GUI,
        f"{root}/src/main/java/{MAIN_PACKAGE}/core/MetadataEngine.java": CONTENT_JAVA_ENGINE,
    }

    # Write files
    for path, content in file_map.items():
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"  Generated: {path}")

    print("\n" + "="*50)
    print(f"SUCCESS! Project '{PROJECT_NAME}' generated.")
    print("Next steps:")
    print("1. Open IntelliJ IDEA.")
    print(f"2. File -> Open -> Select the '{root}' folder.")
    print("3. Wait for Maven to download Bio-Formats and ImageJ dependencies.")
    print("4. Run 'MIA_Main.java' to start debugging.")
    print("="*50)

if __name__ == "__main__":
    create_project_structure()