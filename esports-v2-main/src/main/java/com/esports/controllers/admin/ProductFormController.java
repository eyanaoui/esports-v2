package com.esports.controllers.admin;

import com.esports.dao.ProductDAO;
import com.esports.models.Product;
import com.esports.utils.ProductValidator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class ProductFormController {

    @FXML
    private Label formTitle;

    @FXML
    private Button saveButton;

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<String> categoryBox;

    @FXML
    private TextArea descriptionField;

    @FXML
    private TextField imageField;

    @FXML
    private TextField priceField;

    @FXML
    private TextField stockField;

    @FXML
    private CheckBox activeCheckBox;

    @FXML
    private Label nameError;

    @FXML
    private Label categoryError;

    @FXML
    private Label descriptionError;

    @FXML
    private Label imageError;

    @FXML
    private Label priceError;

    @FXML
    private Label stockError;

    @FXML
    private Label successMessage;

    private final ProductDAO productDAO = new ProductDAO();
    private Product currentProduct = null;

    @FXML
    public void initialize() {
        clearErrors();

        if (successMessage != null) {
            successMessage.setText("");
        }

        categoryBox.getItems().clear();
        categoryBox.getItems().addAll(
                "Accessories",
                "Games",
                "Consoles",
                "Headsets",
                "Controllers",
                "PC Items",
                "Gaming Chairs",
                "Keyboards",
                "Mouses"
        );

        activeCheckBox.setSelected(true);
    }

    public void setProduct(Product product) {
        this.currentProduct = product;

        if (product == null) {
            return;
        }

        formTitle.setText("Update Product");
        saveButton.setText("Update");

        nameField.setText(product.getName());
        categoryBox.setValue(product.getCategory());
        descriptionField.setText(product.getDescription());
        imageField.setText(product.getImage());
        priceField.setText(String.valueOf(product.getPrice()));
        stockField.setText(String.valueOf(product.getStock()));
        activeCheckBox.setSelected(product.isActive());
    }

    @FXML
    public void handleSaveProduct() {
        clearErrors();

        if (successMessage != null) {
            successMessage.setText("");
        }

        String name = nameField.getText();
        String category = categoryBox.getValue();
        String description = descriptionField.getText();
        String image = imageField.getText();
        String priceText = priceField.getText();
        String stockText = stockField.getText();

        String nameMsg = ProductValidator.validateName(name);
        String categoryMsg = ProductValidator.validateCategory(category);
        String descriptionMsg = ProductValidator.validateDescription(description);
        String imageMsg = ProductValidator.validateImage(image);
        String priceMsg = ProductValidator.validatePrice(priceText);
        String stockMsg = ProductValidator.validateStock(stockText);

        boolean isValid = true;

        if (!nameMsg.isEmpty()) {
            nameError.setText(nameMsg);
            isValid = false;
        }

        if (!categoryMsg.isEmpty()) {
            categoryError.setText(categoryMsg);
            isValid = false;
        }

        if (!descriptionMsg.isEmpty()) {
            descriptionError.setText(descriptionMsg);
            isValid = false;
        }

        if (!imageMsg.isEmpty()) {
            imageError.setText(imageMsg);
            isValid = false;
        }

        if (!priceMsg.isEmpty()) {
            priceError.setText(priceMsg);
            isValid = false;
        }

        if (!stockMsg.isEmpty()) {
            stockError.setText(stockMsg);
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        double price = Double.parseDouble(priceText.trim());
        int stock = Integer.parseInt(stockText.trim());
        boolean isActive = activeCheckBox.isSelected();

        if (currentProduct == null) {
            Product product = new Product(
                    category,
                    description,
                    image,
                    isActive,
                    name,
                    price,
                    stock
            );

            productDAO.addProduct(product);

            successMessage.setText("Product added successfully.");
            clearFields();

        } else {
            currentProduct.setName(name);
            currentProduct.setCategory(category);
            currentProduct.setDescription(description);
            currentProduct.setImage(image);
            currentProduct.setPrice(price);
            currentProduct.setStock(stock);
            currentProduct.setActive(isActive);

            productDAO.updateProduct(currentProduct);

            successMessage.setText("Product updated successfully.");
        }
    }

    @FXML
    public void handleClear() {
        clearFields();
        clearErrors();

        if (successMessage != null) {
            successMessage.setText("");
        }
    }

    @FXML
    public void handleCancel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/products.fxml"));
            Parent productsView = loader.load();

            AnchorPane contentArea = (AnchorPane) nameField.getScene().lookup("#contentArea");

            if (contentArea != null) {
                AnchorPane.setTopAnchor(productsView, 0.0);
                AnchorPane.setBottomAnchor(productsView, 0.0);
                AnchorPane.setLeftAnchor(productsView, 0.0);
                AnchorPane.setRightAnchor(productsView, 0.0);
                contentArea.getChildren().setAll(productsView);
            }

        } catch (IOException e) {
            System.out.println("❌ Error loading products view: " + e.getMessage());
        }
    }

    @FXML
    public void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Product Image");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        Stage stage = (Stage) imageField.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            imageField.setText(file.getName());
            imageError.setText("");
        }
    }

    private void clearFields() {
        nameField.clear();
        categoryBox.setValue(null);
        descriptionField.clear();
        imageField.clear();
        priceField.clear();
        stockField.clear();
        activeCheckBox.setSelected(true);
    }

    private void clearErrors() {
        nameError.setText("");
        categoryError.setText("");
        descriptionError.setText("");
        imageError.setText("");
        priceError.setText("");
        stockError.setText("");
    }
}