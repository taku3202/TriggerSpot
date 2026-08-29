# TriggerSpot 実装プラン

`SPECIFICATION.md` に基づき、Bluetoothデバイスの接続を検知してWi-Fiテザリングを自動制御するアプリを実装します。

## Proposed Changes

### 1. プロジェクト構成と依存関係
- `minSdk` を 29 (Android 10) に、`targetSdk` を 34 (Android 14) に設定します。
- DataStore (Preferences) と ViewModel Compose の依存関係を追加します。

#### [MODIFY] [build.gradle.kts](file:///home/owner/AndroidStudioProjects/TriggerSpot/app/build.gradle.kts)
- SDKバージョンの更新。
- 依存関係の追加。

#### [MODIFY] [AndroidManifest.xml](file:///home/owner/AndroidStudioProjects/TriggerSpot/app/src/main/AndroidManifest.xml)
- Bluetooth, Foreground Service, Post Notifications, Write Settings などの権限を追加。
- `TriggerSpotService` と `BootReceiver` の宣言。

---

### 2. データレイヤー (Data Layer)
- トリガーとなるデバイス情報とアプリの設定を保持します。

#### [NEW] [TriggerDevice.kt](file:///home/owner/AndroidStudioProjects/TriggerSpot/app/src/main/java/com/example/triggerspot/data/TriggerDevice.kt)
- デバイス名、MACアドレス、自動起動設定を保持するデータクラス。

#### [NEW] [DeviceDataStore.kt](file:///home/owner/AndroidStudioProjects/TriggerSpot/app/src/main/java/com/example/triggerspot/data/DeviceDataStore.kt)
- Preferences DataStore を使用した設定の永続化ロジック。

---

### 3. サービス・ヘルパーレイヤー (Service & Helper Layer)
- バックグラウンド監視とテザリング制御の核となる部分です。

#### [NEW] [TetheringHelper.kt](file:///home/owner/AndroidStudioProjects/TriggerSpot/app/src/main/java/com/example/triggerspot/util/TetheringHelper.kt)
- リフレクションを用いたテザリング制御および設定画面への遷移ロジック。

#### [NEW] [TriggerSpotService.kt](file:///home/owner/AndroidStudioProjects/TriggerSpot/app/src/main/java/com/example/triggerspot/service/TriggerSpotService.kt)
- Bluetooth接続を監視する Foreground Service。通知の管理も行います。

#### [NEW] [BootReceiver.kt](file:///home/owner/AndroidStudioProjects/TriggerSpot/app/src/main/java/com/example/triggerspot/service/BootReceiver.kt)
- 端末起動時にサービスを自動開始するための Receiver。

---

### 4. UIレイヤー (UI Layer)
- Material 3 を使用した、シンプルで直感的なUIを提供します。

#### [NEW] [TriggerSpotViewModel.kt](file:///home/owner/AndroidStudioProjects/TriggerSpot/app/src/main/java/com/example/triggerspot/ui/TriggerSpotViewModel.kt)
- 画面状態の管理、デバイスリストの取得、サービスの開始/停止指示。

#### [MODIFY] [MainActivity.kt](file:///home/owner/AndroidStudioProjects/TriggerSpot/app/src/main/java/com/example/triggerspot/MainActivity.kt)
- Compose UI のエントリポイント。権限リクエストの処理。

#### [NEW] [DashboardScreen.kt](file:///home/owner/AndroidStudioProjects/TriggerSpot/app/src/main/java/com/example/triggerspot/ui/DashboardScreen.kt)
- サービスの状態表示、トリガーデバイス一覧、デバッグボタン。

#### [NEW] [DeviceSelectionDialog.kt](file:///home/owner/AndroidStudioProjects/TriggerSpot/app/src/main/java/com/example/triggerspot/ui/DeviceSelectionDialog.kt)
- ペアリング済みデバイスからトリガーを選択するダイアログ。

## Verification Plan

### Automated Tests
- `gradlew assembleDebug` でビルドが通ることを確認します。

### Manual Verification
- エミュレータ/実機でのUI確認。
- Bluetoothのモック（インテントの発行）によるテザリング起動通知の確認。
- 設定画面への遷移が正しく動作するか確認。
