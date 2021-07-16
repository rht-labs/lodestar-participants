package com.redhat.labs.lodestar.participants.exception;

public class ParticipantExcpetion extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    public ParticipantExcpetion(String message, Exception ex) {
        super(message, ex);
    }

}
