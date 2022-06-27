package com.example.modumessenger.Grid;

import android.content.Intent;

public class SendOthersGridItem {
    private String itemName;
    private int itemImage;

    public SendOthersGridItem(String itemName, int itemImage) {
        this.itemName = itemName;
        this.itemImage = itemImage;
    }

    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setItemImage(int itemImage) { this.itemImage = itemImage; }

    public String getItemName() { return this.itemName; }
    public int getItemImage() { return this.itemImage; }
}
