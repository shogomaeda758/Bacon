package com.example.simplezakka.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import com.example.simplezakka.dto.cart.CartRespons;
import com.example.simplezakka.dto.customer.CustomerInfo;
import com.example.simplezakka.dto.order.OrderDetailResponse;
import com.example.simplezakka.dto.order.OrderRequest;
import com.example.simplezakka.dto.order.OrderResponse;
import com.example.simplezakka.dto.order.OrderSummaryResponse;
import com.example.simplezakka.entity.Customer;
import com.example.simplezakka.service.CartService;
import com.example.simplezakka.service.OrderService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CustomerController {

    private final OrderService orderService;
    private final CartService cartService;

    public CustomerController(OrderService orderService, CartService cartService) {
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
            return ResponseEntity.badRequest().body(new OrderResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new OrderResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OrderResponse("注文確定中に予期せぬエラーが発生しました。"));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<OrderResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new OrderResponse(errorMessage));
    }

    @PostMapping("/order/preview")
    public ResponseEntity<List<OrderSummaryResponse>> getOrderHistory(@RequestBody String requestBody) {
        List<OrderSummaryResponse> orderHistory = orderService.getOrderHistoryByCustomer(1); // ダミーのcustomerId
        return ResponseEntity.ok(orderHistory);
    }

    @GetMapping("/member/me/orders")
    public ResponseEntity<List<OrderSummaryResponse>> getOrderHistoryForMember(HttpSession session) {
        Integer customerId = 1;
        List<OrderSummaryResponse> orderHistory = orderService.getOrderHistoryByCustomer(customerId);
        return ResponseEntity.ok(orderHistory);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetailByOrderId(@PathVariable Integer orderId) {
        OrderDetailResponse orderDetail = orderService.getOrderDetail(orderId);
        return ResponseEntity.ok(orderDetail);
    }

    // 👇 追加された顧客情報取得用エンドポイント
    @GetMapping("/order/customer-info")
    public ResponseEntity<CustomerInfo> getCustomerInfo(HttpSession session) {
        Customer customer = (Customer) session.getAttribute("customer");
        if (customer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CustomerInfo info = new CustomerInfo();
        info.setName(customer.getFullName());
        info.setEmail(customer.getEmail());
        info.setAddress(customer.getAddress());
        info.setPhoneNumber(customer.getPhoneNumber());

        return ResponseEntity.ok(info);
    }
}