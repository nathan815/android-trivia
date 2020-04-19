package com.hackernate.trivia;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class Utils {

    public static void showToast(Context ctx, String msg, int length, int position) {
        Toast t = Toast.makeText(ctx, msg, length);
        t.setGravity(position, -10, 0);
        t.show();
    }

    public static void showTopToast(Context ctx, String msg, int length) {
        Toast t = Toast.makeText(ctx, msg, length);
        t.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 50);
        t.show();
    }

    public static void showBottomToast(Context ctx, String msg, int length) {
        Toast t = Toast.makeText(ctx, msg, length);
        t.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, -50);
        t.show();
    }
}
