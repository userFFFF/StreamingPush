package com.user.streamingpush;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Created by Shenqing on 2018/1/23.
 */

public class CustomDialog extends Dialog {
    public CustomDialog(@NonNull Context context) {
        super(context);
    }

    public CustomDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    public interface DialogOnKeyDownListner {
        void onKeyDownListener(int keyCode, KeyEvent event);
    }

    private DialogOnKeyDownListner dialogOnKeyDownListner;
    public void setDialogOnKeyDownListner(DialogOnKeyDownListner listener) {
        dialogOnKeyDownListner = listener;
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (dialogOnKeyDownListner != null) {
            dialogOnKeyDownListner.onKeyDownListener(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    public static class Builder {
        private Context context;
        public Builder(Context context) {
            this.context = context;
        }
        public CustomDialog create(View view, int style, int gravity) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final CustomDialog dialog = new CustomDialog(context, style);
            dialog.getWindow().setGravity(gravity);
            dialog.setContentView(view);
            return dialog;
        }
    }
}
