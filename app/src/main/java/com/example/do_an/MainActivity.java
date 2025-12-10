package com.example.do_an;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.do_an.UI.AccountFragment;
import com.example.do_an.UI.MyListFragment;
import com.example.do_an.UI.ReadFragment;
import com.example.do_an.UI.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements ReadFragment.NavigationListener {

    private BottomNavigationView bottomNav;
    private Fragment activeFragment;
    private Fragment myListFragment;
    private Fragment accountFragment;
    private Fragment discoverFragment; // Home fragment
    private final FragmentManager fm = getSupportFragmentManager();

    private static final int FRAGMENT_CONTAINER_ID = R.id.fragment_container;

    public interface ResettableFragment {
        void resetState();
    }

    @Override
    public void setBottomNavVisibility(int visibility) {
        if (bottomNav != null) {
            bottomNav.setVisibility(visibility);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_activity_main);

        bottomNav = findViewById(R.id.bottomNav);

        if (savedInstanceState == null) {
            discoverFragment = new HomeFragment();
            myListFragment = new MyListFragment();
            accountFragment = new AccountFragment();

            fm.beginTransaction()
                    .add(FRAGMENT_CONTAINER_ID, discoverFragment, "nav_home")
                    .add(FRAGMENT_CONTAINER_ID, myListFragment, "nav_read").hide(myListFragment)
                    .add(FRAGMENT_CONTAINER_ID, accountFragment, "nav_profile").hide(accountFragment)
                    .commit();

            activeFragment = discoverFragment;
        } else {
            discoverFragment = fm.findFragmentByTag("nav_home");
            myListFragment = fm.findFragmentByTag("nav_read");
            accountFragment = fm.findFragmentByTag("nav_profile");

            if (discoverFragment != null && !discoverFragment.isHidden()) activeFragment = discoverFragment;
            else if (myListFragment != null && !myListFragment.isHidden()) activeFragment = myListFragment;
            else activeFragment = accountFragment;
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment targetFragment = null;

            if (item.getItemId() == R.id.nav_read) targetFragment = myListFragment;
            else if (item.getItemId() == R.id.nav_profile) targetFragment = accountFragment;
            else if (item.getItemId() == R.id.nav_home) targetFragment = discoverFragment;

            if (targetFragment == null) return false;

            clearBackStack();

            if (activeFragment == targetFragment) {
                if (targetFragment instanceof AccountFragment) {
                    ((AccountFragment) targetFragment).resetToMainScreen();
                } else if (targetFragment instanceof ResettableFragment) {
                    ((ResettableFragment) targetFragment).resetState();
                }
                return true;
            }

            switchFragment(targetFragment);
            return true;
        });

        if (savedInstanceState == null) bottomNav.setSelectedItemId(R.id.nav_home);
    }

    private void switchFragment(Fragment fragmentToShow) {
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.hide(activeFragment);
        transaction.show(fragmentToShow);
        transaction.commit();
        activeFragment = fragmentToShow;
    }

    private void clearBackStack() {
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    public void openFragment(Fragment fragment) {
        fm.beginTransaction()
                .hide(activeFragment) // Ẩn tab đang dùng
                .add(FRAGMENT_CONTAINER_ID, fragment) // Thêm màn phụ lên trên
                .addToBackStack(null)
                .commit();
    }
}
