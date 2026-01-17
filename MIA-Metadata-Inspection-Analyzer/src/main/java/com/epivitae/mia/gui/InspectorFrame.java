package com.epivitae.mia.gui;

import ij.IJ;
import ij.io.OpenDialog;
import com.epivitae.mia.core.MetadataEngine;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;

public class InspectorFrame extends JFrame {

    private final JTextArea summaryArea;
    private final JTable rawTable;
    private final DefaultTableModel tableModel;
    private final JTextField statusField;

    public InspectorFrame() {
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
        String[] cols = {"Key", "Value"};
        tableModel = new DefaultTableModel(cols, 0);
        rawTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(rawTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Raw Metadata Dump"));
        splitPane.setBottomComponent(tableScroll);
        
        splitPane.setDividerLocation(200);
        add(splitPane, BorderLayout.CENTER);
    }

    private void onLoadClicked(ActionEvent e) {
        OpenDialog od = new OpenDialog("Choose Microscopy File", "");
        String dir = od.getDirectory();
        String name = od.getFileName();
        if (dir == null || name == null) return;

        String path = dir + name;
        statusField.setText("Processing: " + name + "...");
        
        // Run in background to avoid freezing UI
        new Thread(() -> {
            try {
                MetadataEngine engine = new MetadataEngine();
                engine.parse(path, summaryArea, tableModel);
                statusField.setText("Done: " + name);
            } catch (Exception ex) {
                IJ.log("Error: " + ex.getMessage());
                statusField.setText("Error occurred.");
                ex.printStackTrace();
            }
        }).start();
    }
}
