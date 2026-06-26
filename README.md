# HTTP 100-Continue 実験アプリ

モバイルデバイスでHTTP 100-Continueの挙動を実験するためのアプリです。

## 現象

スマホアプリがバックグラウンドに移行したり、画面がオフになったり、オフラインになった場合に、
サーバーにHTTPヘッダーだけが届いてボディが届かないという現象を調査します。

## ローカル開発

### 前提条件
- Java 17以上
- Node.js 18以上
- npm

### 手順

1. **フロントエンドをビルドしてSpring Bootに同梱する**

```bash
cd frontend
npm install
npm run build:prod
```

2. **Spring Bootを起動する**

```bash
cd backend
./mvnw spring-boot:run
```

3. **ブラウザでアクセス**

```
http://localhost:8080
```

### 開発時（フロントエンドのホットリロード）

```bash
# ターミナル1: バックエンド起動
cd backend
./mvnw spring-boot:run

# ターミナル2: フロントエンド開発サーバー起動（プロキシ設定あり）
cd frontend
npm start
```

フロントエンドは `http://localhost:4200` でアクセスできます。
`/api` へのリクエストは `http://localhost:8080` にプロキシされます。

## Render.com へのデプロイ

### 前提条件
- Render.comのアカウント
- GitHubリポジトリ

### 手順

1. フロントエンドをビルドしてコミット

```bash
cd frontend
npm install
npm run build:prod
cd ..
git add -A
git commit -m "Build frontend"
git push
```

2. Render.comで新しいWebサービスを作成
   - GitHubリポジトリを接続
   - `render.yaml` が自動検出される
   - または手動でDocker設定:
     - **Dockerfile Path**: `./backend/Dockerfile`
     - **Docker Build Context Directory**: `./backend`

3. デプロイ完了後、提供されたURLでアクセス

## 実験方法

### 基本的な実験

1. スマホのブラウザでアプリを開く
2. 「遅延設定」で実験ウィンドウ秒数を設定（デフォルト: 10秒）
3. テストメッセージを入力して「送信」をタップ
4. 送信直後（「待機中...」が表示されたら）すぐにスマホをバックグラウンドにするか画面をオフにする
5. 10秒後にスマホを確認する
6. リアルタイムログで結果を観察する

### 観察ポイント

| 結果 | 意味 |
|------|------|
| ✅ ボディ受信完了 | バックグラウンドでもボディは送信されていた |
| ❌ エラー/接続断 | バックグラウンド移行でTCPコネクションが切れた |
| ⏳ ヘッダーのみ受信 | 100-Continue待ちでボディが来なかった |

### 技術的な詳細

- `Expect: 100-continue` ヘッダーがある場合、TomcatはgetInputStream()が呼ばれた時点で `100 Continue` を返す
- 意図的にgetInputStream()を遅らせることで、100 Continueの送信タイミングを制御する
- これにより、サーバーがボディ読み取りを開始するまでの間にスマホの状態変化をテストできる

## API エンドポイント

| メソッド | パス | 説明 |
|---------|------|------|
| POST | /api/data | データ送信（遅延付き） |
| GET | /api/logs | 直近50件のログ取得 |
| GET | /api/events | SSEリアルタイムストリーム |
| POST | /api/config | 遅延設定変更 |
| GET | /api/config | 現在の設定取得 |

### POST /api/data

リクエスト:
```json
{ "message": "テストメッセージ" }
```

レスポンス:
```json
{
  "id": "uuid",
  "status": "ok",
  "received": "テストメッセージ",
  "timestamp": "2024-01-01T00:00:00Z",
  "headerReceivedAt": "2024-01-01T00:00:00Z",
  "bodyReceivedAt": "2024-01-01T00:00:10Z",
  "delayMs": 10000
}
```
