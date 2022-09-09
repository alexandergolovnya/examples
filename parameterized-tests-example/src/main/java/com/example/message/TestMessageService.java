package com.example.message;

import java.util.UUID;

public class TestMessageService implements MessageService {
    @Override
    public Message sendMessage(Message message, Channel channel) {
        // send a message based on the channel value, and return the message object with a generated id
        message.setId(UUID.randomUUID().toString());
        return message;
    }
}
