package io.taucoin.android.wallet.module.view.manage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.interop.IpfsPeerInfo;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.db.entity.KeyValue;

public class PeersAdapter extends BaseAdapter {

    private List<IpfsPeerInfo> list = new ArrayList<>();

    void setListData(List<IpfsPeerInfo> list) {
        this.list.clear();
        this.list.addAll(list);
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
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_peers, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        IpfsPeerInfo peer = list.get(position);
        viewHolder.tvId.setText(peer.getPeerId());
        return convertView;
    }

    class ViewHolder {
        @BindView(R.id.tv_id)
        TextView tvId;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
