## 4. クラス設計

ここでは、「シンプル雑貨オンライン」バックエンド（Spring Boot）アプリケーションのクラス構造について定義します。主要なパッケージ構成、クラス図、主要クラスの説明、およびデータ転送オブジェクト（DTO）の定義を示します。

### 4.1. 主要パッケージ構成
com.example.ecsite
├── controller
│   ├── ProductController         // 商品一覧・詳細取得、検索、カテゴリ別
│   ├── CartController            // カート操作（セッション：追加、削除、変更、取得）
│   ├── OrderController           // 注文処理、履歴取得、注文詳細取得（非会員対応含む）
│   └── CustomerController        // 会員登録、ログイン、プロフィール更新
│
├── service
│   ├── ProductService            // 商品検索、カテゴリ検索、詳細取得
│   ├── CartService               // セッションからのカート操作ロジック
│   ├── OrderService              // 注文処理、履歴取得、明細取得、送料計算など
│   └── CustomerService           // 会員認証、登録、更新処理
│
├── repository
│   ├── ProductRepository         // 商品情報の取得
│   ├── OrderRepository           // 注文エンティティのCRUD操作
│   ├── OrderDetailRepository     // 注文明細の一括保存・取得
│   └── CustomerRepository        // 会員情報の取得、メールアドレス検索など
│
├── entity
│   ├── Product                   // 商品情報（名前、説明、価格、カテゴリなど）
│   ├── Category                  // カテゴリ情報（商品に関連付け）
│   ├── Order                     // 注文情報（会員・非会員共通）
│   ├── OrderDetail               // 注文明細（商品、数量、価格）
│   └── Customer                  // 会員情報（名前、メール、パスワードなど）
│
├── dto
│   ├── ProductListItemDto        // 商品一覧表示用（名前、画像、価格）
│   ├── ProductDetailDto          // 商品詳細表示用（説明、在庫含む）
│   ├── CartDto                   // カート全体情報（合計、アイテムリスト）
│   ├── CartItemDto               // カート内アイテム表示用（商品名、数量、小計）
│   ├── CartItemInfo              // カート追加時のリクエスト（商品ID・数量）
│   ├── CartItemQuantityDto       // 数量更新用DTO（数量のみ）
│   ├── OrderRequestDto           // 注文登録用リクエストDTO（会員ID/非会員顧客情報など）
│   ├── CustomerInfo              // 非会員注文時の配送・連絡先情報
│   ├── OrderResponseDto          // 注文登録完了時に返すDTO（注文番号、日付、合計など）
│   ├── CustomerRegisterRequest   // 会員登録フォーム入力情報
│   ├── LoginRequest              // ログイン時に送信される認証情報
│   ├── CustomerUpdateRequest     // 会員情報更新時の入力
│   ├── CustomerResponse          // 会員情報返却用（登録・ログイン・更新後）
│   ├── OrderSummary              // 注文履歴一覧表示用（注文ID、日付、金額など）
│   └── OrderItemSummary          // 注文内商品明細（商品名、単価、数量、小計）
│
└── exception
    ├── GlobalExceptionHandler    // 例外を一元的に処理（@ControllerAdvice）
    ├── ResourceNotFoundException // リソース未検出時
    └── ValidationException       // 入力バリデーション失敗時


## 4.2 クラス図
### 4.2.1. 商品関連クラス図
<div class='mermaid'>
classDiagram
    class ProductController {
        +ProductService productService
        +getAllProducts(): ResponseEntity~List~ProductListItem~~
        +getProductById(productId): ResponseEntity~ProductDetail~
        +getProductsByCategory(categoryId): ResponseEntity~List~ProductListItem~~
        +searchProducts(keyword): ResponseEntity~List~ProductListItem~~
    }

    class ProductService {
        +ProductRepository productRepository
        +findAllProducts(): List~ProductListItem~
        +findProductById(productId): ProductDetail
        +findProductsByCategory(categoryId): List~ProductListItem~
        +searchProducts(keyword): List~ProductListItem~
    }

    class ProductRepository {
        <<Interface>>
        +JpaRepository~Product, Integer~
        +findAll(): List~Product~
        +findById(productId): Optional~Product~
        +findByCategoryId(categoryId): List~Product~
        +findByNameContaining(keyword): List~Product~
    }

    class Product {
        <<Entity>>
        +Integer productId
        +String name
        +String description
        +Integer price
        +Integer stock
        +String imageUrl
        +Boolean isRecommended "Nullable"
        +LocalDateTime createdAt "Nullable"
        +LocalDateTime updatedAt "Nullable"
        +Category category
    }

    class Category {
        <<Entity>>
        +Integer categoryId
        +String categoryName
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class ProductListItem {
        <<DTO>>
        +Integer productId
        +String name
        +Integer price
        +String imageUrl
    }

    class ProductDetail {
        <<DTO>>
        +Integer productId
        +String name
        +Integer price
        +String description
        +Integer stock
        +String imageUrl
    }

    %% 関連
    ProductController --> ProductService : uses
    ProductService --> ProductRepository : uses

    ProductRepository "1" -- "*" Product : manages
    Product --> Category : belongs to

    ProductService ..> ProductListItem : creates
    ProductService ..> ProductDetail : creates

    ProductController ..> ProductListItem : returns
    ProductController ..> ProductDetail : returns

</div>

### 4.2.2. カート関連クラス図 (セッション管理)
<div class='mermaid'>
classDiagram
    class CartController {
        +CartService cartService
        +getCart(HttpSession): ResponseEntity~CartResponse~
        +addItem(CartItemInfo, HttpSession): ResponseEntity~CartResponse~
        +updateItem(itemId, CartItemQuantityDto, HttpSession): ResponseEntity~CartResponse~
        +removeItem(itemId, HttpSession): ResponseEntity~CartResponse~
    }

    class CartService {
        +ProductRepository productRepository
        +getCartFromSession(HttpSession): Cart
        +addItemToCart(productId, quantity, HttpSession): Cart
        +updateItemQuantity(itemId, quantity, HttpSession): Cart
        +removeItemFromCart(itemId, HttpSession): Cart
        +clearCart(HttpSession): void
    }

    class ProductRepository {
        <<Interface>>
        +findAll()
        +findById(id)
    }

    class Cart {
        <<Session Object>>
        +Map~String, CartItem~ items
        +int totalQuantity
        +int totalPrice
        +addItem(product, quantity): void
        +updateQuantity(itemId, quantity): void
        +removeItem(itemId): void
        +calculateTotals(): void
    }

    class CartItem {
        <<Session Object>>
        +String id
        +Integer productId
        +String name
        +Integer price
        +String imageUrl
        +int quantity
        +int subtotal
    }

    %% DTO類

    class CartResponse {
        <<DTO>>
        +List~CartItemResponse~ items
        +int totalQuantity
        +int totalPrice
    }

    class CartItemResponse {
        <<DTO>>
        +String id
        +Integer productId
        +String name
        +Integer price
        +String imageUrl
        +int quantity
        +int subtotal
    }

    class CartItemInfo {
        <<DTO>>
        +Integer productId
        +Integer quantity
    }

    class CartItemQuantityDto {
        <<DTO>>
        +Integer quantity
    }

    %% 関係

    CartController --> CartService : uses
    CartService --> ProductRepository : uses
    CartService ..> Cart : manages (in Session)
    CartService ..> CartItem : uses

    Cart "1" *-- "items *" CartItem : contains

    CartController ..> CartResponse : returns
    CartController ..> CartItemInfo : receives
    CartController ..> CartItemQuantityDto : receives

    CartResponse "1" *-- "items *" CartItemResponse : contains

    CartService ..> CartResponse : creates
    CartService ..> CartItemResponse : creates
</div>

### 4.2.3. 注文関連クラス図 
<div class='mermaid'>
classDiagram
    %% ユーザー関連
    class User {
        +cart: Cart
        +addToCart()
        +viewProduct()
        +placeOrder()
    }

    class Guest {
        +register()
    }

    class Member {
        -email: String
        -password: String
        +login()
        +viewMyPage()
        +editProfile()
    }

    class LoggedInMember {
        +logout()
    }

    %% 商品関連
    class Product {
        <<Entity>>
        +Integer productId
        +String name
        +String description
        +BigDecimal price
        +Integer stock
        +String imageUrl
    }

    %% カート関連
    class Cart {
        -items: List~CartItem~
        +addItem()
        +removeItem()
        +clearCart()
    }

    class CartItem {
        -product: Product
        -quantity: int
    }

    %% 注文関連
    class Order {
        <<Entity>>
        +Integer orderId
        +Member member "Nullable"
        +String orderEmail
        +String orderName
        +String orderPhoneNumber
        +String orderAddress
        +BigDecimal totalPrice
        +BigDecimal shippingFee
        +String paymentMethod
        +LocalDateTime orderDate
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
        +String status
        +Boolean isGuest
        +calculateTotal()
    }

    class OrderDetail {
        <<Entity>>
        +Integer orderDetailId
        +Order order
        +Product product
        +Integer quantity
        +BigDecimal unitPrice
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    %% DTO類

    class OrderRequest {
        <<DTO>>
        +CustomerInfo customerInfo
        +Integer memberId "Nullable"
        +Boolean isGuest
    }

    class CustomerInfo {
        <<DTO>>
        +String name
        +String email
        +String address
        +String phoneNumber
    }

    class OrderResponse {
        <<DTO>>
        +Integer orderId
        +String orderNumber
        +LocalDateTime orderDate
        +BigDecimal totalAmount
        +BigDecimal shippingFee
        +String status
    }

    class OrderSummaryResponse {
        <<DTO>>
        +Integer orderId
        +LocalDateTime orderDate
        +BigDecimal totalPrice
        +String status
    }

    class OrderDetailResponse {
        <<DTO>>
        +Integer orderId
        +CustomerInfo customerInfo
        +LocalDateTime orderDate
        +BigDecimal shippingFee
        +BigDecimal totalPrice
        +String paymentMethod
        +String status
        +List~OrderItemDetailResponse~ items
    }

    class OrderItemDetailResponse {
        <<DTO>>
        +String productName
        +Integer quantity
        +BigDecimal unitPrice
        +BigDecimal subtotal
    }

    class OrderPreview {
        <<DTO>>
        +List~OrderItemPreview~ items
        +BigDecimal subtotal
        +BigDecimal shippingFee
        +BigDecimal totalAmount
        +String paymentMethod
        +CustomerInfo customerInfo
    }

    class OrderItemPreview {
        <<DTO>>
        +Product product
        +Integer quantity
        +BigDecimal unitPrice
    }

    %% コントローラー・サービス・リポジトリ
    class OrderController {
        +OrderService orderService
        +CartService cartService
        +placeOrder(OrderRequest, HttpSession): ResponseEntity~OrderResponse~
        +getOrderHistory(memberId: Integer): ResponseEntity~List~OrderSummaryResponse~~
        +getOrderDetail(orderId: Integer): ResponseEntity~OrderDetailResponse~
    }

    class OrderService {
        +OrderRepository orderRepository
        +OrderDetailRepository orderDetailRepository
        +ProductRepository productRepository
        +CartService cartService
        +placeOrder(Cart, OrderRequest): OrderResponse
        +getOrderHistoryByMember(memberId: Integer): List~OrderSummaryResponse~
        +getOrderDetail(orderId: Integer): OrderDetailResponse
        +calculateShippingFee(address, totalAmount): BigDecimal
    }

    class OrderRepository {
        <<Interface>>
        +JpaRepository~Order, Integer~
        +findByMember(member: Member): List~Order~
    }

    class OrderDetailRepository {
        <<Interface>>
        +JpaRepository~OrderDetail, Integer~
        +saveAll(details): List~OrderDetail~
        +findByOrderId(orderId: Integer): List~OrderDetail~
    }

    class ProductRepository {
        <<Interface>>
        +JpaRepository~Product, Integer~
    }

    %% 継承関係
    User <|-- Guest
    User <|-- Member
    Member <|-- LoggedInMember

    %% 関連
    User --> Cart : owns
    Member --> Order : places
    Order --> OrderDetail : contains
    OrderDetail --> Product : refers to
    Cart --> CartItem : contains
    CartItem --> Product : refers to

    OrderController --> OrderService : uses
    OrderController --> CartService : uses
    OrderService --> OrderRepository : uses
    OrderService --> OrderDetailRepository : uses
    OrderService --> ProductRepository : uses
    OrderService --> CartService : uses

    OrderRepository "1" -- "*" Order : manages
    OrderDetailRepository "1" -- "*" OrderDetail : manages
    OrderDetail "n" -- "1" Order : belongs to
    OrderDetail "n" -- "1" Product : refers to

    OrderController ..> OrderRequest : receives
    OrderController ..> OrderResponse : returns
    OrderController ..> OrderPreview : returns
    OrderController ..> OrderSummaryResponse : returns
    OrderController ..> OrderDetailResponse : returns
    OrderDetailResponse --> OrderItemDetailResponse : contains

    %% ServiceがDTOを作成する関係を追加
    OrderService ..> OrderResponse : creates
    OrderService ..> OrderSummaryResponse : creates
    OrderService ..> OrderDetailResponse : creates
    OrderService ..> OrderPreview : creates

</div>

### 4.2.4. 会員登録関連クラス図
<div class='mermaid'>
classDiagram
    class CustomerController {
        +CustomerService customerService
        +register(CustomerRegisterRequest) ResponseEntity~CustomerRegisterResponse~
        +login(LoginRequest) ResponseEntity~LoginResponse~
        +updateProfile(CustomerUpdateRequest, HttpSession) ResponseEntity~CustomerUpdateResponse~
    }

    class CustomerService {
        +registerCustomer(CustomerRegisterRequest) CustomerRegisterResponse
        +authenticate(LoginRequest) LoginResponse
        +updateCustomerInfo(CustomerUpdateRequest) CustomerUpdateResponse
    }

    class CustomerRepository {
        <<Interface>>
        +extends JpaRepository~Customer, Integer~
        +findByEmail(email) Optional~Customer~
    }

    class Customer {
        <<Entity>>
        +Integer customerId
        +String email
        +String password
        +String lastName
        +String firstName
        +String phoneNumber
        +String address
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class CustomerRegisterRequest {
        <<DTO>>
        +String lastName
        +String firstName
        +String email
        +String password
        +String address
        +String phoneNumber
    }

    class LoginRequest {
        <<DTO>>
        +String email
        +String password
    }

    class CustomerUpdateRequest {
        <<DTO>>
        +String lastName
        +String firstName
        +String email
        +String password
        +String address
        +String phoneNumber
    }

    class CustomerRegisterResponse {
        <<DTO>>
        +Integer customerId
        +String lastName
        +String firstName
        +String email
    }

    class LoginResponse {
        <<DTO>>
        +Integer customerId
        +String lastName
        +String firstName
        +String email
        +String authToken
    }

    class CustomerUpdateResponse {
        <<DTO>>
        +Integer customerId
        +String lastName
        +String firstName
        +String email
        +String address
        +String phoneNumber
        +LocalDateTime updatedAt
    }

    %% 関連
    CustomerController --> CustomerService : uses
    CustomerService --> CustomerRepository : uses
    CustomerRepository --> Customer : manages
    CustomerService --> Customer : uses

    CustomerController ..> CustomerRegisterRequest : receives
    CustomerController ..> LoginRequest : receives
    CustomerController ..> CustomerUpdateRequest : receives

    CustomerController ..> CustomerRegisterResponse : returns
    CustomerController ..> LoginResponse : returns
    CustomerController ..> CustomerUpdateResponse : returns

    %% ServiceがDTO（Response）を生成する関係
    CustomerService ..> CustomerRegisterResponse : creates
    CustomerService ..> LoginResponse : creates
    CustomerService ..> CustomerUpdateResponse : creates

</div>

## 4.3. 主要クラス説明

**Product**  
商品情報エンティティ（`productId`, `name`, `description`, `price`, `stock`, `imageUrl`, `isRecommended`, `createdAt`, `updatedAt`）

**Order**  
注文情報エンティティ（`orderId`, `orderNumber`, `orderDate`, `totalAmount`, `customerName`, `shippingAddress`, `shippingPhoneNumber`, `status`, `memberId`, `paymentMethod`, `paymentStatus`, `orderDetails`, `createdAt`, `updatedAt`）

**OrderDetail**  
注文明細エンティティ（`orderDetailId`, `order`, `product`, `productName`, `price`, `quantity`）

**Customer**  
会員情報エンティティ（`customerId`, `name`, `email`, `password`, `address`, `phoneNumber`, `createdAt`, `updatedAt`）

**CartDto**  
セッション保持用カート情報（`Map<String, CartItemDto> items`, `totalQuantity`, `totalPrice`）

**CartItemDto**  
カート内商品の情報（`id`, `productId`, `name`, `price`, `imageUrl`, `quantity`, `subtotal`）

**OrderRequestDto**  
注文リクエストDTO（`customerInfo`（非会員用）, `memberId`（会員注文用））

**CustomerInfo**  
非会員向け注文時の顧客情報DTO（`name`, `email`, `address`, `phoneNumber`）

**OrderResponseDto**  
注文完了レスポンスDTO（`orderId`, `orderNumber`, `orderDate`, `totalAmount`, `status`）

**CustomerRegisterRequest / LoginRequest / CustomerUpdateRequest**  
会員登録・ログイン・プロフィール更新用リクエストDTO（`name`, `email`, `password`, `address`, `phoneNumber` など）

**CustomerResponse**  
会員情報レスポンスDTO（`customerId`, `name`, `email`, `address`, `phoneNumber`）

**OrderSummary**  
注文履歴一覧表示用DTO（`orderId`, `orderDate`, `totalAmount`, `items`）

**OrderItemSummary**  
注文履歴の商品情報DTO（`productName`, `quantity`, `price`）

