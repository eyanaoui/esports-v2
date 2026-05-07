package com.esports.services;

import com.esports.dao.ForumReportDAO;
import com.esports.dao.MessageDao;
import com.esports.models.ForumReport;

import java.util.List;

public class ForumReportService {
    private final ForumReportDAO reportDAO = new ForumReportDAO();
    private final MessageDao messageDao = new MessageDao();

    public void reportMessage(int userId, int sujetId, int messageId, String reason, String description) {
        reportDAO.createReport(userId, sujetId, messageId, reason, description);
        messageDao.incrementReportCount(messageId);
    }

    public void resolveReport(int reportId) {
        reportDAO.resolveReport(reportId);
    }

    public void rejectReport(int reportId) {
        reportDAO.rejectReport(reportId);
    }

    public List<ForumReport> getPendingReports() {
        return reportDAO.getPendingReports();
    }
}
