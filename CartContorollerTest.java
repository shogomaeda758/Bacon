@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    private Cart cartWithOneItem;
    private Cart emptyCart;
    private MockHttpSession mockSession;

    @BeforeEach
    void setUp() {
        mockSession = new MockHttpSession();

        // アイテム1つ入りカート
        cartWithOneItem = new Cart();
        CartItem item1 = new CartItem("1", 1, "カート商品1", 1000, "/c1.png", 2, 2000);
        cartWithOneItem.setItems(Map.of("1", item1));
        cartWithOneItem.calculateTotals();

        // 空のカート
        emptyCart = new Cart();

        // デフォルトは空カートを返す設定（必要に応じて上書き）
        lenient().when(cartService.getCartFromSession(any(HttpSession.class))).thenReturn(emptyCart);
    }

    @Nested
    @DisplayName("GET /api/cart")
    class GetCartTests {

        @Test
        @DisplayName("API基本動作 (GET), サービス連携, レスポンス形式: カートあり")
        void getCart_WhenCartExists_ShouldReturnCartWithStatusOk() throws Exception {
            when(cartService.getCartFromSession(any(HttpSession.class))).thenReturn(cartWithOneItem);

            mockMvc.perform(get("/api/cart")
                            .session(mockSession)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalQuantity", is(cartWithOneItem.getTotalQuantity())))
                    .andExpect(jsonPath("$.totalPrice", is(cartWithOneItem.getTotalPrice())))
                    .andExpect(jsonPath("$.items", hasKey("1")))
                    .andExpect(jsonPath("$.items.1.productId", is(1)))
                    .andExpect(jsonPath("$.items.1.name", is("カート商品1")))
                    .andExpect(jsonPath("$.items.1.quantity", is(2)))
                    .andExpect(jsonPath("$.items.1.subtotal", is(2000)));

            verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
            verifyNoMoreInteractions(cartService);
        }

        @Test
        @DisplayName("API基本動作 (GET), サービス連携, レスポンス形式: 空カート")
        void getCart_WhenCartNotExists_ShouldReturnEmptyCartWithStatusOk() throws Exception {
            // 空カート返却設定（setUpのデフォルトでも可）
            when(cartService.getCartFromSession(any(HttpSession.class))).thenReturn(emptyCart);

            mockMvc.perform(get("/api/cart")
                            .session(mockSession)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalQuantity", is(0)))
                    .andExpect(jsonPath("$.totalPrice", is(0)))
                    .andExpect(jsonPath("$.items", anEmptyMap()));

            verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
            verifyNoMoreInteractions(cartService);
        }

        @Test
        @DisplayName("エラーハンドリング (サービス例外)")
        void getCart_WhenServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
            when(cartService.getCartFromSession(any(HttpSession.class))).thenThrow(new RuntimeException("サービスエラー"));

            mockMvc.perform(get("/api/cart")
                            .session(mockSession)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message", containsString("サービスエラー")));

            verify(cartService, times(1)).getCartFromSession(any(HttpSession.class));
            verifyNoMoreInteractions(cartService);
        }
    }

    @Nested
    @DisplayName("POST /api/cart")
    class AddItemTests {

        @Test
        @DisplayName("API基本動作 (POST), サービス連携, リクエスト/レスポンス: 有効な商品情報")
        void addItem_WithValidData_ShouldReturnUpdatedCartWithStatusOk() throws Exception {
            CartItemInfo itemInfo = new CartItemInfo();
            itemInfo.setProductId(1);
            itemInfo.setQuantity(2);

            when(cartService.addItemToCart(eq(itemInfo.getProductId()), eq(itemInfo.getQuantity()), any(HttpSession.class)))
                    .thenReturn(cartWithOneItem);

            mockMvc.perform(post("/api/cart")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(itemInfo))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalQuantity", is(cartWithOneItem.getTotalQuantity())))
                    .andExpect(jsonPath("$.totalPrice", is(cartWithOneItem.getTotalPrice())))
                    .andExpect(jsonPath("$.items.1.quantity", is(2)));

            verify(cartService, times(1)).addItemToCart(eq(itemInfo.getProductId()), eq(itemInfo.getQuantity()), any(HttpSession.class));
            verifyNoMoreInteractions(cartService);
        }

        @Test
        @DisplayName("サービス連携 (失敗時), エラーハンドリング: サービスがnullを返す場合")
        void addItem_WhenServiceReturnsNull_ShouldReturnNotFound() throws Exception {
            CartItemInfo itemInfo = new CartItemInfo();
            itemInfo.setProductId(99);
            itemInfo.setQuantity(1);

            when(cartService.addItemToCart(eq(itemInfo.getProductId()), eq(itemInfo.getQuantity()), any(HttpSession.class)))
                    .thenReturn(null);

            mockMvc.perform(post("/api/cart")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(itemInfo))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(cartService, times(1)).addItemToCart(eq(itemInfo.getProductId()), eq(itemInfo.getQuantity()), any(HttpSession.class));
            verifyNoMoreInteractions(cartService);
        }

        @Test
        @DisplayName("入力バリデーション (@NotNull): productIdがnull")
        void addItem_WithNullProductId_ShouldReturnBadRequest() throws Exception {
            CartItemInfo itemInfo = new CartItemInfo();
            itemInfo.setProductId(null);
            itemInfo.setQuantity(1);

            mockMvc.perform(post("/api/cart")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(itemInfo))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.productId", containsString("必須")));

            verifyNoInteractions(cartService);
        }

        @Test
        @DisplayName("入力バリデーション (@Min): quantityが0")
        void addItem_WithZeroQuantity_ShouldReturnBadRequest() throws Exception {
            CartItemInfo itemInfo = new CartItemInfo();
            itemInfo.setProductId(1);
            itemInfo.setQuantity(0);

            mockMvc.perform(post("/api/cart")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(itemInfo))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.quantity", containsString("1以上")));

            verifyNoInteractions(cartService);
        }
    }

    @Nested
    @DisplayName("PUT /api/cart/items/{itemId}")
    class UpdateItemTests {

        @Test
        @DisplayName("API基本動作 (PUT), サービス連携, リクエスト/レスポンス: 有効な数量")
        void updateItem_WithValidData_ShouldReturnUpdatedCartWithStatusOk() throws Exception {
            String itemId = "1";
            CartItemQuantityDto quantityDto = new CartItemQuantityDto();
            quantityDto.setQuantity(5);

            Cart updatedCart = new Cart();
            CartItem updatedItem = new CartItem("1", 1, "カート商品1", 1000, "/c1.png", 5, 5000);
            updatedCart.setItems(Map.of("1", updatedItem));
            updatedCart.calculateTotals();

            when(cartService.updateItemQuantity(eq(itemId), eq(quantityDto.getQuantity()), any(HttpSession.class)))
                    .thenReturn(updatedCart);

            mockMvc.perform(put("/api/cart/items/{itemId}", itemId)
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(quantityDto))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalQuantity", is(updatedCart.getTotalQuantity())))
                    .andExpect(jsonPath("$.totalPrice", is(updatedCart.getTotalPrice())))
                    .andExpect(jsonPath("$.items.1.quantity", is(5)));

            verify(cartService, times(1)).updateItemQuantity(eq(itemId), eq(quantityDto.getQuantity()), any(HttpSession.class));
            verifyNoMoreInteractions(cartService);
        }

        @Test
        @DisplayName("入力バリデーション (@Min): quantityが0")
        void updateItem_WithZeroQuantity_ShouldReturnBadRequest() throws Exception {
            String itemId = "1";
            CartItemQuantityDto quantityDto = new CartItemQuantityDto();
            quantityDto.setQuantity(0);

            mockMvc.perform(put("/api/cart/items/{itemId}", itemId)
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(quantityDto))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.quantity", containsString("1以上")));

            verifyNoInteractions(cartService);
        }

        @Test
        @DisplayName("入力バリデーション (@Min): quantityが負数")
        void updateItem_WithNegativeQuantity_ShouldReturnBadRequest() throws Exception {
            String itemId = "1";
            CartItemQuantityDto quantityDto = new CartItemQuantityDto();
            quantityDto.setQuantity(-5);

            mockMvc.perform(put("/api/cart/items/{itemId}", itemId)
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(quantityDto))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.quantity", containsString("1以上")));

            verifyNoInteractions(cartService);
        }

        @Test
        @DisplayName("入力バリデーション (@NotNull): quantityがnull")
        void updateItem_WithNullQuantity_ShouldReturnBadRequest() throws Exception {
            String itemId = "1";
            CartItemQuantityDto quantityDto = new CartItemQuantityDto();
            quantityDto.setQuantity(null);

            mockMvc.perform(put("/api/cart/items/{itemId}", itemId)
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(quantityDto))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.quantity", containsString("必須")));

            verifyNoInteractions(cartService);
        }
    }

    @Nested
    @DisplayName("DELETE /api/cart/items/{itemId}")
    class RemoveItemTests {

        @Test
        @DisplayName("API基本動作 (DELETE), サービス連携, レスポンス形式: itemId存在")
        void removeItem_WhenItemExists_ShouldReturnUpdatedCartWithStatusOk() throws Exception {
            String itemId = "1";

            when(cartService.removeItemFromCart(eq(itemId), any(HttpSession.class)))
                    .thenReturn(emptyCart);

            mockMvc.perform(delete("/api/cart/items/{itemId}", itemId)
                            .session(mockSession)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalQuantity", is(0)))
                    .andExpect(jsonPath("$.totalPrice", is(0)))
                    .andExpect(jsonPath("$.items", anEmptyMap()));

            verify(cartService, times(1)).removeItemFromCart(eq(itemId), any(HttpSession.class));
            verifyNoMoreInteractions(cartService);
        }

        @Test
        @DisplayName("サービス連携 (境界値): itemId不存在でもカートを返す")
        void removeItem_WhenItemNotExists_ShouldReturnCartFromServiceWithStatusOk() throws Exception {
            String nonExistingItemId = "99";

            when(cartService.removeItemFromCart(eq(nonExistingItemId), any(HttpSession.class)))
                    .thenReturn(cartWithOneItem);

            mockMvc.perform(delete("/api/cart/items/{itemId}", nonExistingItemId)
                            .session(mockSession)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalQuantity", is(cartWithOneItem.getTotalQuantity())));

            verify(cartService, times(1)).removeItemFromCart(eq(nonExistingItemId), any(HttpSession.class));
            verifyNoMoreInteractions(cartService);
        }
    }
}
