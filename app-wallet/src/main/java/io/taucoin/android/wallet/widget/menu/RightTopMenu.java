package io.taucoin.android.wallet.widget.menu;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.android.wallet.R;
import io.taucoin.foundation.util.DimensionsUtil;

import com.github.naturs.logger.Logger;
import com.lwy.righttopmenu.MenuItem;
import com.lwy.righttopmenu.R.style;
import java.util.ArrayList;
import java.util.List;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

public class RightTopMenu {
    private static final int DEFAULT_ANIM_STYLE;
    private Activity mActivity;
    private RecyclerView mMenuContainer;
    private MenuAdapter mAdapter;
    private PopupWindow mPopupWindow;
    private List<MenuItem> mMenuItems;
    private boolean mNeedAnimationStyle;
    private int mAnimationStyle;
    private boolean mDimBackground;
    private float mAlpha = 0.75F;
    private RightTopMenu.OnMenuItemClickListener mOnMenuItemClickListener;

    private RightTopMenu(Activity activity, boolean needAnimationStyle, int animationStyle, boolean dimBackground, List<MenuItem> menuItems, RightTopMenu.OnMenuItemClickListener onMenuItemClickListener) {
        this.mActivity = activity;
        this.mNeedAnimationStyle = needAnimationStyle;
        this.mAnimationStyle = animationStyle;
        this.mDimBackground = dimBackground;
        this.mMenuItems = menuItems;
        this.mOnMenuItemClickListener = onMenuItemClickListener;
        this.init();
    }

    public List<MenuItem> getMenuItems() {
        return this.mMenuItems;
    }

    public void addMenuItem(MenuItem item) {
        this.mMenuItems.add(item);
    }

    public void addMenuList(List<MenuItem> list) {
        this.mMenuItems.addAll(list);
    }

    private void init() {
        this.mMenuContainer = (RecyclerView)LayoutInflater.from(this.mActivity).inflate(R.layout.rt_menu_container, null);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this.mActivity, LinearLayoutManager.VERTICAL, false);
        this.mMenuContainer.setLayoutManager(linearLayoutManager);
        if (this.mMenuItems == null) {
            this.mMenuItems = new ArrayList();
        }

        this.mAdapter = new MenuAdapter(this.mActivity, this, this.mMenuItems);
        this.mAdapter.setOnMenuItemClickListener(this.mOnMenuItemClickListener);
//        this.mMenuContainer.addItemDecoration(new NormalDividerItemDecoration(this.mActivity, 1));
    }

    private PopupWindow getPopupWindow() {
        this.mPopupWindow = new PopupWindow(this.mActivity);
        this.mPopupWindow.setContentView(this.mMenuContainer);

        if (this.mNeedAnimationStyle) {
            this.mPopupWindow.setAnimationStyle(this.mAnimationStyle <= 0 ? DEFAULT_ANIM_STYLE : this.mAnimationStyle);
        }

        this.mPopupWindow.setFocusable(true);
        this.mPopupWindow.setOutsideTouchable(true);
        this.mPopupWindow.setBackgroundDrawable(new ColorDrawable());
        this.mPopupWindow.setOnDismissListener(new OnDismissListener() {
            public void onDismiss() {
                if (RightTopMenu.this.mDimBackground) {
                    RightTopMenu.this.setBackgroundAlpha(RightTopMenu.this.mAlpha, 1.0F, 100);
                }

            }
        });
        this.mAdapter.setData(this.mMenuItems);
        this.mMenuContainer.setAdapter(this.mAdapter);
        return this.mPopupWindow;
    }

    public void showAsDropDown(View anchor) {
        int xOff = 110;
        int yOff = 5;
        xOff = -DimensionsUtil.dip2px(this.mActivity, xOff);
        yOff = -DimensionsUtil.dip2px(this.mActivity, yOff);

        int height = 48 * this.mMenuItems.size();
        height = DimensionsUtil.dip2px(this.mActivity, height);

        DisplayMetrics dm = new DisplayMetrics();
        this.mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int windowWidth = dm.widthPixels;
        int windowHeight = dm.heightPixels;
        Logger.d("windowWidth=%s, windowHeight=%s", windowWidth, windowHeight);

        int[] location = new int[2] ;
        anchor.getLocationInWindow(location);
        Logger.d("getX=%s, getY=%s", location[0], location[1]);

        int totalHeight = height - 10 * yOff + location[1];
        Logger.d("totalHeight=%s, windowHeight=%s", totalHeight, windowHeight);
        if(totalHeight >= windowHeight){
            yOff = - height + 6 * yOff;
            this.mMenuContainer.setBackgroundResource(R.drawable.popup_up_bg);
        }
        this.showAsDropDown(anchor, xOff, yOff);
    }

    public void showAsDropDown(View anchor, int xoff, int yoff) {
        if (this.mPopupWindow == null) {
            this.getPopupWindow();
        }

        if (!this.mPopupWindow.isShowing()) {
            this.mPopupWindow.showAsDropDown(anchor, xoff, yoff);
            this.mAdapter.notifyDataSetChanged();
            if (this.mDimBackground) {
                this.setBackgroundAlpha(1.0F, this.mAlpha, 150);
            }
        }
    }

    private void setBackgroundAlpha(float from, float to, int duration) {
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{from, to});
        animator.setDuration((long)duration);
        animator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                Window window = RightTopMenu.this.mActivity.getWindow();
                LayoutParams lp = window.getAttributes();
                lp.alpha = (Float)animation.getAnimatedValue();
                window.addFlags(LayoutParams.FLAG_DIM_BEHIND);
                window.setAttributes(lp);
            }
        });
        animator.start();
    }

    public void dismiss() {
        if (this.mPopupWindow != null && this.mPopupWindow.isShowing()) {
            this.mPopupWindow.dismiss();
        }
    }

    static {
        DEFAULT_ANIM_STYLE = style.RTM_ANIM_STYLE;
    }

    public static class Builder {
        private Activity mActivity;
        private List<MenuItem> mMenuItems;
        private boolean mNeedAnimationStyle;
        private int mAnimationStyle;
        private boolean mDimBackground;
        private RightTopMenu.OnMenuItemClickListener mOnMenuItemClickListener;

        public Builder(Activity activity) {
            this.mActivity = activity;
        }

        public RightTopMenu.Builder menuItems(List<MenuItem> menuItemList) {
            this.mMenuItems = menuItemList;
            return this;
        }

        public RightTopMenu.Builder needAnimationStyle(boolean needAnimationStyle) {
            this.mNeedAnimationStyle = needAnimationStyle;
            return this;
        }

        public RightTopMenu.Builder animationStyle(int animationStyle) {
            this.mAnimationStyle = animationStyle;
            return this;
        }

        public RightTopMenu.Builder dimBackground(boolean dimBackground) {
            this.mDimBackground = dimBackground;
            return this;
        }

        public RightTopMenu.Builder onMenuItemClickListener(RightTopMenu.OnMenuItemClickListener onMenuItemClickListener) {
            this.mOnMenuItemClickListener = onMenuItemClickListener;
            return this;
        }

        public RightTopMenu build() {
            return new RightTopMenu(this.mActivity, this.mNeedAnimationStyle, this.mAnimationStyle, this.mDimBackground, this.mMenuItems, this.mOnMenuItemClickListener);
        }
    }

    public interface OnMenuItemClickListener {
        void onMenuItemClick(int var1);
    }
}
