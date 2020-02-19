package io.taucoin.android.wallet.module.view.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import butterknife.BindView;
import butterknife.OnClick;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseFragment;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.module.view.forum.TopicAdapter;
import io.taucoin.android.wallet.module.view.forum.TopicAddActivity;
import io.taucoin.android.wallet.module.view.forum.TopicSearchActivity;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.ForumUtil;

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

    private TopicAdapter mAdapter;

    @Override
    public View getViewLayout(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forum, container, false);
        butterKnifeBinder(this, view);
        initView();
        return view;
    }

    // Initialize page view components
    private void initView() {
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setOnLoadmoreListener(this);
        mAdapter = new TopicAdapter(getActivity(), 1);
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
        refreshLayout.finishRefresh(100);
    }

    @Override
    public void onLoadmore(RefreshLayout refreshlayout) {
        refreshLayout.finishLoadmore(100);
    }
}