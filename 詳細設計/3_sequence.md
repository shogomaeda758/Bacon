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

### 3.2.1. 購入フローシーケンス図

<div class="mermaid">
sequenceDiagram
    %% 4.5.1: 購入フローシーケンス図（修正版）
    participant User as ユーザー (Browser)
    participant FE as フロントエンド (JS)
    participant CartController
    participant CartService
    participant ProductRepository
    participant OrderController
    participant OrderService
    participant OrderRepository
    participant OrderDetailRepository
    participant MailService
    participant Session as セッション管理
    participant DB as データベース

    %% カートに商品追加
    User->>FE: 商品をカートに追加
    FE->>CartController: POST /api/cart/add
    CartController->>CartService: addItem(productId, qty, session)
    CartService->>ProductRepository: findById(productId)
    ProductRepository->>DB: SELECT * FROM products WHERE id=?
    DB-->>ProductRepository: Product
    ProductRepository-->>CartService: Product
    CartService-->>CartController: Updated Cart
    CartController-->>FE: JSON (Updated Cart)
    FE-->>User: カートを表示

    %% 購入手続き開始
    User->>FE: 購入手続き
    FE->>OrderController: POST /api/orders

    %% セッションから顧客ID取得
    OrderController->>Session: セッションから顧客ID取得
    Session-->>OrderController: customerId

    %% カート情報をサーバー側で取得（明示）
    OrderController->>CartService: getCart(customerId)
    CartService-->>OrderController: Cart

    %% 注文処理開始
    OrderController->>OrderService: placeOrder(cart, orderRequest, customerId)

    %% 注文エンティティ生成
    OrderService->>OrderService: Orderを生成（cart, orderRequest, customerId）

    %% 注文保存
    OrderService->>OrderRepository: save(Order)
    OrderRepository->>DB: INSERT INTO orders ...
    DB-->>OrderRepository: Order ID
    OrderRepository-->>OrderService: Order ID

    %% 注文詳細生成・設定
    OrderService->>OrderService: OrderDetails = createOrderDetailsFromCart(cart)
    OrderService->>OrderService: Order IDをOrderDetailsに設定

    %% 注文詳細保存
    OrderService->>OrderDetailRepository: saveAll(OrderDetails)
    OrderDetailRepository->>DB: INSERT INTO order_details ...
    DB-->>OrderDetailRepository: Insert Success
    OrderDetailRepository-->>OrderService: Insert Result

    %% 注文確認メール送信
    OrderService->>MailService: sendOrderConfirmationEmail(order)
    MailService->>User: 注文確認メール送信（注文番号、合計金額、配送予定日などを含む）
    MailService-->>OrderService: メール送信完了

    %% OrderResponse生成（注文番号、合計金額、配送予定日など含む）
    OrderService->>OrderService: OrderResponseを生成(orderNo, totalPrice, deliveryDate, ...)

    %% 応答返却
    OrderService-->>OrderController: OrderResponse
    OrderController-->>FE: JSON (OrderResponse)
    FE-->>User: 購入確定画面を表示
</div>

### 3.2.2. ログイン・ログアウトシーケンス図 (セッション管理)

<div class="mermaid">
sequenceDiagram
    %% No.1: 購入フロー一式
    participant User as ユーザー (Browser)
    participant FE as フロントエンド (JS)
    participant CartController
    participant CartService
    participant ProductRepository
    participant OrderController
    participant OrderService
    participant OrderRepository
    participant OrderDetailRepository
    participant MailService
    participant DB as データベース

    User->>FE: 商品をカートに追加
    FE->>CartController: POST /api/cart/add
    CartController->>CartService: addItem(productId, qty, session)
    CartService->>ProductRepository: findById(productId)
    ProductRepository->>DB: SELECT * FROM products WHERE id=?
    DB-->>ProductRepository: Product
    ProductRepository-->>CartService: Product
    CartService-->>CartController: Updated Cart
    CartController-->>FE: JSON (Updated Cart)
    FE-->>User: カートを表示

    User->>FE: 購入手続き
    FE->>OrderController: POST /api/orders
    OrderController->>OrderService: placeOrder(cart, orderRequest)
    OrderService->>OrderRepository: save(Order)
    OrderRepository->>DB: INSERT INTO orders ...
    DB-->>OrderRepository: Order ID
    OrderService->>OrderDetailRepository: saveAll(OrderDetails)
    OrderDetailRepository->>DB: INSERT INTO order_details ...
    DB-->>OrderDetailRepository: Success
    OrderService->>MailService: sendOrderConfirmationEmail()
    MailService->>User: 確認メール送信
    OrderService-->>OrderController: OrderResponse
    OrderController-->>FE: JSON (OrderResponse)
    FE-->>User: 購入確定画面を表示


    %% No.2: 会員登録、ログイン・ログアウト機能
    participant User as ユーザー (Browser)
    participant FE as フロントエンド (JS)
    participant CustomerController
    participant CustomerService
    participant CustomerRepository
    participant Session as セッション管理
    participant DB as データベース

    User->>FE: 会員登録フォーム入力
    FE->>CustomerController: POST /api/customers/register
    CustomerController->>CustomerService: registerCustomer(request)
    CustomerService->>CustomerRepository: save(Customer)
    CustomerRepository->>DB: INSERT INTO customers ...
    DB-->>CustomerRepository: 保存成功
    CustomerService-->>CustomerController: CustomerResponse
    CustomerController-->>FE: JSON (CustomerResponse)
    FE-->>User: 会員登録完了画面を表示

    User->>FE: ログイン情報入力
    FE->>CustomerController: POST /api/customers/login
    CustomerController->>CustomerService: authenticate(request)
    CustomerService->>CustomerRepository: findByEmail()
    CustomerRepository->>DB: SELECT * FROM customers WHERE email=?
    DB-->>CustomerRepository: Customer
    CustomerRepository-->>CustomerService: Customer
    CustomerService->>Session: セッションへ会員情報保存
    CustomerService-->>CustomerController: CustomerResponse
    CustomerController-->>FE: JSON (CustomerResponse)
    FE-->>User: マイページ表示

    User->>FE: ログアウト操作
    FE->>CustomerController: POST /api/customers/logout
    CustomerController->>Session: セッション破棄
    Session-->>CustomerController: OK
    CustomerController-->>FE: OK
    FE-->>User: ログイン画面を表示
</div>

### 3.2.3. マイページ・注文履歴・会員情報変更シーケンス図 

<div class="mermaid">
sequenceDiagram
    %% No.1: 購入フロー一式
    participant User as ユーザー (Browser)
    participant FE as フロントエンド (JS)
    participant CartController
    participant CartService
    participant ProductRepository
    participant OrderController
    participant OrderService
    participant OrderRepository
    participant OrderDetailRepository
    participant MailService
    participant DB as データベース

    User->>FE: 商品をカートに追加
    FE->>CartController: POST /api/cart/add
    CartController->>CartService: addItem(productId, qty, session)
    CartService->>ProductRepository: findById(productId)
    ProductRepository->>DB: SELECT * FROM products WHERE id=?
    DB-->>ProductRepository: Product
    ProductRepository-->>CartService: Product
    CartService-->>CartController: Updated Cart
    CartController-->>FE: JSON (Updated Cart)
    FE-->>User: カートを表示

    User->>FE: 購入手続き
    FE->>OrderController: POST /api/orders
    OrderController->>OrderService: placeOrder(cart, orderRequest)
    OrderService->>OrderRepository: save(Order)
    OrderRepository->>DB: INSERT INTO orders ...
    DB-->>OrderRepository: Order ID
    OrderService->>OrderDetailRepository: saveAll(OrderDetails)
    OrderDetailRepository->>DB: INSERT INTO order_details ...
    DB-->>OrderDetailRepository: Success
    OrderService->>MailService: sendOrderConfirmationEmail()
    MailService->>User: 確認メール送信
    OrderService-->>OrderController: OrderResponse
    OrderController-->>FE: JSON (OrderResponse)
    FE-->>User: 購入確定画面を表示


    %% No.2: 会員登録、ログイン・ログアウト機能
    participant User as ユーザー (Browser)
    participant FE as フロントエンド (JS)
    participant CustomerController
    participant CustomerService
    participant CustomerRepository
    participant Session as セッション管理
    participant DB as データベース

    User->>FE: 会員登録フォーム入力
    FE->>CustomerController: POST /api/customers/register
    CustomerController->>CustomerService: registerCustomer(request)
    CustomerService->>CustomerRepository: save(Customer)
    CustomerRepository->>DB: INSERT INTO customers ...
    DB-->>CustomerRepository: 保存成功
    CustomerService-->>CustomerController: CustomerResponse
    CustomerController-->>FE: JSON (CustomerResponse)
    FE-->>User: 会員登録完了画面を表示

    User->>FE: ログイン情報入力
    FE->>CustomerController: POST /api/customers/login
    CustomerController->>CustomerService: authenticate(request)
    CustomerService->>CustomerRepository: findByEmail()
    CustomerRepository->>DB: SELECT * FROM customers WHERE email=?
    DB-->>CustomerRepository: Customer
    CustomerRepository-->>CustomerService: Customer
    CustomerService->>Session: セッションへ会員情報保存
    CustomerService-->>CustomerController: CustomerResponse
    CustomerController-->>FE: JSON (CustomerResponse)
    FE-->>User: マイページ表示

    User->>FE: ログアウト操作
    FE->>CustomerController: POST /api/customers/logout
    CustomerController->>Session: セッション破棄
    Session-->>CustomerController: OK
    CustomerController-->>FE: OK
    FE-->>User: ログイン画面を表示


    %% No.3: マイページ・注文履歴・会員情報変更
    participant User as ユーザー (Browser)
    participant FE as フロントエンド (JS)
    participant CustomerController
    participant CustomerService
    participant OrderRepository
    participant CustomerRepository
    participant Session as セッション管理
    participant DB as データベース

    User->>FE: マイページアクセス
    FE->>CustomerController: GET /api/customers/mypage
    CustomerController->>Session: 顧客IDを取得
    Session-->>CustomerController: customerId
    CustomerController->>CustomerService: getCustomerProfile(customerId)
    CustomerService->>CustomerRepository: findById(customerId)
    CustomerRepository->>DB: SELECT * FROM customers WHERE id=?
    DB-->>CustomerRepository: Customer
    CustomerRepository-->>CustomerService: Customer
    CustomerService-->>CustomerController: CustomerResponse
    CustomerController-->>FE: JSON (CustomerResponse)
    FE-->>User: プロフィール表示

    User->>FE: 注文履歴の表示
    FE->>CustomerController: GET /api/customers/orders
    CustomerController->>Session: 顧客IDを取得
    Session-->>CustomerController: customerId
    CustomerController->>CustomerService: getOrderHistory(customerId)
    CustomerService->>OrderRepository: findByCustomerId(customerId)
    OrderRepository->>DB: SELECT * FROM orders WHERE customer_id=?
    DB-->>OrderRepository: List<Order>
    OrderRepository-->>CustomerService: List<Order>
    CustomerService-->>CustomerController: List<OrderSummary>
    CustomerController-->>FE: JSON (OrderSummary List)
    FE-->>User: 注文履歴を表示

    User->>FE: 会員情報の変更を入力
    FE->>CustomerController: PUT /api/customers/update
    CustomerController->>Session: 顧客IDを取得
    Session-->>CustomerController: customerId
    CustomerController->>CustomerService: updateCustomerInfo(request)
    CustomerService->>CustomerRepository: save(updatedCustomer)
    CustomerRepository->>DB: UPDATE customers SET ...
    DB-->>CustomerRepository: 更新成功
    CustomerRepository-->>CustomerService: 更新後Customer
    CustomerService-->>CustomerController: CustomerResponse
    CustomerController-->>FE: JSON (CustomerResponse)
    FE-->>User: 更新完了メッセージ表示
</div>

### 3.2.4.  カート操作・購入前ログインシーケンス図

<div class="mermaid">
sequenceDiagram
    %% No.4: カート操作・購入前ログイン
    participant User as ユーザー (Browser)
    participant FE as フロントエンド (JS)
    participant CartController
    participant CartService
    participant ProductRepository
    participant Session as セッション管理
    participant DB as データベース
    participant CustomerController
    participant CustomerService
    participant CustomerRepository

    User->>FE: 商品をカートに追加
    FE->>CartController: POST /api/cart/add
    CartController->>CartService: addItemToCart(productId, quantity, session)
    CartService->>ProductRepository: findById(productId)
    ProductRepository->>DB: SELECT * FROM products WHERE id=?
    DB-->>ProductRepository: Product
    ProductRepository-->>CartService: Product
    CartService-->>CartController: Updated Cart
    CartController-->>FE: JSON (Cart)
    FE-->>User: カートを表示

    User->>FE: ログイン操作
    FE->>CustomerController: POST /api/customers/login
    CustomerController->>CustomerService: authenticate(request)
    CustomerService->>CustomerRepository: findByEmail()
    CustomerRepository->>DB: SELECT * FROM customers WHERE email=?
    DB-->>CustomerRepository: Customer
    CustomerRepository-->>CustomerService: Customer
    CustomerService->>Session: セッションへ会員情報保存
    CustomerService-->>CustomerController: CustomerResponse
    CustomerController-->>FE: JSON (CustomerResponse)
    FE-->>User: ログイン完了後にカート画面を再表示
</div>

### 3.2.5.  商品一覧・詳細表示シーケンス図 

<div class="mermaid">
%% No.5: 商品一覧・詳細表示
sequenceDiagram
    participant User as ユーザー (Browser)
    participant FE as フロントエンド (JS)
    participant ProductController
    participant ProductService
    participant ProductRepository
    participant DB as データベース

    User->>FE: 商品一覧ページへアクセス
    FE->>ProductController: GET /api/products
    ProductController->>ProductService: getAllProducts()
    ProductService->>ProductRepository: findAll()
    ProductRepository->>DB: SELECT * FROM products
    DB-->>ProductRepository: 商品データリスト
    ProductRepository-->>ProductService: List<Product>
    ProductService->>ProductService: ProductエンティティからProductListItem DTOへ変換
    ProductService-->>ProductController: List<ProductListItem>
    ProductController-->>FE: 商品リスト (JSON)
    FE-->>User: 商品一覧画面を表示

    User->>FE: 商品詳細をクリック
    FE->>ProductController: GET /api/products/{id}
    ProductController->>ProductService: getProductById(id)
    ProductService->>ProductRepository: findById(id)
    ProductRepository->>DB: SELECT * FROM products WHERE id=?
    DB-->>ProductRepository: Product
    ProductRepository-->>ProductService: Product
    ProductService->>ProductService: ProductエンティティからProductDetail DTOへ変換
    ProductService-->>ProductController: ProductDetail
    ProductController-->>FE: JSON (ProductDetail)
    FE-->>User: 商品詳細画面を表示
</div>

### 3.2.6. 自動入力シーケンス図

<div class="mermaid">
sequenceDiagram
    participant User as ユーザー (Browser)
    participant FE as フロントエンド (JS)
    participant CustomerController
    participant CustomerService
    participant CustomerRepository
    participant Session as セッション (HttpSession)

    User->>FE: 注文フォームを開く
    FE->>CustomerController: GET /api/customers/me
    CustomerController->>Session: セッションからcustomerIdを取得
    Session-->>CustomerController: customerId

    CustomerController->>CustomerService: getCustomerById(customerId)
    CustomerService->>CustomerRepository: findById(customerId)
    CustomerRepository-->>CustomerService: Customerエンティティ
    CustomerService-->>CustomerController: CustomerResponse DTO
    CustomerController-->>FE: JSON(CustomerResponse)

    FE-->>User: 注文フォームに自動入力表示
</div>

### 3.2.7. 注文確定通知シーケンス図

<div class="mermaid">
sequenceDiagram
    participant OrderService
    participant MailService
    participant User as ユーザー (メール受信者)
    participant Admin as 管理者 (メール受信者)

    OrderService->>MailService: sendOrderConfirmationEmail(order)

    MailService->>User: 注文確定メール送信 (注文内容)
    MailService->>Admin: 注文通知メール送信 (注文内容)

    MailService-->>OrderService: メール送信完了（ユーザー・管理者）
</div>



## 別バージョン


**商品一覧表示フロー**

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
    DB-->>PR: Product[]
    PR-->>PS: Product[]
    PS-->>PC: ProductListItem[]
    PC-->>B: ResponseEntity<List<ProductListItem>>
    B-->>U: 商品一覧画面表示
```

**カテゴリ別一覧表示フロー**

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
    DB-->>PR: Product[]
    PR-->>PS: Product[]
    PS-->>PC: ProductListItem[]
    PC-->>B: ResponseEntity<List<ProductListItem>>
    B-->>U: カテゴリ別商品一覧表示
```

**商品検索フロー**

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
    DB-->>PR: Product[]
    PR-->>PS: Product[]
    PS-->>PC: ProductListItem[]
    PC-->>B: ResponseEntity<List<ProductListItem>>
    B-->>U: 検索結果表示
```

**商品詳細表示フロー**

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
    DB-->>PR: Product
    PR-->>PS: Product
    PS-->>PC: ProductDetail
    PC-->>B: ResponseEntity<ProductDetail>
    B-->>U: 商品詳細画面表示
```

**カート追加フロー**

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
    Note over B,CC: {productId: 1, quantity: 2}
    CC->>CS: addItemToCart(productId, quantity, session)
    CS->>PR: findById(productId)
    PR->>DB: SELECT * FROM PRODUCT WHERE product_id = ?
    DB-->>PR: Product
    PR-->>CS: Product
    CS->>S: セッションからカート取得
    S-->>CS: Cart or null
    CS->>CS: カートに商品追加・計算
    CS->>S: 更新されたカートを保存
    CS-->>CC: Cart
    CC-->>B: ResponseEntity<Cart>
    B-->>U: カート更新成功メッセージ
```

**カート内容確認・編集フロー**

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
    S-->>CS: Cart
    CS-->>CC: Cart
    CC-->>B: ResponseEntity<Cart>
    B-->>U: カート内容表示

    Note over U,S: 数量変更
    U->>B: 数量変更
    B->>CC: PUT /api/cart/items/{itemId}
    Note over B,CC: {quantity: 3}
    CC->>CS: updateItemQuantity(itemId, quantity, session)
    CS->>S: セッションからカート取得
    S-->>CS: Cart
    CS->>CS: 数量更新・再計算
    CS->>S: 更新されたカートを保存
    CS-->>CC: Cart
    CC-->>B: ResponseEntity<Cart>
    B-->>U: カート内容更新表示

    Note over U,S: アイテム削除
    U->>B: 削除ボタン押下
    B->>CC: DELETE /api/cart/items/{itemId}
    CC->>CS: removeItemFromCart(itemId, session)
    CS->>S: セッションからカート取得
    S-->>CS: Cart
    CS->>CS: アイテム削除・再計算
    CS->>S: 更新されたカートを保存
    CS-->>CC: Cart
    CC-->>B: ResponseEntity<Cart>
    B-->>U: カート内容更新表示
```

**注文情報入力フロー**

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant OC as OrderController
    participant CS as CartService
    participant S as HttpSession

    U->>B: 「注文手続きに進む」ボタン押下
    B->>OC: GET /api/order/input
    OC->>CS: getCartFromSession(session)
    CS->>S: セッションからカート取得
    S-->>CS: Cart
    CS-->>OC: Cart
    OC-->>B: ResponseEntity<Cart>
    B-->>U: 注文情報入力画面表示

    U->>B: 注文者情報入力
    Note over U,B: 氏名、住所、電話番号、支払い方法等
    B->>OC: POST /api/order/preview
    Note over B,OC: {customerInfo: {...}}
    OC->>CS: getCartFromSession(session)
    CS->>S: セッションからカート取得
    S-->>CS: Cart
    CS-->>OC: Cart
    OC->>OC: 送料計算
    OC-->>B: ResponseEntity<OrderPreview>
    B-->>U: 注文内容確認画面表示
```

**注文確認・確定フロー**

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
    Note over B,OC: {customerInfo: {...}}
    OC->>CS: getCartFromSession(session)
    CS->>S: セッションからカート取得
    S-->>CS: Cart
    CS-->>OC: Cart
    OC->>OS: placeOrder(cart, orderRequest)
    
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
    OS-->>OC: OrderResponse
    OC-->>B: ResponseEntity<OrderResponse>
    B-->>U: 注文完了画面表示
```
**非会員購入フロー**

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
    PC-->>B: ProductList
    B-->>U: 商品一覧表示
    
    U->>B: 商品選択
    B->>PC: GET /api/products/{productId}
    PC-->>B: ProductDetail
    B-->>U: 商品詳細表示
    
    U->>B: カートに追加
    B->>CC: POST /api/cart/add
    CC->>S: セッションでカート管理
    CC-->>B: Cart
    B-->>U: カート追加完了
    
    Note over U,S: 注文手続き
    U->>B: 注文手続きへ
    B->>OC: GET /api/order/input
    OC-->>B: OrderInput
    B-->>U: 注文情報入力画面
    
    U->>B: 顧客情報入力（非会員）
    B->>OC: POST /api/order/preview
    OC-->>B: OrderPreview
    B-->>U: 注文確認画面
    
    U->>B: 注文確定
    B->>OC: POST /api/order/confirm
    OC-->>B: OrderResponse
    B-->>U: 注文完了画面
```

 **会員購入フロー**

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant CC as CustomerController
    participant PC as ProductController
    participant CaC as CartController
    participant OC as OrderController
    participant S as HttpSession

    Note over U,S: ログイン
    U->>B: ログイン
    B->>CC: POST /api/login
    CC->>S: セッションに会員情報保存
    CC-->>B: CustomerResponse
    B-->>U: ログイン成功
    
    Note over U,S: 商品閲覧〜カート追加
    U->>B: 商品閲覧・選択
    B->>PC: GET /api/products/{productId}
    PC-->>B: ProductDetail
    B-->>U: 商品詳細表示
    
    U->>B: カートに追加
    B->>CaC: POST /api/cart/add
    CaC-->>B: Cart
    B-->>U: カート追加完了
    
    Note over U,S: 注文手続き（会員情報自動入力）
    U->>B: 注文手続きへ
    B->>OC: GET /api/order/input
    OC->>S: セッションから会員情報取得
    OC-->>B: OrderInput (会員情報自動入力)
    B-->>U: 注文情報入力画面（自動入力済み）
    
    U->>B: 配送先・支払い方法確認/修正
    B->>OC: POST /api/order/preview
    OC-->>B: OrderPreview
    B-->>U: 注文確認画面
    
    U->>B: 注文確定
    B->>OC: POST /api/order/confirm
    OC-->>B: OrderResponse
    B-->>U: 注文完了画面
```
**注文完了・通知フロー**

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant OC as OrderController
    participant OS as OrderService
    participant ES as EmailService
    participant MS as MailServer

    Note over U,MS: 注文確定後の処理
    OS->>ES: sendOrderConfirmationEmail(order)
    ES->>MS: メール送信
    MS-->>ES: 送信完了
    ES-->>OS: 送信結果
    
    B-->>U: 注文完了画面表示
    Note over B,U: 注文番号、注文日時、合計金額等
    
    Note over U,MS: メール通知
    MS->>U: 注文確認メール送信
```


**会員登録フロー**

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant CC as CustomerController
    participant CS as CustomerService
    participant CR as CustomerRepository
    participant DB as データベース

    U->>B: 新規会員登録リンク選択
    B-->>U: 会員登録画面表示
    
    U->>B: 会員情報入力・送信
    B->>CC: POST /api/register
    Note over B,CC: {name, email, password, address, phoneNumber}
    CC->>CS: registerCustomer(request)
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
        CS-->>CC: CustomerResponse
        CC-->>B: ResponseEntity<CustomerResponse>
        B-->>U: 登録完了画面表示
    else メールアドレス重複あり
        CS-->>CC: エラー (メールアドレス重複)
        CC-->>B: ResponseEntity<Error>
        B-->>U: エラーメッセージ表示
    end
```

**ログインフロー**

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant CC as CustomerController
    participant CS as CustomerService
    participant CR as CustomerRepository
    participant S as HttpSession
    participant DB as データベース

    U->>B: ログインボタン押下
    B-->>U: ログイン画面表示
    
    U->>B: メールアドレス・パスワード入力
    B->>CC: POST /api/login
    Note over B,CC: {email, password}
    CC->>CS: authenticate(loginRequest)
    CS->>CR: findByEmail(email)
    CR->>DB: SELECT * FROM CUSTOMER WHERE email = ?
    DB-->>CR: Customer or null
    CR-->>CS: Customer or null
    
    alt 会員存在 and パスワード一致
        CS->>CS: パスワード検証
        CS->>S: セッションに会員情報保存
        CS-->>CC: CustomerResponse
        CC-->>B: ResponseEntity<CustomerResponse>
        B-->>U: ログイン成功・マイページ表示
    else 認証失敗
        CS-->>CC: エラー (認証失敗)
        CC-->>B: ResponseEntity<Error>
        B-->>U: エラーメッセージ表示
    end
```

**ログアウトフロー**

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

**注文履歴閲覧フロー**

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
    
    CS-->>CC: OrderSummary[]
    CC-->>B: ResponseEntity<List<OrderSummary>>
    B-->>U: 注文履歴一覧表示
```


**支払い方法選択フロー**

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant OC as OrderController

    U->>B: 注文情報入力画面表示
    B-->>U: 支払い方法選択肢表示
    Note over B,U: 代引き、銀行振込
    
    U->>B: 支払い方法選択
    B->>OC: POST /api/order/preview
    Note over B,OC: {paymentMethod: "cash"}
    OC->>OC: 支払い方法検証
    OC-->>B: ResponseEntity<OrderPreview>
    B-->>U: 選択した支払い方法反映
```

**配送料計算フロー**

```mermaid
sequenceDiagram
    participant U as ユーザー
    participant B as ブラウザ
    participant OC as OrderController
    participant OS as OrderService

    U->>B: 配送先住所入力
    B->>OC: POST /api/order/preview
    Note over B,OC: {customerInfo: {address: "..."}}
    OC->>OS: calculateShippingFee(address, totalAmount)
    OS->>OS: 配送料計算ロジック
    Note over OS: 地域別・金額別送料計算
    OS-->>OC: ShippingFee
    OC-->>B: ResponseEntity<OrderPreview>
    B-->>U: 配送料込み合計金額表示
```

