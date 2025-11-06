package com.example.do_an.application;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.util.Log;

import com.example.do_an.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    EditText edtTenTruyen;
    Button btnTimKiem;
    RecyclerView recyclerTruyen;
    TruyenAdapter adapter;
    List<Truyen> danhSachTruyen = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        edtTenTruyen = findViewById(R.id.edtTenTruyen);
        btnTimKiem = findViewById(R.id.btnTimKiem);
        recyclerTruyen = findViewById(R.id.recyclerTruyen);

        recyclerTruyen.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TruyenAdapter(danhSachTruyen);
        recyclerTruyen.setAdapter(adapter);

        btnTimKiem.setOnClickListener(v -> {
            String ten = edtTenTruyen.getText().toString().trim();
            if (ten.isEmpty()) {
                Toast.makeText(SearchActivity.this, "Vui lòng nhập tên truyện!", Toast.LENGTH_SHORT).show();
                return;
            }
            new TimTruyenTask().execute(ten);
        });
    }

    private class TimTruyenTask extends AsyncTask<String, Void, List<Truyen>> {
        @Override
        protected void onPreExecute() {
            Toast.makeText(SearchActivity.this, "Đang tìm truyện...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<Truyen> doInBackground(String... params) {
            String query = params[0].replace(" ", "+");
            List<Truyen> list = new ArrayList<>();
            try {
                String urlSearch = "https://www.nettruyen.com/tim-truyen?q=" + query;
                Document doc = Jsoup.connect(urlSearch)
                        .userAgent("Mozilla/5.0")
                        .get();

                Elements items = doc.select("div.list-truyen-item");
                for (Element item : items) {
                    String ten = item.select("h3.truyen-title > a").text();
                    String link = item.select("h3.truyen-title > a").attr("href");
                    String tacgia = item.select("p.author").text();
                    String theloai = item.select("p.new-chap").text();

                    list.add(new Truyen(ten, tacgia, theloai, link));
                }

            } catch (Exception e) {
                Log.e("SearchActivity", "Lỗi tìm truyện: " + e.getMessage());
            }
            return list;
        }

        @Override
        protected void onPostExecute(List<Truyen> result) {
            danhSachTruyen.clear();
            if (result.isEmpty()) {
                Toast.makeText(SearchActivity.this, "Không tìm thấy truyện.", Toast.LENGTH_SHORT).show();
                return;
            }
            danhSachTruyen.addAll(result);
            adapter.notifyDataSetChanged();
        }
    }

    public static class Truyen {
        private final String ten, tacgia, theloai, link;

        public Truyen(String ten, String tacgia, String theloai, String link) {
            this.ten = ten;
            this.tacgia = tacgia;
            this.theloai = theloai;
            this.link = link;
        }

        public String getTen() { return ten; }
        public String getTacgia() { return tacgia; }
        public String getTheloai() { return theloai; }
        public String getLink() { return link; }
    }

    public class TruyenAdapter extends RecyclerView.Adapter<TruyenAdapter.ViewHolder> {

        private final List<Truyen> danhSach;

        public TruyenAdapter(List<Truyen> danhSach) { this.danhSach = danhSach; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_truyen, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Truyen t = danhSach.get(position);
            holder.txtTenTruyen.setText("📘 " + t.getTen());
            holder.txtTacGia.setText("👤 " + t.getTacgia());
            holder.txtTheLoai.setText("📚 " + t.getTheloai());

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ReadActivity.class);
                intent.putExtra("tenTruyen", t.getTen());
                intent.putExtra("urlTruyen", t.getLink());
                v.getContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return danhSach.size(); }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtTenTruyen, txtTacGia, txtTheLoai;
            public ViewHolder(View itemView) {
                super(itemView);
                txtTenTruyen = itemView.findViewById(R.id.txtTenTruyen);
                txtTacGia = itemView.findViewById(R.id.txtTacGia);
                txtTheLoai = itemView.findViewById(R.id.txtTheLoai);
            }
        }
    }
}
