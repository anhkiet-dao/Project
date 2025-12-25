package com.example.do_an.presentation.profile.statistics;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.do_an.R;
import com.example.do_an.core.utils.Encryption;
import com.example.do_an.domain.profile.model.History;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.LineDataSet.Mode;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticFragment extends Fragment {
    private TextView tvTotalBooks, tvTotalHours;
    private RadioGroup rgTimeFrame;
    private RadioButton rbDaily, rbWeekly;
    private LinearLayout llDates;
    private HorizontalScrollView scrollDates;
    private LineChart lineChart;
    private BarChart barChart;
    private PieChart pieChart;
    private List<History> histories = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.statistic_fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTotalBooks = view.findViewById(R.id.tvTotalBooks);
        tvTotalHours = view.findViewById(R.id.tvTotalHours);
        lineChart = view.findViewById(R.id.lineChart);
        barChart = view.findViewById(R.id.barChart);
        pieChart = view.findViewById(R.id.pieChart);
        rgTimeFrame = view.findViewById(R.id.rgTimeFrame);
        rbDaily = view.findViewById(R.id.rbDaily);
        rbWeekly = view.findViewById(R.id.rbWeekly);
        llDates = view.findViewById(R.id.llDates);
        scrollDates = view.findViewById(R.id.scrollDates);

        rgTimeFrame.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbDaily) {
                scrollDates.setVisibility(View.VISIBLE);
                showLast5Days();
            } else if (checkedId == R.id.rbWeekly) {
                scrollDates.setVisibility(View.GONE);
                updateStatistics(histories, "weekly");
                drawChartWeekly(histories);
            }
        });

        if (rbDaily.isChecked()) {
            scrollDates.setVisibility(View.VISIBLE);
            showLast5Days();
        }

        // --- Lấy dữ liệu từ Firebase ---
        fetchHistoryFromFirebase();
    }

    private void showLast5Days() {
        llDates.removeAllViews();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 5; i++) {
            Date date = cal.getTime();
            String dateStr = sdf.format(date);

            final Button dayButton = new Button(getContext());
            dayButton.setText(dateStr);
            dayButton.setTag(dateStr);
            dayButton.setBackgroundResource(R.drawable.button_day_selector);
            dayButton.setTextColor(getResources().getColor(R.color.badge_bg));
            dayButton.setAllCaps(false);

            dayButton.setOnClickListener(v -> {
                for (int j = 0; j < llDates.getChildCount(); j++) {
                    llDates.getChildAt(j).setSelected(false);
                }
                dayButton.setSelected(true);

                String selectedDate = (String) dayButton.getTag();
                updateStatisticsByDate(histories, selectedDate);
                drawChartDaily(histories);
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 0, 8, 0);
            dayButton.setLayoutParams(params);

            llDates.addView(dayButton);

            if (i == 0) {
                dayButton.setSelected(true);
                updateStatisticsByDate(histories, dateStr);
            }

            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        drawChartDaily(histories);
    }

    @SuppressLint("StringFormatInvalid")
    private void updateStatisticsByDate(List<History> histories, String dateStr) {
        int totalReads = 0;
        long totalMillis = 0;
        SimpleDateFormat sdfFull = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat sdfDayMonth = new SimpleDateFormat("dd/MM", Locale.getDefault());

        for (History h : histories) {
            try {
                String startStr = Encryption.decrypt(h.getStartTime());
                String endStr = Encryption.decrypt(h.getEndTime());
                String title = Encryption.decrypt(h.getTitle());
                if (startStr == null || endStr == null)
                    continue;

                Date startDate = sdfFull.parse(startStr);
                Date endDate = sdfFull.parse(endStr);
                if (startDate == null || endDate == null)
                    continue;

                String startDay = sdfDayMonth.format(startDate);
                String endDay = sdfDayMonth.format(endDate);

                if (startDay.equals(dateStr) || endDay.equals(dateStr)) {
                    totalReads++;
                    totalMillis += (endDate.getTime() - startDate.getTime());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        tvTotalBooks.setText(String.valueOf(totalReads));
        long totalMinutes = totalMillis / 60000;
        if (totalMinutes < 60) {
            tvTotalHours.setText(getString(R.string.minutes, totalMinutes));
        } else {
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            tvTotalHours.setText(getString(R.string.hours_minutes, hours, minutes));
        }
    }

    private void fetchHistoryFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            String emailKey = user.getEmail().replace(".", "_");
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("History")
                    .child(emailKey);

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    histories.clear();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        History h = child.getValue(History.class);
                        if (h != null)
                            histories.add(h);
                    }
                    if (rbDaily.isChecked() && llDates.getChildCount() > 0) {
                        Button todayBtn = (Button) llDates.getChildAt(0);
                        String today = (String) todayBtn.getTag();
                        updateStatisticsByDate(histories, today);
                        drawChartDaily(histories);
                    } else if (rbWeekly.isChecked()) {
                        updateStatistics(histories, "weekly");
                        drawChartWeekly(histories);
                    }
                    drawBarChart(histories); // GỌI HÀM VẼ BIỂU ĐỒ CỘT
                    drawPieChart(histories); // GỌI HÀM VẼ BIỂU ĐỒ TRÒN
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    tvTotalBooks.setText("Lỗi tải dữ liệu");
                    tvTotalHours.setText("0");
                }
            });
        } else {
            tvTotalBooks.setText("Chưa đăng nhập");
            tvTotalHours.setText("0");
        }
    }

    @SuppressLint("StringFormatInvalid")
    private void updateStatistics(List<History> histories, String timeFrame) {
        int totalReads = 0; // Thay thế Set<String> bằng biến đếm
        long totalMillis = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());

        for (History h : histories) {
            try {
                String startStr = Encryption.decrypt(h.getStartTime());
                String endStr = Encryption.decrypt(h.getEndTime());
                // String title = Encryption.decrypt(h.getTitle()); // Không cần dùng title

                Date startDate = sdf.parse(startStr);
                Date endDate = sdf.parse(endStr);
                if (startDate != null && endDate != null) {
                    totalReads++; // Tăng biến đếm số tập/lần đọc
                    totalMillis += (endDate.getTime() - startDate.getTime());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        tvTotalBooks.setText(String.valueOf(totalReads)); // Sử dụng biến đếm số tập
        long totalMinutes = totalMillis / 60000;
        if (totalMinutes < 60) {
            tvTotalHours.setText(getString(R.string.minutes, totalMinutes));
        } else {
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            tvTotalHours.setText(getString(R.string.hours_minutes, hours, minutes));
        }
    }

    private void drawChartDaily(List<History> histories) {
        lineChart.clear();

        SimpleDateFormat sdfDay = new SimpleDateFormat("dd/MM", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -4);
        List<String> labels = new ArrayList<>();
        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            Date date = cal.getTime();
            String dayStr = sdfDay.format(date);
            labels.add(dayStr);

            long totalMillis = 0;
            SimpleDateFormat sdfFull = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());

            for (History h : histories) {
                try {
                    String startStr = Encryption.decrypt(h.getStartTime());
                    String endStr = Encryption.decrypt(h.getEndTime());
                    if (startStr == null || endStr == null)
                        continue;

                    Date startDate = sdfFull.parse(startStr);
                    Date endDate = sdfFull.parse(endStr);
                    if (startDate == null || endDate == null)
                        continue;

                    String startDay = sdfDay.format(startDate);
                    String endDay = sdfDay.format(endDate);

                    if (startDay.equals(dayStr) || endDay.equals(dayStr)) {
                        totalMillis += (endDate.getTime() - startDate.getTime());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            float hours = totalMillis / 3600000f;
            entries.add(new Entry(i, hours));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        LineDataSet dataSet = new LineDataSet(entries, "Giờ đọc / ngày");

        dataSet.setMode(Mode.CUBIC_BEZIER);

        dataSet.setColor(Color.parseColor("#3F51B5")); // Deep Blue
        dataSet.setLineWidth(3f); // Đường dày hơn

        dataSet.setDrawCircles(true);
        dataSet.setCircleRadius(6f);
        dataSet.setCircleHoleRadius(3f);
        dataSet.setCircleColor(Color.parseColor("#FF4081"));
        dataSet.setCircleHoleColor(Color.WHITE);

        dataSet.setDrawFilled(true);
        // Thay thế 'this' bằng 'requireContext()'
        Drawable drawable = ContextCompat.getDrawable(requireContext(), R.drawable.fade_blue);
        dataSet.setFillDrawable(drawable);

        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(getResources().getColor(R.color.text_on_primary));
        dataSet.setValueFormatter(new ValueFormatter() {
            private final DecimalFormat mFormat = new DecimalFormat("0.000"); // Định dạng 3 chữ số sau dấu phẩy

            @Override
            public String getFormattedValue(float value) {
                return mFormat.format(value);
            }
        });

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = Math.round(value);
                if (idx >= 0 && idx < labels.size())
                    return labels.get(idx);
                return "";
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(getResources().getColor(R.color.text_on_primary));
        xAxis.setTextSize(10f);
        xAxis.setLabelCount(labels.size(), true);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setGridColor(getResources().getColor(R.color.bg_overlay));
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setTextColor(getResources().getColor(R.color.text_on_primary));
        leftAxis.setTextSize(10f);
        leftAxis.setLabelCount(5, true);

        lineChart.getAxisRight().setEnabled(false);

        lineChart.setBackgroundColor(getResources().getColor(R.color.bg_tertiary));
        lineChart.getLegend().setEnabled(true);
        lineChart.getLegend().setTextColor(getResources().getColor(R.color.text_on_primary));
        lineChart.getLegend().setTextSize(12f);
        lineChart.getLegend()
                .setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.LEFT);
        lineChart.getLegend()
                .setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP);
        lineChart.getLegend().setForm(com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE); // Hình dạng
                                                                                                         // chú thích là
                                                                                                         // hình tròn
        lineChart.getLegend().setXOffset(10f);
        lineChart.getLegend().setYOffset(5f);
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawBorders(false);
        lineChart.setExtraOffsets(15f, 15f, 15f, 15f);
        lineChart.animateXY(1000, 1000);

        lineChart.invalidate();
    }

    private void drawChartWeekly(List<History> histories) {
        lineChart.clear();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdfWeek = new SimpleDateFormat("w/yyyy", Locale.getDefault());
        List<String> labels = new ArrayList<>();
        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            Date date = cal.getTime();
            Calendar currentWeekCal = (Calendar) cal.clone();
            currentWeekCal.add(Calendar.WEEK_OF_YEAR, -i);

            int weekOfYear = currentWeekCal.get(Calendar.WEEK_OF_YEAR);
            int year = currentWeekCal.get(Calendar.YEAR);
            String weekLabel = "Tuần " + weekOfYear + "/" + year;
            labels.add(weekLabel);

            long totalMillis = 0;
            SimpleDateFormat sdfFull = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());

            for (History h : histories) {
                try {
                    String startStr = Encryption.decrypt(h.getStartTime());
                    String endStr = Encryption.decrypt(h.getEndTime());
                    if (startStr == null || endStr == null)
                        continue;

                    Date startDate = sdfFull.parse(startStr);
                    Date endDate = sdfFull.parse(endStr);
                    if (startDate == null || endDate == null)
                        continue;

                    Calendar startCal = Calendar.getInstance();
                    startCal.setTime(startDate);
                    int startWeek = startCal.get(Calendar.WEEK_OF_YEAR);
                    int startYear = startCal.get(Calendar.YEAR);

                    if (startWeek == weekOfYear && startYear == year) {
                        totalMillis += (endDate.getTime() - startDate.getTime());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            entries.add(new Entry(i, totalMillis / 3600000f));
        }

        List<Entry> reversedEntries = new ArrayList<>();
        List<String> reversedLabels = new ArrayList<>();
        for (int i = entries.size() - 1; i >= 0; i--) {
            reversedEntries.add(new Entry(entries.size() - 1 - i, entries.get(i).getY()));
            reversedLabels.add(labels.get(i));
        }

        LineDataSet dataSet = new LineDataSet(reversedEntries, "Giờ đọc / tuần");

        dataSet.setMode(Mode.CUBIC_BEZIER);

        dataSet.setColor(Color.parseColor("#388E3C")); // Dark Green
        dataSet.setLineWidth(3f);

        dataSet.setDrawCircles(true);
        dataSet.setCircleRadius(6f);
        dataSet.setCircleHoleRadius(3f);
        dataSet.setCircleColor(Color.parseColor("#FFEB3B")); // Yellow
        dataSet.setCircleHoleColor(Color.WHITE);

        dataSet.setDrawFilled(true);
        Drawable drawable = ContextCompat.getDrawable(requireContext(), R.drawable.fade_green);
        dataSet.setFillDrawable(drawable);

        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(getResources().getColor(R.color.text_on_primary));
        dataSet.setValueFormatter(new ValueFormatter() {
            private final DecimalFormat mFormat = new DecimalFormat("0.000");

            @Override
            public String getFormattedValue(float value) {
                return mFormat.format(value);
            }
        });

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = Math.round(value);
                if (idx >= 0 && idx < reversedLabels.size())
                    return reversedLabels.get(idx);
                return "";
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(getResources().getColor(R.color.text_on_primary));
        xAxis.setTextSize(10f);
        xAxis.setLabelCount(reversedLabels.size(), true);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setGridColor(getResources().getColor(R.color.bg_overlay));
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setTextColor(getResources().getColor(R.color.text_on_primary));
        leftAxis.setTextSize(10f);
        leftAxis.setLabelCount(5, true);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(true);
        lineChart.getLegend().setTextColor(getResources().getColor(R.color.text_on_primary));
        lineChart.getLegend().setTextSize(12f);
        lineChart.getLegend()
                .setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.LEFT);
        lineChart.getLegend()
                .setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP);
        lineChart.getLegend().setForm(com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE);
        lineChart.getLegend().setXOffset(10f);
        lineChart.getLegend().setYOffset(5f);

        lineChart.setDrawBorders(false);
        lineChart.setExtraOffsets(15f, 15f, 15f, 15f);
        lineChart.animateXY(1000, 1000);

        lineChart.invalidate();
    }

    // Bieu do cot
    private void drawBarChart(List<History> histories) {
        barChart.clear();

        Map<String, Integer> readCounts = new HashMap<>();
        for (History h : histories) {
            try {
                String title = Encryption.decrypt(h.getTitle());
                String episodeTitle = Encryption.decrypt(h.getEpisodeTitle());

                String combinedKey;

                if (title == null || title.isEmpty())
                    continue;

                if (episodeTitle == null || episodeTitle.isEmpty() || episodeTitle.equals(title)) {
                    // Giữ nguyên tên truyện chung nếu không có tên tập cụ thể
                    combinedKey = title;
                } else {
                    // TẠO KHÓA KẾT HỢP: "Tên Truyện - Tên Tập"
                    combinedKey = title + " - " + episodeTitle;
                }

                readCounts.put(combinedKey, readCounts.getOrDefault(combinedKey, 0) + 1);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        int index = 0;

        // Sắp xếp và lấy Top 5
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(readCounts.entrySet());
        Collections.sort(sortedEntries, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        for (Map.Entry<String, Integer> entry : sortedEntries.subList(0, Math.min(sortedEntries.size(), 5))) {
            labels.add(entry.getKey());
            entries.add(new BarEntry(index, entry.getValue()));
            index++;
        }

        if (entries.isEmpty()) {
            barChart.setNoDataText("Không có dữ liệu tần suất đọc.");
            barChart.invalidate();
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Số lần đọc");

        // ... (Cài đặt màu sắc) ...
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#42A5F5"));
        colors.add(Color.parseColor("#66BB6A"));
        colors.add(Color.parseColor("#FFA726"));
        colors.add(Color.parseColor("#EF5350"));
        colors.add(Color.parseColor("#AB47BC"));

        dataSet.setColors(colors);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(getResources().getColor(R.color.text_on_primary));
        dataSet.setValueFormatter(new ValueFormatter() {
            private final DecimalFormat mFormat = new DecimalFormat("0");

            @Override
            public String getFormattedValue(float value) {
                return mFormat.format(value);
            }
        });
        dataSet.setDrawValues(true);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);

        barChart.setData(barData);
        barChart.setDrawValueAboveBar(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(labels.size());
        xAxis.setTextColor(getResources().getColor(R.color.text_on_primary));
        xAxis.setTextSize(11f);
        xAxis.setAxisLineColor(Color.LTGRAY);
        xAxis.setLabelRotationAngle(-45);

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = Math.round(value);
                if (idx >= 0 && idx < labels.size()) {
                    return labels.get(idx);
                }
                return "";
            }
        });

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setGridColor(getResources().getColor(R.color.bg_overlay));
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setTextColor(getResources().getColor(R.color.text_on_primary));

        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);

        // TẮT CHÚ THÍCH (LEGEND)
        barChart.getLegend().setEnabled(false);

        barChart.setFitBars(true);
        barChart.setDrawBorders(false);
        barChart.setDrawGridBackground(false);
        barChart.setBackgroundColor(getResources().getColor(R.color.bg_tertiary));
        barChart.animateY(1200);
        barChart.setExtraOffsets(10f, 10f, 10f, 80f);
        barChart.invalidate();
    }

    // Bieu do tron
    private void drawPieChart(List<History> histories) {
        pieChart.clear();

        long morningTime = 0; // 0:00 - 11:59
        long afternoonTime = 0; // 12:00 - 17:59
        long eveningTime = 0; // 18:00 - 23:59

        SimpleDateFormat sdfFull = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        for (History h : histories) {
            try {
                String startStr = Encryption.decrypt(h.getStartTime());
                String endStr = Encryption.decrypt(h.getEndTime());

                Date startDate = sdfFull.parse(startStr);
                Date endDate = sdfFull.parse(endStr);

                if (startDate != null && endDate != null) {
                    long duration = endDate.getTime() - startDate.getTime();

                    cal.setTime(startDate);
                    int hour = cal.get(Calendar.HOUR_OF_DAY);

                    if (hour >= 0 && hour < 12) {
                        morningTime += duration;
                    } else if (hour >= 12 && hour < 18) {
                        afternoonTime += duration;
                    } else {
                        eveningTime += duration;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        long totalTime = morningTime + afternoonTime + eveningTime;
        if (totalTime == 0) {
            pieChart.setNoDataText("Chưa có dữ liệu thời gian đọc.");
            pieChart.invalidate();
            return;
        }

        List<PieEntry> pieEntries = new ArrayList<>();

        if (morningTime > 0)
            pieEntries.add(new PieEntry(morningTime, "Sáng"));
        if (afternoonTime > 0)
            pieEntries.add(new PieEntry(afternoonTime, "Chiều"));
        if (eveningTime > 0)
            pieEntries.add(new PieEntry(eveningTime, "Tối"));

        PieDataSet dataSet = new PieDataSet(pieEntries, "");

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#8BC34A"));
        colors.add(Color.parseColor("#FFD54F"));
        colors.add(Color.parseColor("#42A5F5"));

        dataSet.setColors(colors);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(getResources().getColor(R.color.text_on_primary));
        dataSet.setSliceSpace(2f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);
        dataSet.setValueLinePart1OffsetPercentage(0.f);
        dataSet.setValueLinePart1Length(0.f);
        dataSet.setValueLinePart2Length(0.f);

        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                float percentage = (value / totalTime) * 100;
                return String.format(Locale.getDefault(), "%.1f%%", percentage);
            }
        });
        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);

        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(10f, 10f, 10f, 10f);

        pieChart.setBackgroundColor(getResources().getColor(R.color.bg_tertiary));
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(getResources().getColor(R.color.bg_tertiary));
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setHoleRadius(58f);

        pieChart.setCenterText(getString(R.string.total_reading_time));
        pieChart.setCenterTextSize(18f);
        pieChart.setCenterTextColor(getResources().getColor(R.color.text_on_primary));

        pieChart.setDrawEntryLabels(false);

        pieChart.setRotationEnabled(false);
        pieChart.getLegend().setWordWrapEnabled(true);
        pieChart.getLegend().setTextColor(getResources().getColor(R.color.text_on_primary));
        pieChart.getLegend()
                .setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        pieChart.getLegend().setHorizontalAlignment(
                com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        pieChart.getLegend()
                .setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        pieChart.getLegend().setDrawInside(false);
        pieChart.getLegend().setTextSize(13f);
        pieChart.getLegend().setYEntrySpace(5f);
        pieChart.getLegend().setXEntrySpace(20f);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }
}