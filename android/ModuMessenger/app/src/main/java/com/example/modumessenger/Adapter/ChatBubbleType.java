package com.example.modumessenger.Adapter;

public enum ChatBubbleType {
    INVALID(-1),

    RIGHT_TEXT_SINGLE(1),
    RIGHT_IMAGE_SINGLE(2),

    RIGHT_TEXT_HEADER(3),
    RIGHT_IMAGE_HEADER(4),

    RIGHT_TEXT_TAIL(5),
    RIGHT_IMAGE_TAIL(6),

    LEFT_TEXT_SINGLE(7),
    LEFT_IMAGE_SINGLE(8),

    LEFT_TEXT_HEADER(9),
    LEFT_IMAGE_HEADER(10),

    LEFT_TEXT_BODY(11),
    LEFT_IMAGE_BODY(12),

    LEFT_TEXT_TAIL(13),
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
