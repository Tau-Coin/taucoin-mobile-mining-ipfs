package io.taucoin.android.wallet.module.view.manage;

import android.os.Bundle;
import android.widget.ListView;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.Spammer;
import io.taucoin.android.wallet.module.presenter.ForumPresenter;
import io.taucoin.android.wallet.util.DateUtil;
import io.taucoin.foundation.net.callback.LogicObserver;

public class SpammerActivity extends BaseActivity {

    @BindView(R.id.list_view)
    ListView listView;
    @BindView(R.id.refresh_layout)
    SmartRefreshLayout refreshLayout;

    private SpammerAdapter mAdapter;
    private ForumPresenter mPresenter;
    private List<Spammer> list = new ArrayList<>();
    private int mPageNo = 1;
    private String mTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spammer);
        ButterKnife.bind(this);
        mPresenter = new ForumPresenter();
        initView();
        onRefresh(null);
    }

    @OnClick({R.id.tv_unspam})
    public void unSpamClick() {
        mPresenter.unSpamList(mAdapter.getSelectList(), new LogicObserver<Boolean>(){
            @Override
            public void handleData(Boolean aBoolean) {
                onRefresh(null);
            }
        });
    }

    private void getData() {
        mPresenter.getSpamList(mPageNo, mTime, new LogicObserver<List<Spammer>>() {
            @Override
            public void handleData(List<Spammer> data) {

                if(refreshLayout == null || mAdapter == null){
                    return;
                }
                if(mPageNo == 1){
                    list.clear();
                }
                if(data.size() > 0){
                    list.addAll(data);
                }
                mAdapter.setListData(list);
                boolean isLoadMore = list.size() % TransmitKey.PAGE_SIZE == 0 && list.size() > 0;
                refreshLayout.setEnableLoadmore(isLoadMore);
                refreshLayout.finishLoadmore(100);
                refreshLayout.finishRefresh(100);
            }
        });
    }

    private void initView() {
        refreshLayout.setOnLoadmoreListener(this);
        refreshLayout.setOnRefreshListener(this);
        mAdapter = new SpammerAdapter();
        listView.setAdapter(mAdapter);
    }

    @Override
    public void onRefresh(RefreshLayout refreshlayout) {
        mPageNo = 1;
        mTime = DateUtil.getCurrentTime();
        getData();
    }

    @Override
    public void onLoadmore(RefreshLayout refreshlayout) {
        mPageNo += 1;
        getData();
    }
}