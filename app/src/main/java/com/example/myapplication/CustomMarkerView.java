package com.example.myapplication;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.example.myapplication.R;

public class CustomMarkerView extends MarkerView {

    private TextView tvInfo;

    public CustomMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvInfo = findViewById(R.id.tvInfo);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        CandleEntry ce = (CandleEntry) e;

        String txt = "🕑 Date: " + getFormattedTime((int) ce.getX()) + "\n" +
                "📈 Open: " + ce.getOpen() + "\n" +
                "⬆ High: " + ce.getHigh() + "\n" +
                "⬇ Low: " + ce.getLow() + "\n" +
                "📉 Close: " + ce.getClose();

        tvInfo.setText(txt);
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2), -getHeight());
    }

    private String getFormattedTime(int index) {
        return "Candle #" + index; // سنعدلها لاحقاً بالتاريخ الحقيقي
    }
}
