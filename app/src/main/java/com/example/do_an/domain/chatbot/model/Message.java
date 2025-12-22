package com.example.do_an.domain.chatbot.model;

public class Message {
    public static final int TYPE_USER = 1;
    public static final int TYPE_BOT = 2;

    public int type;
    public String content;

    public Message(int type, String content) {
        this.type = type;
        this.content = content;
    }
}