package io.taucoin.android_app;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;


public class TabsPagerAdapter extends FragmentPagerAdapter {

    int tabsCount;
    private String[] tabTitles = { "Console", "Tests" };

    public TabsPagerAdapter(FragmentManager fragmentManager) {

        super(fragmentManager);
        this.tabsCount = tabTitles.length;
    }

    @Override
    public Fragment getItem(int index) {

        switch (index) {
            case 0:
                ConsoleFragment consoleFragment = new ConsoleFragment();
                return consoleFragment;
            case 1:
                TestsFragment testsFragment = new TestsFragment();
                return testsFragment;
        }

        return null;
    }

    @Override
    public CharSequence getPageTitle(int position) {

        return tabTitles[position];
    }


    @Override
    public int getCount() {

        return tabsCount;
    }
}
