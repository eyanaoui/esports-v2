package com.esports.controllers.admin;

import com.esports.dao.UserDAO;
import com.esports.models.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class UserController {

    @FXML private TextField searchField;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colFirstName, colLastName, colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colCreatedAt;

    private UserDAO userDAO = new UserDAO();
    private ObservableList<User> userList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("roles"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        loadUsers();

        userTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                User selected = userTable.getSelectionModel().getSelectedItem();
                if (selected != null) openForm(selected);
            }
        });
    }

    private void loadUsers() {
        try {
            List<User> users = userDAO.getAll();
            userList.setAll(users);
            userTable.setItems(userList);
        } catch (RuntimeException e) {
            // Display error alert to user
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Failed to Load Users");
            alert.setContentText("Could not retrieve users from database: " + e.getMessage());
            alert.showAndWait();
            // Graceful degradation: keep existing table data visible
            // Do not clear userList or userTable
        }
    }

    @FXML
    private void handleAdd() {
        openForm(null);
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.isEmpty()) { loadUsers(); return; }
        try {
            List<User> filtered = userDAO.getAll().stream()
                    .filter(u -> (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(query))
                            || (u.getLastName() != null && u.getLastName().toLowerCase().contains(query))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(query)))
                    .collect(Collectors.toList());
            userList.setAll(filtered);
            userTable.setItems(userList);
        } catch (RuntimeException e) {
            // Display error alert to user
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Failed to Search Users");
            alert.setContentText("Could not retrieve users from database: " + e.getMessage());
            alert.showAndWait();
            // Graceful degradation: keep existing table data visible
            // Do not clear userList or userTable
        }
    }

    private void openForm(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/user-form.fxml"));
            Stage stage = new Stage();
            stage.setTitle(user == null ? "Add User" : "Edit User");
            stage.setScene(new Scene(loader.load()));
            stage.initModality(Modality.APPLICATION_MODAL);
            UserFormController controller = loader.getController();
            controller.setUser(user);
            controller.setOnSuccess(this::loadUsers);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
