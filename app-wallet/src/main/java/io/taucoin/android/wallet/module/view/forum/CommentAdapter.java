package io.taucoin.android.wallet.module.view.forum;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.db.entity.ForumTopic;

public class CommentAdapter extends BaseAdapter {

    private List<ForumTopic> list = new ArrayList<>();
    private TopicDetailActivity activity;

    CommentAdapter(TopicDetailActivity activity) {
        this.activity = activity;
    }

    void setListData(List<ForumTopic> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        ForumTopic bean = list.get(position);
        viewHolder.tvTitle.setText(bean.getText());
        return convertView;
    }

    class ViewHolder {
        @BindView(R.id.tv_username)
        TextView tvUsername;
        @BindView(R.id.tv_title)
        TextView tvTitle;
        @BindView(R.id.iv_vote_down)
        ImageView ivVoteDown;
        @BindView(R.id.tv_vote)
        TextView tvVote;
        @BindView(R.id.iv_vote_up)
        ImageView ivVoteUp;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}