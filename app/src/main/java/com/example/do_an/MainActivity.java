package com.example.do_an;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.do_an.main.AccountFragment;
import com.example.do_an.main.MyListFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    private Fragment activeFragment;
    private Fragment readFragment;
    private Fragment profileFragment;

    // Initialize fm in onCreate() to avoid calling getSupportFragmentManager() before Activity is created
    private FragmentManager fm;

    private static final int FRAGMENT_CONTAINER_ID = R.id.fragment_container;

    // --- Khai báo Interface Reset Chung ---
    public interface ResettableFragment {
        void resetState();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize FragmentManager here (safe lifecycle point)
        fm = getSupportFragmentManager();

        bottomNav = findViewById(R.id.bottomNav);

        if (savedInstanceState == null) {
            readFragment = new MyListFragment();
            profileFragment = new AccountFragment();

            fm.beginTransaction()
                    .add(FRAGMENT_CONTAINER_ID, profileFragment, "nav_profile")
                    .hide(profileFragment)
                    .add(FRAGMENT_CONTAINER_ID, readFragment, "nav_read")
                    .commit();

            activeFragment = readFragment;

        } else {
            readFragment = fm.findFragmentByTag("nav_read");
            profileFragment = fm.findFragmentByTag("nav_profile");

            if (readFragment != null && !readFragment.isHidden()) {
                activeFragment = readFragment;
            } else if (profileFragment != null && !profileFragment.isHidden()) {
                activeFragment = profileFragment;
            } else {
                activeFragment = readFragment;
            }
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment targetFragment = null;

            if (itemId == R.id.nav_read) {
                targetFragment = readFragment;
            } else if (itemId == R.id.nav_profile) {
                targetFragment = profileFragment;
            }

            if (targetFragment != null) {
                if (activeFragment != targetFragment) {
                    if (activeFragment instanceof AccountFragment) {
                        getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    }
                }

                if (activeFragment == targetFragment) {
                    if (targetFragment instanceof AccountFragment) {
                        ((AccountFragment) targetFragment).resetToMainScreen();
                    } else if (targetFragment instanceof ResettableFragment) {
                        ((ResettableFragment) targetFragment).resetState();
                    }
                    return true;
                }

                showFragment(targetFragment);
                return true;
            }
            return false;
        });

        if (activeFragment == profileFragment) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
        } else {
            bottomNav.setSelectedItemId(R.id.nav_read);
        }
    }

    private void showFragment(Fragment fragmentToShow) {
        if (activeFragment == null) {
            fm.beginTransaction().add(FRAGMENT_CONTAINER_ID, fragmentToShow).commit();
            activeFragment = fragmentToShow;
            return;
        }

        if (activeFragment != fragmentToShow) {
            FragmentTransaction transaction = fm.beginTransaction();

            transaction.hide(activeFragment);
            transaction.show(fragmentToShow);

            transaction.commit();

            activeFragment = fragmentToShow;
        }
    }
}