package com.example.modumessenger.Grid;

import android.content.Intent;

public class SettingGridItem {
    private String itemName;
    private int itemImage;
    private Intent intent;

    public SettingGridItem(Intent intent, String itemName, int itemImage) {
        this.intent = intent;
        this.itemName = itemName;
        this.itemImage = itemImage;
    }

    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setItemImage(int itemImage) { this.itemImage = itemImage; }
    public void setIntent(Intent intent) { this.intent = intent; }

    public String getItemName() { return this.itemName; }
    public int getItemImage() { return this.itemImage; }
    public Intent getIntent() { return this.intent; }
}
