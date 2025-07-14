package com.example.simplezakka.service;

import com.example.simplezakka.dto.cart.CartRespons;
import com.example.simplezakka.dto.cart.CartItemResponse;
import com.example.simplezakka.entity.ProductEntity;
import com.example.simplezakka.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

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
        Optional<ProductEntity> productOpt = productRepository.findById(productId);

        if (productOpt.isPresent()) {
            ProductEntity product = productOpt.get();
            CartRespons cart = getCartFromSession(session);

            String itemId = String.valueOf(productId);
            int currentInCart = 0;

            // カートにすでに商品がある場合は数量を取得
            if (cart.getItems().containsKey(itemId)) {
                currentInCart = cart.getItems().get(itemId).getQuantity();
            }

            // 在庫チェック
            if (currentInCart + quantity > product.getStock()) {
                throw new IllegalArgumentException("在庫が足りません。現在の在庫: " + product.getStock() +
                    "、カート内数量: " + currentInCart + "、追加しようとしている数量: " + quantity);
            }

            // CartItemResponseを作成
            CartItemResponse item = new CartItemResponse(
                itemId,
                product.getProductId(),
                product.getName(),
                product.getPrice().intValue(),
                product.getImageUrl(),
                quantity,
                0 // subtotalはaddItemメソッド内で計算される
            );
            
            // 実装済みのaddItemメソッドを使用
            cart.addItem(item);
            
            // セッションの更新
            session.setAttribute(CART_SESSION_KEY, cart);
            return cart;
        }
        
        // 商品が存在しない場合
        return null;
    }
    
    public CartRespons updateItemQuantity(String itemId, Integer quantity, HttpSession session) {
        CartRespons cart = getCartFromSession(session);
        cart.updateQuantity(itemId, quantity);
        session.setAttribute(CART_SESSION_KEY, cart);
        return cart;
    }
    
    public CartRespons removeItemFromCart(String itemId, HttpSession session) {
        CartRespons cart = getCartFromSession(session);
        cart.removeItem(itemId);
        session.setAttribute(CART_SESSION_KEY, cart);
        return cart;
    }
    
    public void clearCart(HttpSession session) {
        session.removeAttribute(CART_SESSION_KEY);
    }
}