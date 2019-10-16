package io.taucoin.android.wallet.module.view.manage;

import android.os.Bundle;
import android.widget.ListView;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.util.ProgressManager;

public class IPFSInfoActivity extends BaseActivity {

    @BindView(R.id.list_view_help)
    ListView listViewHelp;
    @BindView(R.id.refresh_layout)
    SmartRefreshLayout refreshLayout;

    private PeersAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ipfs_info);
        ButterKnife.bind(this);
        initView();
//        ProgressManager.showProgressDialog(this);
        getData();
    }

    private void getData() {

    }

    public void loadData(List<KeyValue> data) {
        ProgressManager.closeProgressDialog();
        if(data != null){
            mAdapter.setListData(data);
            refreshLayout.finishRefresh();
        }
    }

    private void initView() {
        refreshLayout.setOnRefreshListener(this);
        mAdapter = new PeersAdapter();
        listViewHelp.setAdapter(mAdapter);
    }

    @Override
    public void onRefresh(RefreshLayout refreshlayout) {
        getData();
    }
}