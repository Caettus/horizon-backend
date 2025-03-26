package com.horizon.eventservice.Interface;

import com.horizon.eventservice.model.Event;
import com.horizon.eventservice.DAL.EventDAL;
import com.horizon.eventservice.DTO.EventDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EventInterface {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EventDAL eventDAL;

    public EventDTO getEventById(int id) {
        Optional<Event> eventOptional = eventDAL.findById(id);

        if (eventOptional.isEmpty()) {
            return null;
        }

        Event event = eventOptional.get();

        EventDTO response = new EventDTO();
        response.setId(event.getId());
        response.setEventname(event.getEventname());
        response.setDate(event.getDate());
        response.setLocation(event.getLocation());

        return response;
    }
}
