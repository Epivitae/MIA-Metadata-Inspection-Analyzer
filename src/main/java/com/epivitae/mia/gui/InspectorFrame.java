package com.epivitae.mia.gui;

import ij.IJ;
import ij.io.OpenDialog;
import com.epivitae.mia.core.MetadataEngine;
import com.epivitae.mia.core.VersionUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream; // 新增
import java.io.OutputStreamWriter; // 新增
import java.nio.charset.StandardCharsets; // 新增
import java.net.URL;
import java.util.List;

public class InspectorFrame extends JFrame {

    // --- CNS 家族标准色 ---
    private static final Color BRAND_BLUE = new Color(0, 102, 204);
    private static final Color TEXT_GRAY = new Color(128, 128, 128);
    private static final Color ACTION_ORANGE = new Color(220, 100, 0);

    // --- 组件 ---
    private final JTextPane summaryPane;
    private final JTable rawTable;
    private final DefaultTableModel tableModel;
    private final JTextField pathField;
    private final JProgressBar progressBar;

    public InspectorFrame() {
        String version = VersionUtils.getVersion();
        setTitle("MIA Inspector " + version);
        
        setSize(400, 700); 
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // 1. Header Area
        add(createCenteredHeaderPanel(version), BorderLayout.NORTH);

        // 2. Body Area
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // --- Group 1: Data Source ---
        JPanel sourcePanel = new JPanel(new BorderLayout(5, 5));
        styleGroupBox(sourcePanel, "Data Source");
        
        JPanel fileInputPanel = new JPanel(new BorderLayout(5, 0));
        pathField = new JTextField();
        pathField.setEditable(false);
        pathField.setToolTipText("Drag and drop file here");
        JButton loadBtn = new JButton("Browse...");
        loadBtn.addActionListener(this::onLoadClicked);
        
        fileInputPanel.add(pathField, BorderLayout.CENTER);
        fileInputPanel.add(loadBtn, BorderLayout.EAST);
        sourcePanel.add(new JLabel("File Path:"), BorderLayout.NORTH);
        sourcePanel.add(fileInputPanel, BorderLayout.CENTER);

        // --- Group 2: Key Parameters ---
        JPanel summaryPanel = new JPanel(new BorderLayout());
        styleGroupBox(summaryPanel, "Key Parameters");
        
        summaryPane = new JTextPane();
        summaryPane.setContentType("text/html");
        summaryPane.setEditable(false);
        summaryPane.setBackground(new Color(250, 250, 250));
        summaryPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JScrollPane summaryScroll = new JScrollPane(summaryPane);
        summaryScroll.setBorder(null);
        summaryScroll.setPreferredSize(new Dimension(300, 200)); 
        
        summaryPanel.add(summaryScroll, BorderLayout.CENTER);

        // --- Group 3: Raw Metadata ---
        JPanel rawPanel = new JPanel(new BorderLayout());
        styleGroupBox(rawPanel, "Raw Metadata");
        
        String[] cols = {"Key", "Value"};
        tableModel = new DefaultTableModel(cols, 0);
        rawTable = new JTable(tableModel);
        rawTable.setFillsViewportHeight(true);
        rawTable.setFont(new Font("SansSerif", Font.PLAIN, 11));
        
        JScrollPane rawScroll = new JScrollPane(rawTable);
        rawScroll.setBorder(null);
        rawPanel.add(rawScroll, BorderLayout.CENTER);

        // 组装 Body
        bodyPanel.add(sourcePanel);
        bodyPanel.add(Box.createVerticalStrut(10));
        bodyPanel.add(summaryPanel);
        bodyPanel.add(Box.createVerticalStrut(10));
        bodyPanel.add(rawPanel);
        add(bodyPanel, BorderLayout.CENTER);

        // 3. Footer Area
        JPanel footerPanel = new JPanel(new BorderLayout(0, 5));
        footerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JButton exportBtn = new JButton("Export Report to CSV");
        exportBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        exportBtn.setForeground(ACTION_ORANGE);
        exportBtn.setPreferredSize(new Dimension(100, 40));
        exportBtn.addActionListener(this::onExportClicked);
        footerPanel.add(exportBtn, BorderLayout.NORTH);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Ready (Drag & Drop Supported)");
        progressBar.setForeground(BRAND_BLUE);
        progressBar.setBorder(BorderFactory.createLoweredBevelBorder());
        progressBar.setPreferredSize(new Dimension(100, 20));
        footerPanel.add(progressBar, BorderLayout.SOUTH);

        add(footerPanel, BorderLayout.SOUTH);

        initDragAndDrop();
    }

    private void initDragAndDrop() {
        new DropTarget(this, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>) dtde.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    
                    if (droppedFiles != null && !droppedFiles.isEmpty()) {
                        File file = droppedFiles.get(0); 
                        processFile(file.getAbsolutePath(), file.getName());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    IJ.log("Drag & Drop failed: " + ex.getMessage());
                }
            }
        });
    }

    // =================================================================
    // 核心修改：使用原生 FileDialog + UTF-8 with BOM (解决乱码)
    // =================================================================
    private void onExportClicked(ActionEvent e) {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data to export!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 使用原生文件保存窗口
        FileDialog fd = new FileDialog(this, "Save Report", FileDialog.SAVE);
        fd.setFile("MIA_Report.csv");
        fd.setVisible(true);

        String dir = fd.getDirectory();
        String name = fd.getFile();

        if (dir != null && name != null) {
            File file = new File(dir, name);
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getParent(), file.getName() + ".csv");
            }

            // --- 关键修改开始：使用 OutputStreamWriter 指定 UTF-8 ---
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                
                // 1. 写入 BOM (Byte Order Mark)
                //这是给 Excel 看的，没有它，Excel 打开 UTF-8 CSV 就会乱码
                writer.write("\uFEFF"); 

                // 2. Header
                writer.write("File Path," + pathField.getText() + "\n");
                writer.write("\n");

                // 3. Summary (Key Parameters)
                writer.write("--- Key Parameters ---\n");
                String rawSummary = summaryPane.getDocument().getText(0, summaryPane.getDocument().getLength());
                for (String line : rawSummary.split("\n")) {
                    if (!line.trim().isEmpty()) {
                        writer.write(line.trim() + "\n");
                    }
                }
                writer.write("\n");

                // 4. Raw Metadata Table
                writer.write("--- Raw Metadata ---\n");
                writer.write("Key,Value\n");
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    String key = tableModel.getValueAt(i, 0).toString();
                    Object valObj = tableModel.getValueAt(i, 1);
                    String val = (valObj != null) ? valObj.toString() : "";
                    
                    // CSV 转义：处理逗号和双引号
                    if (key.contains(",")) key = "\"" + key + "\"";
                    
                    // 修复：如果值里本来就有引号，CSV标准是替换为两个双引号
                    if (val.contains("\"")) val = val.replace("\"", "\"\"");
                    if (val.contains(",")) val = "\"" + val + "\"";
                    
                    writer.write(key + "," + val + "\n");
                }

                JOptionPane.showMessageDialog(this, "Export Successful!\nSaved to: " + file.getName());

            } catch (Exception ex) {
                IJ.log("Export failed: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            // --- 关键修改结束 ---
        }
    }

    private void processFile(String path, String name) {
        pathField.setText(name);
        progressBar.setString("Processing...");
        progressBar.setIndeterminate(true);

        new Thread(() -> {
            try {
                MetadataEngine engine = new MetadataEngine();
                engine.parse(path, summaryPane, tableModel);
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                    progressBar.setString("Ready");
                });
            } catch (Exception ex) {
                IJ.log("Error: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                    progressBar.setString("Error");
                });
                ex.printStackTrace();
            }
        }).start();
    }

    private void onLoadClicked(ActionEvent e) {
        // ImageJ 的 OpenDialog 本身通常就是调用 FileDialog，所以这里不用改
        OpenDialog od = new OpenDialog("Choose Microscopy File", "");
        String dir = od.getDirectory();
        String name = od.getFileName();
        if (dir == null || name == null) return;
        processFile(dir + name, name);
    }

    private JPanel createCenteredHeaderPanel(String version) {
        JPanel header = new JPanel(new GridBagLayout());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        header.setBackground(new Color(252, 252, 255));

        JLabel logoLabel = new JLabel();
        URL imgURL = getClass().getResource("/icons/mia.png");
        if (imgURL != null) {
            ImageIcon icon = new ImageIcon(imgURL);
            Image img = icon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
            logoLabel.setIcon(new ImageIcon(img));
        } else {
            logoLabel.setText("🐟");
            logoLabel.setFont(new Font("SansSerif", Font.PLAIN, 32));
        }

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));

        JLabel title = new JLabel("MIA MetaData Inspector");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(BRAND_BLUE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("v" + version + " | © 2026 www.cns.ac.cn");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subtitle.setForeground(TEXT_GRAY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(subtitle);

        header.add(logoLabel);
        header.add(textPanel);

        return header;
    }

    private void styleGroupBox(JPanel panel, String title) {
        Border lineBorder = BorderFactory.createEtchedBorder();
        TitledBorder titledBorder = BorderFactory.createTitledBorder(lineBorder, title);
        titledBorder.setTitleColor(BRAND_BLUE);
        titledBorder.setTitleFont(new Font("SansSerif", Font.BOLD, 12));
        panel.setBorder(titledBorder);
    }
}