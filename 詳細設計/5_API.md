## 5. インターフェース仕様

### 5.1 API一覧
| API名   | API概要                               | HTTPメソッド | リクエストパラメータ                        | レスポンス内容                | エンドポイントURL         | 対象要素ID                             |
|---------|----------------------------------------|--------------|---------------------------------------------|-------------------------------|----------------------------|----------------------------------------|
| API_01  | カートに商品追加                       | POST         | 商品ID, 数量                                 | 成功可否／カート状態          | /api/cart/add              | C0202_B01                              |
| API_02  | ログイン処理                           | POST         | メールアドレス、パスワード                  | 成功可否／トークン等          | /api/login                 | C0601_B01                              |
| API_03  | ログアウト処理                         | POST         | セッションID                                | 成功可否                      | /api/logout                | C0603_B01                              |
| API_04  | 会員登録処理                           | POST         | 氏名、住所、電話番号、パスワード等          | 登録結果                      | /api/register              | C0501_B01                              |
| API_05  | 商品検索（商品一覧再表示）             | POST         | キーワード、カテゴリ、ページ番号等          | 商品一覧JSON                  | /api/products/search       | C0201_B01                              |
| API_06  | キーワード検索条件送信（トップページ） | POST         | キーワード                                  | 商品一覧JSON                  | /api/search                | C0101_TI01, C0201_TI01                 |
| API_07  | 商品一覧の繰り返し表示                 | GET          | カテゴリID、ページ番号など                   | 商品一覧JSON                  | /api/products/list         | C0201_L05                              |
| API_08  | 注文確定処理                           | POST         | 商品、顧客、配送先、支払情報等              | 注文番号／完了メッセージ      | /api/order/confirm         | C0402_B01                              |
| API_09  | 絞り込み解除（全商品表示）             | GET          | なし                                        | 全商品一覧JSON                | /api/products/all          | C0201_L04                              |
| API_10  | 注文情報確認（送料計算含む）           | POST         | カート商品、配送先、支払方法等              | 小計、送料、合計金額          | /api/order/preview         | C0401_B01                              |
| API_11  | 会員情報取得（マイページ等用）         | GET          | 認証トークン                                 | 氏名、住所、連絡先等          | /api/member/me             | C0603_D01                              |
| API_12  | 会員情報更新                           | PUT          | 氏名、住所、連絡先、パスワード等            | 更新結果                      | /api/member/me             | C0501_B01                              |
| API_13  | 注文履歴取得                           | GET          | 認証トークン                                 | 過去の注文一覧JSON            | /api/member/me/orders      | C0604_D01                              |


### 5.2 API詳細
上記APIの詳細について記載する。

- 01.カートに商品追加API
  - リクエスト JSON 形式
    ```
    {
    "productId": "string",    // 必須、商品ID
    "quantity": 1             // 必須、数量（1以上の整数）
    }
    ```
  - リクエストパラメータ詳細
    | パラメータ名    | 型      | 必須 | 説明        | バリデーション |
    | --------- | ------ | -- | --------- | ------- |
    | productId | string | はい | 追加する商品のID | 空文字不可   |
    | quantity  | int    | はい | 追加数量      | 1以上の整数  |
  - パスパラメータ
  なし
  - レスポンスJSON形式
    ```
    {
     "success": true,
    "cart": {
        "items": [
            {
            "productId": "string",
            "quantity": 1,
            "name": "string",
            "price": 1000
         }
        ],
        "totalPrice": 1000
        }
    }
     ```
  - レスポンスコード
   
    200：正常に商品がカートに追加された 
    400：リクエストパラメータ不正    
    404：指定した商品IDが存在しない  
    500：サーバー内部エラー       
   -  エラー時のレスポンス例
      ```
      {
      "error": {
        "code": 400,
        "message": "quantity must be greater than 0"
        }
      }
      ```

   
- 02.ログイン処理API
  - リクエスト JSON 形式
    ```
    {
    "email": "user@example.com",  // 必須、メールアドレス形式
    "password": "string"           // 必須、パスワード
    }
    ```

  - リクエストパラメータ詳細
    | パラメータ名   | 型      | 必須 | 説明             | バリデーション       |
    | -------- | ------ | -- | -------------- | ------------- |
    | email    | string | はい | 会員登録済みのメールアドレス | メールアドレス形式チェック |
    | password | string | はい | パスワード          | 空文字不可         |

  - パスパラメータ
    なし
  - レスポンスJSON形式
    ```
    {
    "success": true,
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",  // JWTなどの認証トークン
    "user": {
        "memberId": "string",
        "name": "string",
        "email": "user@example.com"
    }
    }
    ```
  - レスポンスコード

     200：ログイン成功               
     400：リクエストパラメータ不正          
    401：認証失敗（メールアドレスまたはパスワード不正）
    500：サーバー内部エラー         

  - エラー時のレスポンス例
     ```
    {
        "error": {
         "code": 401,
         "message": "Invalid email or password"
        }
     }
    ```
- 03.ログアウト処理API
  - リクエスト JSON 形式
    ```
    {
      "sessionId": "abc123"
    }
    ```

  - リクエストパラメータ詳細
    | パラメータ名    | 型      | 必須 | 説明         |
      | --------- | ------ | -- | ---------- |
      | sessionId | string | はい | 現在のセッションID |


  - パスパラメータ
    なし
  - レスポンスJSON形式
    ```
    {
      "success": true
    }
    ```
  - レスポンスコード
      200: ログアウト成功
      401: 認証エラー
  - エラー時のレスポンス例
     ```
      {
      "errorCode": "AUTH_EXPIRED",
      "message": "セッションが無効です。"
    }
    ```
- 04.会員登録処理API
  - リクエスト JSON 形式
    ```
    {
      "firstName": "山田",
      "lastName": "太郎",
      "email": "taro@example.com",
      "password": "password123",
      "address": "東京都港区1-1-1",
      "phone": "0312345678"
    }
    ```

  - リクエストパラメータ詳細
    | パラメータ名    | 型      | 必須 | 説明               |
    | --------- | ------ | -- | ---------------- |
    | firstName | string | はい | 姓                |
    | lastName  | string | はい | 名                |
    | email     | string | はい | メールアドレス          |
    | password  | string | はい | パスワード（平文またはハッシュ） |
    | address   | string | はい | 住所               |
    | phone     | string | はい | 電話番号             |
  - パスパラメータ
    なし
  - レスポンスJSON形式
    ```
    {
      "memberId": "M000001",
      "message": "登録が完了しました。"
    }

    ```
  - レスポンスコード
  201: 登録成功

    400: バリデーションエラー（入力不備）
  409: 重複登録
  - エラー時のレスポンス例
     ```
        {
          "errorCode": "EMAIL_DUPLICATE",
          "message": "すでに登録済みのメールアドレスです。"
        }
        ```
- 05.商品検索（商品一覧再表示）API
    - リクエスト JSON 形式
      ```
        {
      "keyword": "文房具",
      "category": "stationery",
      "page": 1
      }
      ```
    
  - リクエストパラメータ詳細
    | パラメータ名   | 型      | 必須 | 説明            |
      | -------- | ------ | -- | ------------- |
      | keyword  | string | 任意 | 検索キーワード       |
      | category | string | 任意 | カテゴリID        |
      | page     | int    | 任意 | ページ番号（デフォルト1） |

  - パスパラメータ
    なし
  - レスポンスJSON形式
    ```
    {
      "products": [
        {
          "productId": "P001",
          "name": "シャープペンシル",
          "price": 330
        }
      ],
      "totalPages": 3,
      "currentPage": 1
    }
    ```
  - レスポンスコード
  200: 成功
  - エラー時のレスポンス例
     ```
    {
    "errorCode": "INVALID_PAGE",
    "message": "ページ番号が不正です。"
    }
    ```

- 06.キーワード検索条件送信（トップページ）API
    - リクエスト JSON 形式
      ```
      {
        "keyword": "キッチン用品"
      }
      ```
    
  - リクエストパラメータ詳細
    | パラメータ名  | 型      | 必須 | 説明      |
    | ------- | ------ | -- | ------- |
    | keyword | string | はい | 検索キーワード |

  - パスパラメータ
    なし
  - レスポンスJSON形式
    ```
      {
      "products": [
        {
          "productId": "K001",
          "name": "フライパン",
          "price": 2200
        }
      ]
    }
    ```
  - レスポンスコード
  200: 成功
  - エラー時のレスポンス例
     ```
    {
      "errorCode": "NO_RESULTS",
      "message": "検索結果が見つかりません。"
    }
    ```


- 07.商品一覧の繰り返し表示API
    - リクエスト JSON 形式
      ```
      {
        "categoryId": "cat001",
        "page": 2
      }
      ```
    
  - リクエストパラメータ詳細
    | パラメータ名     | 型      | 必須 | 説明         |
    | ---------- | ------ | -- | ---------- |
    | categoryId | string | 任意 | カテゴリID     |
    | page       | int    | 任意 | ページ番号（1以上） |

  - パスパラメータ
    なし
  - レスポンスJSON形式
    ```
    {
      "products": [
        {
          "productId": "P021",
          "name": "ノート",
          "price": 120
        }
      ],
      "currentPage": 2,
      "totalPages": 5
    }
    ```
  - レスポンスコード
  200: 成功
  - エラー時のレスポンス例
     ```
    {
      "errorCode": "INVALID_CATEGORY",
      "message": "指定されたカテゴリは存在しません。"
    }
    ```

- 08.注文確定処理API
    - リクエスト JSON 形式
      ```
      {
        "memberId": "M0001",
        "items": [
          {
            "productId": "P001",
            "quantity": 2
          }
        ],
        "shippingAddress": "東京都港区1-1-1",
        "paymentMethod": "credit"
      }
      ```
    
  - リクエストパラメータ詳細
    | パラメータ名          | 型      | 必須 | 説明             |
    | --------------- | ------------- | -- | -------------- |
    | memberId        | string     | はい | 会員ID           |
    | items           | array| はい | 注文商品リスト        |
    | productId     | string        | はい | 商品ID           |
    | quantity      | int           | はい | 数量（1以上）        |
    | shippingAddress | string        | はい | 配送先住所          |
    | paymentMethod   | string        | はい | 支払い方法（credit等） |

  - パスパラメータ
    なし
  - レスポンスJSON形式
    ```
    {
      "orderId": "O2025001",
      "message": "ご注文ありがとうございました。"
    }
    ```
  - レスポンスコード
    201: 注文完了
    400: 入力不備
    409: 商品在庫不足など
  - エラー時のレスポンス例
     ```
    {
      "errorCode": "OUT_OF_STOCK",
      "message": "一部商品が在庫切れです。"
    }
    ```

- 09.絞り込み解除（全商品表示）API
    - リクエスト JSON 形式
      なし
  - リクエストパラメータ詳細
   なし
  - パスパラメータ
    なし
  - レスポンスJSON形式
    ```
    {
      "products": [
        {
          "productId": "P001",
          "name": "鉛筆",
          "price": 100
        },
        ...
      ]
    }
    ```
  - レスポンスコードなど
    200：成功
  - エラー時のレスポンス例
     ```
    {
      "errorCode": "SERVER_ERROR",
      "message": "商品一覧の取得に失敗しました。"
    }
    ```

- 10.注文情報確認API
    - リクエスト JSON 形式
      ```
      {
        "cartItems": [
          {
            "productId": "P001",
            "quantity": 2
          }
        ],
        "shippingAddress": "東京都千代田区1-1-1",
        "paymentMethod": "credit"
      }
      ```
    
  - リクエストパラメータ詳細
    | パラメータ名          | 型             | 必須 | 説明         |
    | --------------- | ------------- | -- | ---------- |
    | cartItems       | array | はい | カート内商品のリスト |
    | productId     | string        | はい | 商品ID       |
    | quantity      | int           | はい | 数量         |
    | shippingAddress | string        | はい | 配送先住所      |
    | paymentMethod   | string        | はい | 支払い方法      |

  - パスパラメータ
    なし
  - レスポンスJSON形式
    ```
    {
      "subtotal": 2000,
      "shippingFee": 500,
      "total": 2500
    }
    ```
  - レスポンスコード
  200: 成功
  - エラー時のレスポンス例
     ```
    {
      "errorCode": "INVALID_ADDRESS",
      "message": "配送先住所が不正です。"
    }
    ```

- 11.会員情報取得API
   - リクエスト 
      なし
  - リクエストパラメータ詳細
   なし

  - パスパラメータ
    なし
  - レスポンスJSON形式
    ```
    {
      "memberId": "M0001",
      "name": "山田 太郎",
      "email": "taro@example.com",
      "address": "東京都港区1-1-1"
    }
    ```
  - レスポンスコード
  200: 成功
  401：認証失敗
  - エラー時のレスポンス例
     ```
    {
      "errorCode": "UNAUTHORIZED",
      "message": "ログインが必要です。"
    }
    ```


- 12.会員情報更新API
    - リクエスト JSON 形式
      ```
      {
        "name": "山田 太郎",
        "email": "taro@example.com",
        "address": "東京都港区1-1-1",
        "password": "newpassword123"
      }
      ```
    
  - リクエストパラメータ詳細
    | パラメータ名   | 型      | 必須 | 説明      |
    | -------- | ------ | -- | ------- |
    | name     | string | はい | 氏名      |
    | email    | string | はい | メールアドレス |
    | address  | string | はい | 住所      |
    | password | string | 任意 | パスワード   |


  - パスパラメータ
    なし
  - レスポンスJSON形式
    ```
    {
      "message": "更新が完了しました。"
    }
    ```
  - レスポンスコード
    200: 成功
    400: 入力エラー
  - エラー時のレスポンス例
     ```
    {
      "errorCode": "INVALID_EMAIL",
      "message": "メールアドレスの形式が不正です。"
    }
    ```
- 13.注文履歴取得API
    - リクエスト JSON 形式
    なし
  - リクエストパラメータ詳細
    なし
  - パスパラメータ
    なし
  - レスポンスJSON形式
    ```
    {
      "subtotal": 2000,
      "shippingFee": 500,
      "total": 2500
    }
    ```
  - レスポンスコード
    200: 成功
    401: 未認証
  - エラー時のレスポンス例
     ```
    {
      "errorCode": "AUTH_REQUIRED",
      "message": "この操作にはログインが必要です。"
    }
    ```
