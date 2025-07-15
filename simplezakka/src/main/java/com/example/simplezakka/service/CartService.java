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
            cart = new CartRespons();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    public CartRespons addItemToCart(Integer productId, Integer quantity, HttpSession session) {
        Optional<Product> productOpt = productRepository.findById(productId);

        if (productOpt.isPresent()) {
            Product product = productOpt.get(); 
            CartRespons cart = getCartFromSession(session);

            String itemId = String.valueOf(productId); 
            int currentInCart = 0;

            // カートにすでに商品がある場合は数量を取得
            if (cart.getItems() != null && cart.getItems().containsKey(itemId)) {
                currentInCart = cart.getItems().get(itemId).getQuantity();
            }

            // 数量が0以下でないかチェック
            if (quantity <= 0) {
                throw new IllegalArgumentException("追加する数量は1以上である必要があります。");
            }

            // 在庫チェック
            if (currentInCart + quantity > product.getStock()) {
                throw new IllegalArgumentException("在庫が足りません。現在の在庫: " + product.getStock() +
                 "、カート内数量: " + currentInCart + "、追加しようとしている数量: " + quantity);
            }

            // CartItemResponse オブジェクトを作成し、必要な情報を設定
            CartItemResponse newItem = new CartItemResponse();
            newItem.setProductId(product.getProductId());
            newItem.setName(product.getName());
            newItem.setImageUrl(product.getImageUrl());
            newItem.setPrice(product.getPrice());
            newItem.setQuantity(quantity);
            cart.addItem(newItem);

            //セッションの更新
            session.setAttribute(CART_SESSION_KEY, cart);
            return cart;
        }
        //商品が存在しない場合
        return null; 
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

            cart.updateQuantity(itemId, quantity);
        }
        session.setAttribute(CART_SESSION_KEY, cart);
        return cart;
    }

    public CartRespons removeItemFromCart(String itemId, HttpSession session) {
        CartRespons cart = getCartFromSession(session);
        
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