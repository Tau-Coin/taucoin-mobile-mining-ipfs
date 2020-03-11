package io.taucoin.android.wallet.module.view.forum;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.google.gson.Gson;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.ForumBaseActivity;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.ForumTopic;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.presenter.ForumPresenter;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.DateUtil;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.StringUtil;

public class TopicDetailActivity extends ForumBaseActivity {

    @BindView(R.id.ll_best_comment)
    LinearLayout llBestComment;
    @BindView(R.id.item_topic)
    View itemTopic;
    @BindView(R.id.list_view)
    ListView listView;
    @BindView(R.id.refresh_layout)
    SmartRefreshLayout refreshLayout;
    @BindView(R.id.scroll_view)
    NestedScrollView scrollView;

    private List<ForumTopic> topicsList = new ArrayList<>();
    private CommentAdapter mAdapter;
    private int mPageNo = 1;
    private String mTime;
    private String replyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_detail);
        ButterKnife.bind(this);
        mPresenter = new ForumPresenter();
        initView();
    }

    // Initialize page view components
    private void initView() {
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setOnLoadmoreListener(this);
        refreshLayout.setEnableAutoLoadmore(false);
        mAdapter = new CommentAdapter(this);
        listView.setAdapter(mAdapter);

        String data = getIntent().getStringExtra(TransmitKey.DATA);
        ForumTopic bean = new Gson().fromJson(data, ForumTopic.class);
        if(bean != null){
            TopicAdapter.ViewHolder viewHolder = new TopicAdapter.ViewHolder(itemTopic);
            TopicAdapter.handleDetailView(this, viewHolder, bean);
            replyId = bean.getTxId();
            onRefresh(null);
        }
    }

    @OnClick({R.id.tv_add_comment, R.id.ll_best_comment, R.id.iv_to_top_bottom})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ll_best_comment:
                break;
            case R.id.tv_add_comment:
                ActivityUtil.startActivity(getIntent(), this, CommentAddActivity.class);
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
            scrollView.fullScroll(NestedScrollView.FOCUS_DOWN);
            view.setTag(1);
            view.setImageResource(R.mipmap.icon_up);
        }else{
            scrollView.fullScroll(NestedScrollView.FOCUS_UP);
            view.setTag(0);
            view.setImageResource(R.mipmap.icon_down);
        }
    }

    private void loadData() {
        mPresenter.getCommentList(mPageNo, mTime, replyId, new LogicObserver<List<ForumTopic>>() {
            @Override
            public void handleData(List<ForumTopic> forumTopics) {
                if(mPageNo == 1){
                    topicsList.clear();
                }
                if(forumTopics.size() > 0){
                    topicsList.addAll(forumTopics);
                    mAdapter.setListData(topicsList);
                }
                boolean isLoadMore = topicsList.size() % TransmitKey.PAGE_SIZE == 0 && topicsList.size() > 0;
                refreshLayout.setEnableLoadmore(isLoadMore);
                refreshLayout.finishLoadmore(100);
                refreshLayout.finishRefresh(100);
            }
        });
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent object) {
        switch (object.getCode()) {
            case COMMENT_REFRESH:
                onRefresh(null);
                break;
            default:
                break;
        }
    }

    @Override
    public void onRefresh(RefreshLayout refreshlayout) {
        mPageNo = 1;
        mTime = DateUtil.getCurrentTime();
        loadData();
    }

    @Override
    public void onLoadmore(RefreshLayout refreshlayout) {
        mPageNo += 1;
        loadData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}