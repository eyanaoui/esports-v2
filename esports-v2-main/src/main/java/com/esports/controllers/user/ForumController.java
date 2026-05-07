package com.esports.controllers.user;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.esports.dao.SujetDao;
import com.esports.models.Sujet;
import com.esports.models.ForumNotification;
import com.esports.services.ForumAdvancedService;
import com.esports.services.ForumActivityService;
import com.esports.services.ForumDuplicateDetectionService;
import com.esports.services.ForumFavoriteService;
import com.esports.services.ForumNotificationService;
import com.esports.services.ForumPinService;
import com.esports.utils.ForumInputRules;
import com.esports.utils.ValidationHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ForumController {
    private static final int TITRE_MIN = 3;
    private static final int TITRE_MAX = 200;
    private static final int CONTENU_MIN = 5;
    private static final int CONTENU_MAX = 5000;

    @FXML private TextField titreField, searchField;
    @FXML private TextField imageField;
    @FXML private TextArea contenuArea;
    @FXML private Label notifCountLabel;
    @FXML private ListView<String> activityListView;
    @FXML private ListView<String> favoritesListView;
    @FXML private Label titreErrorLabel, contenuErrorLabel;
    @FXML private VBox forumContainer;
    @FXML private Button saveTopicButton;

    private final SujetDao sujetDao = new SujetDao();
    private final ForumAdvancedService advancedService = new ForumAdvancedService();
    private final ForumPinService pinService = new ForumPinService();
    private final ForumDuplicateDetectionService duplicateService = new ForumDuplicateDetectionService();
    private final ForumFavoriteService favoriteService = new ForumFavoriteService();
    private final ForumNotificationService notificationService = new ForumNotificationService();
    private final ForumActivityService activityService = new ForumActivityService();
    private final int currentUserId = 1;
    private List<Sujet> allSujets;
    private Sujet editingSujet;
    private File selectedImageFile;

    @FXML
    public void initialize() {
        if (saveTopicButton != null) {
            saveTopicButton.setText("Save topic");
        }
        titreField.textProperty().addListener((o, a, b) -> clearTitreError());
        contenuArea.textProperty().addListener((o, a, b) -> clearContenuError());
        clearAllErrors();
        refreshForum();
        refreshNotificationCount();
        refreshActivityFeed();
        refreshFavorites();
    }

    private void refreshForum() {
        allSujets = sujetDao.getAll();
        for (Sujet sujet : allSujets) {
            advancedService.calculateTrendingScore(sujet.getId());
        }
        allSujets = sujetDao.getAll();
        displaySujets(allSujets);
    }

    private void displaySujets(List<Sujet> sujets) {
        forumContainer.getChildren().clear();
        for (Sujet s : sujets) {
            HBox card = new HBox(14);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setStyle("-fx-background-color: rgba(43, 43, 43, 0.92); -fx-padding: 12; -fx-background-radius: 10;");

            Node preview = createPreviewNode(s.getImage());

            VBox contentBox = new VBox(8);
            Label t = new Label(s.getTitre());
            t.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15;");
            t.setWrapText(true);

            Label statusBadge = new Label(statusBadgeText(s));
            statusBadge.setStyle(statusBadgeStyle(s));

            Label snippet = new Label(buildSnippet(s.getContenu()));
            snippet.setStyle("-fx-text-fill: #d0d3d4;");
            snippet.setWrapText(true);

            Label keywords = new Label(keywordBadgeText(s.getKeywords()));
            keywords.setStyle("-fx-text-fill: #9ad6ff; -fx-font-size: 11; -fx-background-color: rgba(52, 152, 219, 0.18); -fx-padding: 4 8; -fx-background-radius: 10;");

            HBox actions = new HBox(8);
            Button openBtn = new Button("Open");
            openBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
            openBtn.setOnAction(e -> openMessages(s));

            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
            editBtn.setOnAction(e -> startEdit(s));

            Button delBtn = new Button("Delete");
            delBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
            delBtn.setOnAction(e -> confirmDelete(s));

            Button qrBtn = new Button("QR Code");
            qrBtn.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white;");
            qrBtn.setOnAction(e -> showQrCodeForSujet(s));

            Button pinBtn = new Button("Pin");
            pinBtn.setStyle("-fx-background-color: #6c5ce7; -fx-text-fill: white;");
            pinBtn.setOnAction(e -> {
                pinService.pinTopic(s.getId());
                activityService.addActivity(currentUserId, s.getId(), null, "PIN_TOPIC", "Sujet épinglé: " + s.getTitre());
                refreshForum();
            });

            Button unpinBtn = new Button("Unpin");
            unpinBtn.setStyle("-fx-background-color: #636e72; -fx-text-fill: white;");
            unpinBtn.setOnAction(e -> {
                pinService.unpinTopic(s.getId());
                activityService.addActivity(currentUserId, s.getId(), null, "UNPIN_TOPIC", "Sujet désépinglé: " + s.getTitre());
                refreshForum();
            });

            Button favBtn = new Button(favoriteService.isFavorite(currentUserId, s.getId()) ? "★ Unfavorite" : "☆ Favorite");
            favBtn.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: #2d3436;");
            favBtn.setOnAction(e -> {
                if (favoriteService.isFavorite(currentUserId, s.getId())) {
                    favoriteService.removeFromFavorites(currentUserId, s.getId());
                } else {
                    favoriteService.addToFavorites(currentUserId, s.getId());
                }
                refreshFavorites();
                refreshForum();
            });

            actions.getChildren().addAll(openBtn, editBtn, delBtn, qrBtn, pinBtn, unpinBtn, favBtn);
            contentBox.getChildren().addAll(t, statusBadge, snippet, keywords, actions);
            HBox.setHgrow(contentBox, Priority.ALWAYS);
            card.getChildren().addAll(preview, contentBox);
            forumContainer.getChildren().add(card);
        }
    }

    private void startEdit(Sujet s) {
        editingSujet = sujetDao.getById(s.getId());
        if (editingSujet == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Topic");
            alert.setHeaderText(null);
            alert.setContentText("Topic not found.");
            alert.showAndWait();
            return;
        }
        titreField.setText(editingSujet.getTitre());
        contenuArea.setText(editingSujet.getContenu() != null ? editingSujet.getContenu() : "");
        imageField.setText(editingSujet.getImage() != null ? editingSujet.getImage() : "");
        selectedImageFile = null;
        if (saveTopicButton != null) {
            saveTopicButton.setText("Update topic");
        }
    }

    @FXML
    private void handleClearForm() {
        editingSujet = null;
        titreField.clear();
        contenuArea.clear();
        imageField.clear();
        selectedImageFile = null;
        clearAllErrors();
        if (saveTopicButton != null) {
            saveTopicButton.setText("Save topic");
        }
    }

    @FXML
    private void handleSaveTopic() {
        if (!validateSujetInputs()) {
            return;
        }
        String titre = titreField.getText().trim();
        String contenu = contenuArea.getText().trim();
        if (editingSujet == null) {
            List<Sujet> similar = duplicateService.findSimilarTopics(titre, contenu);
            if (!similar.isEmpty()) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Sujet similaire");
                confirm.setHeaderText(null);
                confirm.setContentText("Sujet similaire trouvé. Voulez-vous continuer ?");
                if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                    return;
                }
            }
        }
        String imageFileName = resolveImageFileName();
        if (editingSujet == null) {
            Sujet s = new Sujet();
            s.setTitre(titre);
            s.setContenu(contenu);
            s.setImage(imageFileName);
            sujetDao.add(s);
            activityService.addActivity(currentUserId, null, null, "CREATE_TOPIC", "Nouveau sujet: " + titre);
            notificationService.createNotification(currentUserId, null, null, "Sujet créé: " + titre);
        } else {
            editingSujet.setTitre(titre);
            editingSujet.setContenu(contenu);
            editingSujet.setImage(imageFileName);
            sujetDao.update(editingSujet);
        }
        handleClearForm();
        refreshForum();
    }

    @FXML
    private void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select topic image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        File file = fileChooser.showOpenDialog((Stage) titreField.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            imageField.setText(file.getName());
        }
    }

    private void confirmDelete(Sujet s) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete topic");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete this topic and all its messages?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                sujetDao.delete(s.getId());
                if (editingSujet != null && editingSujet.getId() == s.getId()) {
                    handleClearForm();
                }
                refreshForum();
            }
        });
    }

    @FXML
    private void handleSearch() {
        String raw = searchField.getText();
        if (raw == null || raw.trim().isEmpty()) {
            displaySujets(allSujets);
            return;
        }
        String query = raw.toLowerCase();
        List<Sujet> filtered = sujetDao.advancedSearch(query, null, null, null, null);
        displaySujets(filtered);
    }

    private boolean validateSujetInputs() {
        clearAllErrors();
        String titre = titreField.getText() != null ? titreField.getText().trim() : "";
        String contenu = contenuArea.getText() != null ? contenuArea.getText().trim() : "";
        boolean valid = true;

        String titreErr = ForumInputRules.validateTopicTitle(titre, TITRE_MIN, TITRE_MAX);
        if (titreErr != null) {
            showTitreError(titreErr);
            valid = false;
        }

        String contenuErr = ForumInputRules.validateTopicContent(contenu, CONTENU_MIN, CONTENU_MAX);
        if (contenuErr != null) {
            showContenuError(contenuErr);
            valid = false;
        }
        return valid;
    }

    private void openMessages(Sujet s) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/MessageView.fxml"));
            Parent root = loader.load();
            MessageController controller = loader.getController();
            controller.setSujet(s);
            Stage stage = new Stage();
            stage.setTitle("Messages — " + s.getTitre());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearAllErrors() {
        clearTitreError();
        clearContenuError();
    }

    private void showTitreError(String msg) {
        ValidationHelper.setFieldError(titreField, true);
        setErrorLabel(titreErrorLabel, msg);
    }

    private void showContenuError(String msg) {
        ValidationHelper.setFieldError(contenuArea, true);
        setErrorLabel(contenuErrorLabel, msg);
    }

    private void clearTitreError() {
        ValidationHelper.clearFieldError(titreField);
        setErrorLabel(titreErrorLabel, null);
    }

    private void clearContenuError() {
        ValidationHelper.clearFieldError(contenuArea);
        setErrorLabel(contenuErrorLabel, null);
    }

    private void setErrorLabel(Label label, String msg) {
        if (label == null) return;
        boolean show = msg != null && !msg.isBlank();
        label.setText(show ? msg : "");
        label.setVisible(show);
        label.setManaged(show);
    }

    private String resolveImageFileName() {
        String fieldValue = imageField != null && imageField.getText() != null ? imageField.getText().trim() : "";
        if (selectedImageFile == null) {
            return fieldValue;
        }
        String fileName = selectedImageFile.getName();
        try {
            Path resourcesImages = Path.of("src/main/resources/images");
            Path targetImages = Path.of("target/classes/images");
            Files.createDirectories(resourcesImages);
            Files.createDirectories(targetImages);
            Files.copy(selectedImageFile.toPath(), resourcesImages.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(selectedImageFile.toPath(), targetImages.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Image");
            alert.setHeaderText(null);
            alert.setContentText("Could not copy image: " + e.getMessage());
            alert.showAndWait();
            return fieldValue;
        }
    }

    private Node createPreviewNode(String imageName) {
        if (imageName != null && !imageName.isBlank()) {
            File targetImage = Path.of("target/classes/images", imageName).toFile();
            File resourcesImage = Path.of("src/main/resources/images", imageName).toFile();
            File imageFile = targetImage.exists() ? targetImage : resourcesImage;
            if (imageFile.exists()) {
                try {
                    ImageView preview = new ImageView(new Image(new FileInputStream(imageFile)));
                    preview.setFitWidth(120);
                    preview.setFitHeight(80);
                    preview.setPreserveRatio(true);
                    return preview;
                } catch (Exception ignored) {
                    // Fallback to placeholder.
                }
            }
        }
        Region placeholder = new Region();
        placeholder.setPrefSize(120, 80);
        placeholder.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 8;");
        return placeholder;
    }

    private String buildSnippet(String content) {
        if (content == null || content.isBlank()) {
            return "No content.";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 140) {
            return normalized;
        }
        return normalized.substring(0, 137) + "...";
    }

    private String statusBadgeText(Sujet sujet) {
        if (sujet != null && sujet.isPinned()) return "📌 PINNED";
        String s = sujet == null || sujet.getStatus() == null ? "ACTIVE" : sujet.getStatus().toUpperCase(Locale.ROOT);
        if ("HOT".equals(s)) return "🔥 HOT";
        if ("INACTIVE".equals(s)) return "💤 INACTIVE";
        return "🟢 ACTIVE";
    }

    private String statusBadgeStyle(Sujet sujet) {
        if (sujet != null && sujet.isPinned()) {
            return "-fx-text-fill: #e3d5ff; -fx-font-weight: bold; -fx-background-color: rgba(108, 92, 231, 0.25); -fx-padding: 3 8; -fx-background-radius: 12;";
        }
        String s = sujet == null || sujet.getStatus() == null ? "ACTIVE" : sujet.getStatus().toUpperCase(Locale.ROOT);
        if ("HOT".equals(s)) {
            return "-fx-text-fill: #ffd166; -fx-font-weight: bold; -fx-background-color: rgba(255, 159, 67, 0.2); -fx-padding: 3 8; -fx-background-radius: 12;";
        }
        if ("INACTIVE".equals(s)) {
            return "-fx-text-fill: #bfc8d6; -fx-font-weight: bold; -fx-background-color: rgba(149, 165, 166, 0.22); -fx-padding: 3 8; -fx-background-radius: 12;";
        }
        return "-fx-text-fill: #8de3a7; -fx-font-weight: bold; -fx-background-color: rgba(46, 204, 113, 0.18); -fx-padding: 3 8; -fx-background-radius: 12;";
    }

    private String keywordBadgeText(String keywords) {
        if (keywords == null || keywords.isBlank()) return "Keywords: -";
        return "Keywords: " + keywords;
    }

    @FXML
    private void handleShowNotifications() {
        List<ForumNotification> notifications = notificationService.getUnreadNotifications(currentUserId);
        ListView<String> listView = new ListView<>();
        for (ForumNotification n : notifications) {
            listView.getItems().add("#" + n.getId() + " - " + n.getMessage());
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Notifications");
        dialog.setHeaderText("Notifications non lues");
        dialog.getDialogPane().setContent(listView);
        ButtonType markRead = new ButtonType("Mark as read", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(markRead, ButtonType.CLOSE);
        dialog.showAndWait().ifPresent(bt -> {
            if (bt == markRead) {
                for (ForumNotification n : notifications) notificationService.markAsRead(n.getId());
                refreshNotificationCount();
            }
        });
    }

    private void refreshNotificationCount() {
        if (notifCountLabel != null) {
            notifCountLabel.setText(String.valueOf(notificationService.countUnread(currentUserId)));
        }
    }

    private void refreshActivityFeed() {
        if (activityListView == null) return;
        activityListView.getItems().clear();
        activityService.getRecentActivities(8).forEach(a -> activityListView.getItems().add(a.getDescription()));
    }

    private void refreshFavorites() {
        if (favoritesListView == null) return;
        favoritesListView.getItems().clear();
        favoriteService.getUserFavorites(currentUserId)
                .forEach(s -> favoritesListView.getItems().add("#" + s.getId() + " - " + s.getTitre()));
    }

    private void showQrCodeForSujet(Sujet sujet) {
        String title = sujet.getTitre() != null ? sujet.getTitre().trim() : "";
        if (title.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("QR Code");
            alert.setHeaderText(null);
            alert.setContentText("Topic title is empty.");
            alert.showAndWait();
            return;
        }

        String googleLink = "https://www.google.com/search?q=" + encode(title + " game");

        try {
            ImageView qrView = new ImageView(generateQrImage(googleLink, 320, 320));
            qrView.setFitWidth(300);
            qrView.setFitHeight(300);
            qrView.setPreserveRatio(true);

            Hyperlink link = new Hyperlink(googleLink);
            link.setOnAction(event -> {
                try {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(googleLink));
                } catch (Exception ex) {
                    Alert err = new Alert(Alert.AlertType.WARNING);
                    err.setTitle("Open link");
                    err.setHeaderText(null);
                    err.setContentText("Could not open browser: " + ex.getMessage());
                    err.showAndWait();
                }
            });

            VBox box = new VBox(12, new Label("QR for: " + title), qrView, link);
            box.setStyle("-fx-padding: 14; -fx-background-color: #1f2430;");
            ((Label) box.getChildren().get(0)).setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

            Stage popup = new Stage();
            popup.initOwner((Stage) forumContainer.getScene().getWindow());
            popup.initModality(Modality.WINDOW_MODAL);
            popup.setTitle("QR Code");
            popup.setScene(new Scene(new BorderPane(box)));
            popup.show();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("QR Code");
            alert.setHeaderText(null);
            alert.setContentText("Could not generate QR code: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private Image generateQrImage(String data, int width, int height) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = new MultiFormatWriter().encode(
                data,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
        );

        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                writer.setColor(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return image;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
