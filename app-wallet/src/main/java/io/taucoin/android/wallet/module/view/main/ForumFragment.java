package io.taucoin.android.wallet.module.view.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseFragment;
import io.taucoin.android.wallet.base.ForumBaseActivity;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.ForumTopic;
import io.taucoin.android.wallet.module.presenter.ForumPresenter;
import io.taucoin.android.wallet.module.view.forum.TopicAdapter;
import io.taucoin.android.wallet.module.view.forum.TopicAddActivity;
import io.taucoin.android.wallet.module.view.forum.TopicSearchActivity;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.ForumUtil;
import io.taucoin.android.wallet.util.MediaPlayerUtil;
import io.taucoin.foundation.net.callback.LogicObserver;

/**
 *
 * Forum information display list page
 *
 * */
public class ForumFragment extends BaseFragment {

    @BindView(R.id.refresh_layout)
    SmartRefreshLayout refreshLayout;
    @BindView(R.id.list_view)
    ListView listView;

    private ForumPresenter mPresenter;
    private TopicAdapter mAdapter;
    private List<ForumTopic> topicsList = new ArrayList<>();
    private int mPage = 0;

    @Override
    public View getViewLayout(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forum, container, false);
        butterKnifeBinder(this, view);
        mPresenter = new ForumPresenter();
        initView();
        loadData();
        return view;
    }

    private void loadData() {
        mPresenter.getForumTopicList(new LogicObserver<List<ForumTopic>>() {
            @Override
            public void handleData(List<ForumTopic> forumTopics) {
                topicsList.clear();
                if(forumTopics.size() > 0){
                    topicsList.addAll(forumTopics);
                    mAdapter.setListData(topicsList);
                }
            }
        });
    }

    // Initialize page view components
    private void initView() {
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setOnLoadmoreListener(this);
        ForumBaseActivity activity = (ForumBaseActivity) getActivity();
        if(activity != null){
            activity.mPresenter = mPresenter;
        }
        mAdapter = new TopicAdapter(activity, 1);
        listView.setAdapter(mAdapter);
    }

    @OnClick({R.id.iv_create, R.id.tv_browse, R.id.ll_search_bar})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_create:
                ActivityUtil.startActivity(getActivity(), TopicAddActivity.class);
                break;
            case R.id.ll_search_bar:
                ActivityUtil.startActivity(getActivity(), TopicSearchActivity.class);
                break;
            case R.id.tv_browse:
                ForumUtil.switchBrowseModel((TextView)view);
                mAdapter.switchBrowseModel();
                break;
            default:
                break;
        }
    }

    @Override
    public void onRefresh(RefreshLayout refreshlayout) {
        loadData();
        refreshLayout.finishRefresh(100);
    }

    @Override
    public void onLoadmore(RefreshLayout refreshlayout) {
        loadData();
        refreshLayout.finishLoadmore(100);
    }

    @Override
    public void onResume() {
        super.onResume();
        MediaPlayerUtil.getInstance().resume(ForumFragment.class);
    }

    @Override
    public void onPause() {
        super.onPause();
        MediaPlayerUtil.getInstance().pause(ForumFragment.class);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MediaPlayerUtil.getInstance().destroy();
    }
}