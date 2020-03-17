package io.taucoin.android.wallet.module.view.forum;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.ForumTopic;
import io.taucoin.android.wallet.module.presenter.ForumPresenter;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.android.wallet.util.NotchUtil;
import io.taucoin.android.wallet.widget.ToolbarView;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.FitStateUI;

public class CommentAddActivity extends BaseActivity {

    @BindView(R.id.ll_toolbar)
    RelativeLayout llToolbar;
    @BindView(R.id.tv_topic_title)
    TextView tvTopicTitle;
    @BindView(R.id.et_comment)
    EditText etComment;
    @BindView(R.id.tv_fee)
    TextView tvFee;

    private ForumTopic forumTopic;
    private ForumPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment_add);
        presenter = new ForumPresenter();
        ButterKnife.bind(this);
        initView();
    }

    // Initialize page view components
    private void initView() {

        NotchUtil.resetStatusBarOrNotchHeight(llToolbar);
        FitStateUI.setStatusBarDarkIcon(this, true);

        String data = getIntent().getStringExtra(TransmitKey.DATA);
        ForumTopic bean = new Gson().fromJson(data, ForumTopic.class);
        forumTopic = new ForumTopic();
        if(bean != null){
            tvTopicTitle.setText(bean.getTitle());
            forumTopic.setReferId(bean.getTxId());
        }
        String medianFee = "17.6";
        tvFee.setText(medianFee);
        tvFee.setTag(medianFee);
    }

    @OnClick({R.id.iv_cancel, R.id.tv_send, R.id.ll_fee})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_cancel:
                ToolbarView.handleLeftBack(view);
                break;
            case R.id.tv_send:
                if(forumTopic != null && etComment != null && presenter != null){
                    forumTopic.setText(etComment.getText().toString().trim());
                    String fee = tvFee.getText().toString().trim();
                    forumTopic.setFee(FmtMicrometer.fmtFormatMoney(fee));
                    presenter.postComment(forumTopic, new LogicObserver<Boolean>() {
                        @Override
                        public void handleData(Boolean aBoolean) {
                            etComment.getText().clear();
                        }
                    });
                }
                break;
            case R.id.ll_fee:
                showEditFeeDialog();
                break;
            default:
                break;
        }
    }

    private void showEditFeeDialog() {
        presenter.showEditFeeDialog(this, tvFee, false);
    }
}