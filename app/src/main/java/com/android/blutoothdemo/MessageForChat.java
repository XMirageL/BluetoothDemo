package com.android.blutoothdemo;

public class MessageForChat {
    private boolean isAuthor;//是不是自己发送的
    private String content;//内容

    public MessageForChat(boolean isAuthor, String content) {
        this.isAuthor = isAuthor;
        this.content = content;
    }

    public boolean isAuthor() {
        return isAuthor;
    }

    public String getContent() {
        return content;
    }
}
