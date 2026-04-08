package com.akhilpaila.order.service;

import com.akhilpaila.order.dto.OrderRequest;
import com.akhilpaila.order.dto.OrderResponse;
import java.util.List;

public interface OrderService {
    OrderResponse placeOrder(OrderRequest request);
    OrderResponse getOrderById(Long id);
    List<OrderResponse> getOrdersByUserId(String userId);
}
