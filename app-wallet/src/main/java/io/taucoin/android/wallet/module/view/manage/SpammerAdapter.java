package io.taucoin.android.wallet.module.view.manage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.db.entity.Spammer;
import io.taucoin.android.wallet.util.ForumUtil;
import io.taucoin.android.wallet.util.ResourcesUtil;

public class SpammerAdapter extends BaseAdapter {

    private List<Spammer> list = new ArrayList<>();
    private List<String> selectList = new ArrayList<>();

    void setListData(List<Spammer> list) {
        this.list.clear();
        selectList.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    List<String> getSelectList() {
        return selectList;
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
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_spammer, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        Spammer bean = list.get(position);
        String name = ResourcesUtil.getText(R.string.forum_personal_name);
        name += ForumUtil.getUserName(bean.getName(), bean.getAddress());
        viewHolder.tvName.setText(name);
        viewHolder.tvAddress.setText(bean.getAddress());
        viewHolder.cbSelected.setChecked(false);
        viewHolder.cbSelected.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                selectList.add(bean.getAddress());
            }else{
                selectList.remove(bean.getAddress());
            }
        });
        return convertView;
    }

    class ViewHolder {
        @BindView(R.id.cb_selected)
        CheckBox cbSelected;
        @BindView(R.id.tv_name)
        TextView tvName;
        @BindView(R.id.tv_address)
        TextView tvAddress;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
