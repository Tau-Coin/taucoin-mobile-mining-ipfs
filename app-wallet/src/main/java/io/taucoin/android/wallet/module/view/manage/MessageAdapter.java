package io.taucoin.android.wallet.module.view.manage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.db.entity.ForumTopic;
import io.taucoin.android.wallet.util.ForumUtil;
import io.taucoin.android.wallet.util.ResourcesUtil;

public class MessageAdapter extends BaseAdapter {

    private List<ForumTopic> list = new ArrayList<>();
    private MessageQueActivity activity;
    private boolean isEdit = false;
    MessageAdapter(MessageQueActivity activity){
        this.activity = activity;
    }

    void setListData(List<ForumTopic> list) {
        this.list.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    boolean updateEdit() {
        isEdit = !isEdit;
        notifyDataSetChanged();
        return isEdit;
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
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        ForumTopic bean = list.get(position);
        if(bean.getType() == 0){
            viewHolder.tvText.setText(bean.getTitle());
        }else if(bean.getType() == 1){
            viewHolder.tvText.setText(bean.getText());
        }else if(bean.getType() == 2){
            viewHolder.tvText.setText(bean.getUserName());
        }
        viewHolder.ivDelete.setVisibility(isEdit ? View.VISIBLE : View.GONE);
        viewHolder.ivDelete.setOnClickListener(v -> activity.deleteMessage(bean.getId()));
        return convertView;
    }

    class ViewHolder {
        @BindView(R.id.iv_delete)
        ImageView ivDelete;
        @BindView(R.id.tv_text)
        TextView tvText;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
