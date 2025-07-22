package com.example.simplezakka.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import com.example.simplezakka.dto.cart.CartRespons;
import com.example.simplezakka.dto.order.OrderRequest;
import com.example.simplezakka.dto.order.OrderResponse;
import com.example.simplezakka.dto.order.CustomerInfo; // CustomerInfo をインポート
import com.example.simplezakka.service.CartService;
import com.example.simplezakka.service.OrderService;
// import com.example.simplezakka.entity.Order; // OrderエンティティはControllerでは通常不要
// import com.example.simplezakka.entity.Product; // こちらもControllerでは通常不要

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    public OrderController(OrderService orderService, CartService cartService) {
        this.orderService = orderService;
        this.cartService = cartService;
    }

    @PostMapping("/order/confirm")
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody OrderRequest orderRequest,
            HttpSession session) {

        // セッションからcustomerIdを取得
        // ログイン中の会員の場合、HttpSessionに"customerId"という属性が設定されていることを想定
        Integer customerId = (Integer) session.getAttribute("customerId");

        // OrderRequestにCustomerInfoが設定されていることを確認し、customerIdを設定
        // @ValidでCustomerInfoのnullチェックは行われるはずだが、念のため追加
        if (orderRequest.getCustomerInfo() == null) {
            // このケースは@Validによって通常捕捉されるため、ここに到達することは稀
            return ResponseEntity.badRequest().body(new OrderResponse("顧客情報は必須です。"));
        }
        // ここが重要な修正点：セッションから取得したcustomerIdをOrderRequestのCustomerInfoに設定する
        orderRequest.getCustomerInfo().setCustomerId(customerId);

        // カートの取得とバリデーション
        CartRespons cart = cartService.getCartFromSession(session);
        if (cart == null || cart.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(new OrderResponse("カートが空か無効です。注文を確定できません。"));
        }

        try {
            // OrderService の placeOrder メソッドを呼び出し、OrderRequest と CartRespons を渡します。
            // OrderService内で OrderRequest の CustomerInfo.customerId の有無を見て、
            // isGuest フラグを適切に設定するロジックが必要です。
            OrderResponse orderResponse = orderService.placeOrder(cart, orderRequest);

            // 注文が成功したら、セッションからカートをクリアします。
            // どちらの層でクリアするかは設計によりますが、OrderControllerで行うのがシンプルです。
            cartService.clearCart(session);

            return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new OrderResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new OrderResponse(e.getMessage()));
        } catch (Exception e) {
            // 予期せぬエラーのロギングは重要です。
            // 例: logger.error("注文確定中に予期せぬエラーが発生しました", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new OrderResponse("注文確定中に予期せぬエラーが発生しました。"));
        }
    }

    // --- エラーハンドリングメソッドは既存のままでOK ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<OrderResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new OrderResponse(errorMessage));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<OrderResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new OrderResponse("リクエストボディのJSON形式が不正です。"));
    }
}