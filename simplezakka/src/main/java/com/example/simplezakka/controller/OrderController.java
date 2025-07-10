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
@RequestMapping("/api") // API仕様に合わせてベースパスを /api に変更
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService; // カート情報を取得するために必要

    @Autowired
    public OrderController(OrderService orderService, CartService cartService) {
        this.orderService = orderService;
        this.cartService = cartService;
    }


    @PostMapping("/order/confirm") // API_08のエンドポイント
    public ResponseEntity<OrderResponse> placeOrder(
            // リクエストDTOはAPI仕様に合わせて別途定義・調整してください
            @Valid @RequestBody OrderRequest orderRequest, 
            HttpSession session) {
        
        // セッションからカート情報を取得
        Cart cart = cartService.getCart(session);
        
        // OrderServiceを呼び出して注文を確定
        OrderResponse orderResponse = orderService.placeOrder(cart, orderRequest, session);
        
        // 注文確定成功のレスポンスを返す (HTTP 200 OK)
        return ResponseEntity.ok(orderResponse); // API_08のレスポンスコードは201ですが、ここでは元の実装を維持
    }
    @PostMapping("/order/preview") // API_10のエンドポイント
    public ResponseEntity<List<OrderSummaryResponse>> getOrderHistory(
            // リクエストDTOはAPI仕様に合わせて別途定義・調整してください
            @RequestBody String requestBody) { // 仮のRequest Body
        
        // ここにAPI_10のロジック（小計、送料、合計金額の計算）が入ります
        // 現在はAPI_07のメソッド名と戻り値が合致していませんが、エンドポイントのみ修正します
        List<OrderSummaryResponse> orderHistory = orderService.getOrderHistoryByCustomer(1); // ダミーのcustomerId
        return ResponseEntity.ok(orderHistory);
    }
    
    @GetMapping("/member/me/orders") // API_13のエンドポイント
    public ResponseEntity<OrderDetailResponse> getOrderDetail(
            HttpSession session) { // API_13は認証トークンによるが、ここではセッションを仮定
        
        // ここにAPI_13のロジック（会員IDに基づいた注文履歴の取得）が入ります
        // 現在はAPI_08のメソッド名と戻り値が合致していませんが、エンドポイントのみ修正します
        OrderDetailResponse orderDetail = orderService.getOrderDetail(1); // ダミーのorderId
        return ResponseEntity.ok(orderDetail);
    }
    @GetMapping("/orders/{orderId}") // 従来のOrderDetail取得エンドポイント
    public ResponseEntity<OrderDetailResponse> getOrderDetailByOrderId(
            @PathVariable Integer orderId) {
        OrderDetailResponse orderDetail = orderService.getOrderDetail(orderId);
        return ResponseEntity.ok(orderDetail);
    }
}