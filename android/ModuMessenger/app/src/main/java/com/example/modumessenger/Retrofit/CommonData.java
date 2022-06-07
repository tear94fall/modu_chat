package com.example.modumessenger.Retrofit;

import com.example.modumessenger.dto.ChatRoomDto;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CommonData {

    @SerializedName("key")
    private String key;
    @SerializedName("value")
    private String value;

    public String getKey() { return this.key; }
    public String getValue() { return this.value; }

    public void setKey(String key) { this.key = key; }
    public void setValue(String value) { this.value = value; }

    public CommonData(String key, String value) {
        setKey(key);
        setValue(value);
    }
}
