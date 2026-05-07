package com.esports.services;

import com.esports.dao.ProductDAO;
import com.esports.dao.ProductRecommendationDAO;
import com.esports.models.CartItem;
import com.esports.models.Product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecommendationService {

    private final ProductDAO productDAO = new ProductDAO();
    private final ProductRecommendationDAO recommendationDAO = new ProductRecommendationDAO();

    public List<Product> getRecommendationsForProduct(Product product, int limit) {
        if (product == null) {
            return productDAO.getPopularProducts(limit);
        }

        List<Product> recommendations =
                recommendationDAO.getRecommendedProductsForProduct(product.getId(), limit);

        if (recommendations.size() >= limit) {
            return recommendations;
        }

        List<Product> sameCategory =
                productDAO.getProductsByCategory(product.getCategory(), limit, product.getId());

        recommendations = mergeProducts(recommendations, sameCategory, limit);

        if (recommendations.size() >= limit) {
            return recommendations;
        }

        List<Product> popular = productDAO.getPopularProducts(limit);

        return mergeProducts(recommendations, popular, limit);
    }

    public List<Product> getRecommendationsForCart(List<CartItem> cartItems, int limit) {
        if (cartItems == null || cartItems.isEmpty()) {
            return productDAO.getPopularProducts(limit);
        }

        List<Integer> productIds = cartItems.stream()
                .map(item -> item.getProduct().getId())
                .toList();

        List<Product> recommendations =
                recommendationDAO.getRecommendedProductsForCart(productIds, limit);

        if (recommendations.size() >= limit) {
            return recommendations;
        }

        for (CartItem item : cartItems) {
            Product product = item.getProduct();

            List<Product> sameCategory =
                    productDAO.getProductsByCategory(product.getCategory(), limit, product.getId());

            recommendations = mergeProducts(recommendations, sameCategory, limit);

            if (recommendations.size() >= limit) {
                return recommendations;
            }
        }

        List<Product> popular = productDAO.getPopularProducts(limit);

        return mergeProducts(recommendations, popular, limit);
    }

    private List<Product> mergeProducts(List<Product> firstList, List<Product> secondList, int limit) {
        Map<Integer, Product> map = new LinkedHashMap<>();

        if (firstList != null) {
            for (Product product : firstList) {
                if (product != null && product.getStock() > 0) {
                    map.put(product.getId(), product);
                }
            }
        }

        if (secondList != null) {
            for (Product product : secondList) {
                if (product != null && product.getStock() > 0 && !map.containsKey(product.getId())) {
                    map.put(product.getId(), product);
                }

                if (map.size() >= limit) {
                    break;
                }
            }
        }

        return new ArrayList<>(map.values());
    }
}