package com.example.do_an;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.do_an.presentation.common.AccountFragment;
import com.example.do_an.presentation.common.MyListFragment;
import com.example.do_an.presentation.library.home.HomeFragment;
import com.example.do_an.presentation.library.search.SearchFragment;
import com.example.do_an.presentation.reading.reader.ReadFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements ReadFragment.NavigationListener {

    private BottomNavigationView bottomNavigation;
    private Fragment fragmentActive;
    private Fragment fragmentMyList;
    private Fragment fragmentAccount;
    private Fragment fragmentHome;
    private Fragment fragmentSearch;

    private FragmentManager fragmentManager;
    private static final int FRAGMENT_CONTAINER_ID = R.id.fragmentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_activity_main);

        bindViews();
        initDependencies();
        setupUi(savedInstanceState);
        bindActions();
    }

    // =========================================================
    // 1️⃣ Setup phase
    // =========================================================

    private void bindViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void initDependencies() {
        fragmentManager = getSupportFragmentManager();
    }

    private void setupUi(Bundle savedInstanceState) {
        setupFragments(savedInstanceState);
    }

    private void bindActions() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment targetFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_read)
                targetFragment = fragmentMyList;
            else if (itemId == R.id.nav_profile)
                targetFragment = fragmentAccount;
            else if (itemId == R.id.nav_home)
                targetFragment = fragmentHome;
            else if (itemId == R.id.nav_search)
                targetFragment = fragmentSearch;

            if (targetFragment == null)
                return false;

            clearBackStack();

            if (fragmentActive == targetFragment) {
                if (targetFragment instanceof AccountFragment) {
                    ((AccountFragment) targetFragment).resetToMainScreen();
                }
                return true;
            }

            switchFragment(targetFragment);
            return true;
        });
    }

    // =========================================================
    // 2️⃣ UI helpers
    // =========================================================

    private void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            createFragments();
            addFragmentsToContainer();
            setDefaultFragment();
        } else {
            restoreFragments();
        }
    }

    private void createFragments() {
        fragmentHome = new HomeFragment();
        fragmentMyList = new MyListFragment();
        fragmentAccount = new AccountFragment();
        fragmentSearch = new SearchFragment();
    }

    private void addFragmentsToContainer() {
        fragmentManager.beginTransaction()
                .add(FRAGMENT_CONTAINER_ID, fragmentHome, "nav_home")
                .add(FRAGMENT_CONTAINER_ID, fragmentMyList, "nav_read").hide(fragmentMyList)
                .add(FRAGMENT_CONTAINER_ID, fragmentAccount, "nav_profile").hide(fragmentAccount)
                .add(FRAGMENT_CONTAINER_ID, fragmentSearch, "nav_search").hide(fragmentSearch)
                .commit();
    }

    private void setDefaultFragment() {
        fragmentActive = fragmentHome;
        bottomNavigation.setSelectedItemId(R.id.nav_home);
    }

    private void restoreFragments() {
        fragmentHome = fragmentManager.findFragmentByTag("nav_home");
        fragmentMyList = fragmentManager.findFragmentByTag("nav_read");
        fragmentAccount = fragmentManager.findFragmentByTag("nav_profile");
        fragmentSearch = fragmentManager.findFragmentByTag("nav_search");

        if (fragmentHome != null && !fragmentHome.isHidden())
            fragmentActive = fragmentHome;
        else if (fragmentMyList != null && !fragmentMyList.isHidden())
            fragmentActive = fragmentMyList;
        else if (fragmentSearch != null && !fragmentSearch.isHidden())
            fragmentActive = fragmentSearch;
        else
            fragmentActive = fragmentAccount;
    }

    // =========================================================
    // 3️⃣ Fragment operations
    // =========================================================

    private void switchFragment(Fragment fragmentToShow) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.hide(fragmentActive);
        transaction.show(fragmentToShow);
        transaction.commit();
        fragmentActive = fragmentToShow;
    }

    private void clearBackStack() {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    // =========================================================
    // 4️⃣ Interface implementations
    // =========================================================

    @Override
    public void setBottomNavVisibility(int visibility) {
        if (bottomNavigation != null)
            bottomNavigation.setVisibility(visibility);
    }

    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}