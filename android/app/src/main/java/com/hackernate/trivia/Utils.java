package com.hackernate.trivia;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.function.BiFunction;

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
        t.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 50);
        t.show();
    }

    public static void showInputDialog(Context ctx, String title,
                                String positiveButtonText, String negativeButtonText,
                                BiFunction<String, Dialog, Boolean> callback) {

        final EditText input = new EditText(ctx);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTextSize(25);
        input.setPadding(25, 40, 25, 40);

        final AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setView(input)
                .setPositiveButton(positiveButtonText, null)
                .setNegativeButton(negativeButtonText, null)
                .create();

        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String name = input.getText().toString().trim();
                if (callback.apply(name, dialog)) {
                    dialog.dismiss();
                }
            });
        });
        dialog.show();
    }
}
