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

    private final TextView tvDate, tvOpen, tvHigh, tvLow, tvClose;
    private final List<Long> timestamps;

    public CustomMarkerView(Context context, int layoutResource, List<Long> timestamps) {
        super(context, layoutResource);
        this.timestamps = timestamps;

        tvDate  = findViewById(R.id.tvDate);
        tvOpen  = findViewById(R.id.tvOpen);
        tvHigh  = findViewById(R.id.tvHigh);
        tvLow   = findViewById(R.id.tvLow);
        tvClose = findViewById(R.id.tvClose);
    }

    // 🔥 هذه الدالة يجب أن تكون هنا داخل هذا الكلاس فقط!
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e instanceof CandleEntry) {
            CandleEntry candleEntry = (CandleEntry) e;
            int index = (int) candleEntry.getX();
            long timeStamp = timestamps.get(index);

            tvDate.setText(
                    new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            .format(new Date(timeStamp))
            );

            tvOpen.setText("Open:  " + candleEntry.getOpen());
            tvHigh.setText("High:  " + candleEntry.getHigh());
            tvLow.setText("Low:   " + candleEntry.getLow());
            tvClose.setText("Close: " + candleEntry.getClose());
        }

        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2), -getHeight());
    }
}
