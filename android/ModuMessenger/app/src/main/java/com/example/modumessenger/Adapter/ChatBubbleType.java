package com.example.modumessenger.Adapter;

public enum ChatBubbleType {

    RIGHT_TEXT_SINGLE(1),
    RIGHT_TEXT_HEADER(2),
    RIGHT_TEXT_TAIL(3),
    RIGHT_IMAGE_SINGLE(4),
    RIGHT_IMAGE_HEADER(5),
    RIGHT_IMAGE_TAIL(6),

    LEFT_TEXT_SINGLE(7),
    LEFT_TEXT_HEADER(8),
    LEFT_TEXT_BODY(9),
    LEFT_TEXT_TAIL(10),
    LEFT_IMAGE_SINGLE(11),
    LEFT_IMAGE_HEADER(12),
    LEFT_IMAGE_BODY(13),
    LEFT_IMAGE_TAIL(14)
    ;

    private final int type;

    ChatBubbleType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
