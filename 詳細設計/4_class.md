## 4. クラス設計

ここでは、「シンプル雑貨オンライン」バックエンド（Spring Boot）アプリケーションのクラス構造について定義します。主要なパッケージ構成、クラス図、主要クラスの説明、およびデータ転送オブジェクト（DTO）の定義を示します。

### 4.1. 主要パッケージ構成
com.example.ecsite
├── controller
│   ├── ProductController         // 商品一覧・詳細取得
│   ├── CartController            // カート操作（セッション）
│   ├── OrderController           // 注文処理（非会員含む）
│   └── CustomerController        // 会員登録・ログイン・プロフィール更新
│
├── service
│   ├── ProductService
│   ├── CartService
│   ├── OrderService
│   └── CustomerService
│
├── repository
│   ├── ProductRepository
│   ├── OrderRepository
│   ├── OrderDetailRepository
│   └── CustomerRepository
│
├── entity
│   ├── Product                  // 商品情報
│   ├── Order                    // 注文情報
│   ├── OrderDetail              // 注文明細
│   └── Customer                 // 会員情報
│
├── dto
│   ├── ProductListItemDto       // 商品一覧表示用
│   ├── ProductDetailDto         // 商品詳細表示用
│   ├── CartDto                  // カート情報（セッション用）
│   ├── CartItemDto              // カート内アイテム
│   ├── CartItemInfo             // カート追加リクエスト
│   ├── CartItemQuantityDto      // カート数量変更リクエスト
│   ├── OrderRequestDto          // 注文リクエスト
│   ├── CustomerInfo             // 注文時の顧客情報（非会員）
│   ├── OrderResponseDto         // 注文完了レスポンス
│   ├── CustomerRegisterRequest  // 会員登録リクエスト
│   ├── LoginRequest             // ログインリクエスト
│   ├── CustomerUpdateRequest    // 会員情報更新リクエスト
│   ├── CustomerResponse         // 会員情報レスポンス
│   ├── OrderSummary             // 注文履歴一覧用
│   └── OrderItemSummary         // 注文履歴内商品の概要
│
└── exception                    // エラーハンドリング関連


## 4.2 クラス図
### 4.2.1. 商品関連クラス図
<div class='mermaid'>
classDiagram
    class ProductController {
        +ProductService productService
        +getAllProducts() ResponseEntity~List~ProductListItem~~
        +getProductById(productId) ResponseEntity~ProductDetail~
        +getProductsByCategory(categoryId) ResponseEntity~List~ProductListItem~~
        +searchProducts(keyword) ResponseEntity~List~ProductListItem~~
    }

    class ProductService {
        +ProductRepository productRepository
        +findAllProducts() List~ProductListItem~
        +findProductById(productId) ProductDetail
        +findProductsByCategory(categoryId) List~ProductListItem~
        +searchProducts(keyword) List~ProductListItem~
    }

    class ProductRepository {
        <<Interface>>
        +JpaRepository~Product, Integer~
        +findAll() List~Product~
        +findById(productId) Optional~Product~
        +findByCategoryId(categoryId) List~Product~
        +findByNameContaining(keyword) List~Product~
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
        +Integer categoryId
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
        +getCart(HttpSession) ResponseEntity~Cart~
        +addItem(CartItemInfo, HttpSession) ResponseEntity~Cart~
        +updateItem(itemId, CartItemQuantityDto, HttpSession) ResponseEntity~Cart~
        +removeItem(itemId, HttpSession) ResponseEntity~Cart~
    }

    class CartService {
        +ProductRepository productRepository
        +getCartFromSession(HttpSession) Cart
        +addItemToCart(productId, quantity, HttpSession) Cart
        +updateItemQuantity(itemId, quantity, HttpSession) Cart
        +removeItemFromCart(itemId, HttpSession) Cart
        +clearCart(HttpSession) void
    }

    class ProductRepository {
        <<Interface>>
        +findAll()
        +findById(id)
    }

    class Cart {
        <<DTO/Session Object>>
        +Map~String, CartItem~ items
        +int totalQuantity
        +int totalPrice
        +addItem(product, quantity) void
        +updateQuantity(itemId, quantity) void
        +removeItem(itemId) void
        +calculateTotals() void
    }

    class CartItem {
        <<DTO/Session Object>>
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
    CartController ..> Cart : returns
    CartController ..> CartItemInfo : receives
    CartController ..> CartItemQuantityDto : receives

</div>

### 4.2.3. 注文関連クラス図 
<div class='mermaid'>
classDiagram
    %% ユーザー関連
    class User {
        +cart: Cart
        +addToCart()
        +viewProduct()
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
        +placeOrder()
        +viewOrderHistory()
        +logout()
    }

    %% 商品関連
    class Product {
        <<Entity>>
        +Integer productId
        +String name
        +String description
        +Integer price
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
        +Customer customer
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
        +CustomerInfo customerInfo  // 非会員用
        +Integer memberId "Nullable"
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
        +Integer totalAmount
        +String status
    }

    %% コントローラー・サービス・リポジトリ
    class OrderController {
        +OrderService orderService
        +CartService cartService
        +placeOrder(OrderRequest, HttpSession) ResponseEntity~OrderResponse~
    }

    class OrderService {
        +OrderRepository orderRepository
        +OrderDetailRepository orderDetailRepository
        +ProductRepository productRepository
        +CartService cartService
        +placeOrder(Cart, OrderRequest) OrderResponse
    }

    class OrderRepository {
        <<Interface>>
        +JpaRepository~Order, Integer~
    }

    class OrderDetailRepository {
        <<Interface>>
        +JpaRepository~OrderDetail, Integer~
        +saveAll(details) List~OrderDetail~
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
    LoggedInMember --> Order : places
    Order --> OrderDetail : contains (Cascade PERSIST/MERGE)
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
    OrderService ..> OrderResponse : creates
</div>

### 4.2.4. 会員登録関連クラス図
<div class='mermaid'>
classDiagram
    class CustomerController {
        +CustomerService customerService
        +register(CustomerRegisterRequest) ResponseEntity~CustomerResponse~
        +login(LoginRequest) ResponseEntity~CustomerResponse~
        +updateProfile(CustomerUpdateRequest, HttpSession) ResponseEntity~CustomerResponse~
    }

    class CustomerService {
        +registerCustomer(CustomerRegisterRequest) CustomerResponse
        +authenticate(LoginRequest) CustomerResponse
        +updateCustomerInfo(CustomerUpdateRequest) CustomerResponse
        +getOrderHistory(customerId) List~OrderSummary~
    }

    class CustomerRepository {
        <<Interface>>
        +JpaRepository~Customer, Integer~
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
        +String name
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
        +String name
        +String email
        +String password "Optional"
        +String address
        +String phoneNumber
    }

    class CustomerResponse {
        <<DTO>>
        +Integer customerId
        +String name
        +String email
        +String address
        +String phoneNumber
    }

    class Order {
        <<Entity>>
        +Integer orderId
        +Customer customer
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
    }

    class OrderSummary {
        <<DTO>>
        +Integer orderId
        +LocalDateTime orderDate
        +Integer totalAmount
        +List~OrderItemSummary~ items
    }

    class OrderItemSummary {
        <<DTO>>
        +String productName
        +Integer quantity
        +Integer price
    }

    %% 関連
    CustomerController --> CustomerService : uses
    CustomerService --> CustomerRepository : uses
    CustomerRepository --> Customer : manages
    Customer "1" -- "*" Order : places

    CustomerController ..> CustomerRegisterRequest : receives
    CustomerController ..> LoginRequest : receives
    CustomerController ..> CustomerUpdateRequest : receives
    CustomerController ..> CustomerResponse : returns
    CustomerService ..> CustomerResponse : creates
    CustomerService ..> OrderSummary : returns
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

