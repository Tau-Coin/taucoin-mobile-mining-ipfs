package io.taucoin.android.wallet.widget.menu;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.LayoutParams;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import io.taucoin.android.wallet.R;

import com.lwy.righttopmenu.MenuItem;
import com.lwy.righttopmenu.R.drawable;
import com.lwy.righttopmenu.R.id;

import org.jetbrains.annotations.NotNull;

import java.util.List;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//
public class MenuAdapter extends Adapter<MenuAdapter.MenuViewHolder> {
    private List<MenuItem> mMenuItemList;
    private RightTopMenu mRightTopMenu;
    private RightTopMenu.OnMenuItemClickListener mOnMenuItemClickListener;
    private Context mContext;

    public void setOnMenuItemClickListener(RightTopMenu.OnMenuItemClickListener onMenuItemClickListener) {
        this.mOnMenuItemClickListener = onMenuItemClickListener;
    }

    public MenuAdapter(Context context, RightTopMenu rightTopMenu, List<MenuItem> menuItemList) {
        this.mContext = context;
        this.mRightTopMenu = rightTopMenu;
        this.mMenuItemList = menuItemList;
    }

    public void setData(List<MenuItem> menuItemList) {
        this.mMenuItemList = menuItemList;
        this.notifyDataSetChanged();
    }

    public MenuAdapter.MenuViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        RelativeLayout view = (RelativeLayout)LayoutInflater.from(this.mContext).inflate(R.layout.rt_menu_item, null);
        LayoutParams params = new LayoutParams(-1, -2);
        view.setLayoutParams(params);
        return new MenuAdapter.MenuViewHolder(view);
    }

    public void onBindViewHolder(MenuAdapter.MenuViewHolder holder, final int position) {
        MenuItem menuItem = (MenuItem)this.mMenuItemList.get(position);
        holder.mTextTV.setText(menuItem.getText());
        if (menuItem.getBadgeCount() > 99) {
            holder.mBadgeTV.setVisibility(View.VISIBLE);
            holder.mBadgeTV.setText("99+");
            holder.mBadgeTV.setBackgroundResource(drawable.badge_red);
            android.view.ViewGroup.LayoutParams params = holder.mBadgeTV.getLayoutParams();
            params.width = -2;
            params.height = this.dip2Px(20.0F);
        } else if (menuItem.getBadgeCount() > 0) {
            holder.mBadgeTV.setVisibility(View.VISIBLE);
            holder.mBadgeTV.setText(menuItem.getBadgeCount() + "");
        } else {
            holder.mBadgeTV.setVisibility(View.GONE);
        }

        int iconRes = menuItem.getIcon();
        holder.mIcon.setImageResource(iconRes < 0 ? 0 : iconRes);
//        if (position == 0) {
//            holder.mContainer.setBackground(this.addStateDrawable(this.mContext, -1, com.lwy.righttopmenu.R.drawable.popup_top_pressed));
//        } else if (position == this.mMenuItemList.size() - 1) {
//            holder.mContainer.setBackground(this.addStateDrawable(this.mContext, -1, com.lwy.righttopmenu.R.drawable.popup_bottom_pressed));
//        } else {
//            holder.mContainer.setBackground(this.addStateDrawable(this.mContext, -1, com.lwy.righttopmenu.R.drawable.popup_middle_pressed));
//        }

        holder.mContainer.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (MenuAdapter.this.mOnMenuItemClickListener != null) {
                    MenuAdapter.this.mRightTopMenu.dismiss();
                    MenuAdapter.this.mOnMenuItemClickListener.onMenuItemClick(position);
                }

            }
        });
    }

    public int getItemCount() {
        return this.mMenuItemList.size();
    }

    private StateListDrawable addStateDrawable(Context context, int normalId, int pressedId) {
        StateListDrawable sd = new StateListDrawable();
        Drawable normal = normalId == -1 ? null : context.getResources().getDrawable(normalId);
        Drawable pressed = pressedId == -1 ? null : context.getResources().getDrawable(pressedId);
        sd.addState(new int[]{16842919}, pressed);
        sd.addState(new int[0], normal);
        return sd;
    }

    public int dip2Px(float dip) {
        return (int)(dip * this.mContext.getResources().getDisplayMetrics().density + 0.5F);
    }

    class MenuViewHolder extends ViewHolder {
        private View mContainer;
        private TextView mTextTV;
        private ImageView mIcon;
        private TextView mBadgeTV;

        public MenuViewHolder(View itemView) {
            super(itemView);
            this.mContainer = itemView;
            this.mBadgeTV = (TextView)itemView.findViewById(id.rt_menu_item_badge);
            this.mIcon = (ImageView)itemView.findViewById(id.rt_menu_item_icon);
            this.mTextTV = (TextView)itemView.findViewById(id.rt_menu_item_text);
        }
    }
}
