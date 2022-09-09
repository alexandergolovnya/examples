package com.example;

import com.example.message.Channel;
import com.example.message.Message;
import com.example.message.MessageService;
import com.example.message.TestMessageService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EnumSourceExampleParameterizedTest {

    private final MessageService messageService = new TestMessageService();

    @ParameterizedTest(name = "[{index}] Send a message through the {0} channel")
    @EnumSource(value = Channel.class, names = {"WHATSAPP", "SLACK"})
    void testSendMessage(Channel channel) {
        Message message = messageService.sendMessage(createMessage(channel), channel);
        assertNotNull(message);
        assertNotNull(message.getId());
        assertFalse(message.getId().isEmpty());
    }

    @ParameterizedTest(name = "[{index}] Send a message through the {0} channel")
    @EnumSource(value = Channel.class, names = {"SMS", "EMAIL"})
    void testSendMessageThroughEmailAndSmsChannels(Channel channel) {
        Message message = messageService.sendMessage(createMessage(channel), channel);
        assertNotNull(message);
        assertNotNull(message.getId());
        assertFalse(message.getId().isEmpty());
        // check other custom behavior
    }

    private Message createMessage(Channel channel) {
        // create a message based on the channel value
        return Message.builder()
                .message(String.format("Test %s message", channel))
                .build();
    }
}
