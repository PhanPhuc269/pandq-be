package pandq.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.NotificationTemplateDTO;
import pandq.application.port.repositories.NotificationTemplateRepository;
import pandq.application.port.repositories.UserRepository;
import pandq.domain.models.interaction.NotificationTemplate;
import pandq.domain.models.enums.NotificationType;
import pandq.domain.models.user.User;
import pandq.infrastructure.services.FcmService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    /**
     * Get all notification templates.
     */
    @Transactional(readOnly = true)
    public List<NotificationTemplateDTO.Response> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get templates by active status.
     */
    @Transactional(readOnly = true)
    public List<NotificationTemplateDTO.Response> getTemplatesByStatus(Boolean isActive) {
        return templateRepository.findByIsActive(isActive).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get template by ID.
     */
    @Transactional(readOnly = true)
    public NotificationTemplateDTO.Response getTemplateById(UUID id) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));
        return mapToResponse(template);
    }

    /**
     * Create a new notification template.
     */
    @Transactional
    public NotificationTemplateDTO.Response createTemplate(NotificationTemplateDTO.CreateRequest request) {
        NotificationTemplate template = NotificationTemplate.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .type(request.getType() != null ? request.getType() : NotificationType.SYSTEM)
                .targetUrl(request.getTargetUrl())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .scheduledAt(request.getScheduledAt())
                .targetAudience(request.getTargetAudience() != null ? request.getTargetAudience() : "ALL")
                .sendCount(0)
                .build();

        NotificationTemplate saved = templateRepository.save(template);
        log.info("Created notification template: {}", saved.getId());
        return mapToResponse(saved);
    }

    /**
     * Update an existing template.
     */
    @Transactional
    public NotificationTemplateDTO.Response updateTemplate(UUID id, NotificationTemplateDTO.UpdateRequest request) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));

        if (request.getTitle() != null) template.setTitle(request.getTitle());
        if (request.getBody() != null) template.setBody(request.getBody());
        if (request.getType() != null) template.setType(request.getType());
        if (request.getTargetUrl() != null) template.setTargetUrl(request.getTargetUrl());
        if (request.getIsActive() != null) template.setIsActive(request.getIsActive());
        if (request.getScheduledAt() != null) template.setScheduledAt(request.getScheduledAt());
        if (request.getTargetAudience() != null) template.setTargetAudience(request.getTargetAudience());

        NotificationTemplate saved = templateRepository.save(template);
        log.info("Updated notification template: {}", id);
        return mapToResponse(saved);
    }

    /**
     * Toggle active status of a template.
     */
    @Transactional
    public NotificationTemplateDTO.Response toggleActive(UUID id) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));

        template.setIsActive(!template.getIsActive());
        NotificationTemplate saved = templateRepository.save(template);
        log.info("Toggled template {} active status to: {}", id, saved.getIsActive());
        return mapToResponse(saved);
    }

    /**
     * Send a notification template to all users (broadcast).
     */
    @Transactional
    public NotificationTemplateDTO.Response sendToAllUsers(UUID templateId) {
        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        if (!template.getIsActive()) {
            throw new RuntimeException("Cannot send inactive template");
        }

        // Get all users based on target audience
        List<User> targetUsers = getTargetUsers(template.getTargetAudience());
        
        // Send notification to each user
        for (User user : targetUsers) {
            try {
                notificationService.createNotification(
                        user.getId(),
                        template.getType(),
                        template.getTitle(),
                        template.getBody(),
                        template.getTargetUrl()
                );
            } catch (Exception e) {
                log.error("Failed to send notification to user {}: {}", user.getId(), e.getMessage());
            }
        }

        // Update template stats
        template.setLastSentAt(LocalDateTime.now());
        template.setSendCount(template.getSendCount() + 1);
        NotificationTemplate saved = templateRepository.save(template);

        log.info("Sent notification template {} to {} users", templateId, targetUsers.size());
        return mapToResponse(saved);
    }

    /**
     * Send notification to FCM topic (for broadcast without saving to each user).
     */
    @Transactional
    public NotificationTemplateDTO.Response sendToTopic(UUID templateId, String topic) {
        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        if (!template.getIsActive()) {
            throw new RuntimeException("Cannot send inactive template");
        }

        // Send to FCM topic
        fcmService.sendToTopic(topic, template.getTitle(), template.getBody());

        // Update template stats
        template.setLastSentAt(LocalDateTime.now());
        template.setSendCount(template.getSendCount() + 1);
        NotificationTemplate saved = templateRepository.save(template);

        log.info("Sent notification template {} to topic: {}", templateId, topic);
        return mapToResponse(saved);
    }

    /**
     * Delete a template.
     */
    @Transactional
    public void deleteTemplate(UUID id) {
        templateRepository.deleteById(id);
        log.info("Deleted notification template: {}", id);
    }

    /**
     * Get target users based on audience type.
     */
    private List<User> getTargetUsers(String targetAudience) {
        // For now, return all users. Can be extended for VIP, NEW_USERS, etc.
        return userRepository.findAll();
    }

    private NotificationTemplateDTO.Response mapToResponse(NotificationTemplate template) {
        return NotificationTemplateDTO.Response.builder()
                .id(template.getId())
                .title(template.getTitle())
                .body(template.getBody())
                .type(template.getType())
                .targetUrl(template.getTargetUrl())
                .isActive(template.getIsActive())
                .scheduledAt(template.getScheduledAt())
                .targetAudience(template.getTargetAudience())
                .lastSentAt(template.getLastSentAt())
                .sendCount(template.getSendCount())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
