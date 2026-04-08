package com.akhilpaila.order.service;

import com.akhilpaila.order.dto.*;
import com.akhilpaila.order.entity.Order;
import com.akhilpaila.order.exception.ResourceNotFoundException;
import com.akhilpaila.order.kafka.OrderEventPublisher;
import com.akhilpaila.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final StringRedisTemplate redisTemplate;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderEventPublisher orderEventPublisher,
                            StringRedisTemplate redisTemplate) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public OrderResponse placeOrder(OrderRequest request) {

        // Step 1: Check idempotency key in Redis
        String redisKey = "idempotency:" + request.getIdempotencyKey();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            log.warn("Duplicate order request: {}",
                request.getIdempotencyKey());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Order already placed with this idempotency key");
        }

        // Step 2: Fetch real price from product-service
        BigDecimal unitPrice;
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://localhost:8081/api/products/"
                + request.getProductId();

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> product =
                restTemplate.getForObject(url, java.util.Map.class);

            if (product == null || product.get("price") == null) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found with id: "
                    + request.getProductId());
            }

            unitPrice = new BigDecimal(
                product.get("price").toString());
            log.info("Fetched price {} for productId: {}",
                unitPrice, request.getProductId());

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch product price: {}",
                e.getMessage());
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Could not fetch product details. " +
                "Is product-service running?");
        }

        // Step 3: Calculate total amount
        BigDecimal totalAmount = unitPrice.multiply(
            new BigDecimal(request.getQuantity()));

        // Step 4: Save order with PENDING status
        Order order = Order.builder()
            .userId(request.getUserId())
            .productId(request.getProductId())
            .quantity(request.getQuantity())
            .totalAmount(totalAmount)
            .status(Order.OrderStatus.PENDING)
            .idempotencyKey(request.getIdempotencyKey())
            .build();
        Order saved = orderRepository.save(order);
        log.info("Order saved with id: {}", saved.getId());

        // Step 5: Store idempotency key in Redis (24 hours)
        redisTemplate.opsForValue().set(redisKey, "PROCESSED",
            24, TimeUnit.HOURS);

        // Step 6: Publish Kafka event
        OrderPlacedEvent event = OrderPlacedEvent.builder()
            .orderId(saved.getId())
            .userId(saved.getUserId())
            .productId(saved.getProductId())
            .quantity(saved.getQuantity())
            .totalAmount(saved.getTotalAmount())
            .idempotencyKey(saved.getIdempotencyKey())
            .build();
        orderEventPublisher.publishOrderPlaced(event);

        return mapToResponse(saved);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Order not found with id: " + id));
        return mapToResponse(order);
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
            .id(order.getId())
            .userId(order.getUserId())
            .productId(order.getProductId())
            .quantity(order.getQuantity())
            .totalAmount(order.getTotalAmount())
            .status(order.getStatus())
            .idempotencyKey(order.getIdempotencyKey())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .build();
    }
}