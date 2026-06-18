package com.bank.frauddetection.controller;

import com.bank.frauddetection.dto.notification.NotificationResponse;
import com.bank.frauddetection.service.NotificationService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list(Principal principal) {
        return notificationService.list(principal.getName());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Principal principal) {
        return Map.of("count", notificationService.unreadCount(principal.getName()));
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id, Principal principal) {
        return notificationService.markRead(id, principal.getName());
    }

    @PatchMapping("/read-all")
    public List<NotificationResponse> markAllRead(Principal principal) {
        return notificationService.markAllRead(principal.getName());
    }
}
