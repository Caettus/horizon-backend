package com.horizon.eventservice.DAL;

import com.horizon.eventservice.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventDAL extends JpaRepository<Event, UUID> {
    void deleteAllByOrganizerId(String organizerId);

    @Query("SELECT e FROM Event e JOIN e.attendees a WHERE a = :userId")
    List<Event> findByAttendeesContains(@Param("userId") String userId);

    @Query("SELECT e FROM Event e JOIN e.waitlist w WHERE w = :userId")
    List<Event> findByWaitlistContains(@Param("userId") String userId);

    @Query("SELECT e FROM Event e JOIN e.allowedUsers au WHERE au = :userId")
    List<Event> findByAllowedUsersContains(@Param("userId") String userId);
}