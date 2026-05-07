package com.esports.tests;

import com.esports.dao.OrderDAO;
import com.esports.dao.OrderItemDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderDAOTest {

    private OrderDAO orderDAO;
    private OrderItemDAO orderItemDAO;
    private String uniqueReference;

    @BeforeEach
    void setUp() {
        orderDAO = new OrderDAO();
        orderItemDAO = new OrderItemDAO();
        uniqueReference = "TEST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    void testAjouterOrder() {
        com.esports.models.Order order = new com.esports.models.Order(
                LocalDateTime.now(),
                "test@mail.com",
                "Ali",
                "Ben Salah",
                "12345678",
                "Cash",
                "PENDING",
                uniqueReference,
                "NEW",
                199.99
        );

        int insertedId = orderDAO.addOrder(order);
        assertTrue(insertedId > 0);

        List<com.esports.models.Order> orders = orderDAO.getAllOrders();
        assertFalse(orders.isEmpty());

        boolean found = orders.stream()
                .anyMatch(o -> o.getReference().equals(uniqueReference));

        assertTrue(found);
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void testAfficherOrder() {
        com.esports.models.Order order = new com.esports.models.Order(
                LocalDateTime.now(),
                "afficher@mail.com",
                "Oussema",
                "Amri",
                "99887766",
                "Card",
                "PENDING",
                uniqueReference,
                "NEW",
                300.0
        );

        int insertedId = orderDAO.addOrder(order);
        assertTrue(insertedId > 0);

        List<com.esports.models.Order> orders = orderDAO.getAllOrders();

        com.esports.models.Order foundOrder = orders.stream()
                .filter(o -> o.getReference().equals(uniqueReference))
                .findFirst()
                .orElse(null);

        assertNotNull(foundOrder);
        assertEquals("Oussema", foundOrder.getCustomerFirstName());
        assertEquals("Amri", foundOrder.getCustomerLastName());
        assertEquals("Card", foundOrder.getPaymentMethod());
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void testSupprimerOrder() {
        com.esports.models.Order order = new com.esports.models.Order(
                LocalDateTime.now(),
                "delete@mail.com",
                "Delete",
                "Test",
                "11112222",
                "PayPal",
                "PENDING",
                uniqueReference,
                "NEW",
                99.0
        );

        int insertedId = orderDAO.addOrder(order);
        assertTrue(insertedId > 0);

        boolean itemsDeleted = orderItemDAO.deleteItemsByOrderId(insertedId);
        boolean orderDeleted = orderDAO.deleteOrder(insertedId);

        assertTrue(itemsDeleted);
        assertTrue(orderDeleted);

        List<com.esports.models.Order> orders = orderDAO.getAllOrders();
        boolean exists = orders.stream()
                .anyMatch(o -> o.getId() == insertedId);

        assertFalse(exists);
    }

    @AfterEach
    void cleanUp() {
        List<com.esports.models.Order> orders = orderDAO.getAllOrders();

        orders.stream()
                .filter(o -> o.getReference() != null && o.getReference().startsWith("TEST-"))
                .forEach(o -> {
                    orderItemDAO.deleteItemsByOrderId(o.getId());
                    orderDAO.deleteOrder(o.getId());
                });
    }
}