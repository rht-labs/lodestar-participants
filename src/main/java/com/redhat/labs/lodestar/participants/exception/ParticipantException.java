package com.redhat.labs.lodestar.participants.exception;

public class ParticipantException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    public ParticipantException(String message, Exception ex) {
        super(message, ex);
    }

}
