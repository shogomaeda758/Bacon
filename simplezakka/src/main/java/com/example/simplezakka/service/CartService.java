package com.example.simplezakka.service;

import com.example.simplezakka.dto.cart.CartItemResponse;
import com.example.simplezakka.dto.cart.CartRespons;
import com.example.simplezakka.entity.Product;
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

    public CartRespons addItemToCart(long productId, Integer quantity, HttpSession session) {
        Optional<Product> productOpt = productRepository.findById((int) productId);

        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            CartRespons cart = getCartFromSession(session);

            char[] l = null;
            String itemId = String.valueOf(l);
            int currentInCart = 0;

            if (cart.getItems() != null && cart.getItems().containsKey(itemId)) {
                currentInCart = cart.getItems().get(itemId).getQuantity();
            }

            if (quantity <= 0) {
                throw new IllegalArgumentException("追加する数量は1以上である必要があります。");
            }

            if (currentInCart + quantity > product.getStock()) {
                throw new IllegalArgumentException("在庫が足りません。現在の在庫: " + product.getStock() +
                    "、カート内数量: " + currentInCart + "、追加しようとしている数量: " + quantity);
            }

            CartItemResponse newItem = new CartItemResponse();
            newItem.setProductId(product.getProductId());
            newItem.setName(product.getName());
            newItem.setImageUrl(product.getImageUrl());
            newItem.setPrice(product.getPrice());
            newItem.setQuantity(quantity);
            
            cart.addItem(newItem);

            session.setAttribute(CART_SESSION_KEY, cart);
            return cart;
        }
        return null;
    }

    public CartRespons updateItemQuantity(String itemId, Integer quantity, HttpSession session) {
        CartRespons cart = getCartFromSession(session);

        if (cart.getItems() == null || !cart.getItems().containsKey(itemId)) {
            throw new IllegalArgumentException("カートに商品が見つかりません: " + itemId);
        }

        if (quantity <= 0) {
            cart.removeItem(itemId);
        } else {
            CartItemResponse itemToUpdate = cart.getItems().get(itemId);
            Product product = productRepository.findById(itemToUpdate.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: " + itemToUpdate.getProductId()));

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