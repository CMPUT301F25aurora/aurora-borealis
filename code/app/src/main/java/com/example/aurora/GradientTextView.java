package com.example.aurora;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.text.TextPaint;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

public class GradientTextView extends AppCompatTextView {

    public GradientTextView(Context context) {
        super(context);
    }

    public GradientTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GradientTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void applyGradient(int width) {
        TextPaint paint = getPaint();
        Shader shader = new LinearGradient(
                0, 0, width, getTextSize(),
                new int[] {
                        Color.parseColor("#49D5FF"),
                        Color.parseColor("#9E6BFF"),
                        Color.parseColor("#FF6AC4")
                },
                null,
                Shader.TileMode.CLAMP
        );
        paint.setShader(shader);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0) {
            applyGradient(w);
        }
    }
}
