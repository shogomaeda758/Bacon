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

### 3.2.1. 商品一覧表示フロー

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant CuC as CustomerController
    participant S as HttpSession

    # 添削・修正済みシークエンス図 - パッケージ構造対応版

## 🔍 主な問題点と修正

### 1. **DTOの不整合**
- パッケージ構造に `OrderPreview` が存在しない → `OrderResponseDto` で代用
- `OrderInput` が存在しない → 削除またはDTOで代用

### 2. **例外処理の問題**
- `ResourceNotFoundException` は商品・顧客が見つからない場合のみ
- セッション関連は `IllegalStateException` が適切

### 3. **Controller略語の不統一**
- `CartC` → `CC` に統一（CustomerControllerと重複回避）

### 4. **不足しているDTO**
- `OrderSummaryDto` → `OrderSummary` (パッケージ構造通り)
- `OrderItemSummaryDto` → `OrderItemSummary` (パッケージ構造通り)

---

## 3.2.1. 商品一覧表示フロー

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant PC as ProductController
    participant PS as ProductService
    participant PR as ProductRepository
    participant DB as データベース

    U->>B: サイト訪問/メニュー選択
    B->>PC: GET /api/products/all
    PC->>PS: findAllProducts()
    PS->>PR: findAll()
    PR->>DB: SELECT * FROM PRODUCT
    
    alt 正常処理
        DB-->>PR: Product[]
        PR-->>PS: Product[]
        PS-->>PC: ProductListItemDto[]
        PC-->>B: ResponseEntity<List<ProductListItemDto>>
        B-->>U: 商品一覧画面表示
    else DBエラー
        DB-->>PR: SQLException
        PR-->>PS: DataAccessException
        PS-->>PC: ResourceNotFoundException
        PC-->>B: ResponseEntity<ErrorResponse>(500)
        B-->>U: システムエラー画面表示
    end
```



### 3.2.2. カテゴリ別一覧表示フロー

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant PC as ProductController
    participant PS as ProductService
    participant PR as ProductRepository
    participant DB as データベース

    U->>B: カテゴリ選択
    B->>PC: GET /api/products/list?categoryId={categoryId}
    PC->>PS: findProductsByCategory(categoryId)
    PS->>PR: findByCategoryId(categoryId)
    PR->>DB: SELECT * FROM PRODUCT WHERE category_id = ?
    
    alt 正常処理
        DB-->>PR: Product[]
        PR-->>PS: Product[]
        PS-->>PC: ProductListItemDto[]
        PC-->>B: ResponseEntity<List<ProductListItemDto>>
        B-->>U: カテゴリ別商品一覧表示
    else 不正カテゴリID
        PS-->>PC: ValidationException
        PC-->>B: ResponseEntity<ErrorResponse>(400)
        B-->>U: 不正な要求エラー表示
    end
```

### 3.2.3. 商品検索フロー

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant PC as ProductController
    participant PS as ProductService
    participant PR as ProductRepository
    participant DB as データベース

    U->>B: 検索ワード入力・実行
    B->>PC: POST /api/products/search
    Note over B,PC: {keyword: "検索ワード"}
    PC->>PS: searchProducts(keyword)
    PS->>PR: findByNameContaining(keyword)
    PR->>DB: SELECT * FROM PRODUCT WHERE product_name LIKE '%keyword%'
    
    alt 正常処理
        DB-->>PR: Product[]
        PR-->>PS: Product[]
        PS-->>PC: ProductListItemDto[]
        PC-->>B: ResponseEntity<List<ProductListItemDto>>
        B-->>U: 検索結果表示
    else 空の検索ワード
        PS-->>PC: ValidationException
        PC-->>B: ResponseEntity<ErrorResponse>(400)
        B-->>U: 検索ワード必須エラー表示
    end
```

### 3.2.4. 商品詳細表示フロー

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant PC as ProductController
    participant PS as ProductService
    participant PR as ProductRepository
    participant DB as データベース

    U->>B: 商品選択
    B->>PC: GET /api/products/{productId}
    PC->>PS: findProductById(productId)
    PS->>PR: findById(productId)
    PR->>DB: SELECT * FROM PRODUCT WHERE product_id = ?
    
    alt 正常処理
        DB-->>PR: Product
        PR-->>PS: Product
        PS-->>PC: ProductDetailDto
        PC-->>B: ResponseEntity<ProductDetailDto>
        B-->>U: 商品詳細画面表示
    else 商品不存在
        DB-->>PR: null
        PR-->>PS: null
        PS-->>PC: ResourceNotFoundException
        PC-->>B: ResponseEntity<ErrorResponse>(404)
        B-->>U: 商品が見つからないエラー表示
    end
```

### 3.2.5. カート追加フロー

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant CC as CartController
    participant CS as CartService
    participant PR as ProductRepository
    participant S as HttpSession
    participant DB as データベース

    U->>B: 「カートに追加」ボタン押下
    B->>CC: POST /api/cart/add
    Note over B,CC: CartItemInfo{productId: 1, quantity: 2}
    CC->>CS: addItemToCart(cartItemInfo, session)
    CS->>PR: findById(productId)
    PR->>DB: SELECT * FROM PRODUCT WHERE product_id = ?
    
    alt 商品存在
        DB-->>PR: Product
        PR-->>CS: Product
        CS->>CS: 在庫確認
        alt 在庫十分
            CS->>S: セッションからカート取得
            S-->>CS: CartDto or null
            CS->>CS: カートに商品追加・計算
            CS->>S: 更新されたカートを保存
            CS-->>CC: CartDto
            CC-->>B: ResponseEntity<CartDto>
            B-->>U: カート更新成功メッセージ
        else 在庫不足
            CS-->>CC: ValidationException
            CC-->>B: ResponseEntity<ErrorResponse>(400)
            B-->>U: 在庫不足エラー表示
        end
    else 商品不存在
        DB-->>PR: null
        PR-->>CS: null
        CS-->>CC: ResourceNotFoundException
        CC-->>B: ResponseEntity<ErrorResponse>(404)
        B-->>U: 商品が見つからないエラー表示
    end
```

### 3.2.6. カート内容確認・編集フロー

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant CC as CartController
    participant CS as CartService
    participant S as HttpSession

    Note over U,S: カート内容確認
    U->>B: カート画面へ遷移
    B->>CC: GET /api/cart
    CC->>CS: getCartFromSession(session)
    CS->>S: セッションからカート取得
    
    alt セッション有効
        S-->>CS: CartDto
        CS-->>CC: CartDto
        CC-->>B: ResponseEntity<CartDto>
        B-->>U: カート内容表示
    else セッション無効
        S-->>CS: null
        CS-->>CC: IllegalStateException
        CC-->>B: ResponseEntity<ErrorResponse>(401)
        B-->>U: セッション切れエラー・ログイン画面表示
    end

    Note over U,S: 数量変更
    U->>B: 数量変更
    B->>CC: PUT /api/cart/items/{itemId}
    Note over B,CC: CartItemQuantityDto{quantity: 3}
    CC->>CS: updateItemQuantity(itemId, quantity, session)
    CS->>S: セッションからカート取得
    
    alt 正常処理
        S-->>CS: CartDto
        CS->>CS: 数量更新・再計算
        CS->>S: 更新されたカートを保存
        CS-->>CC: CartDto
        CC-->>B: ResponseEntity<CartDto>
        B-->>U: カート内容更新表示
    else 不正数量
        CS-->>CC: ValidationException
        CC-->>B: ResponseEntity<ErrorResponse>(400)
        B-->>U: 不正な数量エラー表示
    end
```

### 3.2.7. 注文情報入力フロー
```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant OC as OrderController
    participant CS as CartService
    participant CuS as CustomerService
    participant S as HttpSession

    U->>B: 「注文手続きに進む」ボタン押下
    B->>OC: GET /api/order/input
    OC->>CS: getCartFromSession(session)
    CS->>S: セッションからカート取得

    alt カート存在
        S-->>CS: CartDto
        CS-->>OC: CartDto

        OC->>S: セッションから customerId 取得
        S-->>OC: customerId

        alt 会員ログイン中
            OC->>CuS: getCustomerById(customerId)
            CuS-->>OC: CustomerResponseDto (氏名・住所等)
            
            alt カート空でない
                OC-->>B: ResponseEntity<CartDto + CustomerResponseDto>
                B-->>U: 注文情報入力画面表示（自動入力）
            else カート空
                OC-->>B: ResponseEntity<ErrorResponse>(400)
                B-->>U: カートが空エラー表示
            end
        else 非会員
            alt カート空でない
                OC-->>B: ResponseEntity<CartDto>
                B-->>U: 注文情報入力画面表示（空白フォーム）
            else カート空
                OC-->>B: ResponseEntity<ErrorResponse>(400)
                B-->>U: カートが空エラー表示
            end
        end
    else セッション無効
        S-->>CS: null
        CS-->>OC: IllegalStateException
        OC-->>B: ResponseEntity<ErrorResponse>(401)
        B-->>U: セッション切れエラー表示
    end

    U->>B: 注文者情報入力（自動入力された内容を確認・修正）
    Note over U,B: 氏名、住所、電話番号、支払い方法等
    B->>OC: POST /api/order/preview
    Note over B,OC: OrderRequestDto（CustomerInfo含む）
    OC->>CS: getCartFromSession(session)
    CS->>S: セッションからカート取得
    S-->>CS: CartDto
    CS-->>OC: CartDto
    OC->>OC: 送料計算
    OC-->>B: ResponseEntity<OrderResponseDto>
    B-->>U: 注文内容確認画面表示
```

### 3.2.8. 注文確認・確定フロー

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant OC as OrderController
    participant OS as OrderService
    participant CS as CartService
    participant OR as OrderRepository
    participant ODR as OrderDetailRepository
    participant PR as ProductRepository
    participant S as HttpSession
    participant DB as データベース

    U->>B: 「注文確定」ボタン押下
    B->>OC: POST /api/order/confirm
    Note over B,OC: OrderRequestDto{customerInfo: {...}}
    OC->>CS: getCartFromSession(session)
    CS->>S: セッションからカート取得
    S-->>CS: CartDto
    CS-->>OC: CartDto
    OC->>OS: placeOrder(cartDto, orderRequestDto)
    
    OS->>DB: BEGIN TRANSACTION
    OS->>OR: save(order)
    OR->>DB: INSERT INTO ORDER
    DB-->>OR: Order (with orderId)
    OR-->>OS: Order
    
    loop カート内の各商品
        OS->>ODR: save(orderDetail)
        ODR->>DB: INSERT INTO ORDER_DETAIL
        OS->>PR: findById(productId)
        PR->>DB: SELECT FOR UPDATE (在庫確認)
        DB-->>PR: Product
        PR-->>OS: Product
        OS->>PR: updateStock(productId, newStock)
        PR->>DB: UPDATE PRODUCT SET stock_quantity = ?
    end
    
    OS->>DB: COMMIT
    OS->>CS: clearCart(session)
    CS->>S: セッションからカート削除
    OS-->>OC: OrderResponseDto
    OC-->>B: ResponseEntity<OrderResponseDto>
    B-->>U: 注文完了画面表示
```
### 3.2.9. 非会員購入フロー**

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant PC as ProductController
    participant CC as CartController
    participant OC as OrderController
    participant S as HttpSession

    Note over U,S: 商品閲覧〜カート追加
    U->>B: 商品一覧閲覧
    B->>PC: GET /api/products/all
    PC-->>B: List<ProductListItemDto>
    B-->>U: 商品一覧表示
    
    U->>B: 商品選択
    B->>PC: GET /api/products/{productId}
    PC-->>B: ProductDetailDto
    B-->>U: 商品詳細表示
    
    U->>B: カートに追加
    B->>CC: POST /api/cart/add
    CC->>S: セッションでカート管理
    CC-->>B: CartDto
    B-->>U: カート追加完了
    
    Note over U,S: 注文手続き
    U->>B: 注文手続きへ
    B->>OC: GET /api/order/input
    OC-->>B: CartDto
    B-->>U: 注文情報入力画面
    
    U->>B: 顧客情報入力（非会員）
    B->>OC: POST /api/order/preview
    Note over B,OC: OrderRequestDto{customerInfo: CustomerInfo{...}}
    OC-->>B: OrderResponseDto
    B-->>U: 注文確認画面
    
    U->>B: 注文確定
    B->>OC: POST /api/order/confirm
    OC-->>B: OrderResponseDto
    B-->>U: 注文完了画面
```

### 3.2.10. 会員購入フロー

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant CuC as CustomerController
    participant PC as ProductController
    participant CC as CartController
    participant OC as OrderController
    participant S as HttpSession

    Note over U,S: ログイン
    U->>B: ログイン
    B->>CuC: POST /api/login
    Note over B,CuC: LoginRequestDto{email, password}
    CuC->>S: セッションに会員情報保存
    CuC-->>B: CustomerResponseDto
    B-->>U: ログイン成功
    
    Note over U,S: 商品閲覧〜カート追加
    U->>B: 商品閲覧・選択
    B->>PC: GET /api/products/{productId}
    PC-->>B: ProductDetailDto
    B-->>U: 商品詳細表示
    
    U->>B: カートに追加
    B->>CC: POST /api/cart/add
    CC-->>B: CartDto
    B-->>U: カート追加完了
    
    Note over U,S: 注文手続き（会員情報自動入力）
    U->>B: 注文手続きへ
    B->>OC: GET /api/order/input
    OC->>S: セッションから会員情報取得
    OC-->>B: CartDto + CustomerResponseDto (会員情報自動入力)
    B-->>U: 注文情報入力画面（自動入力済み）
    
    U->>B: 配送先・支払い方法確認/修正
    B->>OC: POST /api/order/preview
    Note over B,OC: OrderRequestDto{customerInfo: CustomerInfo{...}}
    OC-->>B: OrderResponseDto
    B-->>U: 注文確認画面
    
    U->>B: 注文確定
    B->>OC: POST /api/order/confirm
    OC-->>B: OrderResponseDto
    B-->>U: 注文完了画面
```
### 3.2.11. 注文完了・通知フロー

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant OC as OrderController
    participant OS as OrderService

    Note over U,OS: 注文確定後の処理
    OS->>OS: sendOrderConfirmationEmail(order)
    Note over OS: メール送信処理（外部サービス連携）
    
    B-->>U: 注文完了画面表示
    Note over B,U: 注文番号、注文日時、合計金額等
    
    Note over U,OS: メール通知
    OS->>U: 注文確認メール送信
```


### 3.2.12. 会員登録フロー

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant CuC as CustomerController
    participant CS as CustomerService
    participant CR as CustomerRepository
    participant DB as データベース

    U->>B: 新規会員登録リンク選択
    B-->>U: 会員登録画面表示
    
    U->>B: 会員情報入力・送信
    B->>CuC: POST /api/register
    Note over B,CuC: CustomerRegisterRequestDto{name, email, password, address, phoneNumber}
    CuC->>CS: registerCustomer(customerRegisterRequestDto)
    CS->>CR: findByEmail(email)
    CR->>DB: SELECT * FROM CUSTOMER WHERE email = ?
    DB-->>CR: Customer or null
    CR-->>CS: Customer or null
    
    alt メールアドレス重複なし
        CS->>CS: パスワードハッシュ化
        CS->>CR: save(customer)
        CR->>DB: INSERT INTO CUSTOMER
        DB-->>CR: Customer (with customerId)
        CR-->>CS: Customer
        CS-->>CuC: CustomerResponseDto
        CuC-->>B: ResponseEntity<CustomerResponseDto>
        B-->>U: 登録完了画面表示
    else メールアドレス重複あり
        CS-->>CuC: ValidationException (メールアドレス重複)
        CuC-->>B: ResponseEntity<ErrorResponse>(400)
        B-->>U: エラーメッセージ表示
    end
```

### 3.2.13. ログインフロー

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant CuC as CustomerController
    participant CS as CustomerService
    participant CR as CustomerRepository
    participant S as HttpSession
    participant DB as データベース

    U->>B: ログインボタン押下
    B-->>U: ログイン画面表示
    
    U->>B: メールアドレス・パスワード入力
    B->>CuC: POST /api/login
    Note over B,CuC: LoginRequestDto{email, password}
    CuC->>CS: authenticate(loginRequestDto)
    CS->>CR: findByEmail(email)
    CR->>DB: SELECT * FROM CUSTOMER WHERE email = ?
    DB-->>CR: Customer or null
    CR-->>CS: Customer or null
    
    alt 会員存在 and パスワード一致
        CS->>CS: パスワード検証
        CS->>S: セッションに会員情報保存
        CS-->>CuC: CustomerResponseDto
        CuC-->>B: ResponseEntity<CustomerResponseDto>
        B-->>U: ログイン成功・マイページ表示
    else 認証失敗
        CS-->>CuC: ValidationException (認証失敗)
        CuC-->>B: ResponseEntity<ErrorResponse>(401)
        B-->>U: エラーメッセージ表示
    end
```

### 3.2.14. ログアウトフロー

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant CC as CustomerController
    participant S as HttpSession

    U->>B: ログアウトボタン押下
    B->>CC: POST /api/logout
    CC->>S: セッション無効化
    S-->>CC: 完了
    CC-->>B: ResponseEntity<Success>
    B-->>U: ログアウト完了・トップページ表示
```

### 3.2.15. 注文履歴閲覧フロー

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant CC as CustomerController
    participant CS as CustomerService
    participant OR as OrderRepository
    participant ODR as OrderDetailRepository
    participant S as HttpSession
    participant DB as データベース

    U->>B: マイページから注文履歴選択
    B->>CC: GET /api/member/me/orders
    CC->>S: セッションから会員情報取得
    S-->>CC: Customer
    CC->>CS: getOrderHistory(customerId)
    CS->>OR: findByCustomerId(customerId)
    OR->>DB: SELECT * FROM ORDER WHERE customer_id = ?
    DB-->>OR: Order[]
    OR-->>CS: Order[]
    
    loop 各注文
        CS->>ODR: findByOrderId(orderId)
        ODR->>DB: SELECT * FROM ORDER_DETAIL WHERE order_id = ?
        DB-->>ODR: OrderDetail[]
        ODR-->>CS: OrderDetail[]
    end
    
    CS-->>CC: List<OrderSummaryDto>
    CC-->>B: ResponseEntity<List<OrderSummaryDto>>
    B-->>U: 注文履歴一覧表示
```


### 3.2.16. 支払い方法選択フロー

```mermaid
sequenceDiagram
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant OC as OrderController
    participant OS as OrderService

    U->>B: 注文情報入力画面表示
    B-->>U: 支払い方法選択肢表示
    Note over B,U: 代引き、銀行振込
    
    U->>B: 支払い方法選択
    B->>OC: POST /api/order/preview
    Note over B,OC: OrderRequestDto{paymentMethod: "cash"}
    OC->>OS: validatePaymentMethod(paymentMethod)
    OS->>OS: 支払い方法検証
    OS-->>OC: 検証結果
    OC-->>B: ResponseEntity<OrderPreview>
    B-->>U: 選択した支払い方法反映
```

### 3.2.17. 配送料計算フロー

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant OC as OrderController
    participant OS as OrderService

    U->>B: 配送先住所入力
    B->>OC: POST /api/order/preview
    Note over B,OC: OrderRequestDto{customerInfo: CustomerInfo{address: "..."}}
    OC->>OS: calculateShippingFee(address, totalAmount)
    OS->>OS: 配送料計算ロジック
    Note over OS: 地域別・金額別送料計算
    OS-->>OC: ShippingFee
    OC-->>B: ResponseEntity<OrderPreview>
    B-->>U: 配送料込み合計金額表示
```

