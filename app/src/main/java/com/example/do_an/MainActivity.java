package com.example.do_an;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction; // Thêm import này
import com.example.do_an.UI.AccountFragment;
import com.example.do_an.UI.MyListFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    private Fragment activeFragment;
    private Fragment readFragment;
    private Fragment profileFragment;

    private FragmentManager fm = getSupportFragmentManager();

    private static final int FRAGMENT_CONTAINER_ID = R.id.fragment_container;

    // --- Khai báo Interface Reset Chung ---
    public interface ResettableFragment {
        void resetState();
    }
    // ------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

                // --- LOGIC XỬ LÝ BACK STACK KHI CHUYỂN TAB ---
                if (activeFragment != targetFragment) {
                    // Nếu Fragment hiện tại là AccountFragment VÀ nó có Fragment con đang hiển thị
                    // thì phải xóa Back Stack của Activity trước khi chuyển tab
                    if (activeFragment instanceof AccountFragment) {
                        // Xóa tất cả các Fragment con đang có trong Back Stack của Activity
                        // Điều này sẽ khiến giao diện trở về AccountFragment gốc trước khi bị ẩn
                        getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    }
                }
                // -----------------------------------------------

                // --- Logic Xử lý Nhấp Lại (Clicking on the active tab) ---
                if (activeFragment == targetFragment) {
                    if (targetFragment instanceof AccountFragment) {
                        ((AccountFragment) targetFragment).resetToMainScreen();
                    } else if (targetFragment instanceof ResettableFragment) {
                        ((ResettableFragment) targetFragment).resetState();
                    }
                    return true;
                }
                // -----------------------------------------------------------

                // Nếu là Fragment mới, chuyển Fragment như bình thường
                showFragment(targetFragment);
                return true;
            }
            return false;
        });

        // Đảm bảo mục trên BottomNav được chọn đúng với activeFragment
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
            // Khai báo rõ ràng FragmentTransaction
            FragmentTransaction transaction = fm.beginTransaction(); // Đã sửa lỗi FragmentTransaction

            transaction.hide(activeFragment);
            transaction.show(fragmentToShow);

            transaction.commit();

            activeFragment = fragmentToShow;
        }
    }
}