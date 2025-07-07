### 4.4. DTO定義
主要なAPIや機能で使用されるDTOの構造を示します。 (バリデーションルールは簡略化)

**商品関連 DTO**

```java
// 商品一覧用
public class ProductListItem {
    private Integer productId;
    private String name;
    private Integer price;
    private String imageUrl;

    public ProductListItem(Integer productId, String name, Integer price, String imageUrl) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    public Integer getProductId() { return productId; }
    public String getName() { return name; }
    public Integer getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
}

// 商品詳細用
public class ProductDetail {
    private Integer productId;
    private String name;
    private Integer price;
    private String description;
    private Integer stock;
    private String imageUrl;

    public ProductDetail(Integer productId, String name, Integer price, String description, Integer stock, String imageUrl) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.description = description;
        this.stock = stock;
        this.imageUrl = imageUrl;
    }

    public Integer getProductId() { return productId; }
    public String getName() { return name; }
    public Integer getPrice() { return price; }
    public String getDescription() { return description; }
    public Integer getStock() { return stock; }
    public String getImageUrl() { return imageUrl; }
}
```
**カート関連 DTO** 

```java

// カート全体 (セッション格納/APIレスポンス用)
public class Cart {
    private Map<String, CartItem> items = new LinkedHashMap<>();
    private int totalQuantity;
    private int totalPrice;

    public Map<String, CartItem> getItems() { return items; }
    public int getTotalQuantity() { return totalQuantity; }
    public int getTotalPrice() { return totalPrice; }

    public void addItem(ProductListItem product, int quantity) {
        String key = product.getProductId().toString();
        if (items.containsKey(key)) {
            CartItem item = items.get(key);
            item.setQuantity(item.getQuantity() + quantity);
            item.updateSubtotal();
        } else {
            CartItem newItem = new CartItem(
                key,
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                product.getImageUrl(),
                quantity
            );
            items.put(key, newItem);
        }
        calculateTotals();
    }

    public void updateQuantity(String id, int quantity) {
        CartItem item = items.get(id);
        if (item != null) {
            item.setQuantity(quantity);
            item.updateSubtotal();
        }
        calculateTotals();
    }

    public void removeItem(String id) {
        items.remove(id);
        calculateTotals();
    }

    public void calculateTotals() {
        totalQuantity = items.values().stream().mapToInt(CartItem::getQuantity).sum();
        totalPrice = items.values().stream().mapToInt(CartItem::getSubtotal).sum();
    }
}

// カート内商品 (セッション格納/APIレスポンス用)
public class CartItem {
    private String id;
    private Integer productId;
    private String name;
    private Integer price;
    private String imageUrl;
    private int quantity;
    private int subtotal;

    public CartItem(String id, Integer productId, String name, Integer price, String imageUrl, int quantity) {
        this.id = id;
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.subtotal = price * quantity;
    }

    public String getId() { return id; }
    public Integer getProductId() { return productId; }
    public String getName() { return name; }
    public Integer getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public int getQuantity() { return quantity; }
    public int getSubtotal() { return subtotal; }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        updateSubtotal();
    }

    public void updateSubtotal() {
        this.subtotal = price * quantity;
    }
}

// カート追加APIリクエスト用
public class CartItemInfo {
    @NotNull
    private Integer productId;

    @NotNull
    @Min(1)
    private Integer quantity;

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}

// カート数量更新APIリクエスト用
public class CartItemQuantityDto {
    @NotNull
    @Min(1)
    private Integer quantity;

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
```
**注文関連 DTO**

```java

// 注文APIリクエスト用
public class OrderRequest {
    @Valid
    @NotNull
    private CustomerInfo customerInfo;

    public CustomerInfo getCustomerInfo() { return customerInfo; }
    public void setCustomerInfo(CustomerInfo customerInfo) { this.customerInfo = customerInfo; }
}

// 注文APIリクエスト内の顧客情報用 (非会員用)
public class CustomerInfo {
    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String address;

    @NotBlank
    private String phoneNumber;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}

// 注文APIレスポンス用
public class OrderResponse {
    private Integer orderId;
    private LocalDateTime orderDate;

    public OrderResponse(Integer orderId, LocalDateTime orderDate) {
        this.orderId = orderId;
        this.orderDate = orderDate;
    }

    public Integer getOrderId() { return orderId; }
    public LocalDateTime getOrderDate() { return orderDate; }
}


