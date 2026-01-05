package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pandq.domain.models.interaction.AdminNotification;
import pandq.infrastructure.persistence.repositories.jpa.JpaAdminNotificationRepository;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final JpaAdminNotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<Page<AdminNotification>> getNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(notificationRepository.findAllByOrderByCreatedAtDesc(pageable));
    }
}
