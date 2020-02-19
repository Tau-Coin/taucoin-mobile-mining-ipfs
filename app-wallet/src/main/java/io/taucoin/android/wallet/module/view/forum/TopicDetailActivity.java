package io.taucoin.android.wallet.module.view.forum;


import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.foundation.util.StringUtil;

public class TopicDetailActivity extends BaseActivity {

    @BindView(R.id.ll_best_comment)
    LinearLayout llBestComment;
    @BindView(R.id.item_topic)
    View itemTopic;
    @BindView(R.id.list_view)
    ListView listView;
    @BindView(R.id.refresh_layout)
    SmartRefreshLayout refreshLayout;

    private CommentAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_detail);
        ButterKnife.bind(this);
        initView();
    }

    // Initialize page view components
    private void initView() {
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setOnLoadmoreListener(this);
        refreshLayout.setEnableAutoLoadmore(false);
        mAdapter = new CommentAdapter(this);
        listView.setAdapter(mAdapter);

        TopicAdapter.ViewHolder viewHolder = new TopicAdapter.ViewHolder(itemTopic);
        TopicAdapter.handleDetailView(this, viewHolder, 0);
    }

    @OnClick({R.id.tv_add_comment, R.id.ll_best_comment, R.id.iv_to_top_bottom})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ll_best_comment:
                break;
            case R.id.tv_add_comment:
                ActivityUtil.startActivity(this, CommentAddActivity.class);
                break;
            case R.id.iv_to_top_bottom:
                scrollToTopOrBottom((ImageView)view);
                break;
            default:
                break;
        }
    }

    private void scrollToTopOrBottom(ImageView view){
        int tag = StringUtil.getIntTag(view);
        if (tag == 0) {
            listView.smoothScrollByOffset(10);
            listView.smoothScrollToPosition(listView.getCount() - 1);
            view.setTag(1);
            view.setImageResource(R.mipmap.icon_up);
        }else{
            listView.smoothScrollByOffset(10);
            listView.smoothScrollToPosition(0);
            view.setTag(0);
            view.setImageResource(R.mipmap.icon_down);
        }
    }

    @Override
    public void onRefresh(RefreshLayout refreshlayout) {
        refreshLayout.finishRefresh(100);
    }

    @Override
    public void onLoadmore(RefreshLayout refreshlayout) {
        refreshLayout.finishLoadmore(100);
    }
}