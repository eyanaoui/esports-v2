package com.esports.tests;

import com.esports.dao.ProductDAO;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductDAOTest {

    private ProductDAO productDAO;
    private int idProductTest;
    private String uniqueName;

    @BeforeEach
    void setUp() {
        productDAO = new ProductDAO();
        uniqueName = "ProductTest_" + UUID.randomUUID().toString().substring(0, 8);
        idProductTest = -1;
    }

    @Test
    @Order(1)
    void testAjouterProduct() {
        Product p = new Product(
                "Accessories",
                "Test description",
                "test.jpg",
                true,
                uniqueName,
                99.99,
                10
        );

        productDAO.addProduct(p);

        List<Product> products = productDAO.getAllProducts();

        assertFalse(products.isEmpty());

        Product inserted = products.stream()
                .filter(prod -> prod.getName().equals(uniqueName))
                .findFirst()
                .orElse(null);

        assertNotNull(inserted);
        assertEquals("Accessories", inserted.getCategory());
        assertEquals(99.99, inserted.getPrice());
        assertEquals(10, inserted.getStock());

        idProductTest = inserted.getId();
    }

    @Test
    @Order(2)
    void testModifierProduct() {
        Product p = new Product(
                "Accessories",
                "Before update",
                "before.jpg",
                true,
                uniqueName,
                120.0,
                5
        );

        productDAO.addProduct(p);

        List<Product> products = productDAO.getAllProducts();
        Product inserted = products.stream()
                .filter(prod -> prod.getName().equals(uniqueName))
                .findFirst()
                .orElse(null);

        assertNotNull(inserted);

        inserted.setName("ProductModifie");
        inserted.setCategory("Headsets");
        inserted.setDescription("After update");
        inserted.setImage("after.jpg");
        inserted.setPrice(150.0);
        inserted.setStock(20);

        productDAO.updateProduct(inserted);

        Product updated = productDAO.getProductById(inserted.getId());

        assertNotNull(updated);
        assertEquals("ProductModifie", updated.getName());
        assertEquals("Headsets", updated.getCategory());
        assertEquals("After update", updated.getDescription());
        assertEquals(150.0, updated.getPrice());
        assertEquals(20, updated.getStock());

        idProductTest = inserted.getId();
    }

    @Test
    @Order(3)
    void testSupprimerProduct() {
        Product p = new Product(
                "Games",
                "Delete test",
                "delete.jpg",
                true,
                uniqueName,
                50.0,
                3
        );

        productDAO.addProduct(p);

        List<Product> products = productDAO.getAllProducts();
        Product inserted = products.stream()
                .filter(prod -> prod.getName().equals(uniqueName))
                .findFirst()
                .orElse(null);

        assertNotNull(inserted);

        boolean deleted = productDAO.softDeleteProduct(inserted.getId());
        assertTrue(deleted);

        List<Product> updatedProducts = productDAO.getAllProducts();
        boolean exists = updatedProducts.stream()
                .anyMatch(prod -> prod.getId() == inserted.getId());

        assertFalse(exists);
    }

    @AfterEach
    void cleanUp() {
        List<Product> products = productDAO.getAllProducts();

        products.stream()
                .filter(prod -> prod.getName().startsWith("ProductTest_")
                        || prod.getName().equals("ProductModifie"))
                .forEach(prod -> productDAO.softDeleteProduct(prod.getId()));
    }
}