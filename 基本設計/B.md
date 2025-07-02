# 2. システム概要

## 2.1 システム構成の全体像

### 提供するシステム
株式会社〇〇が展開する雑貨ブランド向けのオンライン販売サイト（ECサイト）。  
商品の閲覧、検索、購入、注文管理をオンラインで完結できる仕組みを構築する。

### 主なユーザー区分と利用形態

このシステムでは、大きく以下の2種類のユーザーを想定している。

- **一般顧客（エンドユーザー）**  
  商品の閲覧、購入、およびマイページ上での注文履歴確認（任意会員登録による）を行う。  
  主にスマートフォン、PC、タブレットといったデバイスからアクセスすることを想定する。

- **管理者（サイト運営者）**  
  商品情報の登録・編集や、注文状況の管理などを行う。  
  主にPCブラウザからアクセスし、ブラウザ上の管理画面を通じて操作を実施する。

### 利用チャネル
- インターネット経由で公開（HTTPS対応）
- モバイルファーストのレスポンシブデザイン

### インフラ構成（想定）
- クラウド環境（AWS, GCP 等）
- Webアプリケーションサーバ、DBサーバ
- SSL/TLS による暗号化通信

##　2.2システム概要図


<div class="mermaid">
graph TD
    subgraph ユーザー環境
        Client[一般顧客<br>（スマホ・PC・タブレット）]
        Admin[管理者<br>（PCブラウザ）]
    end

    subgraph インターネット
        Firewall[ファイアウォール]
        LoadBalancer[ロードバランサー]
        CDN[CDN（画像配信）]
    end

    subgraph クラウド環境
        subgraph Webサーバー
            WebServer1[Webサーバー1]
            WebServer2[Webサーバー2]
        end

        subgraph アプリケーションサーバー
            AppServer1[Appサーバー1]
            AppServer2[Appサーバー2]
        end

        subgraph DBサーバー
            DB[DBサーバー<br>]
            end

        Storage[S3互換ストレージ]
        ExternalAPI[外部APIサービス]
    end

    %% ユーザーからインターネット
    Client --> Firewall
    Admin --> Firewall

    %% インターネット内部の流れ
    Firewall --> LoadBalancer
    LoadBalancer --> WebServer1
    LoadBalancer --> WebServer2
    WebServer1 --> CDN
    WebServer2 --> CDN

    %% Webサーバーからアプリサーバー
    WebServer1 --> AppServer1
    WebServer2 --> AppServer2

    %% アプリサーバーからDBサーバー
    AppServer1 --> DB
    AppServer2 --> DB

    %% アプリサーバーから外部ストレージ・API
    AppServer1 --> Storage
    AppServer2 --> ExternalAPI

  
    
    style Client fill:#f9f,stroke:#333,stroke-width:1.5px
    style Admin fill:#f9f,stroke:#333,stroke-width:1.5px
    style Firewall fill:#ffcc99,stroke:#333,stroke-width:1.5px
    style LoadBalancer fill:#ffcc99,stroke:#333,stroke-width:1.5px
    style CDN fill:#ccffcc,stroke:#333,stroke-width:1.5px
    style WebServer1 fill:#99ccff,stroke:#333,stroke-width:1.5px
    style WebServer2 fill:#99ccff,stroke:#333,stroke-width:1.5px
    style AppServer1 fill:#6699cc,stroke:#333,stroke-width:1.5px
    style AppServer2 fill:#6699cc,stroke:#333,stroke-width:1.5px
    style DB fill:#99ff99,stroke:#333,stroke-width:1.5px
    style Storage fill:#cccccc,stroke:#333,stroke-width:1.5px
    style ExternalAPI fill:#ff9999,stroke:#333,stroke-width:1.5px
</div>


---

##  2.3各項目の説明

### ユーザー環境
- **一般顧客（スマホ・PC・タブレット）**
  - Webブラウザを通じてサービスを利用するエンドユーザー。
- **管理者（PCブラウザ）**
  - 管理画面にログインし、ユーザー管理や商品管理を行う。

---

### インターネット
- **ファイアウォール**
  - 外部からの不正アクセスを遮断する。
- **ロードバランサー**
  - 複数のWebサーバーへの負荷分散を行い、可用性を確保。
- **CDN（画像配信）**
  - 静的コンテンツ（画像・CSS・JS）を高速配信するためのネットワーク。

---

### クラウド環境
#### Webサーバー
- **Webサーバー1, Webサーバー2**
  - ユーザーからのHTTP(S)リクエストを受け付け、HTML等を返却。
  - 負荷分散によりスケールアウト可能。

#### アプリケーションサーバー
- **Appサーバー1, Appサーバー2**
  - ビジネスロジックを実行し、データベースとの橋渡しを行う。

#### DBサーバー
- **DBサーバー**
 -データを整合性を取って管理する。


#### その他サービス
- **S3互換ストレージ**
  - 画像やファイルなどを保存する。
- **外部APIサービス**
  - 他サービスとの連携を行う。

---