package io.taucoin.android.wallet.module.view.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.naturs.logger.Logger;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.math.BigInteger;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseFragment;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.presenter.UserPresenter;
import io.taucoin.android.wallet.module.service.UpgradeService;
import io.taucoin.android.wallet.module.view.main.iview.IManageView;
import io.taucoin.android.wallet.module.view.manage.AddressBookActivity;
import io.taucoin.android.wallet.module.view.manage.HelpActivity;
import io.taucoin.android.wallet.module.view.manage.IPFSInfoActivity;
import io.taucoin.android.wallet.module.view.manage.ImportKeyActivity;
import io.taucoin.android.wallet.module.view.manage.KeysActivity;
import io.taucoin.android.wallet.module.view.manage.ProfileActivity;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.ForumUtil;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.android.wallet.util.ProgressManager;
import io.taucoin.android.wallet.util.SharedPreferencesHelper;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.android.wallet.widget.CommonDialog;
import io.taucoin.android.wallet.widget.InputDialog;
import io.taucoin.android.wallet.widget.ItemTextView;
import io.taucoin.android.wallet.widget.MultilineTextView;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.AppUtil;

public class ManageFragment extends BaseFragment implements IManageView {

    @BindView(R.id.tv_nick)
    TextView tvNick;
    @BindView(R.id.tv_version)
    TextView tvVersion;
    @BindView(R.id.version_upgrade)
    View versionUpgrade;
    @BindView(R.id.item_transaction_expiry)
    ItemTextView transactionExpiry;
    private UserPresenter mPresenter;

    @Override
    public View getViewLayout(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage, container, false);
        butterKnifeBinder(this, view);
        mPresenter = new UserPresenter();
        initView();
        loadTransExpiryView();
        return view;
    }

    @Override
    public void initView() {
        String versionName = getResources().getString(R.string.manager_version);
        versionName = String.format(versionName, AppUtil.getVersionName(getActivity()));
        tvVersion.setText(versionName);
        onEvent(EventBusUtil.getMessageEvent(MessageEvent.EventCode.ALL));
    }

    @OnClick({R.id.tv_nick, R.id.item_keys, R.id.item_address_book, R.id.item_help, R.id.tv_version,
            R.id.item_reset_data, R.id.item_p2p_exchange, R.id.item_mining_group, R.id.item_transaction_expiry,
            R.id.item_ipfs_info, R.id.item_change_name, R.id.item_mode_switched})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_nick:
                ActivityUtil.startActivity(getActivity(), ProfileActivity.class);
                break;
            case R.id.item_keys:
                ActivityUtil.startActivity(getActivity(), KeysActivity.class);
                break;
            case R.id.item_address_book:
                ActivityUtil.startActivity(getActivity(), AddressBookActivity.class);
                break;
            case R.id.item_help:
                ActivityUtil.startActivity(getActivity(), HelpActivity.class);
                break;
            case R.id.tv_version:
                ProgressManager.showProgressDialog(getActivity());
                Intent intent = new Intent();
                intent.putExtra(TransmitKey.ISSHOWTIP, true);
                UpgradeService.startUpdateService(intent);
                break;
            case R.id.item_reset_data:
                resetData();
                break;
            case R.id.item_p2p_exchange:
                ActivityUtil.openUri(getActivity(), TransmitKey.ExternalUrl.P2P_EXCHANGE);
                break;
            case R.id.item_mining_group:
                ActivityUtil.openUri(getActivity(), TransmitKey.ExternalUrl.MINING_GROUP);
                break;
            case R.id.item_transaction_expiry:
                showInputDialog();
                break;
            case R.id.item_ipfs_info:
                ActivityUtil.startActivity(getActivity(), IPFSInfoActivity.class);
                break;
            case R.id.item_change_name:
                showNewNameDialog();
                break;
            case R.id.item_mode_switched:
                showModeSwitchDialog((ItemTextView)view);
                break;
            default:
                break;
        }
    }

    @OnTouch({R.id.tv_nick, R.id.item_keys, R.id.item_address_book,
            R.id.item_reset_data, R.id.item_transaction_expiry,
            R.id.item_change_name, R.id.item_mode_switched})
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && !UserUtil.isImportKey()) {
            ActivityUtil.startActivity(getActivity(), ImportKeyActivity.class);
            return true;
        }
        return false;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            showUpgradeView();
        }
        UserUtil.setNickName(tvNick);
    }

    private void showUpgradeView() {
        boolean isUpgrade = SharedPreferencesHelper.getInstance().getBoolean(TransmitKey.UPGRADE, false);
        versionUpgrade.setVisibility(isUpgrade ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent object) {
        if (object == null) {
            return;
        }
        switch (object.getCode()) {
            case ALL:
            case NICKNAME:
                UserUtil.setNickName(tvNick);
                break;
            case UPGRADE:
                showUpgradeView();
                break;
            default:
                break;
        }
    }

    private void resetData() {
        if (UserUtil.isImportKey()) {
            View view = LinearLayout.inflate(getActivity(), R.layout.view_dialog_keys, null);
            TextView tvMsg = view.findViewById(R.id.tv_msg);
            tvMsg.setText(R.string.setting_reset_data_tips);
            new CommonDialog.Builder(getActivity())
                    .setContentView(view)
                    .setButtonWidth(240)
                    .setPositiveButton(R.string.send_dialog_yes, (dialog, which) -> {
                        dialog.cancel();
                        ProgressManager.showProgressDialog(getActivity());
                        MiningUtil.clearAndReloadBlocks(new LogicObserver<Boolean>() {
                            @Override
                            public void handleData(Boolean isSuccess) {
                                ProgressManager.closeProgressDialog();
                                if (isSuccess) {
                                    ToastUtils.showShortToast(R.string.setting_reset_data_success);
                                } else {
                                    ToastUtils.showShortToast(R.string.setting_reset_data_fail);
                                }
                            }
                        });
                    }).setNegativeButton(R.string.send_dialog_no, (dialog, which) -> dialog.cancel())
                    .create().show();
        }
    }


    private void showInputDialog() {
        int inputHint = R.string.setting_transaction_expiry_tip;
        new InputDialog.Builder(getActivity())
                .setInputType(InputType.TYPE_CLASS_NUMBER)
                .setInputHint(inputHint)
                .setNegativeButton(R.string.common_cancel, (InputDialog.InputDialogListener) (dialog, text) -> dialog.cancel())
                .setPositiveButton(R.string.common_done, (InputDialog.InputDialogListener) (dialog, text) -> {
                    try {
                        long input = new BigInteger(text).longValue();
                        if (input > TransmitKey.MAX_TRANS_EXPIRY || input < TransmitKey.MIN_TRANS_EXPIRY) {
                            String expiryLimit = getText(R.string.setting_transaction_expiry_limit).toString();
                            expiryLimit = String.format(expiryLimit, TransmitKey.MIN_TRANS_EXPIRY, TransmitKey.MAX_TRANS_EXPIRY);
                            ToastUtils.showShortToast(expiryLimit);
                            return;
                        }
                        mPresenter.saveTransExpiry(input, logicObserver);
                    } catch (Exception e) {
                        Logger.e("new BigInteger is error", e);
                    }
                    dialog.cancel();
                }).create().show();
    }

    private void showModeSwitchDialog(ItemTextView itemTextView) {
        View view = LinearLayout.inflate(getActivity(), R.layout.view_dialog_switch, null);
        boolean isFastModel = ForumUtil.isFastMiningModel();
        isFastModel = !isFastModel;
        int titleRid = isFastModel ? R.string.forum_switched_fast_title : R.string.forum_switched_full_node_title;
        int contentRid = isFastModel ? R.string.forum_switched_fast_content : R.string.forum_switched_full_node_content;
        ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.tvTitle.setText(titleRid);
        viewHolder.tvContent.setText(contentRid);
        new CommonDialog.Builder(getActivity())
                .setContentView(view)
                .setHorizontal()
                .setNegativeButton(R.string.common_back, (dialog, which) -> dialog.cancel())
                .setPositiveButton(R.string.common_yes, (dialog, which) -> {
                    dialog.cancel();
                    ForumUtil.switchMiningModel(itemTextView);
                }).create().show();
    }

    private void showNewNameDialog() {
        int inputHint = R.string.setting_new_name;
        new InputDialog.Builder(getActivity())
                .setInputType(InputType.TYPE_CLASS_TEXT)
                .setInputHint(inputHint)
                .setNegativeButton(R.string.common_back, (InputDialog.InputDialogListener) (dialog, text) -> dialog.cancel())
                .setPositiveButton(R.string.common_yes, (InputDialog.InputDialogListener) (dialog, text) -> {
                    ToastUtils.showShortToast(R.string.forum_change_name_successfully);
                    dialog.cancel();
                }).create().show();
    }

    private LogicObserver<KeyValue> logicObserver = new LogicObserver<KeyValue>() {
        @Override
        public void handleData(KeyValue keyValue) {
            MyApplication.setKeyValue(keyValue);
            loadTransExpiryView();
        }
    };

    private void loadTransExpiryView() {
        KeyValue keyValue = MyApplication.getKeyValue();
        if (keyValue != null) {
            transactionExpiry.setRightText(UserUtil.getTransExpiryTime() + "min");
        }
    }

    class ViewHolder {
        @BindView(R.id.tv_title)
        MultilineTextView tvTitle;
        @BindView(R.id.tv_content)
        TextView tvContent;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}