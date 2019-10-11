/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.android.wallet.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import io.taucoin.android.wallet.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.foundation.util.DimensionsUtil;
import io.taucoin.foundation.util.StringUtil;

public class InputDialog extends Dialog {

    public InputDialog(Context context) {
        super(context);
    }

    public InputDialog(Context context, int theme) {
        super(context, theme);
    }

    public static class Builder {
        private Context context;
        private String positiveButtonText;
        private String negativeButtonText;
        private boolean isCanCancel = true;
        private boolean isEnabledPositive = true;
        private boolean isEnabledNegative = true;
        private int btnWidth;
        private int inputType = -1;
        private int inputHint = -1;
        private OnClickListener positiveButtonClickListener;
        private OnClickListener negativeButtonClickListener;
        private InputDialogListener negativeListener;
        private InputDialogListener positiveListener;
        private View mContentView;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setPositiveButton(int positiveButtonText,
                                         OnClickListener listener) {
            this.positiveButtonText = (String) context
                    .getText(positiveButtonText);
            this.positiveButtonClickListener = listener;
            return this;
        }

        public Builder setPositiveButton(String positiveButtonText,
                                         OnClickListener listener) {
            this.positiveButtonText = positiveButtonText;
            this.positiveButtonClickListener = listener;
            return this;
        }

        public Builder setPositiveButton(int positiveButtonText, InputDialogListener listener) {
            this.positiveButtonText = (String) context
                    .getText(positiveButtonText);
            this.positiveListener = listener;
            return this;
        }

        public Builder setNegativeButton(int negativeButtonText, InputDialogListener listener) {
            this.negativeButtonText = (String) context
                    .getText(negativeButtonText);
            this.negativeListener  = listener;
            return this;
        }

        public Builder setButtonWidth(int dpWidth) {
            this.btnWidth = dpWidth;
            return this;
        }

        public Builder isEnabledPositive(boolean isEnabled) {
            this.isEnabledNegative = isEnabled;
            return this;
        }

        public Builder isEnabledNegative(boolean isEnabled) {
            this.isEnabledNegative = isEnabled;
            return this;
        }

        public Builder setCanceledOnTouchOutside(boolean cancel) {
            this.isCanCancel = cancel;
            return this;
        }

        public Builder setContentView(View view) {
            this.mContentView = view;
            return this;
        }

        public Builder setNegativeButton(int negativeButtonText,
                                         OnClickListener listener) {
            this.negativeButtonText = (String) context
                    .getText(negativeButtonText);
            this.negativeButtonClickListener = listener;
            return this;
        }

        public Builder setNegativeButton(String negativeButtonText,
                                         OnClickListener listener) {
            this.negativeButtonText = negativeButtonText;
            this.negativeButtonClickListener = listener;
            return this;
        }

        public Builder setInputType(int inputType) {
            this.inputType = inputType;
            return this;
        }

        public Builder setInputHint(int inputHint) {
            this.inputHint = inputHint;
            return this;
        }

        public InputDialog create() {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final InputDialog dialog = new InputDialog(context, R.style.CommonDialog);
            ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.dialog_input_layout, null);
            dialog.addContentView(layout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            if(mContentView != null){
                layout.addView(mContentView, 0);
            }
            resetDialogWidth(layout, dialog);
            ViewHolder viewHolder = new ViewHolder(layout);
            viewHolder.positiveButton.setText(positiveButtonText);
            if (StringUtil.isEmpty(positiveButtonText)) {
                viewHolder.positiveButton.setVisibility(View.GONE);
            }
            viewHolder.positiveButton.setEnabled(isEnabledPositive);
            viewHolder.negativeButton.setEnabled(isEnabledNegative);

            if(inputType != -1){
                viewHolder.etInput.setInputType(inputType);
            }

            if(inputHint != -1){
                viewHolder.etInput.setHint(inputHint);
            }

            if(!isEnabledPositive){
                viewHolder.positiveButton.setBackgroundResource(R.drawable.grey_rect_round_bg);
            }
            if(!isEnabledNegative){
                viewHolder.negativeButton.setBackgroundResource(R.drawable.grey_rect_round_bg);
            }

            viewHolder.negativeButton.setText(negativeButtonText);
            if (StringUtil.isEmpty(negativeButtonText)) {
                viewHolder.negativeButton.setVisibility(View.GONE);
            }

            if (positiveButtonClickListener != null) {
                viewHolder.positiveButton.setOnClickListener(v -> positiveButtonClickListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE));
            }
            if (negativeButtonClickListener != null) {
                viewHolder.negativeButton.setOnClickListener(v -> negativeButtonClickListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE));
            }

            if (positiveListener != null) {
                viewHolder.positiveButton.setOnClickListener(v -> {
                    String text = viewHolder.etInput.getText().toString().trim();
                    positiveListener.onClick(dialog, text);
                });
            }
            if (negativeListener != null) {
                viewHolder.negativeButton.setOnClickListener(v -> {
                    String text = viewHolder.etInput.getText().toString().trim();
                    negativeListener.onClick(dialog, text);
                });
            }

            if(btnWidth > 0){
                viewHolder.negativeButton.setWidth(DimensionsUtil.dip2px(context, btnWidth));
                viewHolder.positiveButton.setWidth(DimensionsUtil.dip2px(context, btnWidth));
            }
            dialog.setCanceledOnTouchOutside(isCanCancel);
            return dialog;
        }

        private void resetDialogWidth(ViewGroup layout, InputDialog dialog) {
            WindowManager windowManager = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            layout.setMinimumWidth((int) (display.getWidth() * 0.85));
        }

        class ViewHolder {
            @BindView(R.id.positiveButton)
            Button positiveButton;
            @BindView(R.id.negativeButton)
            Button negativeButton;
            @BindView(R.id.etInput)
            EditText etInput;

            ViewHolder(View view) {
                ButterKnife.bind(this, view);
            }
        }
    }

    public interface InputDialogListener{
        void onClick(Dialog dialog, String text);
    }
}

