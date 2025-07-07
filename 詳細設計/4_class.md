## 4. クラス設計

ここでは、「シンプル雑貨オンライン」バックエンド（Spring Boot）アプリケーションのクラス構造について定義します。主要なパッケージ構成、クラス図、主要クラスの説明、およびデータ転送オブジェクト（DTO）の定義を示します。

### 4.1. 主要パッケージ構成
com.example.ecsite
├── controller
│   ├── ProductController
│   ├── CartController
│   ├── OrderController
│   ├── MemberController
│
├── service
│   ├── ProductService
│   ├── CartService
│   ├── OrderService
│   ├── MemberService
│
├── repository
│   ├── ProductRepository
│   ├── OrderRepository
│   ├── MemberRepository
│
├── entity
│   ├── Product
│   ├── Order
│   ├── OrderDetail
│   ├── Member
│   ├── Address
│
├── dto
│   ├── ProductListItemDto
│   ├── ProductDetailDto
│   ├── CartDto
│   ├── CartItemDto
│   ├── OrderRequestDto
│   ├── OrderResponseDto
│   ├── MemberDto
│   ├── AddressDto
│
└── exception

## 4.2 クラス図
### 4.2.1. 商品関連クラス図
<div class='mermaid'>
classDiagram
    class ProductController {
        +ProductService productService
        +getAllProducts() ResponseEntity~List~ProductListItem~~
        +getProductById(productId) ResponseEntity~ProductDetail~
    }

    class ProductService {
        +ProductRepository productRepository
        +findAllProducts() List~ProductListItem~
        +findProductById(productId) ProductDetail
    }

    class ProductRepository {
        <<Interface>>
        +JpaRepository~Product, Integer~
        +findAll() List~Product~
        +findById(productId) Optional~Product~
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

### 4.2.3. 注文関連クラス図 (非会員注文)
<div class='mermaid'>
classDiagram
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

    class Order {
        <<Entity>>
        +Integer orderId
        +String orderNumber
        +LocalDateTime orderDate
        +Integer totalAmount
        +String customerName // 非会員用
        +String shippingAddress
        +String shippingPhoneNumber
        +String status
        +Integer memberId "Nullable" // 会員ログイン時のみ
        +String paymentMethod "Nullable"
        +String paymentStatus "Nullable"
        +List~OrderDetail~ orderDetails
        +LocalDateTime createdAt "Nullable"
        +LocalDateTime updatedAt "Nullable"
    }

    class OrderDetail {
        <<Entity>>
        +Integer orderDetailId
        +Order order
        +Product product // 商品リレーション（更新時など）
        +String productName // 履歴用途
        +Integer price // 注文時点価格
        +Integer quantity
    }

    class Product {
        <<Entity>>
        +Integer productId
        +String name
        +String description
        +Integer price
        +Integer stock
        +String imageUrl
    }

    class OrderRequest {
        <<DTO>>
        +CustomerInfo customerInfo // 非会員のみ使用
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

    %% 関連
    OrderController --> OrderService : uses
    OrderController --> CartService : uses
    OrderService --> OrderRepository : uses
    OrderService --> OrderDetailRepository : uses
    OrderService --> ProductRepository : uses
    OrderService --> CartService : uses

    OrderRepository "1" -- "*" Order : manages
    OrderDetailRepository "1" -- "*" OrderDetail : manages
    Order "1" -- "*" OrderDetail : contains (Cascade PERSIST/MERGE)
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
        +String name
        +String email
        +String password
        +String address
        +String phoneNumber
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
        +String orderNumber
        +LocalDateTime orderDate
        +Integer totalAmount
        +String customerName // 非会員用
        +String shippingAddress
        +String shippingPhoneNumber
        +String status
        +Integer memberId "Nullable" // 会員ログイン時のみ
        +String paymentMethod "Nullable"
        +String paymentStatus "Nullable"
        +List~OrderDetail~ orderDetails
        +LocalDateTime createdAt "Nullable"
        +LocalDateTime updatedAt "Nullable"
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

Product : 商品情報エンティティ（id, name, price, stock, description）


Order : 注文情報エンティティ（id, orderDate, memberId, totalPrice）


OrderDetail : 注文明細エンティティ（id, orderId, productId, quantity, price）


Member : 会員情報エンティティ（id, name, email, password）


Address : 住所情報エンティティ（id, postalCode, prefecture, city, detail）


CartDto : セッション保持用カート情報（Map<productId, CartItemDto>、totalPrice）


OrderRequestDto : 注文情報リクエスト（顧客情報、カート内容）


OrderResponseDto : 注文完了レスポンス（注文ID、注文日時）
