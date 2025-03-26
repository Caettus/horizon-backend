package com.horizon.eventservice.DAL;

import com.horizon.eventservice.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventDAL extends JpaRepository<Event, Integer> {
}
