package com.esports.controllers.admin;

import com.esports.controllers.user.MessageController;
import com.esports.dao.SujetDao;
import com.esports.models.ForumActivity;
import com.esports.models.Message;
import com.esports.models.MessageHistory;
import com.esports.models.Sujet;
import com.esports.services.ForumAdminPdfReportService;
import com.esports.services.ForumActivityService;
import com.esports.services.ForumAdvancedService;
import com.esports.services.ForumArchiveService;
import com.esports.services.ForumBestAnswerService;
import com.esports.services.ForumKeywordService;
import com.esports.services.ForumMessageHistoryService;
import com.esports.services.ForumModerationService;
import com.esports.services.ForumPdfExportService;
import com.esports.services.ForumPinService;
import com.esports.services.ForumReportService;
import com.esports.services.ForumStatisticsService;
import com.esports.services.ForumVoteService;
import com.esports.services.ForumNotificationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.css.PseudoClass;

public class AdminForumController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> messageStatusFilterCombo;
    @FXML private ComboBox<String> topicStatusFilterCombo;
    @FXML private Label totalTopicsLabel;
    @FXML private Label notifCountLabel;
    @FXML private VBox topicsContainer;
    @FXML private Label mostRepliedLabel;
    @FXML private Label todayMessagesLabel;
    @FXML private Label pendingMessagesLabel;
    @FXML private Label rejectedMessagesLabel;
    @FXML private Label hotTopicsLabel;
    @FXML private ListView<String> topTrendingList;
    @FXML private ListView<String> topUsersList;
    @FXML private ListView<String> pendingReportsList;
    @FXML private ListView<String> activityListView;
    @FXML private PieChart statusPieChart;

    private final SujetDao sujetDao = new SujetDao();
    private final ForumModerationService moderationService = new ForumModerationService();
    private final ForumPinService pinService = new ForumPinService();
    private final ForumBestAnswerService bestAnswerService = new ForumBestAnswerService();
    private final ForumVoteService voteService = new ForumVoteService();
    private final ForumAdvancedService summaryService = new ForumAdvancedService();
    private final ForumKeywordService keywordService = new ForumKeywordService();
    private final ForumPdfExportService pdfExportService = new ForumPdfExportService();
    private final ForumMessageHistoryService historyService = new ForumMessageHistoryService();
    private final ForumActivityService activityService = new ForumActivityService();
    private final ForumStatisticsService statisticsService = new ForumStatisticsService();
    private final ForumReportService reportService = new ForumReportService();
    private final ForumArchiveService archiveService = new ForumArchiveService();
    private final ForumAdminPdfReportService adminPdfReportService = new ForumAdminPdfReportService();
    private final ForumNotificationService notificationService = new ForumNotificationService();
    private final int currentUserId = 1;

    private List<Sujet> currentTopics = new ArrayList<>();

    @FXML
    public void initialize() {
        messageStatusFilterCombo.getItems().setAll("ALL", "ACCEPTED", "PENDING", "REJECTED");
        messageStatusFilterCombo.getSelectionModel().selectFirst();
        topicStatusFilterCombo.getItems().setAll("ALL", "HOT", "ACTIVE", "INACTIVE", "ARCHIVED");
        topicStatusFilterCombo.getSelectionModel().selectFirst();

        searchField.textProperty().addListener((o, a, b) -> applyFilters());
        messageStatusFilterCombo.valueProperty().addListener((o, a, b) -> applyFilters());
        topicStatusFilterCombo.valueProperty().addListener((o, a, b) -> applyFilters());

        loadGroupedTopics();
        refreshNotificationCount();
    }

    @FXML
    private void handleRefresh() {
        loadGroupedTopics();
    }

    @FXML
    private void handleExportReportPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export topic report");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        chooser.setInitialFileName("forum_admin_report.pdf");
        File file = chooser.showSaveDialog(topicsContainer.getScene().getWindow());
        if (file == null) return;
        try {
            adminPdfReportService.exportForumAdminReport(file);
            new Alert(Alert.AlertType.INFORMATION, "Export terminé.").showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.WARNING, "Erreur export: " + e.getMessage()).showAndWait();
        }
    }

    private void loadGroupedTopics() {
        currentTopics = sujetDao.getAllTopicsWithMessagesGrouped();
        applyFilters();
        refreshDashboardStats();
        refreshNotificationCount();
    }

    @FXML
    private void handleShowAdminNotifications() {
        var notifications = notificationService.getRecentNotifications(currentUserId, 80);

        ListView<com.esports.models.ForumNotification> listView = new ListView<>();
        listView.getStyleClass().add("notif-list");
        listView.getItems().setAll(notifications);
        PseudoClass unreadPc = PseudoClass.getPseudoClass("unread");
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(com.esports.models.ForumNotification item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    pseudoClassStateChanged(unreadPc, false);
            return;
        }
                getStyleClass().add("notif-cell");
                pseudoClassStateChanged(unreadPc, !item.isRead());
                String when = item.getCreatedAt() == null ? "" : ("  •  " + item.getCreatedAt());
                setText(item.getMessage() + when);
            }
        });

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Notifications");
        dialog.setHeaderText("Toutes les notifications (comme Facebook)");
        dialog.getDialogPane().setContent(listView);
        dialog.getDialogPane().getStyleClass().add("notif-dialog");
        try {
            var css = getClass().getResource("/forum-admin.css");
            if (css != null) {
                dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {}

        ButtonType markSelected = new ButtonType("Lu (selected)", ButtonBar.ButtonData.OK_DONE);
        ButtonType markAll = new ButtonType("Mark all as read", ButtonBar.ButtonData.APPLY);
        dialog.getDialogPane().getButtonTypes().addAll(markSelected, markAll, ButtonType.CLOSE);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == markSelected) {
                var sel = listView.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    notificationService.markAsRead(sel.getId());
                }
            } else if (bt == markAll) {
                notificationService.markAllAsRead(currentUserId);
            }
            refreshNotificationCount();
        });
    }

    private void refreshNotificationCount() {
        if (notifCountLabel == null) return;
        notifCountLabel.setText(String.valueOf(notificationService.countUnread(currentUserId)));
        notifCountLabel.setVisible(notificationService.countUnread(currentUserId) > 0);
        notifCountLabel.setManaged(notificationService.countUnread(currentUserId) > 0);
    }

    private void applyFilters() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String msgStatus = messageStatusFilterCombo.getValue();
        String topicStatus = topicStatusFilterCombo.getValue();

        List<Sujet> filtered = currentTopics.stream().filter(topic -> {
            if (!"ALL".equalsIgnoreCase(topicStatus)) {
                String tStatus = topic.getStatus() == null ? "ACTIVE" : topic.getStatus();
                if (!tStatus.equalsIgnoreCase(topicStatus)) return false;
            }
            boolean topicMatch = keyword.isBlank()
                    || safe(topic.getTitre()).toLowerCase(Locale.ROOT).contains(keyword)
                    || safe(topic.getContenu()).toLowerCase(Locale.ROOT).contains(keyword);
            boolean messageMatch = topic.getMessages().stream().anyMatch(m -> safe(m.getContenu()).toLowerCase(Locale.ROOT).contains(keyword));
            if (!keyword.isBlank() && !(topicMatch || messageMatch)) return false;
            if (!"ALL".equalsIgnoreCase(msgStatus)) {
                return topic.getMessages().stream().anyMatch(m -> msgStatus.equalsIgnoreCase(safe(m.getStatus())));
            }
            return true;
        }).collect(Collectors.toList());

        renderTopics(filtered);
        totalTopicsLabel.setText(String.valueOf(filtered.size()));
    }

    private void renderTopics(List<Sujet> topics) {
        topicsContainer.getChildren().clear();
        for (Sujet topic : topics) {
            topicsContainer.getChildren().add(createTopicCard(topic));
        }
    }

    private VBox createTopicCard(Sujet topic) {
        VBox card = new VBox(8);
        card.getStyleClass().add("topic-card");

        HBox header = new HBox(8);
        header.getStyleClass().add("topic-header");
        Label title = new Label(safe(topic.getTitre()));
        title.getStyleClass().add("topic-title");
        Label statusBadge = badgeForTopicStatus(topic.getStatus());
        header.getChildren().addAll(title, statusBadge);
        if (topic.isPinned()) {
            Label pinned = new Label("📌 Pinned");
            pinned.getStyleClass().add("badge-pinned");
            header.getChildren().add(pinned);
        }
        header.getChildren().add(new Region());
        HBox.setHgrow(header.getChildren().get(header.getChildren().size() - 1), Priority.ALWAYS);

        Label meta = new Label("Messages: " + topic.getMessages().size() + " | Score: " + topic.getTrendingScore() + " | Keywords: " + safe(topic.getKeywords()));
        Label summary = new Label(safe(topic.getAutoSummary()));
        summary.setWrapText(true);

        HBox actions = new HBox(8);
        Button pin = new Button("Pin");
        pin.getStyleClass().add("action-button");
        pin.setOnAction(e -> handlePinTopic(topic));
        Button unpin = new Button("Unpin");
        unpin.getStyleClass().add("warning-button");
        unpin.setOnAction(e -> handleUnpinTopic(topic));
        Button genSummary = new Button("Generate Summary");
        genSummary.getStyleClass().add("action-button");
        genSummary.setOnAction(e -> handleGenerateSummary(topic));
        Button genKeywords = new Button("Generate Keywords");
        genKeywords.getStyleClass().add("action-button");
        genKeywords.setOnAction(e -> handleGenerateKeywords(topic));
        Button exportPdf = new Button("Export PDF");
        exportPdf.getStyleClass().add("success-button");
        exportPdf.setOnAction(e -> handleExportPdf(topic));
        Button archive = new Button("Archive");
        archive.getStyleClass().add("danger-button");
        archive.setOnAction(e -> {
            archiveService.archiveTopic(topic.getId(), "Archivage manuel admin");
            activityService.addActivity(currentUserId, topic.getId(), null, "ARCHIVE", "Topic archived");
            notificationService.createNotification(currentUserId, topic.getId(), null, "Sujet archivé: " + safe(topic.getTitre()));
            loadGroupedTopics();
        });
        Button restore = new Button("Restore");
        restore.getStyleClass().add("success-button");
        restore.setOnAction(e -> {
            archiveService.restoreTopic(topic.getId());
            activityService.addActivity(currentUserId, topic.getId(), null, "RESTORE", "Topic restored");
            notificationService.createNotification(currentUserId, topic.getId(), null, "Sujet restauré: " + safe(topic.getTitre()));
            loadGroupedTopics();
        });
        Button viewPublic = new Button("View Public");
        viewPublic.getStyleClass().add("action-button");
        viewPublic.setOnAction(e -> handleViewPublic(topic));
        Button toggle = new Button("Toggle Messages");
        toggle.getStyleClass().add("warning-button");
        actions.getChildren().addAll(pin, unpin, genSummary, genKeywords, exportPdf, archive, restore, viewPublic, toggle);

        VBox messagesBox = new VBox(6);
        messagesBox.setVisible(true);
        messagesBox.setManaged(true);
        String msgStatusFilter = messageStatusFilterCombo.getValue();
        for (Message message : topic.getMessages()) {
            if (!"ALL".equalsIgnoreCase(msgStatusFilter) && !msgStatusFilter.equalsIgnoreCase(safe(message.getStatus()))) continue;
            messagesBox.getChildren().add(createMessageCard(message, topic));
        }
        toggle.setOnAction(e -> {
            boolean show = !messagesBox.isVisible();
            messagesBox.setVisible(show);
            messagesBox.setManaged(show);
        });

        card.getChildren().addAll(header, meta, summary, actions, messagesBox);
        return card;
    }

    private VBox createMessageCard(Message message, Sujet topic) {
        VBox box = new VBox(6);
        box.getStyleClass().add("message-card");
        Label content = new Label(safe(message.getContenu()));
        content.setWrapText(true);
        Label status = new Label(safe(message.getStatus()));
        status.getStyleClass().add(cssByMessageStatus(message.getStatus()));
        HBox row = new HBox(8, status, new Label("👍 " + message.getLikes()), new Label("👎 " + message.getDislikes()));
        if (message.isBest()) {
            Label best = new Label("✅ Best Answer");
            best.getStyleClass().add("badge-best");
            row.getChildren().add(best);
        }
        if (message.getModerationReason() != null && !message.getModerationReason().isBlank()) {
            row.getChildren().add(new Label("Raison: " + message.getModerationReason()));
        }

        HBox actions = new HBox(8);
        Button approve = new Button("Approve");
        approve.getStyleClass().add("success-button");
        approve.setOnAction(e -> handleApproveMessage(message));
        Button reject = new Button("Reject");
        reject.getStyleClass().add("danger-button");
        reject.setOnAction(e -> handleRejectMessage(message));
        Button best = new Button("Mark Best Answer");
        best.getStyleClass().add("action-button");
        best.setOnAction(e -> handleBestAnswer(message, topic));
        Button history = new Button("View History");
        history.getStyleClass().add("action-button");
        history.setOnAction(e -> handleViewHistory(message));
        Button like = new Button("Like");
        like.getStyleClass().add("action-button");
        like.setOnAction(e -> { voteService.likeMessage(message.getId(), currentUserId); loadGroupedTopics(); });
        Button dislike = new Button("Dislike");
        dislike.getStyleClass().add("warning-button");
        dislike.setOnAction(e -> { voteService.dislikeMessage(message.getId(), currentUserId); loadGroupedTopics(); });
        actions.getChildren().addAll(approve, reject, best, history, like, dislike);

        if (message.getFilePath() != null && !message.getFilePath().isBlank()) {
            Hyperlink openFile = new Hyperlink("Open Attachment");
            openFile.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().open(new File(message.getFilePath()));
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.WARNING, "Cannot open file: " + ex.getMessage()).showAndWait();
                }
            });
            box.getChildren().add(openFile);
        }

        box.getChildren().addAll(content, row, actions);
        return box;
    }

    private void handleApproveMessage(Message message) {
        moderationService.approveMessage(message.getId());
        activityService.addActivity(currentUserId, message.getSujetId(), message.getId(), "APPROVE", "Message approved");
        notificationService.createNotification(currentUserId, message.getSujetId(), message.getId(), "Admin a approuvé un message (#" + message.getId() + ")");
        loadGroupedTopics();
    }

    private void handleRejectMessage(Message message) {
        TextInputDialog input = new TextInputDialog();
        input.setTitle("Reject message");
        input.setHeaderText("Reason");
        String reason = input.showAndWait().orElse("Rejected by admin");
        moderationService.rejectMessage(message.getId(), reason);
        activityService.addActivity(currentUserId, message.getSujetId(), message.getId(), "REJECT", "Message rejected: " + reason);
        notificationService.createNotification(currentUserId, message.getSujetId(), message.getId(), "Admin a rejeté un message (#" + message.getId() + "): " + reason);
        loadGroupedTopics();
    }

    private void handlePinTopic(Sujet topic) {
        pinService.pinTopic(topic.getId());
        activityService.addActivity(currentUserId, topic.getId(), null, "PIN", "Topic pinned: " + safe(topic.getTitre()));
        notificationService.createNotification(currentUserId, topic.getId(), null, "Admin a épinglé le sujet: " + safe(topic.getTitre()));
        loadGroupedTopics();
    }

    private void handleUnpinTopic(Sujet topic) {
        pinService.unpinTopic(topic.getId());
        activityService.addActivity(currentUserId, topic.getId(), null, "UNPIN", "Topic unpinned: " + safe(topic.getTitre()));
        notificationService.createNotification(currentUserId, topic.getId(), null, "Admin a désépinglé le sujet: " + safe(topic.getTitre()));
        loadGroupedTopics();
    }

    private void handleBestAnswer(Message message, Sujet topic) {
        bestAnswerService.markAsBestAnswer(topic.getId(), message.getId());
        activityService.addActivity(currentUserId, topic.getId(), message.getId(), "BEST_ANSWER", "Best answer selected");
        notificationService.createNotification(currentUserId, topic.getId(), message.getId(), "Best Answer choisi sur: " + safe(topic.getTitre()));
        loadGroupedTopics();
    }

    private void handleGenerateSummary(Sujet topic) {
        summaryService.generateSummary(topic.getId());
        activityService.addActivity(currentUserId, topic.getId(), null, "SUMMARY", "Summary generated");
        notificationService.createNotification(currentUserId, topic.getId(), null, "Summary généré pour: " + safe(topic.getTitre()));
        loadGroupedTopics();
    }

    private void handleGenerateKeywords(Sujet topic) {
        keywordService.generateKeywords(topic.getId());
        activityService.addActivity(currentUserId, topic.getId(), null, "KEYWORDS", "Keywords generated");
        notificationService.createNotification(currentUserId, topic.getId(), null, "Keywords générés pour: " + safe(topic.getTitre()));
        loadGroupedTopics();
    }

    private void handleExportPdf(Sujet topic) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export topic PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("forum_topic_" + topic.getId() + ".pdf");
        File f = chooser.showSaveDialog(topicsContainer.getScene().getWindow());
        if (f == null) return;
        try {
            pdfExportService.exportTopicToPdf(topic.getId(), f);
            activityService.addActivity(currentUserId, topic.getId(), null, "EXPORT_PDF", "PDF exported");
            notificationService.createNotification(currentUserId, topic.getId(), null, "PDF exporté pour: " + safe(topic.getTitre()));
        } catch (Exception e) {
            new Alert(Alert.AlertType.WARNING, "Export error: " + e.getMessage()).showAndWait();
        }
    }

    private void handleViewHistory(Message message) {
        List<MessageHistory> history = historyService.getHistoryByMessage(message.getId());
        ListView<String> list = new ListView<>();
        if (history.isEmpty()) list.getItems().add("Aucun historique.");
        for (MessageHistory h : history) {
            list.getItems().add("[" + h.getDateModif() + "] " + safe(h.getOldContent()) + " -> " + safe(h.getNewContent()));
        }
        Dialog<Void> d = new Dialog<>();
        d.setTitle("Message History");
        d.setHeaderText("Message #" + message.getId());
        d.getDialogPane().setContent(list);
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        d.showAndWait();
    }

    private void handleViewPublic(Sujet topic) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/MessageView.fxml"));
            Parent root = loader.load();
            MessageController controller = loader.getController();
            controller.setSujet(topic);
            Stage stage = new Stage();
            stage.setTitle("Public View - " + safe(topic.getTitre()));
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.WARNING, "Cannot open public view: " + e.getMessage()).showAndWait();
        }
    }

    private void refreshDashboardStats() {
        mostRepliedLabel.setText(statisticsService.getMostRepliedTopic());
        todayMessagesLabel.setText("Today: " + statisticsService.getTodayMessagesCount());
        pendingMessagesLabel.setText("Pending: " + statisticsService.getPendingMessagesCount());
        rejectedMessagesLabel.setText("Rejected: " + statisticsService.getRejectedMessagesCount());
        hotTopicsLabel.setText("Hot topics: " + statisticsService.getHotTopicsCount());

        topTrendingList.getItems().setAll(
                statisticsService.getTop5TrendingTopics().stream()
                        .map(s -> safe(s.getTitre()) + " (" + String.format(Locale.ROOT, "%.1f", s.getTrendingScore()) + ")")
                        .collect(Collectors.toList())
        );
        topUsersList.getItems().setAll(
                statisticsService.getTopForumUsers().stream()
                        .map(u -> "User #" + u.getUserId() + " | " + u.getLevel() + " | score " + u.getScore())
                        .collect(Collectors.toList())
        );
        pendingReportsList.getItems().setAll(
                reportService.getPendingReports().stream()
                        .map(r -> "#"+r.getId()+" msg#"+r.getMessageId()+" "+r.getReason())
                        .collect(Collectors.toList())
        );
        activityListView.getItems().setAll(
                activityService.getRecentActivities(10).stream().map(ForumActivity::getDescription).collect(Collectors.toList())
        );

        statusPieChart.getData().clear();
        for (Map.Entry<String, Integer> e : statisticsService.getMessagesByStatusStats().entrySet()) {
            statusPieChart.getData().add(new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue()));
        }
    }

    private Label badgeForTopicStatus(String status) {
        String s = safe(status).isBlank() ? "ACTIVE" : status.toUpperCase(Locale.ROOT);
        Label label = new Label(s);
        switch (s) {
            case "HOT" -> label.getStyleClass().add("badge-hot");
            case "INACTIVE" -> label.getStyleClass().add("badge-inactive");
            default -> label.getStyleClass().add("badge-active");
        }
        return label;
    }

    private String cssByMessageStatus(String status) {
        String s = safe(status).toUpperCase(Locale.ROOT);
        if ("PENDING".equals(s)) return "badge-pending";
        if ("REJECTED".equals(s)) return "badge-rejected";
        return "badge-accepted";
    }

    private String safe(String v) { return v == null ? "" : v; }
}
