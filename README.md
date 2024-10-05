# Redmine Team Tracker

Redmine Team Trackerは、Redmineチケットの集計を行うためのツールです。このツールは、指定された期間内のチケットのステータスや担当チームの履歴を追跡し、集計結果をSQLiteデータベースに保存します。

## 特徴

- Redmineチケットのステータスとチーム履歴の集計
- SQLiteデータベースへの集計結果の保存
- コマンドラインインターフェースを使用した操作

## 必要条件

- Java 8以上
- Gradle
- Redmineデータベースへのアクセス権

## セットアップ

1. リポジトリをクローンします。

   ```bash
   git clone https://github.com/mito99/redmine_team_tracker.git
   cd redmine_team_tracker
   ```

2. 必要な依存関係をインストールします。

   ```bash
   ./gradlew build
   ```

3. `.env`ファイルを作成し、Redmineデータベースの接続情報を設定します。

   ```
   REDMINE_JDBC_URL=jdbc:mysql://your-redmine-db-url
   REDMINE_DB_USER=your-db-user
   REDMINE_DB_PASSWORD=your-db-password
   SQLITE_DB_URL=jdbc:sqlite:output.db
   ```

## 使用方法

コマンドラインから以下のように実行します。
