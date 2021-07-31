package com.redhat.labs.lodestar.participants.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class ErrorMessage {
    
    String message;
}
