/**
 * Copyright 2018 Taucoin Core Developers.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.android.wallet.util;

import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import android.text.Html;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.widget.CommonDialog;
import io.taucoin.foundation.util.StringUtil;

public class DialogManager {

    public static void showSureDialog(FragmentActivity activity, int msg, int negButton, int posButton,
                                      DialogOnClickListener negListener, DialogOnClickListener posListener) {
        String message = activity.getText(msg).toString();
        String negativeButton = activity.getText(negButton).toString();
        String positiveButton = activity.getText(posButton).toString();
        showSureDialog(activity, message, negativeButton, positiveButton, negListener, posListener);
    }

    public static void showSureDialog(FragmentActivity activity, int msg, int negButton, int posButton, DialogOnClickListener posListener) {
        String message = activity.getText(msg).toString();
        String negativeButton = activity.getText(negButton).toString();
        String positiveButton = activity.getText(posButton).toString();
        showSureDialog(activity, message, negativeButton, positiveButton, null, posListener);
    }

    public static void showSureDialog(FragmentActivity activity, int msg, int posButton, DialogOnClickListener posListener) {
        String message = activity.getText(msg).toString();
        String positiveButton = activity.getText(posButton).toString();
        showSureDialog(activity, message, null, positiveButton, null, posListener);
    }

    private static void showSureDialog(FragmentActivity activity, String msg, String negButton, String posButton,
                                       DialogOnClickListener negListener, DialogOnClickListener posListener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setMessage(Html.fromHtml(msg))
                .setCancelable(false);
        if (StringUtil.isNotEmpty(negButton)) {
            builder.setNegativeButton(negButton, null);
        }
        if (StringUtil.isNotEmpty(posButton)) {
            builder.setPositiveButton(posButton, null);
        }

        AlertDialog mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setCancelable(false);
        mDialog.show();

        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (posListener != null) {
                posListener.onClick(v);
            }
            mDialog.cancel();
        });

        mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            if (negListener != null) {
                negListener.onClick(v);
            }
            mDialog.cancel();
        });
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
        mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
    }

    public interface DialogOnClickListener {
        void onClick(View view);
    }

    private static void showTipDialog(FragmentActivity activity, CharSequence title, CharSequence subheading, CharSequence msg) {
        View view = LinearLayout.inflate(activity, R.layout.dialog_tip_layout, null);
        ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.tvMsg.setText(msg);
        if(StringUtil.isNotEmpty(title)){
            viewHolder.tvTitle.setText(title);
        }else{
            viewHolder.tvTitle.setVisibility(View.GONE);
        }
        if(StringUtil.isNotEmpty(subheading)){
            viewHolder.tvSubheading.setText(subheading);
        }else {
            viewHolder.tvSubheading.setVisibility(View.GONE);
        }
        new CommonDialog.Builder(activity)
                .setContentView(view)
                .create().show();
    }

    public static void showTipDialog(FragmentActivity activity, int msgRes) {
        showTipDialog(activity, null, null, ResourcesUtil.getText(msgRes));
    }

    public static void showTipDialog(FragmentActivity activity, int subheadingRes, int msgRes) {
        showTipDialog(activity, null, ResourcesUtil.getText(subheadingRes),
                ResourcesUtil.getText(msgRes));
    }

    public static void showTipDialog(FragmentActivity activity, int titleRes, int subheadingRes, int msgRes) {
        showTipDialog(activity, ResourcesUtil.getText(titleRes), ResourcesUtil.getText(subheadingRes),
                ResourcesUtil.getText(msgRes));
    }

    static class ViewHolder {
        @BindView(R.id.tv_title)
        TextView tvTitle;
        @BindView(R.id.tv_subheading)
        TextView tvSubheading;
        @BindView(R.id.tv_msg)
        TextView tvMsg;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}