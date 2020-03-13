package io.taucoin.android.wallet.util;

import android.view.View;

import com.lwy.righttopmenu.MenuItem;

import java.util.List;

import androidx.fragment.app.FragmentActivity;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.widget.menu.RightTopMenu;

public class PopupMenuUtil {

    public static void showMenuItem(FragmentActivity activity, View view, List<MenuItem> menuItems,
                                    RightTopMenu.OnMenuItemClickListener onMenuItemClickListener){
        RightTopMenu mRightTopMenu = new RightTopMenu.Builder(activity)
                .dimBackground(true)                    // Background darkens, default is true
                .needAnimationStyle(false)               // Display animation, default is true
                .animationStyle(R.style.RTM_ANIM_STYLE) // default is R.style.RTM_ANIM_STYLE
                .menuItems(menuItems)
                .onMenuItemClickListener(onMenuItemClickListener)
                .build();
        mRightTopMenu.showAsDropDown(view);
    }
}