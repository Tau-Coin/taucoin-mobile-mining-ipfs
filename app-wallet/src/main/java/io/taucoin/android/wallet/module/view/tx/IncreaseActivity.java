package io.taucoin.android.wallet.module.view.tx;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import butterknife.OnTouch;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.core.Wallet;
import io.taucoin.android.wallet.db.entity.IncreasePower;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.presenter.TxPresenter;
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.android.wallet.module.view.main.iview.ISendView;
import io.taucoin.android.wallet.module.view.manage.SettingActivity;
import io.taucoin.android.wallet.util.FixMemLeak;
import io.taucoin.android.wallet.util.KeyboardUtils;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.android.wallet.util.MoneyValueFilter;
import io.taucoin.android.wallet.util.ProgressManager;
import io.taucoin.android.wallet.util.SharedPreferencesHelper;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.android.wallet.widget.CommonDialog;
import io.taucoin.android.wallet.widget.EditInput;
import io.taucoin.android.wallet.widget.SelectionEditText;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.StringUtil;

public class IncreaseActivity extends BaseActivity implements ISendView {

    @BindView(R.id.ll_root)
    LinearLayout llRoot;
    @BindView(R.id.tv_address)
    TextView tvAddress;
    @BindView(R.id.et_amount)
    EditText etAmount;
    @BindView(R.id.et_fee)
    EditInput etFee;
    @BindView(R.id.btn_send)
    Button btnSend;
    @BindView(R.id.tv_total_amount)
    TextView tvTotalAmount;

    private TxPresenter mTxPresenter;
    private ViewHolder mViewHolder;
    private String mAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_increase);
        ButterKnife.bind(this);
        mTxPresenter = new TxPresenter();
        TxService.startTxService(TransmitKey.ServiceType.GET_SEND_DATA);
        initView();
    }

    private void initView() {
        mAddress = SharedPreferencesHelper.getInstance().getString(TransmitKey.ADDRESS, "");
        String address = getText(R.string.send_tx_increase_address).toString();
        address = String.format(address, mAddress);
        tvAddress.setText(Html.fromHtml(address));

        MiningUtil.initSenderTxFee(etFee);
        etAmount.setFilters(new InputFilter[]{new MoneyValueFilter()});
        initTxFeeView();

        KeyboardUtils.registerSoftInputChangedListener(this, height -> {
            if(etFee != null){
                boolean isFeeFocus = etFee.hasFocus();
                boolean isVisible = KeyboardUtils.isSoftInputVisible(IncreaseActivity.this);
                if(isFeeFocus && !isVisible){
                    resetViewFocus(llRoot);
                }
            }
        });

        Observable.create((ObservableOnSubscribe<View>)
                e -> btnSend.setOnClickListener(e::onNext))
                .throttleFirst(2, TimeUnit.SECONDS)
                .subscribe(new LogicObserver<View>() {
                    @Override
                    public void handleData(View view) {
                        KeyboardUtils.hideSoftInput(IncreaseActivity.this);
                        checkForm();
                    }
                });
    }

    private void initTxFeeView() {
        SelectionEditText editText = etFee.getEditText();
        editText.setTextAppearance(this, R.style.style_normal_yellow);
        editText.setFilters(new InputFilter[]{new MoneyValueFilter().setDigits(2).setEndSpace()});
        editText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setMaxLines(1);
    }

    @OnTextChanged({R.id.et_amount})
    void onAmountTextChanged(CharSequence text){
        String amount = etAmount.getText().toString().trim();
        if(StringUtil.isNotEmpty(amount) ){
            String totalAmount = getText(R.string.send_tx_budget_amount).toString();
            totalAmount = String.format(totalAmount, amount);
            tvTotalAmount.setText(Html.fromHtml(totalAmount));
            tvTotalAmount.setVisibility(View.VISIBLE);
        }else{
            tvTotalAmount.setVisibility(View.GONE);
        }
    }

    @OnTextChanged({R.id.et_input})
    void onFeeTextChanged(CharSequence text){
        Wallet.validateTxFee(etFee);
    }

    @OnClick({R.id.iv_fee})
    void onFeeSelectedClicked() {
        showSoftInput();
    }

    @OnTouch(R.id.et_fee)
    boolean onTxFeeClick(){
        return true;
    }

    private void resetViewFocus(View view) {
        if(view != null){
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.requestFocus();
        }
    }

    @Override
    public void checkForm() {
        String address = mAddress;
        String amount = etAmount.getText().toString().trim();
        String fee = etFee.getText().trim();

        IncreasePower budget = new IncreasePower();
        budget.setAddress(address);
        budget.setBudget(amount);
        budget.setFee(amount);
        budget.setFee(fee);

        Wallet.validateTxBudget(etFee, budget, new LogicObserver<Boolean>() {
            @Override
            public void handleData(Boolean isSuccess) {
                if(isSuccess){
                    showSureDialog(budget);
                }
            }
        });
    }

    private void showSureDialog(IncreasePower budget) {
        String amount = etAmount.getText().toString().trim();
        View view = LinearLayout.inflate(this, R.layout.view_dialog_send, null);
        mViewHolder = new ViewHolder(view);
        mViewHolder.tvToAddress.setText(budget.getAddress());
        mViewHolder.tvToAmount.setText(amount);
        mViewHolder.tvToAmountTitle.setText(R.string.send_budget);
        mViewHolder.tvToMemo.setText(R.string.tx_increase);
        loadTransExpiryView();
        new CommonDialog.Builder(this)
                .setContentView(view)
                .setNegativeButton(R.string.send_dialog_no, (dialog, which) -> {
                    dialog.cancel();
                    mViewHolder = null;
                })
                .setPositiveButton(R.string.send_dialog_yes, (dialog, which) -> {
                    dialog.cancel();
                    mViewHolder = null;
                    handleSendBudget(budget);
                })
                .create().show();

    }

    private void loadTransExpiryView() {
        if(mViewHolder == null || mViewHolder.tvTransExpiry == null){
            return;
        }
        String transExpiry = getText(R.string.send_transaction_expiry).toString();
        transExpiry = String.format(transExpiry, UserUtil.getTransExpiryBlock(), UserUtil.getTransExpiryTime());
        mViewHolder.tvTransExpiry.setText(Html.fromHtml(transExpiry));
    }

    private LogicObserver<Boolean> sendLogicObserver = new LogicObserver<Boolean>() {
        @Override
        public void handleData(Boolean isSuccess) {
            ProgressManager.closeProgressDialog();
            if(isSuccess){
                ToastUtils.showShortToast(R.string.send_txs_sending);
                // clear all editText data
                clearAllForm();
            }
        }
    };

    private void handleSendBudget(IncreasePower budget) {
        mTxPresenter.handleSendBudget(budget, sendLogicObserver);
    }
    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent object) {
        if (object == null) {
            return;
        }
        switch (object.getCode()) {
            case CLEAR_SEND:
                clearAllForm();
                break;
        }
    }

    private void clearAllForm() {
        etAmount.getText().clear();
        MiningUtil.initSenderTxFee(etFee);
    }

    private void showSoftInput() {
        etFee.setText(etFee.getText());
        resetViewFocus(etFee.getEditText());
        KeyboardUtils.showSoftInput(etFee.getEditText());
    }

    @Override
    protected void onDestroy() {
        if(KeyboardUtils.isSoftInputVisible(this)){
            KeyboardUtils.hideSoftInput(this);
            // handler InputMethodManager Leak
            FixMemLeak.fixLeak(this);
        }
        KeyboardUtils.unregisterSoftInputChangedListener(this);
        super.onDestroy();
    }

    class ViewHolder {
        @BindView(R.id.tv_to_address)
        TextView tvToAddress;
        @BindView(R.id.tv_to_amount)
        TextView tvToAmount;
        @BindView(R.id.tv_to_amount_title)
        TextView tvToAmountTitle;
        @BindView(R.id.tv_to_Memo)
        TextView tvToMemo;
        @BindView(R.id.tv_trans_expiry)
        TextView tvTransExpiry;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }

        @OnClick(R.id.tv_trans_expiry)
        void onClick(){
            Intent intent = new Intent(IncreaseActivity.this, SettingActivity.class);
            startActivityForResult(intent, 100);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        loadTransExpiryView();
    }
}