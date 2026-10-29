# 🪚 PipeCut - Linear Cut List & Pipe Optimizer for Android

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-blue.svg)](https://developer.android.com/jetpack/compose)
[![MinSDK](https://img.shields.io/badge/Min%20SDK-24%20%28Android%207.0%29-brightgreen.svg)](https://developer.android.com/about/dashboards)
[![TargetSDK](https://img.shields.io/badge/Target%20SDK-37-orange.svg)](https://developer.android.com/about/versions/15)

**PipeCut** is a high-performance 1D Cutting Stock & Linear Cut List Optimizer designed for metalworkers, carpenters, plumbers, fabricators, and DIY enthusiasts. It minimizes raw material scrap waste and maximizes material utilization when cutting pipes, tubes, bars, beams, lumber, and profiles from standard inventory stock.

---

## ✨ Features

- 🎯 **Advanced 1D Cutting Stock Optimization**: Employs multi-strategy heuristics (Best-Fit Decreasing, First-Fit Decreasing, Best Stock Selection, 2-Opt Swaps) to calculate optimal cut layouts with maximum material yield.
- 📐 **Comprehensive Blade & Trim Adjustments**:
  - **Saw Blade Kerf**: Accounts for material loss caused by blade thickness per cut.
  - **End Trim Margin**: Squaring trim on single or both pipe ends.
  - **Extra Piece Allowance**: Additional margin for facing, beveling, or safety per cut.
  - **Reusable Offcut Threshold**: Distinguishes reusable stock offcuts from scrap waste.
- 📏 **Multi-Unit Support & Real-Time Conversion**: Full metric and imperial support—Millimeters (`mm`), Centimeters (`cm`), Meters (`m`), Inches (`in`), and Feet (`ft`).
- 📊 **Visual Pipe Diagrams**: Interactive, color-coded visual representations of cut sequences, kerf losses, end trims, offcuts, and remaining stock.
- 📄 **Export & Sharing**: Generate detailed text reports and CSV spreadsheets of cutting sequences to export or share with workshop operators.
- 💾 **Project History & Saved Profiles**: Save cutting project configurations locally for quick retrieval and re-calculation.
- 🎨 **Modern Material 3 Design**: Clean Jetpack Compose UI with Light, Dark, and System dynamic themes and full edge-to-edge support.

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 100%
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (`androidx.compose.material3`)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Domain Layer
- **State Management**: Kotlin StateFlow & Coroutines
- **Local Storage**: `SharedPreferences` with JSON serialization for project history and settings
- **Build System**: Gradle Version Catalogs (`libs.versions.toml`)

### Project Directory Structure

```
PipeCut/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/pipecut/
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/         # Data classes (StockPipe, RequiredCut, CuttingSettings, etc.)
│   │   │   │   │   └── repository/    # Project & Settings repository
│   │   │   │   ├── domain/
│   │   │   │   │   ├── algorithm/     # PipeCutOptimizer (1D Bin Packing & Heuristics)
│   │   │   │   │   ├── converter/     # Length Unit Converter (mm, cm, m, in, ft)
│   │   │   │   │   └── exporter/      # Text & CSV Plan Exporters
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/    # PipeDiagram, MetricCard, NumberInputField, UnitSelector
│   │   │   │   │   ├── screens/       # Home, Results, History, Settings, About, Splash
│   │   │   │   │   ├── theme/        # Material 3 Color, Type, Theme definitions
│   │   │   │   │   └── viewmodel/    # PipeCutViewModel
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/                   # Drawables, mipmaps, XML resources
│   │   └── test/                      # Unit tests for Kerf, Waste, Unit Conversions & Optimization
│   └── build.gradle.kts
├── build.gradle.kts
└── README.md
```

---

## 🔬 How The Optimization Works

`PipeCutOptimizer` implements a multi-pass optimization engine operating with integer/double internal precision normalized to millimeters (`mm`):

1. **Input Validation**: Checks for impossible cuts exceeding available stock length or invalid kerf/trim settings.
2. **Heuristic Evaluation**:
   - **Best-Fit Decreasing (BFD)**: Prioritizes minimal remaining bin space per stock piece.
   - **First-Fit Decreasing (FFD)**: Fast allocation across active inventory stock.
   - **Best Stock Selection**: Chooses optimal combination when multiple stock sizes are defined.
   - **2-Opt Swaps**: Post-processing optimization that interchanges cuts across stock pipes to maximize reusable offcuts and consolidate scrap waste.
3. **Yield & Scrap Classification**: Offcut lengths exceeding `minReusableLength` are classified as **Reusable Stock**, while smaller remainders are counted as **Scrap Waste**.

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Ladybug (2024.2.1) or newer recommended.
- **JDK**: Java 11 or higher.
- **Android Device / Emulator**: Android 7.0 (API Level 24) or higher.

### Building & Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/abdullah-ateeq/PipeCut.git
   cd PipeCut
   ```

2. **Open in Android Studio**:
   - Select `Open an Existing Project` and choose the `PipeCut` folder.

3. **Build the APK**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run Unit Tests**:
   ```bash
   ./gradlew test
   ```

---

## 📱 Screenshots & User Interface

- **Home Screen**: Add available stock pipe inventory and required target cuts with quantities and labels.
- **Results & Diagram Screen**: Detailed scorecard showing material yield %, scrap waste %, total stock consumed, kerf losses, and visual cut diagram sequences for each pipe.
- **History**: Load, review, or delete past cutting projects.
- **Settings**: Configure default unit preferences, saw blade kerf width, end trims, and theme preferences.

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check out the [issues page](https://github.com/abdullah-ateeq/PipeCut/issues).

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
