package chat.tox.antox.fragments;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.astuetz.PagerSlidingTabStrip;
import com.balysv.materialripple.MaterialRippleLayout;

import chat.tox.antox.R;
import chat.tox.antox.activities.MainActivityJ;
import chat.tox.antox.data.State;
import chat.tox.antox.theme.ThemeManagerJ;
import scala.Option;

/**
 * Created by Nechypurenko on 16.02.2018.
 */

public class LeftPaneFragmentJ extends Fragment {

    private ViewPager pager;
    private PagerSlidingTabStrip tabs;

    private class LeftPagerAdapter extends FragmentPagerAdapter implements PagerSlidingTabStrip.CustomTabProvider {

        int[] ICONS = new int[]{R.drawable.ic_chat_white_24dp, R.drawable.ic_person_white_24dp};

        public LeftPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return "Recent";
                default: return "Contacts";
            }
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return new RecentFragmentJ();
                default: return new ContactsFragmentJ();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public View getCustomTabView(ViewGroup parent, int position) {
            //hack to center the image only for left pane
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            params.addRule(RelativeLayout.CENTER_VERTICAL);

            //disable the material ripple layout on pre-honeycomb devices
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                FrameLayout customTabLayout = (FrameLayout) LayoutInflater.from(getActivity()).inflate(R.layout.custom_tab_old, parent, false);
                ImageView imageView = (ImageView) customTabLayout.findViewById(R.id.image);
                imageView.setImageResource(ICONS[position]);
                imageView.setLayoutParams(params);
                return customTabLayout;
            } else {
                MaterialRippleLayout materialRippleLayout = (MaterialRippleLayout) LayoutInflater.from(getActivity()).inflate(R.layout.custom_tab, parent, false);
                ImageView imageView = (ImageView) materialRippleLayout.findViewById(R.id.image);
                imageView.setImageResource(ICONS[position]);
                imageView.setLayoutParams(params);
                return materialRippleLayout;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pager != null){
            outState.putInt("tab_position", pager.getCurrentItem());
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        MainActivityJ thisActivity = (MainActivityJ) getActivity();
        ActionBar actionBar = thisActivity.getSupportActionBar();
        ThemeManagerJ.applyTheme(getActivity(), actionBar);

        View rootView = inflater.inflate(R.layout.fragment_leftpane, container, false);
        pager = (ViewPager) rootView.findViewById(R.id.pager);
        tabs = (PagerSlidingTabStrip) rootView.findViewById(R.id.pager_tabs);
        pager.setAdapter(new LeftPagerAdapter(getFragmentManager()));

        int recentViewPagerTab = 0;
        int contactsViewPagerTab = 1;

        int tab_position;
        if(savedInstanceState == null) {
            if (State.db().getMessageList(Option.empty(), -1).isEmpty()) {
                tab_position = contactsViewPagerTab;
            } else {
                tab_position = recentViewPagerTab;
            }
        } else {
            tab_position = savedInstanceState.getInt("tab_position", contactsViewPagerTab);
        }
        pager.setCurrentItem(tab_position);
        tabs.setViewPager(pager);
        tabs.setBackgroundColor(ThemeManagerJ.primaryColor());

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(tabs != null) {
            tabs.setBackgroundColor(ThemeManagerJ.primaryColor());
        }
    }
}
