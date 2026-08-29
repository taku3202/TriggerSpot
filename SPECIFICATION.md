# TriggerSpot 技術仕様書 (Specification)

## 1. 概要 (Overview)
`TriggerSpot` は、ユーザーが指定した特定のBluetoothデバイス（車載ハンズフリー、ナビゲーションシステム、Bluetoothイヤホンなど）がAndroid端末に接続されたことを検知し、自動的にWi-Fiテザリング（ホットスポット）を有効化して、他のデバイス（車載Androidナビ、タブレット、PC等）へシームレスにインターネット接続を提供するユーティリティアプリです。

---

## 2. 背景と技術的な実現可能性の分析 (Technical Feasibility & Constraints)
Android OSはセキュリティおよび省電力性の観点から、アプリによるWi-Fiテザリングの直接制御に厳しい制限を設けています。本アプリを開発・運用するにあたり、以下のプラットフォーム制約を考慮した設計を行います。

### 2.1 AndroidのテザリングAPI制限
1. **LocalOnlyHotspot (`WifiManager.startLocalOnlyHotspot`)**
   - **特徴**: アプリから直接起動できるパブリックAPI。ただし、**インターネット共有（テザリング）は行えず**、ローカルなWi-Fiネットワークのみを構築します。
   - **用途**: インターネット共有が不要で、端末間の直接通信（ファイル転送など）のみを行う場合。本アプリの主目的（インターネット共有）には適しませんが、フォールバック（接続テスト等）として検討します。

2. **Tethering API (`ConnectivityManager.startTethering`)**
   - **特徴**: 実際のインターネット共有を伴うテザリングを有効化するAPI。
   - **制限**: `@SystemApi`（システムアプリ専用）および `android.permission.TETHER_PRIVILEGED`（署名・システムアプリ専用パーミッション）が必要です。通常のサードパーティアプリからは直接呼び出せません。

### 2.2 サードパーティアプリでのテザリング有効化アプローチ（解決策・代替案）
本アプリでは、実行される端末のOSバージョンやメーカー特性に応じて最適な動作をするよう、以下のマルチレイヤーアプローチを採用します。

| レベル | アプローチ方法 | メリット | デメリット / 制限 |
| :--- | :--- | :--- | :--- |
| **A (推奨/実用的)** | **通知経由の手動起動・設定画面ナビゲーション** | 安全、Google Playストアのポリシーに100%適合。 | 接続時にユーザーが通知を1タップする必要がある（完全自動ではない）。 |
| **B (自動化/ハック)** | **隠しAPI（リフレクション）と `WRITE_SETTINGS` 権限** | 一部の端末・Androidバージョン（主にAndroid 10以下、一部の11+メーカー端末）で完全自動化が可能。 | 将来のOSアップデートで動作しなくなる可能性。Playストアの審査で制限される場合がある。 |
| **C (最上位自動化)** | **ユーザー補助サービス (AccessibilityService) の活用** | 接続検知時にテザリング設定画面をバックグラウンドまたは一瞬表示させ、プログラムで自動的にスイッチをONにする。 | ユーザー補助権限の取得が必要。設定手順がやや複雑。 |

本仕様書では、**「レベルA（通知・ナビゲーション）」を基本（フォールバック）とし、可能な限り「レベルB（リフレクションまたはWRITE_SETTINGS等による自動有効化）」を試行するハイブリッド構成**で設計します。

---

## 3. 機能要件 (Functional Requirements)

### 3.1 Bluetoothデバイス管理
- 端末にペアリング済みのBluetoothデバイス一覧を取得・表示する。
- ユーザーが一覧から特定のデバイスを選択し、「トリガーデバイス」として登録できる（複数登録可）。
- 各トリガーデバイスに対して、以下の個別設定を行える：
  - 自動起動のオン/オフ
  - 切断時の動作（自動的にテザリングをオフにするか否か）

### 3.2 バックグラウンド監視（Bluetooth接続検知）
- バックグラウンドでBluetoothの接続状態を常時監視する。
- システムブロードキャスト `BluetoothDevice.ACTION_ACL_CONNECTED` および `ACTION_ACL_DISCONNECTED` を検知する。
- 常時監視を安定させるため、フォアグラウンドサービス（Foreground Service）として実装し、ステータスバーに常時通知を表示する。

### 3.3 テザリング制御処理
- 登録されたBluetoothデバイスの接続を検知した場合：
  1. システムにWi-Fiテザリングの有効化を試行（リフレクション等の裏技的APIを利用）。
  2. 自動起動に失敗、または非対応のOSバージョンの場合、バイブレーションや高優先度通知を発生させ、ユーザーに通知する。通知をタップすると、テザリング設定画面（Tethering Settings）へダイレクトに遷移する。
- 登録されたBluetoothデバイスの切断を検知した場合：
  - 自動でテザリングをオフにする（設定がオンの場合）。

---

## 4. 非機能要件 (Non-Functional Requirements)

### 4.1 パフォーマンス・省電力性
- Bluetooth接続・切断はOSのシステムブロードキャストを契機に動くため、ポーリング処理（定期ループによる監視）は行わず、バッテリー消費を極限まで抑える。
- フォアグラウンドサービス動作時も、不要なCPUウェイクロックは保持しない。

### 4.2 対応OSバージョン
- **最小サポート（Min SDK）**: Android 10 (API レベル 29)
- **ターゲット（Target SDK）**: Android 14 (API レベル 34)

### 4.3 UI/UXデザイン
- Android 12以降で推奨される **Material Design 3 (Material You)** に準拠。
- ダークモードに完全対応。
- 初回起動時に必要なパーミッション（Bluetooth、通知、システム設定書き込み等）の許認可をグラフィカルに説明する「セットアップウィザード」を搭載。

---

## 5. システム・アーキテクチャ (Architecture & Tech Stack)

```
[ UI Layer (Jetpack Compose) ]
             ▲
             │ (Flow / StateObserver)
[ ViewModel (TriggerSpotViewModel) ]
             ▲
             │ (Data Store Access / Control commands)
[ Repository (DeviceRepository) ] ───▶ [ Preferences / DataStore (設定保存) ]
             ▲
             │ (Service State & Trigger Broadcasts)
[ Foreground Service (TriggerSpotService) ]
   ├── [ BroadcastReceiver (Bluetooth & Boot Receiver) ]
   └── [ TetheringManagerHelper (Reflection / OS Specific Tethering controls) ]
```

### 5.1 技術スタック
- **言語**: Kotlin
- **UIフレームワーク**: Jetpack Compose (マテリアルデザイン3)
- **非同期・並行処理**: Kotlin Coroutines & Flow
- **設定/データ保存**: Jetpack DataStore (Preferences DataStore)
- **バックグラウンドサービス**: Foreground Service（接続型デバイス向けサービス）
- **DI・コンポーネント管理**: 手動DI、または軽量な依存注入パターン

---

## 6. パーミッション要件 (Permissions)

アプリが動作するために、マニフェストファイルに以下の権限を宣言し、実行時にユーザーから許可を得る必要があります。

```xml
<!-- Bluetoothの接続状態検知用 -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<!-- Android 12 (API 31) 以上でペアリング済デバイス名等を取得するために必要 -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- 常時起動サービス（フォアグラウンドサービス）用 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<!-- Android 14 (API 34) でフォアグラウンドサービスに型指定が必要なため -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />

<!-- 端末起動時に自動でサービスを開始するため -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Android 13 (API 33) 以上での通知表示用 -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- テザリング有効化の試行用（システム設定の書き込み） -->
<uses-permission android:name="android.permission.WRITE_SETTINGS"
    tools:ignore="ProtectedPermissions" />
```

---

## 7. 画面設計 (UI Screen Structure)

本アプリは、シンプルで迷わない2画面構成（またはタブ構成）で設計します。

### 7.1 メインダッシュボード画面 (Dashboard Screen)
- **ステータス表示**:
  - バックグラウンド監視サービスの稼働状態（稼働中 / 停止中）のトグルスイッチ。
  - 現在のテザリング状態（ON / OFF）。
  - 現在接続中のBluetoothデバイス名（存在する場合）。
- **トリガーデバイス設定カード**:
  - 現在登録されているトリガーデバイスのリスト。
  - リスト項目：デバイス名、MACアドレス、接続時アクション設定（「テザリングをONにする」トグル等）。
  - デバイスの追加ボタン（フローティングアクションボタン、またはリスト下部）。
- **デバッグ・テストセクション**:
  - 「今すぐテザリングのテスト起動」ボタン。

### 7.2 デバイス追加ダイアログ / 画面 (Device Selection Screen)
- ペアリング済みのBluetoothデバイスの一覧をリスト表示。
- 登録したいデバイスをタップすることで、簡単にトリガーリストに追加できる。

### 7.3 設定・権限管理画面 (Settings & Permissions Screen)
- アプリが正しく動作するために必要な各種権限のステータス（許可済 / 未許可）を一目で確認できるインジケーター。
- 「バッテリー最適化の除外（Dozeモード回避）」設定へのショートカットボタン。

---

## 8. 実装設計詳細 (Implementation Details)

### 8.1 Bluetooth接続の検知方法
`BroadcastReceiver` を定義し、以下の意図（Intent Filter）を購読します。

```kotlin
val filter = IntentFilter().apply {
    addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
    addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
}
context.registerReceiver(bluetoothReceiver, filter)
```

受信時、`intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)` から接続されたデバイスの情報を抽出し、DataStoreに登録されたMACアドレスと照合します。

### 8.2 テザリングのトグルの試行コード (Tethering Control Helper)
セキュリティ制限を回避しながらテザリングを制御するために、ヘルパークラス `TetheringHelper` を作成し、端末のバージョンごとに処理を分岐します。

```kotlin
object TetheringHelper {
    fun setWifiTetheringEnabled(context: Context, enabled: Boolean): Boolean {
        // 1. Android 8.0 ~ 10 に向けたリフレクションによる隠しAPI呼び出しの試行
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val method = wifiManager.javaClass.getMethod("setWifiApEnabled", WifiConfiguration::class.java, Boolean::class.javaPrimitiveType)
            // 一部の古い、またはカスタムOS向けの処理
            return method.invoke(wifiManager, null, enabled) as Boolean
        } catch (e: Exception) {
            // リフレクション失敗時
        }

        // 2. ConnectivityManagerのstartTetheringを介したリフレクション（WRITE_SETTINGS権限が必要な場合あり）
        // ※ 失敗時はfalseを返し、レベルA（設定画面遷移通知）へフォールバックする
        return false
    }

    fun openTetheringSettings(context: Context) {
        val intent = Intent().apply {
            action = Intent.ACTION_MAIN
            className = "com.android.settings", "com.android.settings.TetherSettings"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // フォールバック：無線設定画面を開く
            val fallbackIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        }
    }
}
```

---

## 9. 開発・検証フェーズ (Milestones)

1. **フェーズ1: 骨格の構築 (UI & Background Foundation)**
   - Jetpack ComposeによるUI画面の実装。
   - Preferences DataStoreによるトリガーデバイス保存ロジック。
   - Foreground Serviceと常駐通知の設定。
2. **フェーズ2: Bluetooth監視の実装 (Bluetooth Integration)**
   - `BluetoothDevice.ACTION_ACL_CONNECTED` の検知機能と、フォアグラウンドサービスとの連携。
   - Android 12+ 向けBluetoothパーミッションの適切なリクエスト処理。
3. **フェーズ3: テザリング制御とフォールバック (Tethering Implementation)**
   - `TetheringHelper` による各種有効化試行処理の実装。
   - 自動起動失敗時の高優先度通知機能（設定画面へのリンク付き）の実装。
4. **フェーズ4: テスト・最適化 (Verification & Optimization)**
   - 複数デバイス、OSバージョンでのテザリング連携確認。
   - 低消費電力動作の確認、および強制終了への耐性テスト（サービス再起動処理）。
