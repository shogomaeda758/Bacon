package com.example.simplezakka.controller;

import com.example.simplezakka.dto.cart.Cart;
import com.example.simplezakka.dto.order.OrderRequest;
import com.example.simplezakka.dto.order.OrderResponse;
import com.example.simplezakka.dto.order.OrderDetailResponse;
import com.example.simplezakka.dto.order.OrderSummaryResponse;
import com.example.simplezakka.service.CartService; // CartService が必要
import com.example.simplezakka.service.OrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid; // バリデーションのため
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // RESTful APIのエンドポイントであることを示す
@RequestMapping("/api/orders") // このコントローラのマッピングのベースパス
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService; // カート情報を取得するために必要

    @Autowired
    public OrderController(OrderService orderService, CartService cartService) {
        this.orderService = orderService;
        this.cartService = cartService;
    }

    /**
     * 注文を確定するエンドポイント。
     * カートの内容と注文リクエストを受け取り、注文をデータベースに保存します。
     *
     * @param orderRequest 注文情報（顧客情報、支払い方法など）
     * @param session 現在のHTTPセッション (カート情報を取得するため)
     * @return 確定された注文のレスポンス (OrderResponse)
     */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody OrderRequest orderRequest, // リクエストボディをOrderRequestにマッピングし、バリデーションを行う
            HttpSession session) {
        
        // セッションからカート情報を取得
        Cart cart = cartService.getCart(session);
        
        // OrderServiceを呼び出して注文を確定
        OrderResponse orderResponse = orderService.placeOrder(cart, orderRequest, session);
        
        // 注文確定成功のレスポンスを返す (HTTP 200 OK)
        return ResponseEntity.ok(orderResponse);
    }

    /**
     * 顧客の注文履歴を取得するエンドポイント。
     *
     * @param customerId 顧客ID
     * @return 注文履歴のリスト (OrderSummaryResponse のリスト)
     */
    @GetMapping("/history/{customerId}")
    public ResponseEntity<List<OrderSummaryResponse>> getOrderHistory(
            @PathVariable Integer customerId) {
        
        List<OrderSummaryResponse> orderHistory = orderService.getOrderHistoryByCustomer(customerId);
        
        // 注文履歴を返す (HTTP 200 OK)
        return ResponseEntity.ok(orderHistory);
    }

    /**
     * 特定の注文の詳細情報を取得するエンドポイント。
     *
     * @param orderId 取得したい注文のID
     * @return 注文の詳細情報 (OrderDetailResponse)
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(
            @PathVariable Integer orderId) {
        
        OrderDetailResponse orderDetail = orderService.getOrderDetail(orderId);
        
        // 注文詳細を返す (HTTP 200 OK)
        return ResponseEntity.ok(orderDetail);
    }
}