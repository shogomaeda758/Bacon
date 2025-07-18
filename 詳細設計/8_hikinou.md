## 8. 非機能要件詳細

### 8.1 セキュリティ

**認証・認可**
- 認証はセッションベース（HTTP Cookie）で実施し、ログイン後にセッションIDを発行し、ブラウザのCookieに格納して認証に利用する。
- 認可はロールベースアクセス制御（RBAC）を採用し、一般ユーザを対象に各画面・API単位でアクセス権限を定義する。
- セッションIDはHTTPOnly属性とSecure属性を設定し、JavaScriptから参照できないようにしつつHTTPS通信限定とする。

**通信の保護**
- 全ての通信はHTTPS（TLS1.2以上）により暗号化し、平文データが送信されないようにする。
- サーバ証明書の期限は監視対象とし、有効期限切れがないよう自動更新または計画的な差替を行う。

**アプリケーションレベルの対策**
- SQLインジェクション対策として、プリペアドステートメント（PreparedStatement）を使用し、ユーザ入力はすべてバインド変数を通じて処理する。
- XSS対策として、出力箇所で適切なHTMLエスケープを行い、ヘッダに `X-Frame-Options: DENY`、`X-Content-Type-Options: nosniff` を付与する。
- 外部からの入力は全項目でサーバサイドバリデーションを実施し、不正値が登録・処理されないようにする。

---

### 8.2 性能

**性能目標**
- API・画面表示のレスポンスは通常時に3秒以内の応答を目指す。
- データベースアクセス（一般的なSELECT）は0.1秒未満で完了することを基準とする。

**実現のための工夫**
- ページネーション（limit/offset）を用い、大量データを一括返却しない設計とする。
- 頻繁に検索されるカラムや条件にインデックスを設定し、テーブルフルスキャンを回避する。
- マスタ系データはアプリケーション起動時にメモリにキャッシュする方式を採用し、DBアクセス回数を抑制する。
- N+1問題を避けるため、ORM（JPA/Hibernate）のFetchTypeを適切に設定し、必要に応じてJOIN FETCHでまとめて取得する。

---

### 8.3 可用性

**可用性確保の方針**
- プロセスは `systemd` により監視し、異常終了時には自動再起動する設定を行う。
- データベースは単一構成（シンプルなマスタのみ）としつつ、障害発生時には手動で迅速に復旧できる手順を整備する。

---

### 8.4 バックアップ・リストア

**バックアップ**
- データベースは `pg_dump` による日次フルバックアップを取得し、必要に応じてWALアーカイブ（Write Ahead Log）を利用し、増分リカバリを可能とする。
- ファイルアップロードを伴う場合はアップロードディレクトリも同様に日次でスナップショットを取得する。

**リストア**
- 障害発生時は、直近の `pg_dump` バックアップをリストア後、WALファイルを適用して障害発生直前まで復旧する。
- アプリケーションサーバ障害時は、新規インスタンスへ再デプロイし設定ファイルを復元することで短時間で復旧する。
