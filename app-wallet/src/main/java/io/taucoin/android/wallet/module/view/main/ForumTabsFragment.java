package io.taucoin.android.wallet.module.view.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.OnClick;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseFragment;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.module.view.forum.TopicAddActivity;
import io.taucoin.android.wallet.module.view.forum.TopicSearchActivity;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.ResourcesUtil;

/**
 *
 * Forum tabs page
 *
 * */
public class ForumTabsFragment extends BaseFragment {

    @BindView(R.id.tab_layout)
    TabLayout tabLayout;
    @BindView(R.id.view_pager)
    ViewPager viewPager;
    private List<Fragment> fragmentList = new ArrayList<>();
    private int[] titles = new int[]{R.string.forum_home, R.string.forum_bookmark, R.string.forum_follow};

    @Override
    public View getViewLayout(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forum_tabs, container, false);
        butterKnifeBinder(this, view);
        initView();
        return view;
    }

    private void initView() {
        fragmentList.add(new ForumFragment());
        Fragment bookmark = new ForumFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(TransmitKey.FORUM_BOOKMARK, 1);
        bookmark.setArguments(bundle);
        fragmentList.add(bookmark);
        fragmentList.add(new FollowFragment());
        FragmentManager fragmentManager = this.getFragmentManager();
        MyAdapter fragmentAdapter = new MyAdapter(fragmentManager);
        viewPager.setAdapter(fragmentAdapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    @OnClick({R.id.iv_create, R.id.ll_search_bar})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_create:
                ActivityUtil.startActivity(getActivity(), TopicAddActivity.class);
                break;
            case R.id.ll_search_bar:
                ActivityUtil.startActivity(getActivity(), TopicSearchActivity.class);
                break;
            default:
                break;
        }
    }

    public class MyAdapter extends FragmentPagerAdapter {
        MyAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return ResourcesUtil.getText(titles[position]);
        }
    }
}