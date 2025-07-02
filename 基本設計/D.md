## 5. データ設計
本節では、システムにおける主なエンティティ間の関係と、主要なデータの流れの概要を示す。

### 5.1. 概念データモデル（ER図）
<div class='mermaid'>
erDiagram
    CUSTOMER ||--o{ ORDER : has
    ORDER ||--o{ ORDER_DETAIL : contains
    PRODUCT ||--o{ ORDER_DETAIL : has
    PRODUCT ||--o{ CATEGORY : belongs_to
    ADMIN ||--o{ PRODUCT : manages
    ADMIN ||--o{ ORDER : manages
    ORDER ||--o{ PAYMENT_METHOD : uses
    ORDER ||--o{ ORDER_STATUS : has

    CUSTOMER {
        string customer_id PK "顧客ID"
        string email UK "メールアドレス"
        string password "パスワード (ハッシュ化)"
        string last_name "姓"
        string first_name "名"
        string phone_number "電話番号"
        string address "住所"
        datetime created_at "登録日時"
        datetime updated_at "更新日時"
    }

    PRODUCT {
        string product_id PK "商品ID"
        string product_name "商品名"
        string description "商品説明"
        decimal price "単価"
        string image_url "画像URL"
        string material "素材"
        string size "サイズ"
        string category_id FK "カテゴリID"
        int stock_quantity "在庫数"
        datetime created_at "登録日時"
        datetime updated_at "更新日時"
    }

    CATEGORY {
        string category_id PK "カテゴリID"
        string category_name "カテゴリ名"
        datetime created_at "登録日時"
        datetime updated_at "更新日時"
    }

    ORDER {
        string order_id PK "注文ID"
        string customer_id FK "顧客ID（会員のみ）"
        string order_email "注文者メールアドレス"
        string order_name "注文者氏名"
        string order_phone_number "注文者電話番号"
        string order_address "注文者住所"
        decimal total_price "合計金額"
        decimal shipping_fee "配送料"
        string payment_method_code FK "支払い方法コード"
        string order_status_code FK "注文ステータスコード"
        datetime payment_confirmed_at "入金確認日時"
        datetime order_date "注文日時"
        datetime created_at "登録日時"
        datetime updated_at "更新日時"
    }

    ORDER_DETAIL {
        string order_detail_id PK "注文明細ID"
        string order_id FK "注文ID"
        string product_id FK "商品ID"
        int quantity "数量"
        decimal unit_price "購入時単価"
        datetime created_at "登録日時"
        datetime updated_at "更新日時"
    }

    ADMIN {
        string admin_id PK "管理者ID"
        string email UK "メールアドレス"
        string password "パスワード (ハッシュ化)"
        string user_name "ユーザー名"
        datetime created_at "登録日時"
        datetime updated_at "更新日時"
    }

    PAYMENT_METHOD {
        string payment_method_code PK "支払い方法コード"
        string label "表示名"
        int sort_order "表示順"
        bool is_active "有効フラグ"
        datetime created_at "登録日時"
        datetime updated_at "更新日時"
    }

    ORDER_STATUS {
        string order_status_code PK "注文ステータスコード"
        string label "表示名"
        string description "ステータス説明"
        int sort_order "表示順"
        bool is_active "有効フラグ"
        datetime created_at "登録日時"
        datetime updated_at "更新日時"
    }

</div>

- **エンティティ**: CUSTOMER(顧客), PRODUCT(商品), CATEGORY(カテゴリ), ORDER(注文), ORDER_DETAIL(注文明細), ADMIN(管理者),PAYMENT_METHOD (支払い方法マスタ),ORDER_STATUS (注文ステータスマスタ) 

- **リレーション**:
  - 顧客は注文を行う (1対多、CUSTOMER -> ORDER)
  - 注文は注文明細を含む (1対多、ORDER -> ORDER_DETAIL)
  - 商品は注文明細に含まれる (1対多、PRODUCT -> ORDER_DETAIL)
  - 商品はカテゴリに属する (1対多、PRODUCT -> CATEGORY)
  - 管理者は商品を管理する (1対多、ADMIN -> PRODUCT)
  - 管理者は注文を管理する (1対多、ADMIN -> ORDER)
  - 注文は支払い方法を利用する (1対多、ORDER -> PAYMENT_METHOD)
  - 注文は注文ステータスを持つ (1対多、ORDER -> ORDER_STATUS)

## 5.2. 主要テーブル概要
  - **CUSTOMER (顧客情報テーブル)**
    - 会員登録を行った顧客の基本情報を保持する。
    - `customer_id`を主キーとし、`email`はユニーク制約を持つ。
    - パスワードはハッシュ化して保存する。
    - ログイン認証、注文履歴の紐付け、配送先情報の自動入力、管理者による顧客情報参照に利用される。
  - **PRODUCT (商品情報テーブル)**
    - オンライン販売する各商品の詳細情報を管理する。
    - `product_id`を主キーとする。
    - `商品名`、`価格`、`画像URL`、素材、サイズなどの詳細情報を保持する。
    - `category_id`でカテゴリ情報に紐づく。
    - 商品一覧表示、商品詳細表示、カートへの追加、注文明細での参照、管理者による商品登録・編集に利用される。
  - **CATEGORY (カテゴリ情報テーブル)**   
    - 商品を分類するためのカテゴリ情報を管理するマスタテーブル。
    - `category_id`を主キーとする。
    - 商品一覧のカテゴリ絞り込み、商品登録時のカテゴリ選択に利用される。
  - **ORDER (注文情報テーブル)**
    - 顧客からの注文全体に関する情報を管理する。会員・非会員問わず、全ての注文が記録される
    - `order_id`を主キーとする。
    - 会員の場合は`customer_id`でCUSTOMERテーブルに紐づくが、非会員の場合はNULLとなり、order_email、order_nameなどに直接情報が格納される。
    - total_price、shipping_fee（アプリケーションロジックで計算）、payment_method_code（支払い方法マスタ参照）、order_status_code（注文ステータスマスタ参照）などの情報を保持する。
    - payment_confirmed_at は、銀行振込や代引きにおける入金確認日時を記録する。
    - 注文履歴の表示、管理者による注文管理、注文確認メールのトリガーに利用される。

  - **ORDER_DETAIL (注文明細情報テーブル)**
    - 各注文に含まれる個々の商品の詳細（どの商品をいくつ購入したか、その時点の単価）を管理する。
    - order_detail_idを主キーとし、order_idでORDERテーブルに、product_idでPRODUCTテーブルにそれぞれ紐づく。
    - 注文内容の詳細表示、注文合計金額の算出に利用される。
  - **ADMIN (管理者情報テーブル)**
    - ECサイトの運営・管理を行う担当者の情報を管理する。
    - admin_idを主キーとし、emailはユニーク制約を持つ。
    - パスワードはハッシュ化して保存する。
    - 管理者ログイン認証、管理画面へのアクセス制御に利用される。
  - **PAYMENT_METHOD** (支払い方法マスタテーブル) 
    - ECサイトで利用できる支払い方法の種類を定義・管理する。
    - payment_method_codeを主キーとする。
    - 表示名（label）、表示順（sort_order）、有効無効フラグ（is_active）を保持する。
    - ORDERテーブルのpayment_method_codeがこのテーブルを参照する。
  - ORDER_STATUS (注文ステータスマスタテーブル) 
    - 注文が現在どの段階にあるかを示すステータスの種類を定義・管理する。
    - order_status_codeを主キーとする。
    - 表示名（label）、詳細説明（description）、表示順（sort_order）、有効無効フラグ（is_active）を保持する。
    - ORDERテーブルのorder_status_codeがこのテーブルを参照する。
### 5.3. データフロー概要
本節では、主要な機能におけるデータの流れを詳細に記述します。
**商品一覧表示**

1. **画面(C02)**:
    - 顧客がカテゴリ選択や検索ワード・絞り込み条件を指定して商品一覧を表示。
2. **アプリケーション(Backend)**:
    - カテゴリID、検索ワード、絞り込み条件を受け取る。
    - `PRODUCT`テーブルから該当商品の一覧を取得。
    - `CATEGORY`テーブルからカテゴリ名を取得し、商品に紐づける。
3. **データベース(DB)**: 
    - `PRODUCT`テーブル、`CATEGORY`テーブルを参照。
4. **アプリケーション(Backend)**:
    - 取得した商品リストを画面に返す。
5. **画面(C02)**: 商品一覧を表示。


**商品詳細表示**

1. **画面(C02)**: 顧客が商品一覧から商品を選択。
2. **アプリケーション(Backend)**:
    - `product_id`を受け取り、該当商品の詳細情報を取得。
3. **データベース(DB)**: `PRODUCT`テーブルを参照。
4. **アプリケーション(Backend)**: 商品詳細を返却。
5. **画面(C03)**: 商品詳細画面を表示。


**カート管理（セッション使用）**

1. **画面(C03)**: 顧客が商品をカートに追加・削除・数量変更。
2. **アプリケーション(Backend)**:
    - `product_id`、数量、操作タイプ（追加/削除/数量変更）を受け取り、
    - セッション（ユーザー別の一時記憶）に格納されたカートデータを更新。
3. **データベース(DB)**: DBは使用せず、サーバー側セッションストレージを使用。
4. **アプリケーション(Backend)**: 更新後のカート情報をセッションから取得して返却。
5. **画面(C04)**: カート内容を表示。


**購入フロー（注文情報入力～完了）**

1. **画面(C04)**: カート画面から「購入手続き」を選択。
2. **アプリケーション(Backend)**:
    - ログイン状態をチェック。
    - 非ログイン時はログイン(C10)、会員登録(C08)、非会員購入(C05)のいずれかへ遷移。
3. **画面(C05)**: ユーザーが注文情報を入力。
    - 入力項目（変数）:
        - 氏名 (`name`)
        - メールアドレス (`email`)
        - 電話番号 (`phone`)
        - 配送先住所 (`address`)
        - 支払い方法 (`payment_method_id`)
        - 備考欄 (`notes`)
4. **アプリケーション(Backend)**:
    - 会員の場合、`CUSTOMER`テーブルから上記情報を自動入力。
    - 送料・合計金額を計算し、セッションに保存。
5. **画面(C06)**: ユーザーが最終確認。
6. **アプリケーション(Backend)**:
    - 注文内容をもとにトランザクション開始。
    - `ORDER`, `ORDER_DETAIL`に保存。
    - メール通知（ユーザーと管理者）を送信。
7. **データベース(DB)**:
    - `ORDER`, `ORDER_DETAIL`テーブルにINSERT。
8. **画面(C07)**: 注文完了画面を表示。


**会員登録・認証**

1. **画面(C08)**: 会員登録フォームを表示、顧客情報入力。
2. **アプリケーション(Backend)**:
    - 入力情報を`CUSTOMER`テーブルに保存（パスワードはハッシュ化）。
    - 認証メール送信（必要に応じて）。
3. **データベース(DB)**: `CUSTOMER`テーブルにINSERT。
4. **画面(C09)**: 認証完了 or 案内を表示。


**ログイン処理**

1. **画面(C10)**: 顧客がログイン情報を入力。
2. **アプリケーション(Backend)**:
    - `CUSTOMER`テーブル照合し、セッション／トークンを生成。
3. **データベース(DB)**: `CUSTOMER`を参照。
4. **アプリケーション(Backend)**: 認証成功／失敗を返す。
5. **画面(C01 または C12)**: トップページまたはマイページへ遷移。


**マイページと注文履歴**

1. **画面(C12)**: マイページ表示。
2. **画面(C09)**: 会員情報変更。
3. **アプリケーション(Backend)**:
    - 現在の会員情報を取得して自動入力。
    - 更新情報を`CUSTOMER`テーブルに保存。
4. **画面(C13)**: 注文履歴表示。
5. **アプリケーション(Backend)**:
    - `ORDER`, `ORDER_DETAIL`, `PRODUCT`を参照し履歴情報を返却。
6. **データベース(DB)**: 上記テーブルをSELECT。


**ログアウト**

1. **画面(C11)**: ログアウト操作。
2. **アプリケーション(Backend)**:
    - セッション破棄。
3. **画面(C01)**: トップページに遷移。


**管理者ログイン**

1. **画面(A01)**: 管理者がログイン情報を入力。
2. **アプリケーション(Backend)**:
    - `ADMIN`テーブルを認証。
3. **データベース(DB)**: `ADMIN`テーブルを参照。
4. **画面(A02)**: 管理トップ表示。


**商品管理**

1. **画面(A02)**: 商品管理一覧表示。
2. **アプリケーション(Backend)**:
    - `PRODUCT`を全件取得。
3. **データベース(DB)**: `PRODUCT`テーブルをSELECT。
4. **画面(A02)**: 商品データ一覧を表示。
5. **画面(A03)**: 新規商品登録。
6. **アプリケーション(Backend)**:
    - 入力データを検証し、`PRODUCT`テーブルにINSERT。
7. **データベース(DB)**: `PRODUCT`にINSERT。
8. **画面(A04)**: 商品編集。
9. **アプリケーション(Backend)**:
    - 入力データを検証し、`PRODUCT`テーブルをUPDATE。
10. **データベース(DB)**: `PRODUCT`にUPDATE。


**注文管理**

1. **画面(A05)**: 注文管理一覧表示。
2. **アプリケーション(Backend)**:
    - `ORDER`, `ORDER_DETAIL`, `CUSTOMER`, `PAYMENT_METHOD`, `ORDER_STATUS`などを参照。
3. **データベース(DB)**: 上記テーブルをSELECT。
4. **画面(A05)**: 一覧データを表示。
5. **画面(A06)**: 注文詳細確認。
6. **アプリケーション(Backend)**:
    - 注文情報の取得。
    - ステータス変更時、在庫の戻しや入金確認などの処理。
7. **データベース(DB)**: `ORDER`などをUPDATE。
8. **画面(A06)**: 注文詳細情報を表示。


**顧客管理**

1. **画面(A08)**: 顧客一覧／詳細表示。
2. **アプリケーション(Backend)**:
    - `CUSTOMER`, `ORDER`を取得。
3. **データベース(DB)**: 顧客・注文データをSELECT。
4. **画面(A08)**: 顧客情報と注文履歴を表示。


**管理者ログアウト**

1. **画面(A07)**: ログアウト操作。
2. **アプリケーション(Backend)**:
    - セッション破棄。
3. **画面(A01)**: 管理者ログイン画面に遷移。

