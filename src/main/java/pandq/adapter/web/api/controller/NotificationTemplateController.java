package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.NotificationTemplateDTO;
import pandq.application.services.NotificationTemplateService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/notification-templates")
@RequiredArgsConstructor
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;

    /**
     * Get all notification templates.
     */
    @GetMapping
    public ResponseEntity<List<NotificationTemplateDTO.Response>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    /**
     * Get templates by active status.
     */
    @GetMapping("/status")
    public ResponseEntity<List<NotificationTemplateDTO.Response>> getByStatus(
            @RequestParam Boolean isActive) {
        return ResponseEntity.ok(templateService.getTemplatesByStatus(isActive));
    }

    /**
     * Get template by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationTemplateDTO.Response> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(templateService.getTemplateById(id));
    }

    /**
     * Create a new notification template.
     */
    @PostMapping
    public ResponseEntity<NotificationTemplateDTO.Response> create(
            @RequestBody NotificationTemplateDTO.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(templateService.createTemplate(request));
    }

    /**
     * Update an existing template.
     */
    @PutMapping("/{id}")
    public ResponseEntity<NotificationTemplateDTO.Response> update(
            @PathVariable UUID id,
            @RequestBody NotificationTemplateDTO.UpdateRequest request) {
        return ResponseEntity.ok(templateService.updateTemplate(id, request));
    }

    /**
     * Toggle active status.
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<NotificationTemplateDTO.Response> toggleActive(@PathVariable UUID id) {
        return ResponseEntity.ok(templateService.toggleActive(id));
    }

    /**
     * Send notification to all users.
     */
    @PostMapping("/{id}/send")
    public ResponseEntity<NotificationTemplateDTO.Response> sendToAllUsers(@PathVariable UUID id) {
        return ResponseEntity.ok(templateService.sendToAllUsers(id));
    }

    /**
     * Send notification to FCM topic.
     */
    @PostMapping("/{id}/send-topic")
    public ResponseEntity<NotificationTemplateDTO.Response> sendToTopic(
            @PathVariable UUID id,
            @RequestParam String topic) {
        return ResponseEntity.ok(templateService.sendToTopic(id, topic));
    }

    /**
     * Delete a template.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
