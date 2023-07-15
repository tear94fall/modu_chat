package com.example.modumessenger.Global;

import android.content.Context;
import android.util.TypedValue;

public class UiUtil {

    public static int DpToPx(Context context, float dp){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
