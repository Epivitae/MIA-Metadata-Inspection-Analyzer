package com.epivitae.mia;

import ij.plugin.PlugIn;
import com.epivitae.mia.gui.InspectorFrame;

import javax.swing.SwingUtilities;

/**
 * Plugin entry point for ImageJ.
 */
public class MIA_Main implements PlugIn {

    @Override
    public void run(String arg) {
        // 建议在 EDT 线程中启动 Swing 界面
        SwingUtilities.invokeLater(() -> {
            InspectorFrame frame = new InspectorFrame();
            frame.setVisible(true);
        });
    }
    
    /**
     * Main method for testing/debugging outside of ImageJ.
     */
    public static void main(String[] args) {
        new ij.ImageJ(); // Open ImageJ console
        new MIA_Main().run("");
    }
}