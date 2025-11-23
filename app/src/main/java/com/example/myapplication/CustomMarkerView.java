package com.example.myapplication;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomMarkerView extends MarkerView {

    private final TextView tvContent;
    private final List<Long> timestamps;  // 👈 استقبلنا التواريخ من النشاط

    public CustomMarkerView(Context context, int layoutResource, List<Long> timestamps) {
        super(context, layoutResource);
        this.tvContent = findViewById(R.id.tvContent);
        this.timestamps = timestamps;  // ✔ حفظها
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e instanceof CandleEntry) {
            CandleEntry ce = (CandleEntry) e;

            // 👈 ce.getX() = INDEX فقط
            int index = (int) ce.getX();
            long timestamp = timestamps.get(index);  // ✔ الآن timestamp الحقيقي!

            String date = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                    .format(new Date(timestamp));

            tvContent.setText(
                    "Open: " + ce.getOpen() +
                            "\nHigh: " + ce.getHigh() +
                            "\nLow: " + ce.getLow() +
                            "\nClose: " + ce.getClose() +
                            "\nDate: " + date
            );
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}
