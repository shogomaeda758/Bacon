### 4.4. DTO定義
public class ProductListItemDto {
    private Integer productId;
    private String name;
    private Integer price;
}

public class CartItemDto {
    private Integer productId;
    private String name;
    private Integer price;
    private Integer quantity;
}

public class OrderRequestDto {
    private String name;
    private String email;
    private String address;
    private List<CartItemDto> items;
}

public class OrderResponseDto {
    private Integer orderId;
    private LocalDateTime orderDate;
}


