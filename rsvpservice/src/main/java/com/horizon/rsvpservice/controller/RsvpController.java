package com.horizon.rsvpservice.controller;

import com.horizon.rsvpservice.model.Rsvp;
import com.horizon.rsvpservice.model.RsvpStatus;
import com.horizon.rsvpservice.service.RsvpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/rsvps")
@RequiredArgsConstructor
public class RsvpController {
    private final RsvpService rsvpService;

    @PostMapping
    public ResponseEntity<Rsvp> createRsvp(
            @RequestParam UUID eventId,
            @RequestParam RsvpStatus status,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        String displayName = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(rsvpService.createRsvp(eventId, userId, status, displayName));
    }

    @PutMapping("/{rsvpId}")
    public ResponseEntity<Rsvp> updateRsvp(
            @PathVariable Long rsvpId,
            @RequestParam RsvpStatus status) {
        return ResponseEntity.ok(rsvpService.updateRsvp(rsvpId, status));
    }

    @DeleteMapping("/{rsvpId}")
    public ResponseEntity<Void> deleteRsvp(@PathVariable Long rsvpId) {
        rsvpService.deleteRsvp(rsvpId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{rsvpId}")
    public ResponseEntity<Rsvp> getRsvp(@PathVariable Long rsvpId) {
        return ResponseEntity.ok(rsvpService.getRsvp(rsvpId));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<Rsvp>> getRsvpsByEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(rsvpService.getRsvpsByEvent(eventId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Rsvp>> getRsvpsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(rsvpService.getRsvpsByUser(userId));
    }

    @GetMapping("/event/{eventId}/user/{userId}")
    public ResponseEntity<Rsvp> getRsvpByEventAndUser(
            @PathVariable UUID eventId,
            @PathVariable String userId) {
        return ResponseEntity.ok(rsvpService.getRsvpByEventAndUser(eventId, userId));
    }

    @GetMapping("/event/{eventId}/status/{status}")
    public ResponseEntity<List<Rsvp>> getRsvpsByEventAndStatus(
            @PathVariable UUID eventId,
            @PathVariable RsvpStatus status) {
        return ResponseEntity.ok(rsvpService.getRsvpsByEventAndStatus(eventId, status));
    }

    @GetMapping("/event/{eventId}/status/{status}/count")
    public ResponseEntity<Long> countRsvpsByEventAndStatus(
            @PathVariable UUID eventId,
            @PathVariable RsvpStatus status) {
        return ResponseEntity.ok(rsvpService.countRsvpsByEventAndStatus(eventId, status));
    }
} 