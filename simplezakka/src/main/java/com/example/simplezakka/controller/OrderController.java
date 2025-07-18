package com.example.simplezakka.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.example.simplezakka.service.CartService;
import com.example.simplezakka.service.OrderService;

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

        CartRespons cart = cartService.getCartFromSession(session);

        if (cart == null || cart.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(new OrderResponse("カートが空か無効です。注文を確定できません。"));
        }
        try {
            OrderResponse orderResponse = orderService.placeOrder(cart, orderRequest, session);
            return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
        } catch (IllegalArgumentException e) {
            // 商品が見つからない、会員情報が見つからないなどの引数不正
            return ResponseEntity.badRequest().body(new OrderResponse( e.getMessage()));
        } catch (IllegalStateException e) {
            // 在庫不足など、処理状態の不正
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new OrderResponse(e.getMessage()));
        } catch (Exception e) {
            // その他の予期せぬエラー。cartControllerlogger.error("注文確定中に予期せぬエラーが発生しました", e); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new OrderResponse( "注文確定中に予期せぬエラーが発生しました。"));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<OrderResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new OrderResponse( errorMessage));
    }

}