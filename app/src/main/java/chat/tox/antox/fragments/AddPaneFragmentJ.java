package chat.tox.antox.fragments;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;
import com.balysv.materialripple.MaterialRippleLayout;

import chat.tox.antox.R;
import chat.tox.antox.pager.BetterFragmentPagerAdapter;
import chat.tox.antox.theme.ThemeManagerJ;

/**
 * Created by Nechypurenko on 08.02.2018.
 */

public class AddPaneFragmentJ extends Fragment {

    private ViewPager pager;
    private PagerSlidingTabStrip tabs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_pane, container, false);
        pager = (ViewPager) rootView.findViewById(R.id.pager);
        tabs = (PagerSlidingTabStrip) rootView.findViewById(R.id.pager_tabs);

        pager.setAdapter(new AddPagerAdapter(getFragmentManager()));
        tabs.setViewPager(pager);
        tabs.setBackgroundColor(ThemeManagerJ.primaryColor());
        //tabs.setIndicatorColor(ThemeManagerJ.primaryColorDark());
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(tabs != null) {
            tabs.setBackgroundColor(ThemeManagerJ.primaryColor());
            //tabs.setIndicatorColor(ThemeManagerJ.primaryColorDark());
        }
    }

    public Fragment getSelectedFragment() {
        return ((AddPagerAdapter)pager.getAdapter()).getActiveFragment(pager, pager.getCurrentItem());
    }

    private class AddPagerAdapter extends BetterFragmentPagerAdapter implements PagerSlidingTabStrip.CustomTabProvider {

        private int[] ICONS = new int[] {
                R.drawable.ic_person_add_white_24dp,
                R.drawable.ic_group_add_white_24dp
        };
        private String[] LABELS = new String[] {
                getString(R.string.addpane_friend_label),
                getString(R.string.addpane_group_label)
        };


        public AddPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new AddFriendFragmentJ();
                case 1: //def
                    return new AddGroupFragmentJ();
            }
            return null;
        }


        @Override
        public int getCount() {
            return 2;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return LABELS[position];
        }


        @Override
        public View getCustomTabView(ViewGroup parent, int position) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                FrameLayout customTabLayout  = (FrameLayout) LayoutInflater.from(getActivity()).inflate(R.layout.custom_tab_old, parent, false);
                ((ImageView)customTabLayout.findViewById(R.id.image)).setImageResource(ICONS[position]);
                ((TextView)customTabLayout.findViewById(R.id.text)).setText(LABELS[position]);
                return customTabLayout;
            } else {
                MaterialRippleLayout materialRippleLayout = (MaterialRippleLayout) LayoutInflater.from(getActivity()).inflate(R.layout.custom_tab, parent, false);
                ((ImageView)materialRippleLayout.findViewById(R.id.image)).setImageResource(ICONS[position]);
                ((TextView)materialRippleLayout.findViewById(R.id.text)).setText(LABELS[position]);
                return materialRippleLayout;
            }
        }

    }
}
