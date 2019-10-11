package io.taucoin.android.wallet.module.view.main;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.android.wallet.widget.banner.BannerLayout;
import io.taucoin.foundation.util.DrawablesUtil;
import io.taucoin.foundation.util.StringUtil;

public class WebBannerAdapter extends RecyclerView.Adapter<WebBannerAdapter.ViewHolder> {

    private List<KeyValue> array = new ArrayList<>();
    private BannerLayout.OnBannerItemClickListener onBannerItemClickListener;
    private int windowWidth = -1;

    void setDataList(List<KeyValue> list) {
        if(list != null && list.size() > 0){
            array.clear();
            if(list.size() <= 3){
                array.addAll(list);
            }else{
                array.add(list.get(0));
                array.add(list.get(1));
                array.add(list.get(2));
            }
        }
        array.add(null);
        notifyDataSetChanged();
    }

    KeyValue getItemData(int pos) {
        if(pos >= 0 && pos <= array.size()){
            return array.get(pos);
        }
       return null;
    }


    void setOnBannerItemClickListener(BannerLayout.OnBannerItemClickListener onBannerItemClickListener) {
        this.onBannerItemClickListener = onBannerItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(windowWidth == -1){
            WindowManager wm = (WindowManager) parent.getContext().getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            windowWidth = dm.widthPixels;
        }

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_three_address, parent, false);
        RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view.getLayoutParams();
        layoutParams.width = windowWidth * 6 / 10;
        view.setLayoutParams(layoutParams);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        if (array == null || array.size() == 0)
            return;
        final int pos = position % array.size();
        KeyValue keyValue = array.get(pos);
        if(keyValue != null){
            holder.rlAddress.setVisibility(View.VISIBLE);
            holder.rlAddressAdd.setVisibility(View.GONE);
            int visibility = UserUtil.isSelf(keyValue.getAddress()) ? View.VISIBLE : View.INVISIBLE;
            holder.ivSelected.setVisibility(visibility);
            holder.tvName.setText(UserUtil.parseNickName(keyValue));
            holder.tvBalance.setText(FmtMicrometer.fmtBalance(keyValue.getBalance()));
            holder.tvAddress.setText(hiddenAddress(keyValue.getAddress()));
            DrawablesUtil.setEndDrawable(holder.tvAddress, R.mipmap.icon_copy, 14);
            holder.tvAddress.setOnClickListener(v -> {
                if (onBannerItemClickListener != null) {
                    onBannerItemClickListener.onViewClick(pos, v);
                }
            });
            holder.itemView.setOnClickListener(v -> {
                if (onBannerItemClickListener != null) {
                    onBannerItemClickListener.onItemClick(pos);
                }
            });
        }else{
            holder.rlAddress.setVisibility(View.GONE);
            holder.rlAddressAdd.setVisibility(View.VISIBLE);
            holder.ivSelected.setVisibility(View.INVISIBLE);
            holder.ivAddressAdd.setOnClickListener(v -> {
                if (onBannerItemClickListener != null) {
                    onBannerItemClickListener.onViewClick(pos, v);
                }
            });
        }
    }

    private String hiddenAddress(String address) {
        StringBuilder stringBuilder = new StringBuilder();
        if(StringUtil.isNotEmpty(address) && address.length() > 9){
            stringBuilder.append(address.substring(0, 3));
            stringBuilder.append("*********************");
            stringBuilder.append(address.substring(address.length() - 6));
        }
        return stringBuilder.toString();
    }

    @Override
    public int getItemCount() {
        if (array != null) {
            return array.size();
        }
        return 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        @BindView(R.id.tv_name)
        TextView tvName;
        @BindView(R.id.tv_balance)
        TextView tvBalance;
        @BindView(R.id.tv_address)
        TextView tvAddress;
        @BindView(R.id.iv_selected)
        ImageView ivSelected;
        @BindView(R.id.rl_address_add)
        View rlAddressAdd;
        @BindView(R.id.rl_address)
        View rlAddress;
        @BindView(R.id.iv_address_add)
        ImageView ivAddressAdd;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
