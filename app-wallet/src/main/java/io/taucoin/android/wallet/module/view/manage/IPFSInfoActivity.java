package io.taucoin.android.wallet.module.view.manage;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.interop.IpfsHomeNodeInfo;
import io.taucoin.android.interop.IpfsPeerInfo;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.util.ProgressManager;
import io.taucoin.android.wallet.util.ResourcesUtil;

public class IPFSInfoActivity extends BaseActivity {

    @BindView(R.id.list_view_help)
    ListView listViewHelp;
    @BindView(R.id.refresh_layout)
    SmartRefreshLayout refreshLayout;
    @BindView(R.id.tv_version)
    TextView tvVersion;
    @BindView(R.id.tv_id)
    TextView tvId;
    @BindView(R.id.tv_total_peers)
    TextView tvTotalPeers;

    private PeersAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ipfs_info);
        ButterKnife.bind(this);
        initView();
        ProgressManager.showProgressDialog(this);
        getData();
    }

    private void getData() {
        if(!MyApplication.getRemoteConnector().isInit()){
            ProgressManager.closeProgressDialog();
        }
       MyApplication.getRemoteConnector().getIpfsPeers();
       MyApplication.getRemoteConnector().getHomeNodeInfo();
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent object) {
        if (object == null || object.getData() == null) {
            return;
        }
        switch (object.getCode()) {
            case PEERS_LIST:
                loadPeerData(object.getData());
                break;
            case HOME_NODE:
                loadHomeNodeData(object.getData());
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPeerData(Object data) {
        ProgressManager.closeProgressDialog();
        List<IpfsPeerInfo> peers = (List<IpfsPeerInfo>) data;
        if(peers == null || mAdapter == null || refreshLayout == null){
            return;
        }
        mAdapter.setListData(peers);
        refreshLayout.finishRefresh(100);
        loadPeerView(peers.size());
    }

    private void loadHomeNodeData(Object data) {
        ProgressManager.closeProgressDialog();
        IpfsHomeNodeInfo homeNode = (IpfsHomeNodeInfo) data;
        if(homeNode != null && tvId != null && tvVersion != null){
            tvId.setText(homeNode.getId());
            String version = ResourcesUtil.getText(R.string.ipfs_go_ipfs_version);
            version = String.format(version, homeNode.getVersion());
            tvVersion.setText(version);
        }
    }

    private void initView() {
        refreshLayout.setOnRefreshListener(this);
        mAdapter = new PeersAdapter();
        listViewHelp.setAdapter(mAdapter);
        loadPeerView(0);
    }

    private void loadPeerView(int total) {
        if(tvTotalPeers == null){
            return;
        }
        String totalPeers = ResourcesUtil.getText(R.string.ipfs_all_peers);
        totalPeers = String.format(totalPeers, total);
        tvTotalPeers.setText(totalPeers);
    }

    @Override
    public void onRefresh(RefreshLayout refreshlayout) {
        getData();
    }
}