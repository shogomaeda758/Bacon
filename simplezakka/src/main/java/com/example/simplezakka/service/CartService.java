package com.example.simplezakka.service;

import com.example.simplezakka.dto.cart.CartItemResponse;
import com.example.simplezakka.dto.cart.CartRespons;
import com.example.simplezakka.entity.Product; // Productエンティティをインポート
import com.example.simplezakka.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal; // BigDecimalをインポート
import java.util.Optional;

@Service
public class CartService {

    private static final String CART_SESSION_KEY = "cart";

    private final ProductRepository productRepository;

    public CartService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public CartRespons getCartFromSession(HttpSession session) {
        CartRespons cart = (CartRespons) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            // CartResponsのコンストラクタまたはフィールド初期化で
            // itemsマップ、totalQuantity、totalPriceが適切に初期化されることを確認してください。
            // 例: private Map<String, CartItemResponse> items = new HashMap<>();
            //     private int totalQuantity = 0;
            //     private BigDecimal totalPrice = BigDecimal.ZERO;
            cart = new CartRespons();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    public CartRespons addItemToCart(Integer productId, Integer quantity, HttpSession session) {
        Optional<Product> productOpt = productRepository.findById(productId);

        if (productOpt.isPresent()) {
            Product product = productOpt.get(); // ProductのpriceはBigDecimalであるべき
            CartRespons cart = getCartFromSession(session);

            String itemId = String.valueOf(productId); // カートのキー
            int currentInCart = 0;

            // 🔽 カートにすでに商品がある場合は数量を取得
            if (cart.getItems() != null && cart.getItems().containsKey(itemId)) {
                currentInCart = cart.getItems().get(itemId).getQuantity();
            }

            // 🔽 数量が0以下でないかチェック（負の値や0は追加できないようにする）
            if (quantity <= 0) {
                throw new IllegalArgumentException("追加する数量は1以上である必要があります。");
            }

            // 🔽 在庫チェック
            if (currentInCart + quantity > product.getStock()) {
                throw new IllegalArgumentException("在庫が足りません。現在の在庫: " + product.getStock() +
                 "、カート内数量: " + currentInCart + "、追加しようとしている数量: " + quantity);
            }

            // CartItemResponse オブジェクトを作成し、必要な情報を設定
            CartItemResponse newItem = new CartItemResponse();
            newItem.setProductId(product.getProductId());
            newItem.setName(product.getName());
            newItem.setImageUrl(product.getImageUrl()); // 画像URLもセット

            // ★BigDecimalに揃える処理: Productのpriceをそのままセット（Product.priceがBigDecimalであることを前提）
            newItem.setPrice(product.getPrice());
            newItem.setQuantity(quantity);
            
            // CartItemResponseのsubtotalは、newItemのpriceとquantityから計算します
            // newItem.setSubtotal(newItem.getPrice().multiply(BigDecimal.valueOf(quantity)));
            // もしCartRespons.addItem内でsubtotalが設定されるのであれば、ここでの設定は不要ですが、
            // CartResponsのaddItem内でnewItemのsubtotalが使われる前提であれば設定しておくと安全です。

            // CartResponsの addItem(CartItemResponse item) メソッドを呼び出す
            // このメソッドがCartRespons内のitemsマップを更新し、
            // totalQuantityとtotalPriceを再計算することを前提とします。
            cart.addItem(newItem);

            //セッションの更新
            session.setAttribute(CART_SESSION_KEY, cart);
            return cart;
        }

        //商品が存在しない場合
        return null; // または適切な例外をスロー
    }

    public CartRespons updateItemQuantity(String itemId, Integer quantity, HttpSession session) {
        CartRespons cart = getCartFromSession(session);

        if (cart.getItems() == null || !cart.getItems().containsKey(itemId)) {
            throw new IllegalArgumentException("カートに商品が見つかりません: " + itemId);
        }

        // 数量が0以下なら削除
        if (quantity <= 0) {
            cart.removeItem(itemId);
        } else {
            CartItemResponse itemToUpdate = cart.getItems().get(itemId);
            Product product = productRepository.findById(itemToUpdate.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: " + itemToUpdate.getProductId()));

            // 在庫チェック
            if (quantity > product.getStock()) {
                throw new IllegalArgumentException("在庫が足りません。現在の在庫: " + product.getStock() +
                    "、設定しようとしている数量: " + quantity);
            }

            // CartResponsのupdateQuantityメソッド（引数にitemIdとquantity）を呼び出す
            // このメソッドがCartRespons内のitemsマップの該当アイテムの数量と小計を更新し、
            // totalQuantityとtotalPriceを再計算することを前提とします。
            cart.updateQuantity(itemId, quantity);
        }
        session.setAttribute(CART_SESSION_KEY, cart);
        return cart;
    }

    public CartRespons removeItemFromCart(String itemId, HttpSession session) {
        CartRespons cart = getCartFromSession(session);
        // CartRespons内の removeItem メソッドを呼び出す
        // このメソッドがitemsマップからアイテムを削除し、totalQuantityとtotalPriceを再計算することを前提とします。
        if (cart.getItems() != null && cart.getItems().containsKey(itemId)) {
             cart.removeItem(itemId);
        }
        session.setAttribute(CART_SESSION_KEY, cart);
        return cart;
    }

    public void clearCart(HttpSession session) {
        session.removeAttribute(CART_SESSION_KEY);
    }
}