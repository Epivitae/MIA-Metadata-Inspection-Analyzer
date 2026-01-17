<div align="center">
<img src="src/main/resources/icons/mia.png" width="120" alt="MIA Logo" />

# MIA: Metadata Inspection Analyzer

[![Release](https://img.shields.io/github/v/release/Epivitae/MIA-Metadata-Inspection-Analyzer?color=blue)](https://github.com/Epivitae/MIA-Metadata-Inspection-Analyzer/releases)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-ImageJ%20%7C%20Fiji-orange)](https://imagej.net/)

**MIA (Microscopy Information Assistant, or Metadata Inspection Analyzer)** is a lightweight, "idiot-proof" tool designed for biologists to instantly extract hidden metadata from microscopy files without writing code.

</div>

> Part of the **Biosensor Tools** suite (FIA, RIA, NIA, MIA, WinMan).

<div align="center">

<img src="image/mia-gui.png" width="220" alt="MIA Microscopy Metadata Inspection Analyzer Gui" />

</div>


## 🚀 Key Features

* **Instant Inspection**: Drag & Drop any microscopy file to view dimensions, pixel size, and time intervals.
* **Spectrum Mapping**: Auto-generates color-coded tags for Excitation/Emission wavelengths (e.g., <span style="color:#00FFFF">● Ex488</span>).
* **Deep Extraction**:
    * **Olympus (.oir)**: Advanced parsing of proprietary tags (Exact Laser Lines, Emission Ranges, Real-Time Intervals).
    * **General (.czi, .lif, .nd2)**: Standard OME-XML support via Bio-Formats.
* **Report Export**: One-click export to CSV (Excel compatible) containing both summary and full raw metadata.

## 📥 Installation

1.  Download `MIA-1.0.0.jar` from the [Releases](https://github.com/Epivitae/MIA-Metadata-Inspection-Analyzer/releases) page.
2.  Drag the `.jar` file into your **Fiji/ImageJ** window (or copy to the `plugins/` folder).
3.  Restart Fiji.
4.  Find it under: `Plugins > Epivitae > MIA`.

## 🎮 Usage

1.  **Launch** the plugin.
2.  **Drag and drop** a microscopy file into the window.
3.  View the **Key Parameters** summary.
4.  Click **Export Report** to save details to a CSV file.

## 🛠️ Supported Formats

| Format | Extension | Support Level |
| :--- | :--- | :--- |
| **Olympus** | `.oir` | **Full** (Hybrid Raw Parsing) |
| **Zeiss** | `.czi` | Standard (OME-XML) |
| **Leica** | `.lif` | Standard (OME-XML) |
| **Nikon** | `.nd2` | Standard (OME-XML) |
| **Others** | `.tiff`, etc. | Basic Bio-Formats Support |

## ⚖️ License

MIT License. Copyright (c) 2026 Epivitae (CNS Team).