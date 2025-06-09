package com.horizon.userservice.saga;

import com.horizon.userservice.DAL.UserDAL;
import com.horizon.userservice.Interface.UserService;
import com.horizon.userservice.configuration.UserDeletionRabbitMQConfig;
import com.horizon.userservice.event.UserDeletionConfirmedEvent;
import com.horizon.userservice.event.UserDeletionRequestedEvent;
import com.horizon.userservice.model.User;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserDeletionSaga {

    private final RabbitTemplate rabbitTemplate;
    private final Keycloak keycloak;
    private final UserDAL userDAL;
    private final ConcurrentHashMap<String, Set<String>> pendingSagas = new ConcurrentHashMap<>();
    private final Set<String> expectedConfirmations = Set.of("events", "rsvps");

    @Autowired
    public UserDeletionSaga(RabbitTemplate rabbitTemplate, Keycloak keycloak, UserDAL userDAL) {
        this.rabbitTemplate = rabbitTemplate;
        this.keycloak = keycloak;
        this.userDAL = userDAL;
    }

    public void startSaga(UserDeletionRequestedEvent event) {
        String keycloakId = event.getKeycloakId();
        // The saga starts here. We expect confirmations from other services.
        pendingSagas.put(keycloakId, Collections.synchronizedSet(new HashSet<>()));
        // Publish event to trigger deletion in other services
        rabbitTemplate.convertAndSend(UserDeletionRabbitMQConfig.EXCHANGE_NAME, "user.deletion.requested", event);
    }

    @RabbitListener(queues = UserDeletionRabbitMQConfig.USER_DELETION_REQUESTED_QUEUE)
    public void onUserDeletionRequested(UserDeletionRequestedEvent event) {
        startSaga(event);
    }

    @RabbitListener(queues = "user.deletion.events.confirmed")
    public void onEventsConfirmed(String keycloakId) {
        handleConfirmation(keycloakId, "events");
    }

    @RabbitListener(queues = "user.deletion.rsvps.confirmed")
    public void onRsvpsConfirmed(String keycloakId) {
        handleConfirmation(keycloakId, "rsvps");
    }

    private void handleConfirmation(String keycloakId, String service) {
        Set<String> confirmations = pendingSagas.get(keycloakId);
        if (confirmations != null) {
            confirmations.add(service);
            if (confirmations.containsAll(expectedConfirmations)) {
                
                try {
                    // Delete from Keycloak
                    RealmResource realmResource = keycloak.realm("horizon-realm");
                    UsersResource usersResource = realmResource.users();
                    usersResource.delete(keycloakId);
                    System.out.println("User " + keycloakId + " deleted from Keycloak.");

                    // Delete from user service DB
                    userDAL.findByKeycloakId(keycloakId).ifPresent(user -> {
                        userDAL.delete(user);
                        System.out.println("User " + keycloakId + " deleted from user service database.");
                    });


                    // Publish confirmation event
                    rabbitTemplate.convertAndSend(UserDeletionRabbitMQConfig.EXCHANGE_NAME, "user.deletion.confirmed", new UserDeletionConfirmedEvent(keycloakId));
                    System.out.println("Published UserDeletionConfirmedEvent for " + keycloakId);

                } catch (Exception e) {
                    System.err.println("Error during final user deletion for " + keycloakId + ": " + e.getMessage());
                    // Implement compensation logic here if needed
                } finally {
                    pendingSagas.remove(keycloakId);
                }
            }
        }
    }
} 