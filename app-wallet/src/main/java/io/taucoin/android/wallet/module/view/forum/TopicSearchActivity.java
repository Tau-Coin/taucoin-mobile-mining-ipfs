package io.taucoin.android.wallet.module.view.forum;


import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.github.naturs.logger.Logger;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.net.callback.CommonObserver;
import io.taucoin.android.wallet.util.KeyboardUtils;
import io.taucoin.android.wallet.util.NotchUtil;
import io.taucoin.android.wallet.widget.ToolbarView;
import io.taucoin.foundation.util.FitStateUI;
import io.taucoin.foundation.util.StringUtil;

public class TopicSearchActivity extends BaseActivity {

    @BindView(R.id.list_view)
    ListView listView;
    @BindView(R.id.refresh_layout)
    SmartRefreshLayout refreshLayout;
    @BindView(R.id.ll_search_bar)
    LinearLayout llSearchBar;
    @BindView(R.id.et_search_key)
    EditText etSearchKey;
    @BindView(R.id.tv_search_key)
    TextView tvSearchKey;

    private TopicAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_search);
        ButterKnife.bind(this);
        initView();
        hideOrShowSearchView(false);
    }

    // Initialize page view components
    private void initView() {
        NotchUtil.resetStatusBarOrNotchHeight(llSearchBar);
        FitStateUI.setStatusBarDarkIcon(this, true);

        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setOnLoadmoreListener(this);
        refreshLayout.setEnableAutoLoadmore(false);
        mAdapter = new TopicAdapter(this, 2);
        listView.setAdapter(mAdapter);

        etSearchKey.setOnEditorActionListener((v, actionId, event) -> {
            Logger.d("actionId=%s, keyCode=%s", actionId, (event == null ? -1 : event.getKeyCode()));
            if(isEnable && (actionId == EditorInfo.IME_ACTION_UNSPECIFIED       // enter key
                    || actionId == EditorInfo.IME_ACTION_DONE      // done key
                    || actionId == EditorInfo.IME_ACTION_NEXT)){    // next key
                throttleFirst(200);
                String searchKey = etSearchKey.getText().toString().trim();
                String searchLast = StringUtil.getTag(etSearchKey);
                Logger.d("searchKey=%s, searchLast=%s", searchKey, searchLast);
                if(StringUtil.isNotSame(searchKey, searchLast)){
                    tvSearchKey.setText(searchKey);
                    etSearchKey.setTag(searchKey);
                    hideOrShowSearchView(StringUtil.isNotEmpty(searchKey));
                }
                try {
                    KeyboardUtils.hideSoftInput(this);
                }catch (Exception ignore){}
                return true;
            }
            return false;
        });

    }

    private boolean isEnable = true;
    public void throttleFirst(long delaySeconds){
        isEnable = false;
        Observable.timer(delaySeconds, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(new CommonObserver<Long>() {
                @Override
                public void onComplete() {
                    isEnable = true;
                }
            });
    }

    @OnClick({R.id.iv_back})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_back:
                ToolbarView.handleLeftBack(view);
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

    private void hideOrShowSearchView(boolean isVisible){
        Logger.d("hideOrShowSearchView=" +  isVisible);
        ViewGroup parent = (ViewGroup) llSearchBar.getParent();
        if(null != parent){
            int visible = isVisible ? View.VISIBLE : View.GONE;
            for (int i = 0; i < parent.getChildCount(); i++) {
                if(i != 0){
                    parent.getChildAt(i).setVisibility(visible);
                }
            }
        }
    }
}