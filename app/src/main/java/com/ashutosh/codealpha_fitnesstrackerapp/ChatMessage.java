package com.ashutosh.codealpha_fitnesstrackerapp;

public class ChatMessage {

    public static final int TYPE_USER = 1;
    public static final int TYPE_AI = 2;

    private final String message;
    private final int messageType;

    public ChatMessage(String message, int messageType) {
        this.message = message;
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    }

    public int getMessageType() {
        return messageType;
    }
}