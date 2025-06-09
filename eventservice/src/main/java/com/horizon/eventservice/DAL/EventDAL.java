package com.horizon.eventservice.DAL;

import com.horizon.eventservice.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventDAL extends JpaRepository<Event, UUID> {

    void deleteByOrganizerId(String organizerId);

    @Query("SELECT e FROM Event e WHERE :userId MEMBER OF e.attendees OR :userId MEMBER OF e.waitlist OR :userId MEMBER OF e.allowedUsers")
    List<Event> findEventsWithUserAsParticipant(@Param("userId") String userId);
}