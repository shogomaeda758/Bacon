# 3. 機能仕様
## 3.1. 機能一覧

本システムが提供する必須機能は以下の通りです。

| 機能ID | 機能名                   |
|--------|--------------------------|
| F0101  | トップページ表示          |
| F0201  | 商品一覧表示              |
| F0202  | 商品詳細表示              |
| F0301  | カート操作                |
| F0401  | 注文情報入力              |
| F0402  | 注文確認                  |
| F0403  | 注文完了                  |
| F0501  | 会員登録                  |
| F0502  | 会員情報変更              |
| F0601  | ログイン                  |
| F0602  | ログアウト                |
| F0603  | マイページ表示            |
| F0604  | 注文履歴表示              |
| F0605  | 購入前ログイン            |
| F0701  | 特定商取引法表示          |
| F0801  | プライバシーポリシー表示  |
| F0901  | FAQ表示                   |
| F1001  | 共通エラーページ表示      |
| F1101  | 自動入力                  |
| F1201  | 注文確定メール通知（銀行口座） |

---

## 3.2. 機能詳細

### 3.2.1. 商品一覧表示機能 (F0201)
<div class='mermaid'>
sequenceDiagram
    participant ユーザー
    participant Webサイト
    participant ProductService
    participant ProductRepository
    participant データベース

    ユーザー ->> Webサイト : 商品一覧ページアクセス
    Webサイト ->> ProductService : getAllProducts() 【uses】
    ProductService ->> ProductRepository : findAll() 【uses】
    ProductRepository ->> データベース : SELECT * FROM products 【uses】
    データベース -->> ProductRepository : 商品リスト
    ProductRepository -->> ProductService : 商品リスト
    ProductService -->> Webサイト : 商品DTOリスト
    Webサイト -->> ユーザー : 商品一覧表示
</div>

### 3.2.2. 商品詳細表示機能 (F0202)
<div class='mermaid'>
sequenceDiagram
    participant ユーザー
    participant Webサイト
    participant ProductService
    participant ProductRepository
    participant データベース

    ユーザー ->> Webサイト : 商品詳細ページアクセス (商品ID)
    Webサイト ->> ProductService : getProductById(productId) 【uses】
    ProductService ->> ProductRepository : findById(productId) 【uses】
    ProductRepository ->> データベース : SELECT * FROM products WHERE id = ? 【uses】
    データベース -->> ProductRepository : 商品データ or null
    ProductRepository -->> ProductService : 商品データ or null
    ProductService -->> Webサイト : 商品DTO or エラー
    Webサイト -->> ユーザー : 商品詳細表示 or エラーメッセージ
</div>

### 3.2.3. カート操作機能 (F0301)
<div class='mermaid'>
sequenceDiagram
    participant ユーザー
    participant Webサイト
    participant CartService
    participant ProductRepository

    ユーザー ->> Webサイト : カート追加・編集操作
    Webサイト ->> CartService : カート更新・取得処理 【uses】
    CartService ->> ProductRepository : 商品情報取得（必要時）【uses】
    CartService -->> Webサイト : 更新後カートDTO
    Webサイト -->> ユーザー : カート情報表示・更新
</div>

### 3.2.4. 注文情報入力～完了 (F0401～F0403)
<div class='mermaid'>
sequenceDiagram
    participant ユーザー
    participant Webサイト
    participant OrderService
    participant ProductRepository
    participant OrderRepository
    participant メールサーバ

    ユーザー ->> Webサイト : 注文情報入力
    Webサイト ->> OrderService : 注文入力受付 【uses】
    OrderService ->> ProductRepository : カート情報検証 【uses】
    ユーザー ->> Webサイト : 注文確認画面表示
    ユーザー ->> Webサイト : 注文確定
    Webサイト ->> OrderService : placeOrder() 【uses】
    OrderService ->> ProductRepository : 在庫確認・減算 【uses】
    OrderService ->> OrderRepository : 注文情報登録 【uses】
    OrderService -->> Webサイト : 注文確定結果
    Webサイト ->> メールサーバ : 注文確定メール送信
    Webサイト -->> ユーザー : 注文完了画面表示
</div>



