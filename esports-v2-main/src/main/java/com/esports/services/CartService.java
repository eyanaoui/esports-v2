package com.esports.services;

import com.esports.models.CartItem;
import com.esports.models.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CartService {

    private static final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();

    private CartService() {
    }

    public static ObservableList<CartItem> getCartItems() {
        return cartItems;
    }

    public static void addToCart(Product product, int quantity) {
        if (product == null || quantity <= 0) {
            return;
        }

        CartItem existingItem = findCartItemByProductId(product.getId());

        if (existingItem != null) {
            existingItem.increaseQuantity(quantity);
        } else {
            cartItems.add(new CartItem(product, quantity));
        }
    }

    public static void removeItem(CartItem item) {
        if (item != null) {
            cartItems.remove(item);
        }
    }

    public static void clearCart() {
        cartItems.clear();
    }

    public static boolean isEmpty() {
        return cartItems.isEmpty();
    }

    public static double getTotal() {
        return cartItems.stream()
                .mapToDouble(CartItem::getSubtotal)
                .sum();
    }

    public static int getItemCount() {
        return cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    public static boolean containsProduct(int productId) {
        return findCartItemByProductId(productId) != null;
    }

    public static int getQuantityForProduct(int productId) {
        CartItem item = findCartItemByProductId(productId);
        return item == null ? 0 : item.getQuantity();
    }

    private static CartItem findCartItemByProductId(int productId) {
        for (CartItem item : cartItems) {
            if (item.getProduct() != null && item.getProduct().getId() == productId) {
                return item;
            }
        }

        return null;
    }
}