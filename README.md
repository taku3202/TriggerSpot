# TriggerSpot

TriggerSpot is a utility Android application that automatically enables Wi-Fi tethering (hotspot) when specific Bluetooth devices (such as car systems or earphones) are connected. It is designed to work reliably on modern Android versions by leveraging Accessibility Services to interact with system settings.

## Features

- **Automated Hotspot Toggling**: Detects Bluetooth connection/disconnection and triggers hotspot state.
- **Modern Android Support**: Uses a multi-layered approach to bypass API restrictions on Android 11+.
- **Quick Settings Integration**: Employs Accessibility Services to automatically interact with the Quick Settings panel for a seamless experience.
- **Accurate State Detection**: Scans network interfaces to reliably determine if the hotspot is active, even when standard APIs fail.
- **Background Persistence**: Operates as a Foreground Service to ensure continuous monitoring even when the app is not in use.
- **Material 3 UI**: Modern, intuitive interface built with Jetpack Compose.

## How It Works

1.  **Bluetooth Monitoring**: The app listens for `ACTION_ACL_CONNECTED` broadcasts.
2.  **Interface Scanning**: When a connection is detected, it checks the physical network interfaces (like `ap0`) to see if the hotspot is already ON.
3.  **Automation Sequence**: If the hotspot is OFF, it uses the **Accessibility Service** to:
    - Expand the Quick Settings panel.
    - Locate the "Hotspot" or "Tethering" tile.
    - Click it to enable.
    - Close the panel and return to the previous screen.

## Installation & Setup

1.  **Clone & Build**: Import the project into Android Studio and deploy to your device.
2.  **Permissions**:
    - Grant **Bluetooth** and **Notification** permissions when prompted.
    - Enable **System Settings Writing** (`WRITE_SETTINGS`) via the in-app prompt.
3.  **Accessibility Service**:
    - Go to **Settings > Accessibility > TriggerSpot** and turn it **ON**. This is crucial for the automated UI operations.
4.  **Quick Settings**:
    - Ensure that the "Hotspot" or "Tethering" tile is visible in your Quick Settings panel (swipe down from the top). If not, add it via the "Edit" (pencil) icon.

## Usage

1.  Open TriggerSpot.
2.  Tap the **"+" button** to add a paired Bluetooth device as a trigger.
3.  Toggle the **Monitoring Active** switch to start the background service.
4.  Connect your Bluetooth device, and watch TriggerSpot do the rest!

## Technical Stack

- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with StateFlow
- **Data Persistence**: Jetpack Preferences DataStore
- **Serialization**: Kotlinx Serialization (JSON)
- **Background**: Android Foreground Service
- **Automation**: AccessibilityService API

## Disclaimer

This app uses Accessibility Services to automate UI interactions. It only interacts with the Hotspot toggle in your Quick Settings panel. Use it responsibly and in accordance with your local network policy.
