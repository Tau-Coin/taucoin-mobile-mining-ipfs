package io.taucoin.android.wallet.module.view.manage;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.module.presenter.UserPresenter;
import io.taucoin.android.wallet.module.view.manage.iview.IImportKeyView;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.foundation.util.DrawablesUtil;
import io.taucoin.foundation.util.StringUtil;
import io.taucoin.platform.adress.Key;
import io.taucoin.platform.adress.KeyManager;

public class ImportKeyActivity extends BaseActivity implements IImportKeyView {

    @BindView(R.id.et_private_key)
    EditText etPrivateKey;
    @BindView(R.id.btn_import)
    Button btnImport;
    @BindView(R.id.btn_generate)
    Button btnGenerate;
    @BindView(R.id.tv_how_import)
    TextView tvHowImport;

    private UserPresenter mUserPresenter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_key);
        ButterKnife.bind(this);
        DrawablesUtil.setUnderLine(tvHowImport);
        mUserPresenter = new UserPresenter(this);
    }

    @OnClick(R.id.btn_import)
    public void onBtnImportClicked() {
        String privateKey = etPrivateKey.getText().toString().trim();
        if(StringUtil.isEmpty(privateKey)){
            ToastUtils.showShortToast(R.string.keys_private_invalid);
            return;
        }
        Key key = KeyManager.validateKey(privateKey);
        if(key == null){
            ToastUtils.showShortToast(R.string.keys_private_invalid);
        }else{
            KeyValue keyValue = new KeyValue();
            keyValue.setPriKey(privateKey);
            keyValue.setPubKey(key.getPubKey());
            keyValue.setAddress(key.getAddress());
            keyValue.setRawAddress(key.getRawAddress());
            mUserPresenter.showSureDialog(this, keyValue);
        }
    }
    @OnClick(R.id.tv_how_import)
    public void onHowImportClicked() {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(TransmitKey.TITLE, getText(R.string.manager_help));
        intent.putExtra(TransmitKey.URL, TransmitKey.ExternalUrl.HOW_IMPORT_KEY_URL);
        startActivity(intent);
    }

    @OnClick(R.id.btn_generate)
    public void onBtnGenerateClicked() {
        mUserPresenter.showSureDialog(this);
    }

    @Override
    public void gotoKeysActivity() {
        Intent intent = new Intent(this, KeysActivity.class);
        startActivity(intent);
        this.finish();
    }
}