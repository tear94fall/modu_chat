package com.example.modumessenger.dto;

import com.google.gson.annotations.SerializedName;

public class NotificationDto {

    private String key;
    private String value;
    private boolean expanded;

    public String getKey() { return this.key; }
    public String getValue() { return this.value; }
    public boolean getExpanded() { return this.expanded; }

    public void setKey(String key) { this.key = key; }
    public void setValue(String value) { this.value = value; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }

    public NotificationDto(String key, String value) {
        setKey(key);
        setValue(value);
        setExpanded(false);
    }
}
