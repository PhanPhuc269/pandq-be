package pandq.infrastructure.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pandq.application.services.NotificationTemplateService;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationTemplateService notificationTemplateService;

    /**
     * Run every minute to check for scheduled notifications.
     */
    @Scheduled(fixedRate = 60000)
    public void checkScheduledNotifications() {
        log.debug("Checking for scheduled notifications...");
        notificationTemplateService.processScheduledTemplates();
    }
}
