### 4.4. DTO定義
主要なAPIや機能で使用されるDTOの構造を示します。 (バリデーションルールは簡略化)

**商品関連 DTO**

```java
// 商品一覧用 DTO
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

// 商品詳細用 DTO
public class ProductDetailResponse {
    private Integer productId;
    private String name;
    private Integer price;
    private String description;
    private Integer stock;
    private String imageUrl;

    public ProductDetailResponse(Integer productId, String name, Integer price, String description, Integer stock, String imageUrl) {
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
// カート全体（セッション・レスポンス用）
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

// カート内アイテム
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
// カート商品追加リクエスト用
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

// 数量更新用
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
// 注文リクエストDTO
public class OrderRequest {
    @Valid
    private CustomerInfo customerInfo;

    private Integer memberId;
    private Boolean isGuest;

    public CustomerInfo getCustomerInfo() { return customerInfo; }
    public void setCustomerInfo(CustomerInfo customerInfo) { this.customerInfo = customerInfo; }

    public Integer getMemberId() { return memberId; }
    public void setMemberId(Integer memberId) { this.memberId = memberId; }

    public Boolean getIsGuest() { return isGuest; }
    public void setIsGuest(Boolean isGuest) { this.isGuest = isGuest; }
}

// 顧客情報（非会員）
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

// 注文完了レスポンスDTO
public class OrderResponse {
    private Integer orderId;
    private String orderNumber;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private String status;

    public OrderResponse(Integer orderId, String orderNumber, LocalDateTime orderDate, BigDecimal totalAmount, BigDecimal shippingFee, String status) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.shippingFee = shippingFee;
        this.status = status;
    }

    public Integer getOrderId() { return orderId; }
    public String getOrderNumber() { return orderNumber; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public String getStatus() { return status; }
}

// 注文履歴一覧表示用DTO
public class OrderSummaryResponse {
    private Integer orderId;
    private LocalDateTime orderDate;
    private BigDecimal totalPrice;
    private String status;

    public OrderSummaryResponse(Integer orderId, LocalDateTime orderDate, BigDecimal totalPrice, String status) {
        this.orderId = orderId;
        this.orderDate = orderDate;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    public Integer getOrderId() { return orderId; }
    public LocalDateTime getOrderDate() { return orderDate; }

// 注文明細の商品情報DTO
public class OrderItemDetailResponse {
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    public OrderItemDetailResponse(String productName, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public String getProductName() { return productName; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
}

// 注文詳細画面全体のレスポンスDTO
public class OrderDetailResponse {
    private Integer orderId;
    private CustomerInfo customerInfo;
    private LocalDateTime orderDate;
    private BigDecimal shippingFee;
    private BigDecimal totalPrice;
    private String paymentMethod;
    private String status;
    private List<OrderItemDetailResponse> items;

    public OrderDetailResponse(Integer orderId, CustomerInfo customerInfo, LocalDateTime orderDate, BigDecimal shippingFee,
                               BigDecimal totalPrice, String paymentMethod, String status, List<OrderItemDetailResponse> items) {
        this.orderId = orderId;
        this.customerInfo = customerInfo;
        this.orderDate = orderDate;
        this.shippingFee = shippingFee;
        this.totalPrice = totalPrice;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.items = items;
    }

    public Integer getOrderId() { return orderId; }
    public CustomerInfo getCustomerInfo() { return customerInfo; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getStatus() { return status; }
    public List<OrderItemDetailResponse> getItems() { return items; }
}

// 注文プレビュー画面用DTO
public class OrderPreview {
    private List<OrderItemPreview> items;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private CustomerInfo customerInfo;

    public OrderPreview(List<OrderItemPreview> items, BigDecimal subtotal, BigDecimal shippingFee,
                        BigDecimal totalAmount, String paymentMethod, CustomerInfo customerInfo) {
        this.items = items;
        this.subtotal = subtotal;
        this.shippingFee = shippingFee;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.customerInfo = customerInfo;
    }

    public List<OrderItemPreview> getItems() { return items; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public CustomerInfo getCustomerInfo() { return customerInfo; }
}

// 注文プレビュー用の各商品情報DTO
public class OrderItemPreview {
    private ProductListItem product;
    private Integer quantity;
    private BigDecimal unitPrice;

    public OrderItemPreview(ProductListItem product, Integer quantity, BigDecimal unitPrice) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public ProductListItem getProduct() { return product; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}

```


