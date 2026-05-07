package com.esports.controllers.user;

import com.esports.dao.MessageDao;
import com.esports.models.Message;
import com.esports.models.MessageHistory;
import com.esports.models.ModerationResult;
import com.esports.models.Sujet;
import com.esports.services.ForumAdvancedService;
import com.esports.services.ForumAttachmentService;
import com.esports.services.ForumBestAnswerService;
import com.esports.services.ForumMessageHistoryService;
import com.esports.services.ForumModerationService;
import com.esports.services.ForumNotificationService;
import com.esports.services.ForumPaginationService;
import com.esports.services.ForumPdfExportService;
import com.esports.services.ForumRecommendationService;
import com.esports.services.ForumReportService;
import com.esports.services.ForumReputationService;
import com.esports.services.ForumVoteService;
import com.esports.services.ForumUserScoreService;
import com.esports.utils.ForumInputRules;
import com.esports.utils.ValidationHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class MessageController {
    private static final int MESSAGE_MIN = 3;
    private static final int MESSAGE_MAX = 2000;

    @FXML private Label sujetTitreLabel;
    @FXML private Label sujetStatusLabel;
    @FXML private Label summaryLabel;
    @FXML private Label pageLabel;
    @FXML private Label attachmentNameLabel;
    @FXML private ListView<String> similarTopicsListView;
    @FXML private Label composerHintLabel;
    @FXML private VBox editingPreviewBox;
    @FXML private Label editingOriginalLabel;
    @FXML private Label messageErrorLabel;
    @FXML private TextArea messageField;
    @FXML private VBox messagesContainer;
    @FXML private Button sendButton;
    @FXML private Button cancelEditButton;
    @FXML private Button loadMoreButton;

    private final MessageDao messageDao = new MessageDao();
    private final ForumModerationService moderationService = new ForumModerationService();
    private final ForumAdvancedService advancedService = new ForumAdvancedService();
    private final ForumPdfExportService pdfExportService = new ForumPdfExportService();
    private final ForumVoteService voteService = new ForumVoteService();
    private final ForumBestAnswerService bestAnswerService = new ForumBestAnswerService();
    private final ForumAttachmentService attachmentService = new ForumAttachmentService();
    private final ForumMessageHistoryService historyService = new ForumMessageHistoryService();
    private final ForumPaginationService paginationService = new ForumPaginationService();
    private final ForumNotificationService notificationService = new ForumNotificationService();
    private final ForumUserScoreService userScoreService = new ForumUserScoreService();
    private final ForumReputationService reputationService = new ForumReputationService();
    private final ForumRecommendationService recommendationService = new ForumRecommendationService();
    private final ForumReportService reportService = new ForumReportService();
    private Sujet currentSujet;
    private Message editingMessage;
    private File selectedAttachment;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;
    private final int currentUserId = 1;

    public void setSujet(Sujet s) {
        this.currentSujet = s;
        currentPage = 1;
        if (sujetTitreLabel != null) {
            sujetTitreLabel.setText(s.getTitre());
        }
        if (sujetStatusLabel != null) {
            sujetStatusLabel.setText("Status: " + safe(s.getStatus()));
        }
        if (summaryLabel != null) {
            summaryLabel.setText(safe(s.getAutoSummary()));
        }
        refreshMessages();
        refreshRecommendations();
    }

    @FXML
    public void initialize() {
        messageField.textProperty().addListener((o, a, b) -> clearMessageError());
    }

    private void refreshMessages() {
        if (currentSujet == null) {
            return;
        }
        List<Message> list = paginationService.getMessagesByPage(currentSujet.getId(), currentPage, PAGE_SIZE);
        messagesContainer.getChildren().clear();

        if (list.isEmpty()) {
            Label empty = new Label("No replies yet — be the first to post below.");
            empty.getStyleClass().add("forum-msg-empty");
            empty.setMaxWidth(Double.MAX_VALUE);
            messagesContainer.getChildren().add(empty);
            return;
        }

        Message best = bestAnswerService.getBestAnswer(currentSujet.getId());
        if (best != null) {
            Label bestLabel = new Label("✅ Best Answer");
            bestLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
            messagesContainer.getChildren().add(bestLabel);
        }

        for (Message m : list) {
            HBox card = new HBox(14);
            card.getStyleClass().add("forum-msg-card");
            card.setPadding(new Insets(4, 6, 4, 6));

            Label avatar = new Label(avatarLetter(m.getContenu()));
            avatar.getStyleClass().add("forum-msg-avatar");
            avatar.setMinSize(40, 40);

            VBox body = new VBox(8);
            HBox.setHgrow(body, Priority.ALWAYS);

            Label text = new Label(m.getContenu());
            text.getStyleClass().add("forum-msg-text");
            text.setWrapText(true);
            text.setMaxWidth(Double.MAX_VALUE);

            if (m.isBest()) {
                Label bestBadge = new Label("✅ Best Answer");
                bestBadge.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                body.getChildren().add(bestBadge);
            }

            if (m.getUpdatedAt() != null) {
                Label modified = new Label("Modifié le : " + m.getUpdatedAt());
                modified.getStyleClass().add("forum-msg-header-sub");
                body.getChildren().add(modified);
            }

            if (m.getFilePath() != null && !m.getFilePath().isBlank()) {
                Hyperlink fileLink = new Hyperlink("Open File");
                fileLink.setOnAction(e -> {
                    try {
                        java.awt.Desktop.getDesktop().open(new File(m.getFilePath()));
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.WARNING, "Impossible d'ouvrir le fichier: " + ex.getMessage()).showAndWait();
                    }
                });
                body.getChildren().add(fileLink);
            }

            HBox actions = new HBox(8);
            actions.getStyleClass().add("forum-msg-actions");
            actions.setPadding(new Insets(4, 0, 0, 0));

            Button editBtn = new Button("Edit");
            editBtn.getStyleClass().addAll("forum-msg-btn", "forum-msg-btn-edit");
            editBtn.setOnAction(e -> startEdit(m));

            Button delBtn = new Button("Delete");
            delBtn.getStyleClass().addAll("forum-msg-btn", "forum-msg-btn-del");
            delBtn.setOnAction(e -> confirmDelete(m));

            Button likeBtn = new Button("👍 " + m.getLikes());
            likeBtn.getStyleClass().addAll("forum-msg-btn", "forum-msg-btn-edit");
            likeBtn.setOnAction(e -> {
                voteService.likeMessage(m.getId(), currentUserId);
                if (m.getUserId() != null && m.getUserId() > 0) reputationService.onLikeReceived(m.getUserId());
                refreshMessages();
            });

            Button dislikeBtn = new Button("👎 " + m.getDislikes());
            dislikeBtn.getStyleClass().addAll("forum-msg-btn", "forum-msg-btn-del");
            dislikeBtn.setOnAction(e -> {
                voteService.dislikeMessage(m.getId(), currentUserId);
                refreshMessages();
            });

            Button bestBtn = new Button("Mark as Best Answer");
            bestBtn.getStyleClass().addAll("forum-msg-btn", "forum-msg-btn-edit");
            bestBtn.setOnAction(e -> {
                bestAnswerService.markAsBestAnswer(currentSujet.getId(), m.getId());
                notificationService.createNotification(currentUserId, currentSujet.getId(), m.getId(), "Un message a été marqué Best Answer.");
                if (m.getUserId() != null && m.getUserId() > 0) reputationService.onBestAnswer(m.getUserId());
                refreshMessages();
            });

            Button historyBtn = new Button("Voir historique");
            historyBtn.getStyleClass().addAll("forum-msg-btn", "forum-msg-btn-edit");
            historyBtn.setOnAction(e -> showHistoryPopup(m.getId()));

            Button reportBtn = new Button("Report");
            reportBtn.getStyleClass().addAll("forum-msg-btn", "forum-msg-btn-del");
            reportBtn.setOnAction(e -> handleReportMessage(m));

            actions.getChildren().addAll(editBtn, delBtn, likeBtn, dislikeBtn, bestBtn, historyBtn, reportBtn);
            body.getChildren().addAll(text, actions);
            card.getChildren().addAll(avatar, body);
            messagesContainer.getChildren().add(card);
        }

        int total = paginationService.countMessages(currentSujet.getId());
        int maxPage = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        if (pageLabel != null) pageLabel.setText("Page " + currentPage + " / " + maxPage);
        if (loadMoreButton != null) loadMoreButton.setVisible(currentPage < maxPage);
    }

    private static String avatarLetter(String contenu) {
        if (contenu == null || contenu.isBlank()) {
            return "?";
        }
        String t = contenu.trim();
        int cp = t.codePointAt(0);
        return new String(Character.toChars(cp)).toUpperCase();
    }

    private void startEdit(Message m) {
        editingMessage = m;
        String text = m.getContenu() != null ? m.getContenu() : "";
        if (editingOriginalLabel != null) {
            editingOriginalLabel.setText(text.isEmpty() ? "(empty)" : text);
        }
        if (editingPreviewBox != null) {
            editingPreviewBox.setVisible(true);
            editingPreviewBox.setManaged(true);
        }
        if (composerHintLabel != null) {
            composerHintLabel.setText("Modify your message below");
        }
        messageField.setText(text);
        if (sendButton != null) {
            sendButton.setText("Save changes");
        }
        if (cancelEditButton != null) {
            cancelEditButton.setVisible(true);
            cancelEditButton.setManaged(true);
        }
        Platform.runLater(() -> {
            messageField.requestFocus();
            messageField.positionCaret(text.length());
        });
    }

    @FXML
    private void handleCancelEdit() {
        editingMessage = null;
        messageField.clear();
        clearMessageError();
        if (editingPreviewBox != null) {
            editingPreviewBox.setVisible(false);
            editingPreviewBox.setManaged(false);
        }
        if (composerHintLabel != null) {
            composerHintLabel.setText("Your reply");
        }
        if (sendButton != null) {
            sendButton.setText("Post reply");
        }
        if (cancelEditButton != null) {
            cancelEditButton.setVisible(false);
            cancelEditButton.setManaged(false);
        }
    }

    @FXML
    private void handleSend() {
        if (currentSujet == null) {
            return;
        }
        if (!validateMessageInput()) {
            return;
        }
        String text = messageField.getText().trim();
        if (editingMessage == null) {
            ModerationResult moderation = moderationService.moderateMessage(text, currentSujet.getId());
            Message m = new Message();
            m.setSujetId(currentSujet.getId());
            m.setContenu(text);
            m.setStatus(moderation.getStatus());
            m.setSpamScore(moderation.getSpamScore());
            m.setModerationReason(moderation.getReason());
            if (selectedAttachment != null) {
                try {
                    m.setFilePath(attachmentService.copyFileToUploadFolder(selectedAttachment));
                } catch (Exception e) {
                    new Alert(Alert.AlertType.WARNING, "Pièce jointe ignorée: " + e.getMessage()).showAndWait();
                }
            }
            messageDao.addWithModeration(m);
            advancedService.updateTopicActivity(currentSujet.getId());
            notificationService.createNotification(currentUserId, currentSujet.getId(), null, "Nouvelle réponse sur le sujet: " + currentSujet.getTitre());

            if ("REJECTED".equalsIgnoreCase(moderation.getStatus())) {
                userScoreService.addScore(currentUserId, -2);
                reputationService.onMessageRejected(currentUserId);
                new Alert(Alert.AlertType.WARNING, moderation.getReason() == null ? "Message rejeté." : moderation.getReason()).showAndWait();
            } else if ("PENDING".equalsIgnoreCase(moderation.getStatus())) {
                new Alert(Alert.AlertType.INFORMATION, "Message en attente de validation admin.").showAndWait();
            } else {
                userScoreService.incrementMessageCount(currentUserId);
                userScoreService.addScore(currentUserId, 5);
                reputationService.onMessageAccepted(currentUserId);
            }
        } else {
            historyService.updateMessageWithHistory(editingMessage.getId(), text);
            handleCancelEdit();
        }
        if (editingMessage == null) {
            messageField.clear();
        }
        selectedAttachment = null;
        if (attachmentNameLabel != null) attachmentNameLabel.setText("No file selected");
        refreshMessages();
    }

    @FXML
    private void handleLoadMore() {
        currentPage++;
        refreshMessages();
    }

    @FXML
    private void handleAttachFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Attach File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Allowed", "*.png", "*.jpg", "*.jpeg", "*.pdf", "*.txt")
        );
        File file = chooser.showOpenDialog(messageField.getScene().getWindow());
        if (file == null) return;
        try {
            attachmentService.validateAttachment(file);
            selectedAttachment = file;
            if (attachmentNameLabel != null) attachmentNameLabel.setText(file.getName());
        } catch (Exception e) {
            new Alert(Alert.AlertType.WARNING, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleGenerateSummary() {
        if (currentSujet == null) return;
        try {
            String summary = advancedService.generateSummary(currentSujet.getId());
            if (summaryLabel != null) {
                summaryLabel.setText(summary);
            }
            TextArea summaryArea = new TextArea(summary);
            summaryArea.setWrapText(true);
            summaryArea.setEditable(false);
            summaryArea.setPrefRowCount(8);
            summaryArea.setPrefColumnCount(40);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Résumé automatique");
            dialog.setHeaderText("Résumé du sujet: " + safe(currentSujet.getTitre()));
            dialog.getDialogPane().setContent(summaryArea);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.WARNING, "Erreur génération résumé: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleExportPdf() {
        if (currentSujet == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export topic to PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        chooser.setInitialFileName("forum_topic_" + currentSujet.getId() + ".pdf");
        File file = chooser.showSaveDialog(messageField.getScene().getWindow());
        if (file == null) return;
        try {
            pdfExportService.exportTopicToPdf(currentSujet.getId(), file);
            new Alert(Alert.AlertType.INFORMATION, "PDF exporté: " + file.getAbsolutePath()).showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.WARNING, "Erreur export PDF: " + e.getMessage()).showAndWait();
        }
    }

    private void confirmDelete(Message m) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete message");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete this message?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                messageDao.delete(m.getId());
                if (editingMessage != null && editingMessage.getId() == m.getId()) {
                    handleCancelEdit();
                }
                refreshMessages();
            }
        });
    }

    private boolean validateMessageInput() {
        clearMessageError();
        String text = messageField.getText() != null ? messageField.getText().trim() : "";
        String err = ForumInputRules.validateReply(text, MESSAGE_MIN, MESSAGE_MAX);
        if (err != null) {
            showMessageError(err);
            return false;
        }
        return true;
    }

    private void showMessageError(String message) {
        ValidationHelper.setFieldError(messageField, true);
        if (messageErrorLabel != null) {
            messageErrorLabel.setText(message);
            messageErrorLabel.setVisible(true);
            messageErrorLabel.setManaged(true);
        }
    }

    private void clearMessageError() {
        ValidationHelper.clearFieldError(messageField);
        if (messageErrorLabel != null) {
            messageErrorLabel.setText("");
            messageErrorLabel.setVisible(false);
            messageErrorLabel.setManaged(false);
        }
    }

    private void showHistoryPopup(int messageId) {
        List<MessageHistory> history = historyService.getHistoryByMessage(messageId);
        ListView<String> list = new ListView<>();
        if (history.isEmpty()) {
            list.getItems().add("Aucun historique.");
        } else {
            for (MessageHistory h : history) {
                list.getItems().add("[" + h.getDateModif() + "] " + safe(h.getOldContent()) + " -> " + safe(h.getNewContent()));
            }
        }
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Historique message");
        dialog.setHeaderText("Modifications du message #" + messageId);
        dialog.getDialogPane().setContent(list);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void handleReportMessage(Message message) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Report message");
        dialog.setHeaderText("Choisir une raison");
        ComboBox<String> reasonBox = new ComboBox<>();
        reasonBox.getItems().setAll("SPAM", "INSULTE", "HORS_SUJET", "HARCELEMENT", "AUTRE");
        reasonBox.getSelectionModel().selectFirst();
        TextArea description = new TextArea();
        description.setPromptText("Description optionnelle");
        VBox box = new VBox(8, reasonBox, description);
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                reportService.reportMessage(currentUserId, currentSujet == null ? 0 : currentSujet.getId(), message.getId(), reasonBox.getValue(), description.getText());
                new Alert(Alert.AlertType.INFORMATION, "Message signalé.").showAndWait();
                refreshMessages();
            }
        });
    }

    private void refreshRecommendations() {
        if (similarTopicsListView == null || currentSujet == null) return;
        similarTopicsListView.getItems().clear();
        recommendationService.recommendSimilarTopics(currentSujet.getId(), 3)
                .forEach(s -> similarTopicsListView.getItems().add("#" + s.getId() + " - " + s.getTitre()));
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}
