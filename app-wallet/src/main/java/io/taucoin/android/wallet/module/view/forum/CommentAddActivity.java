package io.taucoin.android.wallet.module.view.forum;


import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.util.NotchUtil;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.android.wallet.widget.ToolbarView;
import io.taucoin.foundation.util.FitStateUI;

public class CommentAddActivity extends BaseActivity {

    @BindView(R.id.ll_toolbar)
    RelativeLayout llToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment_add);
        ButterKnife.bind(this);
        initView();
    }

    // Initialize page view components
    private void initView() {
        NotchUtil.resetStatusBarOrNotchHeight(llToolbar);
        FitStateUI.setStatusBarDarkIcon(this, true);

    }

    @OnClick({R.id.iv_cancel, R.id.tv_send})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_cancel:
                ToolbarView.handleLeftBack(view);
                break;
            case R.id.tv_send:
                ToastUtils.showShortToast(R.string.forum_comment_successfully);
                break;
            default:
                break;
        }
    }


}