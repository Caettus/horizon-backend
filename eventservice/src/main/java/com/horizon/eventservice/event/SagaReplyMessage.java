package com.horizon.eventservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SagaReplyMessage {
    private UUID sagaId;
    private boolean success;
    private String sourceService;
    private String failureReason;
} 