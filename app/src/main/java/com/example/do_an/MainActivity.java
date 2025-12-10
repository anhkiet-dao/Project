package com.example.do_an;

import android.os.Bundle;

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
    private Fragment discoverFragment;
    private FragmentManager fm = getSupportFragmentManager();

    private static final int FRAGMENT_CONTAINER_ID = R.id.fragment_container;

    public interface ResettableFragment {
        void resetState();
    }

    public interface NavigationListener {
        void setBottomNavVisibility(int visibility);
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

        // ⭐ Bước 1: Khởi tạo và Quản lý Fragment bằng Attach/Detach ⭐
        if (savedInstanceState == null) {
            myListFragment = new MyListFragment();
            accountFragment = new AccountFragment();
            discoverFragment = new HomeFragment();

            fm.beginTransaction()
                    // Thêm tất cả Fragment
                    .add(FRAGMENT_CONTAINER_ID, accountFragment, "nav_profile")
                    .add(FRAGMENT_CONTAINER_ID, myListFragment, "nav_read")
                    .add(FRAGMENT_CONTAINER_ID, discoverFragment, "nav_home")
                    // Detach (giống như Hide nhưng ổn định hơn)
                    .detach(accountFragment)
                    .detach(myListFragment)
                    // Mặc định hiển thị Home
                    .commit();

            activeFragment = discoverFragment;

        } else {
            // Trường hợp quay lại sau khi bị Kill Process
            myListFragment = fm.findFragmentByTag("nav_read");
            accountFragment = fm.findFragmentByTag("nav_profile");
            discoverFragment = fm.findFragmentByTag("nav_home");

            // Tìm Fragment đang được attach để đặt làm activeFragment
            if (discoverFragment != null && discoverFragment.isAdded() && !discoverFragment.isDetached()) {
                activeFragment = discoverFragment;
            } else if (myListFragment != null && myListFragment.isAdded() && !myListFragment.isDetached()) {
                activeFragment = myListFragment;
            } else if (accountFragment != null && accountFragment.isAdded() && !accountFragment.isDetached()) {
                activeFragment = accountFragment;
            }
        }

        // ⭐ Bước 2: Khởi tạo lần đầu sau khi Attach/Detach ⭐
        // Sau khi add/detach, cần attach fragment mặc định
        if (savedInstanceState == null) {
            fm.beginTransaction().attach(discoverFragment).commit();
        }

        // ⭐ Bước 3: Cập nhật Listener (Đã sửa lỗi Pop Back Stack) ⭐
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment targetFragment = null;

            if (itemId == R.id.nav_read) {
                targetFragment = myListFragment;
            } else if (itemId == R.id.nav_profile) {
                targetFragment = accountFragment;
            } else if (itemId == R.id.nav_home) {
                targetFragment = discoverFragment;
            }

            if (targetFragment != null) {

                // 1. Xử lý PopBackStack khi rời AccountFragment (CẦN XỬ LÝ TRƯỚC)
                // Nếu activeFragment là AccountFragment VÀ targetFragment KHÔNG phải là AccountFragment
                if (activeFragment instanceof AccountFragment && activeFragment != targetFragment) {
                    // Nếu AccountFragment đang có Back Stack (các Fragment con), xóa chúng đi.
                    // Lệnh này phải chạy trước khi detach activeFragment
                    // Tuy nhiên, việc xóa Back Stack có thể làm Fragment Manager bị lỗi
                    // Tôi sẽ comment lại lệnh này để kiểm tra xem nó có gây crash không.
                    // Nếu không crash, bạn có thể uncomment nó TÙY THUỘC vào cấu trúc Fragment con

                    // GỌI PHƯƠNG THỨC RESET CỦA FRAGMENT CON trước khi detach (An toàn hơn)
                    if (activeFragment instanceof AccountFragment) {
                        ((AccountFragment) activeFragment).resetToMainScreen();
                    }

                    // Hầu hết crash do popBackStackImmediate(null, ...) nên ta cần tránh dùng nó
                    // trừ khi cần xóa Stack của Fragment con:
                    // activeFragment.getChildFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }

                // 2. Trường hợp bấm lại tab (Reset State)
                if (activeFragment == targetFragment) {
                    if (targetFragment instanceof AccountFragment) {
                        ((AccountFragment) targetFragment).resetToMainScreen();
                    } else if (targetFragment instanceof ResettableFragment) {
                        ((ResettableFragment) targetFragment).resetState();
                    }
                    return true;
                }

                // 3. Chuyển Fragment
                showFragment(targetFragment);

                return true;
            }
            return false;
        });

        // ⭐ Bước 4: Thiết lập Fragment mặc định ban đầu ⭐
        if (savedInstanceState == null) {
            // Mặc định chọn Home (discoverFragment)
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    // ⭐ Bước 5: Sửa phương thức showFragment để dùng Attach/Detach (Đã fix lỗi Add lại) ⭐
    private void showFragment(Fragment fragmentToShow) {
        if (activeFragment == null || activeFragment == fragmentToShow) return;

        FragmentTransaction transaction = fm.beginTransaction();

        // 1. Detach Fragment cũ
        if (activeFragment.isAdded()) {
            transaction.detach(activeFragment);
        }

        // 2. Attach Fragment mới
        if (fragmentToShow.isDetached() && fragmentToShow.isAdded()) {
            // Fragment tồn tại và chỉ bị detached -> Attach lại
            transaction.attach(fragmentToShow);
        } else if (!fragmentToShow.isAdded()) {
            // Fragment bị remove khỏi FM (do PopBackStack hoặc replace ở nơi khác) -> Add lại
            // CẦN TẠO MỚI (nếu bị mất) HOẶC SỬ DỤNG LẠI (nếu có tham chiếu)

            // ⭐ Vấn đề: Các biến myListFragment, accountFragment, discoverFragment
            // đang giữ tham chiếu cũ. Khi chúng bị xóa khỏi FM, ta phải tạo Fragment mới

            // Tạm thời, giả định rằng các Fragment chính luôn còn tham chiếu và chỉ bị detach,
            // trừ khi có Fragment đọc truyện dùng REPLACE.
            // Nếu bạn dùng REPLACE trong SeriesFragment, các Fragment chính này đã bị xóa.
            // Nếu bị xóa, bạn PHẢI tạo Fragment mới:

            Fragment newFragment;
            if (fragmentToShow instanceof MyListFragment) {
                newFragment = new MyListFragment();
                myListFragment = newFragment; // Cập nhật tham chiếu
            } else if (fragmentToShow instanceof AccountFragment) {
                newFragment = new AccountFragment();
                accountFragment = newFragment; // Cập nhật tham chiếu
            } else { // discoverFragment
                newFragment = new HomeFragment();
                discoverFragment = newFragment; // Cập nhật tham chiếu
            }

            transaction.add(FRAGMENT_CONTAINER_ID, newFragment, fragmentToShow.getTag());
            fragmentToShow = newFragment; // Đặt fragmentToShow = newFragment để activeFragment được cập nhật

        } else {
            // Trường hợp an toàn (chỉ attach nếu đã tồn tại)
            transaction.attach(fragmentToShow);
        }

        transaction.commit();
        activeFragment = fragmentToShow;
    }
}