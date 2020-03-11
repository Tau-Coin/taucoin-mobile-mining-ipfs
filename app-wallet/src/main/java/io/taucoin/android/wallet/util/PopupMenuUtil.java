package io.taucoin.android.wallet.util;

import android.view.View;
import com.lwy.righttopmenu.MenuItem;
import com.lwy.righttopmenu.RightTopMenu;

import java.util.List;

import androidx.fragment.app.FragmentActivity;
import io.taucoin.android.wallet.R;

public class PopupMenuUtil {

    public static void showMenuItem(FragmentActivity activity, View view, List<MenuItem> menuItems,
                                    RightTopMenu.OnMenuItemClickListener onMenuItemClickListener){
        RightTopMenu mRightTopMenu = new RightTopMenu.Builder(activity)
//              .windowHeight(480)                      // When the number of menus is greater than 3, it is wrap ﹣ content. Otherwise, the default height is 320
//              .windowWidth(320)                       // Default width wrap [content
                .dimBackground(false)                    // Background darkens, default is true
                .needAnimationStyle(true)               // Display animation, default is true
                .animationStyle(R.style.RTM_ANIM_STYLE) // default is R.style.RTM_ANIM_STYLE
                .menuItems(menuItems)
                .onMenuItemClickListener(onMenuItemClickListener)
                .build();
        mRightTopMenu.showAsDropDown(view, 0, 0);
    }
}