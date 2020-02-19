package io.taucoin.android.wallet.module.view.forum;

import android.text.Html;
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
import io.taucoin.android.wallet.db.entity.KeyValue;

public class PermalinkAdapter extends BaseAdapter {

    private List<KeyValue> list = new ArrayList<>();
    private PermalinkActivity activity;

    PermalinkAdapter(PermalinkActivity activity) {
        this.activity = activity;
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
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_permalink, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        String url = "http://ipfs.io/ipfs/Qmaisz6NMhDB51cCcNWa1GMS7LU1pAxdF4Ld6Ft9kZEp2a";
        String content;
        if(position % 2 == 0){
            content = parent.getResources().getString(R.string.forum_gateway_offline);
        }else{
            content = parent.getResources().getString(R.string.forum_gateway_online);
        }
        content = String.format(content, url);
        viewHolder.tvContent.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
        return convertView;
    }

    class ViewHolder {
        @BindView(R.id.tv_content)
        TextView tvContent;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
