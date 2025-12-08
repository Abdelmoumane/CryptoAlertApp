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

    private final TextView tvDate;
    private final TextView tvOpen;
    private final TextView tvHigh;
    private final TextView tvLow;
    private final TextView tvClose;

    private final List<Long> timestamps;

    //  Aquí agregamos la hora junto con la fecha

    private final SimpleDateFormat dateTimeFormat =
            new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());

    public CustomMarkerView(Context context, int layoutResource, List<Long> timestamps) {
        super(context, layoutResource);
        this.timestamps = timestamps;

        tvDate = findViewById(R.id.tvDate);
        tvOpen = findViewById(R.id.tvOpen);
        tvHigh = findViewById(R.id.tvHigh);
        tvLow  = findViewById(R.id.tvLow);
        tvClose= findViewById(R.id.tvClose);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = (int) e.getX();

        if (index >= 0 && index < timestamps.size()) {
            long ts = timestamps.get(index);
            String dateTime = dateTimeFormat.format(new Date(ts));
            tvDate.setText(dateTime);  //  Ahora muestra fecha + hora
        }

        if (e instanceof CandleEntry) {
            CandleEntry ce = (CandleEntry) e;

            tvOpen.setText(String.format(Locale.getDefault(),
                    "Open: %.2f", ce.getOpen()));
            tvHigh.setText(String.format(Locale.getDefault(),
                    "High: %.2f", ce.getHigh()));
            tvLow.setText(String.format(Locale.getDefault(),
                    "Low: %.2f", ce.getLow()));
            tvClose.setText(String.format(Locale.getDefault(),
                    "Close: %.2f", ce.getClose()));
        }

        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}
