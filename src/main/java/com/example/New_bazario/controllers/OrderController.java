package com.example.New_bazario.controllers;

import com.example.New_bazario.entities.CartItem;
import com.example.New_bazario.entities.Order;
import com.example.New_bazario.exceptions.OrderNotFoundException;
import com.example.New_bazario.security.user.User;
import com.example.New_bazario.services.OrderService;
import com.example.New_bazario.services.UserService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    @PostMapping("/create-from-cart")
    public String createOrderFromCartAndRedirect(HttpSession session, Model model) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }
        User user = userService.getUserById(userId);

        Order newOrder = orderService.createOrderFromCart(user);
        if (newOrder == null || newOrder.getOrderItems() == null) {
            throw new RuntimeException("Order creation failed or no items in the order.");
        }

        BigDecimal totalAmount = newOrder.getOrderItems().stream()
                .filter(cartItem -> cartItem != null && cartItem.getProduct() != null)
                .map(cartItem -> cartItem.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("order", newOrder);
        model.addAttribute("cartItems", new ArrayList<>(newOrder.getOrderItems())); // Convert to List if needed
        model.addAttribute("totalAmount", totalAmount);

        return "redirect:/orders/view/" + newOrder.getOrderId();
    }

    @GetMapping("/view/{orderId}")
    public String viewOrder(
            @PathVariable Integer orderId,
            HttpSession session,
            Model model) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }

        User user = userService.getUserById(userId);
        Order order = orderService.getOrderByIdForUser(user, orderId);
        if (order == null) {
            throw new RuntimeException("Order not found");
        }

        BigDecimal totalAmount = order.getOrderItems().stream()
                .filter(cartItem -> cartItem != null && cartItem.getProduct() != null)
                .map(cartItem -> cartItem.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("order", order);
        model.addAttribute("cartItems", new ArrayList<>(order.getOrderItems())); // Convert Set to List
        model.addAttribute("totalAmount", totalAmount);

        return "order-details"; // Name of the Thymeleaf template
    }
    
    @PostMapping("/payment")
    public String goToPayment(
            @RequestParam("orderId") Integer orderId,
            HttpSession session,
            Model model) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }

        // Fetch the user and the order
        User user = userService.getUserById(userId);
        Order order = orderService.getOrderByIdForUser(user, orderId);
        if (order == null) {
            throw new OrderNotFoundException("Order with ID " + orderId + " not found.");
        }

        // Prepare order details for the frontend
        model.addAttribute("order", order);
        model.addAttribute("totalAmount", order.getOrderItems().stream()
                .filter(item -> item != null && item.getProduct() != null)
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return "payment-details"; // Redirect to a payment details page
    }
    

    @GetMapping("/payment-success")
    public String paymentSuccess() {
        return "payment-success"; // Thymeleaf template to show payment success message
    }

    
    @PostMapping("/drop")
    public String dropOrder(
            @RequestParam("orderId") Integer orderId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }

    
        return "redirect:/cart";
    }
    @PostMapping("/{orderId}/items/{cartItemId}")
    public String removeCartItemFromOrder(
            @PathVariable Integer orderId,
            @PathVariable Integer cartItemId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("userId");
        

        User user = userService.getUserById(userId);
        boolean removed = orderService.removeCartItemFromOrder(orderId, cartItemId, user);

        if (removed) {
            redirectAttributes.addFlashAttribute("successMessage", "Item removed successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to remove item.");
        }

        return "redirect:/orders/view/" + orderId; // Redirect back to the order details page
    }



}

