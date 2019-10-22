package io.taucoin.android.wallet.module.view.manage;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.module.presenter.AppPresenter;
import io.taucoin.android.wallet.module.view.manage.iview.IIpfsView;
import io.taucoin.android.wallet.util.ProgressManager;
import io.taucoin.android.wallet.util.ResourcesUtil;
import io.taucoin.ipfs.node.IpfsHomeNodeInfo;
import io.taucoin.ipfs.node.IpfsPeerInfo;

public class IPFSInfoActivity extends BaseActivity implements IIpfsView {

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
    private AppPresenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ipfs_info);
        ButterKnife.bind(this);
        mPresenter = new AppPresenter(this);
        initView();
        ProgressManager.showProgressDialog(this);
        getData();
    }

    private void getData() {
        mPresenter.getPeersList();
        mPresenter.getIpfsNode();
    }

    @Override
    public void loadPeerData(List<IpfsPeerInfo> peers) {
        ProgressManager.closeProgressDialog();
        if(peers == null || mAdapter == null || refreshLayout == null){
            return;
        }
        mAdapter.setListData(peers);
        refreshLayout.finishRefresh(100);
        loadPeerView(peers.size());
    }

    @Override
    public void loadHomeNodeData(IpfsHomeNodeInfo homeNode) {
        ProgressManager.closeProgressDialog();
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