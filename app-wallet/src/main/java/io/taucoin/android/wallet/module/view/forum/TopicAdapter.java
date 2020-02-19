package io.taucoin.android.wallet.module.view.forum;

import androidx.fragment.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.ForumUtil;
import io.taucoin.android.wallet.util.GlideUtil;

public class TopicAdapter extends BaseAdapter {

    private List<KeyValue> list = new ArrayList<>();
    private FragmentActivity activity;
    private static boolean isNormalModel;
    private int type;

    public TopicAdapter(FragmentActivity activity, int type) {
        this.type = type;
        isNormalModel = ForumUtil.isNormalModel();
        this.activity = activity;
    }

    public void switchBrowseModel(){
        isNormalModel = ForumUtil.isNormalModel();
        notifyDataSetChanged();
    }

    void setListData(List<KeyValue> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return 10;
    }

    @Override
    public Object getItem(int position) {
//        return list.get(position);
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_topic, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        handleItemView(activity, viewHolder, position, type);
        return convertView;
    }

    static void handleDetailView(FragmentActivity activity, ViewHolder viewHolder, int pos){
        isNormalModel = ForumUtil.isNormalModel();
        handleItemView(activity, viewHolder, pos, 3);
    }

    /**
     * handle list, search and detailed data of topic
     * type: 1 : list
     *       2 : search
     *       3 : detailed
     * */
    private static void handleItemView(FragmentActivity activity, ViewHolder viewHolder, int pos, int type){
        String picUrl = "https://ss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=138677456,3853264585&fm=26&gp=0.jpg";
        if(pos %2 == 0){
            if(isNormalModel){
                viewHolder.ivPic.setVisibility(View.VISIBLE);
                Glide.with(activity)
                        .load(picUrl)
                        .apply(GlideUtil.getRequestOptions())
                        .into(viewHolder.ivPic);
            }else {
                viewHolder.ivPic.setVisibility(View.GONE);
            }
            viewHolder.tvText.setVisibility(View.GONE);
        }else{
            viewHolder.tvText.setVisibility(View.VISIBLE);
            viewHolder.ivPic.setVisibility(View.GONE);
        }

        int txtRid = type == 2 ? R.string.forum_block : R.string.forum_posted;
        viewHolder.tvUsername.setText(txtRid);

        if(type != 3){
            viewHolder.rootView.setOnClickListener(v -> ActivityUtil.startActivity(activity, TopicDetailActivity.class));
        }
        viewHolder.llPermalink.setVisibility(type == 2 ? View.GONE : View.VISIBLE);
        viewHolder.llPermalink.setOnClickListener(v -> ActivityUtil.startActivity(activity, PermalinkActivity.class));

    }

    static class ViewHolder {
        @BindView(R.id.tv_community_name)
        TextView tvCommunityName;
        @BindView(R.id.tv_username)
        TextView tvUsername;
        @BindView(R.id.tv_permalink_coins)
        TextView tvPermalinkCoins;
        @BindView(R.id.ll_permalink)
        LinearLayout llPermalink;
        @BindView(R.id.tv_title)
        TextView tvTitle;
        @BindView(R.id.tv_text)
        TextView tvText;
        @BindView(R.id.iv_pic)
        ImageView ivPic;
        View rootView;

        ViewHolder(View view) {
            rootView = view;
            ButterKnife.bind(this, view);
        }
    }
}
