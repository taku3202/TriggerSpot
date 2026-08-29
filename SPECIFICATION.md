# TriggerSpot 技術仕様書 (Specification) - Updated 2026-08-30

## 1. 概要 (Overview)
`TriggerSpot` は、ユーザーが指定した特定のBluetoothデバイス（車載機、イヤホン等）が接続されたことを検知し、自動的にWi-Fiホットスポット（テザリング）を有効化するAndroidアプリです。OSの制限を回避し、可能な限り自動で、かつ安定した動作を提供することを目的としています。

---

## 2. 技術的な実現方法 (Technical Implementation)
Android 11以降の厳しいセキュリティ制限に対応するため、本アプリでは以下の「マルチレイヤー・アプローチ」を採用しています。

### 2.1 テザリング状態の正確な検知
APIによる状態取得が制限されているデバイスに対応するため、**ネットワークインターフェースのスキャン**を実施します。
- **手法**: `NetworkInterface.getNetworkInterfaces()` を取得し、`ap0`, `wlan1`, `softap` 等のテザリング専用インターフェースが稼働しているかを確認します。
- **メリット**: OSのAPI制限に依存せず、物理的な通信状態から確実にON/OFFを判定可能です。

### 2.2 テザリングの自動制御 (Accessibility Service 活用)
システムアプリ以外に許可されていないテザリング制御を、**ユーザー補助サービス (AccessibilityService)** によるUI操作の自動化で実現します。
- **自動化シーケンス**:
    1. Bluetooth接続を検知。
    2. テザリングがすでにONであれば処理をスキップ。
    3. `GLOBAL_ACTION_QUICK_SETTINGS` を実行し、クイック設定パネルを自動展開。
    4. パネル内から「ホットスポット」「テザリング」「アクセスポイント」等のタイル（ボタン）を検索（日本語・英語・多言語対応）。
    5. ボタンがOFFであれば自動クリックしてONに変更。
    6. 完了後、自動的にパネルを閉じて元の画面に戻る。
- **安定化処理**: 連打防止のためのクールダウン時間、および判定の遅延リトライ機能を搭載。

---

## 3. 主要な機能要件 (Functional Requirements)

### 3.1 Bluetooth監視とトリガー
- `ACTION_ACL_CONNECTED` / `ACTION_ACL_DISCONNECTED` システムブロードキャストを購読。
- 登録されたデバイスのMACアドレス（大文字小文字不問）と一致した場合にアクションを実行。

### 3.2 フォアグラウンドサービス
- 監視を安定させるため、`connectedDevice` 型の **Foreground Service** として動作。
- 常駐通知を表示し、システムによるメモリ解放から保護。

### 3.3 永続化
- **Jetpack DataStore (Preferences)** を使用。
- トリガーデバイスのリストは Kotlin Serialization を用いて JSON 形式で保存。

---

## 4. パーミッション (Permissions)
アプリの動作には以下の権限が必要です。

```xml
<!-- Bluetooth通信用 -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />

<!-- テザリング状態監視用 -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- サービス・自動化用 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
<uses-permission android:name="android.permission.WRITE_SETTINGS" />
```

---

## 5. UI/UX 設計 (UI Design)

### 5.1 ダッシュボード
- サービス稼働状態のトグルスイッチ。
- 権限不足時の警告カード（タップで各設定画面へ直接遷移）。
- 登録済みトリガーデバイスの一覧表示。

### 5.2 デバイス選択
- ペアリング済みデバイスをリストアップし、タップで簡単に追加可能。

---

## 6. アーキテクチャ (Architecture)
- **UI**: Jetpack Compose (Material 3)
- **ViewModel**: StateFlow を用いた単一データソースの管理。
- **Automation**: `TriggerSpotAccessibilityService` によるUI操作。
- **Helper**: `TetheringHelper` によるマルチレイヤーな制御試行。

---

## 7. 既知の制限事項 (Constraints)
- ユーザー補助機能の初回有効化は、ユーザーによる手動操作が必要です。
- クイック設定パネルに「ホットスポット」タイルが表示されている必要があります。
