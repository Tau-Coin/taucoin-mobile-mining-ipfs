package io.taucoin.android.wallet.module.view.forum;


import android.os.Bundle;
import android.widget.ListView;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseActivity;

public class PermalinkActivity extends BaseActivity {

    @BindView(R.id.list_view)
    ListView listView;
    @BindView(R.id.refresh_layout)
    SmartRefreshLayout refreshLayout;

    private PermalinkAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permalink);
        ButterKnife.bind(this);
        initView();
    }

    // Initialize page view components
    private void initView() {
        refreshLayout.setOnRefreshListener(this);
        mAdapter = new PermalinkAdapter(this);
        listView.setAdapter(mAdapter);
    }

    @Override
    public void onRefresh(RefreshLayout refreshlayout) {
        refreshLayout.finishRefresh(100);
    }
}