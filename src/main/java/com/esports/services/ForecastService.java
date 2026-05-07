package com.esports.services;

import com.esports.dao.ProductForecastDAO;
import com.esports.models.ProductForecast;

import java.util.List;

public class ForecastService {

    private final ProductForecastDAO productForecastDAO = new ProductForecastDAO();

    public List<ProductForecast> getAllForecasts() {
        return productForecastDAO.getAllForecasts();
    }

    public List<ProductForecast> searchForecasts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllForecasts();
        }

        return productForecastDAO.searchForecasts(keyword.trim());
    }

    public List<ProductForecast> getRestockNeededForecasts() {
        return productForecastDAO.getRestockNeededForecasts();
    }

    public int countRestockNeeded(List<ProductForecast> forecasts) {
        if (forecasts == null) {
            return 0;
        }

        return (int) forecasts.stream()
                .filter(forecast -> forecast.getRecommendedReorderQty() > 0)
                .count();
    }

    public double totalPredictedQty(List<ProductForecast> forecasts) {
        if (forecasts == null) {
            return 0.0;
        }

        return forecasts.stream()
                .mapToDouble(ProductForecast::getPredictedQty)
                .sum();
    }

    public int totalRecommendedReorder(List<ProductForecast> forecasts) {
        if (forecasts == null) {
            return 0;
        }

        return forecasts.stream()
                .mapToInt(ProductForecast::getRecommendedReorderQty)
                .sum();
    }
}