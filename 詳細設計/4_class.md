## 4. クラス設計

ここでは、「シンプル雑貨オンライン」バックエンド（Spring Boot）アプリケーションのクラス構造について定義します。主要なパッケージ構成、クラス図、主要クラスの説明、およびデータ転送オブジェクト（DTO）の定義を示します。

### 4.1. 主要パッケージ構成
## 4.1 主要パッケージ構成
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
    ProductController --> ProductService : uses
    ProductService --> ProductRepository : uses
    ProductRepository --> Product : manages

    class Product {
        Integer id
        String name
        Integer price
        Integer stock
        String description
    }
</div>

### 4.2.2. カート関連クラス図 (セッション管理)
<div class='mermaid'>
classDiagram
    CartController --> CartService : uses
    CartService --> ProductRepository : uses
    CartService --> CartDto : manages

    class CartDto {
        Map<productId, CartItemDto> items
        Integer totalPrice
    }

    class CartItemDto {
        Integer productId
        String name
        Integer price
        Integer quantity
    }
</div>

### 4.2.3. 注文関連クラス図 (非会員注文)
<div class='mermaid'>
classDiagram
    OrderController --> OrderService : uses
    OrderService --> ProductRepository : uses
    OrderService --> OrderRepository : uses
    OrderRepository --> Order : manages
    Order --> OrderDetail : has

    class Order {
        Integer id
        LocalDateTime orderDate
        Integer memberId
        Integer totalPrice
    }

    class OrderDetail {
        Integer id
        Integer orderId
        Integer productId
        Integer quantity
        Integer price
    }
</div>

### 4.2.4. 会員登録関連クラス図
<div class='mermaid'>
classDiagram
    MemberController --> MemberService : uses
    MemberService --> MemberRepository : uses
    MemberRepository --> Member : manages
    Member --> Address : has

    class Member {
        Integer id
        String name
        String email
        String password
    }

    class Address {
        Integer id
        String postalCode
        String prefecture
        String city
        String detail
    }
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
