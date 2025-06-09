package com.horizon.rsvpservice.repository;

import com.horizon.rsvpservice.model.Rsvp;
import com.horizon.rsvpservice.model.RsvpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RsvpRepository extends JpaRepository<Rsvp, Long> {
    List<Rsvp> findByEventId(UUID eventId);
    List<Rsvp> findByUserId(String userId);
    Optional<Rsvp> findByEventIdAndUserId(UUID eventId, String userId);
    List<Rsvp> findByEventIdAndStatus(UUID eventId, RsvpStatus status);
    long countByEventIdAndStatus(UUID eventId, RsvpStatus status);

    @Modifying
    @Query("UPDATE Rsvp r SET r.userDisplayName = :displayName WHERE r.userId = :userId")
    void updateUserDisplayName(String userId, String displayName);

    @Modifying
    @Query("DELETE FROM Rsvp r WHERE r.userId = :userId")
    void deleteByUserId(String userId);
} 