package io.taucoin.android.wallet.module.view.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseFragment;

/**
 *
 * Forum information display list page
 *
 * */
public class FollowFragment extends BaseFragment {

    @Override
    public View getViewLayout(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_follow, container, false);
    }
}