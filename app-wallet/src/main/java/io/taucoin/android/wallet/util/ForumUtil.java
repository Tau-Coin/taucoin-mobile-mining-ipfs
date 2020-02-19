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
package io.taucoin.android.wallet.util;

import android.widget.TextView;

import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.widget.ItemTextView;
import io.taucoin.foundation.util.StringUtil;

public class ForumUtil {

    // Switch forum browse mode
    public static void switchBrowseModel(TextView tvBrowse) {
        // Get previous forum browse mode
        boolean isNormalModel = isNormalModel();
        isNormalModel = !isNormalModel;
        // Save forum browse mode
        String browseModel = isNormalModel ? TransmitKey.ForumBrowseModel.NORMAL : TransmitKey.ForumBrowseModel.SAFE;
        SharedPreferencesHelper.getInstance().putString(TransmitKey.FORUM_BROWSE_MODEL, browseModel);
        // Show forum browse mode
        tvBrowse.setBackgroundResource(isNormalModel ? R.drawable.shape_oval_young : R.drawable.shape_oval_grey);
        int resModel = isNormalModel ? R.string.forum_normal : R.string.forum_safe;
        tvBrowse.setText(tvBrowse.getResources().getText(resModel));
    }

    // Switch forum browse mode
    public static void switchMiningModel(ItemTextView tvText) {
        // Get previous mining mode
        boolean isFastModel = isFastMiningModel();
        isFastModel = !isFastModel;
        // Save mining mode
        String miningModel = isFastModel ? TransmitKey.MiningModel.FAST : TransmitKey.MiningModel.FULL_NODE;
        SharedPreferencesHelper.getInstance().putString(TransmitKey.MINING_MODEL, miningModel);
        // Show mining mode
        int resModel = isFastModel ? R.string.setting_mode_fast : R.string.setting_mode_full_node;
        tvText.setLeftText(tvText.getResources().getText(resModel).toString());

        int resSwitchTip = isFastModel ? R.string.forum_switched_fast : R.string.forum_switched_full_node;
        ToastUtils.showShortToast(resSwitchTip);
    }

    public static boolean isNormalModel() {
        String browseModel = SharedPreferencesHelper.getInstance().getString(TransmitKey.FORUM_BROWSE_MODEL, TransmitKey.ForumBrowseModel.NORMAL);
        return StringUtil.isSame(browseModel, TransmitKey.ForumBrowseModel.NORMAL);
    }

    public static boolean isFastMiningModel() {
        String browseModel = SharedPreferencesHelper.getInstance().getString(TransmitKey.MINING_MODEL, TransmitKey.MiningModel.FAST);
        return StringUtil.isSame(browseModel, TransmitKey.MiningModel.FAST);
    }
}