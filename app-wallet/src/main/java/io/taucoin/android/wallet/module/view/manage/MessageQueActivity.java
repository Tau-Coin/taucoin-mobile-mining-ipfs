package io.taucoin.android.wallet.module.view.manage;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

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
import io.taucoin.android.wallet.db.entity.ForumTopic;
import io.taucoin.android.wallet.module.presenter.ForumPresenter;
import io.taucoin.android.wallet.util.DateUtil;
import io.taucoin.foundation.net.callback.LogicObserver;

public class MessageQueActivity extends BaseActivity {

    @BindView(R.id.list_view)
    ListView listView;
    @BindView(R.id.refresh_layout)
    SmartRefreshLayout refreshLayout;

    private MessageAdapter mAdapter;
    private ForumPresenter mPresenter;
    private List<ForumTopic> list = new ArrayList<>();
    private int mPageNo = 1;
    private String mTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_que);
        ButterKnife.bind(this);
        mPresenter = new ForumPresenter();
        initView();
        onRefresh(null);
    }

    @OnClick({R.id.tv_right})
    public void onRightClick(View view) {
        boolean isEdit = mAdapter.updateEdit();
        TextView textView = (TextView) view;
        textView.setText(isEdit ? R.string.setting_message_done : R.string.setting_message_edit);
    }

    private void getData() {
        mPresenter.getMessageQue(mPageNo, mTime, new LogicObserver<List<ForumTopic>>() {
            @Override
            public void handleData(List<ForumTopic> data) {

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

    protected void deleteMessage(long id) {
        mPresenter.deleteMessage(id, new LogicObserver<Boolean>() {
            @Override
            public void handleData(Boolean aBoolean) {
                onRefresh(null);
            }
        });
    }

    private void initView() {
        refreshLayout.setOnLoadmoreListener(this);
        refreshLayout.setOnRefreshListener(this);
        mAdapter = new MessageAdapter(this);
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