package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.NotificationDTO;
import pandq.application.services.NotificationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationDTO.Response>> getNotificationsByUserId(@PathVariable UUID userId, 
                                                                                   @RequestParam(required = false) pandq.domain.models.enums.NotificationType type) {
        // Need to fix type reference. Using FQDN or Import. The file uses imports.
        return ResponseEntity.ok(notificationService.getNotificationsByUserId(userId, type));
    }

    @GetMapping("/by-email")
    public ResponseEntity<List<NotificationDTO.Response>> getNotificationsByEmail(@RequestParam String email) {
        return ResponseEntity.ok(notificationService.getNotificationsByEmail(email));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/preferences/{userId}")
    public ResponseEntity<NotificationDTO.PreferenceResponse> getPreferences(@PathVariable UUID userId) {
        return ResponseEntity.ok(notificationService.getPreferences(userId));
    }

    @PutMapping("/preferences/{userId}")
    public ResponseEntity<Void> updatePreferences(@PathVariable UUID userId, @RequestBody NotificationDTO.PreferenceRequest request) {
        notificationService.updatePreferences(userId, request);
        return ResponseEntity.ok().build();
    }
}

