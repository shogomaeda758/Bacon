package com.example.simplezakka.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.simplezakka.dto.cart.CartRespons;
import com.example.simplezakka.dto.order.OrderDetailResponse;
import com.example.simplezakka.dto.order.OrderRequest;
import com.example.simplezakka.dto.order.OrderResponse;
import com.example.simplezakka.dto.order.OrderSummaryResponse;
import com.example.simplezakka.service.CartService; // CartService が必要
import com.example.simplezakka.service.OrderService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid; // バリデーションのため
import java.util.List;

@RestController // RESTful APIのエンドポイントであることを示す
@RequestMapping("/api") // API仕様に合わせてベースパスを /api に変更
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService; // カート情報を取得するために必要

    public OrderController(OrderService orderService, CartService cartService) {
        this.orderService = orderService;
        this.cartService = cartService;
    }


    @PostMapping("/order/confirm") // API_08のエンドポイント
    public ResponseEntity<OrderResponse> placeOrder(
            // リクエストDTOはAPI仕様に合わせて別途定義・調整してください
            @Valid @RequestBody OrderRequest orderRequest, 
            HttpSession session) {

           CartRespons cart = cartService.getCartFromSession(session);

        if (cart == null || cart.getItems().isEmpty()) {
            // カートが空の場合は400 Bad Requestを返す
            return ResponseEntity.badRequest().body(new OrderResponse("カートが空か無効です。注文を確定できません。"));
        }
        try {
            // ②のorderService.placeOrder呼び出し
            OrderResponse orderResponse = orderService.placeOrder(cart, orderRequest, session);
            // API_08の仕様である201 Createdを返す
            return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
        } catch (Exception e) {
            // 予期せぬサーバー内部エラーが発生した場合は500 Internal Server Errorを返す
            // 必要に応じてログ出力も追加 (例: logger.error("注文確定中に予期せぬエラーが発生しました", e);)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new OrderResponse("注文確定中に予期せぬエラーが発生しました。"));
        }
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