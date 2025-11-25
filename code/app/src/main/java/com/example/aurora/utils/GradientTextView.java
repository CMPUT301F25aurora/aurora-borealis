/*
 * References:
 *
 * 1) author: Stack Overflow user — "How to set gradient as the text color of a TextView?"
 *    https://stackoverflow.com/questions/16958111/how-to-set-gradient-as-the-text-color-of-a-textview
 *    Used as a reference for applying a LinearGradient shader to a TextView's Paint to draw gradient text.
 *
 * 2) GitHub Gist — "GradientTextViewUtil.java"
 *    https://gist.github.com/mirmilad/97182248c42d8477bb5edf07e5b40a08
 *    Used as a reference for recalculating and applying the gradient in onSizeChanged.
 */

/**
 * GradientTextView.java
 *
 * A custom TextView that applies a horizontal color gradient to its text.
 * - Extends AppCompatTextView to support custom styling and backward compatibility.
 * - Creates a LinearGradient shader that blends blue (#49D5FF), purple (#9E6BFF),
 *   and pink (#FF6AC4) across the text width.
 * - The gradient is recalculated and applied dynamically whenever the view size changes.
 *
 * This component is typically used for visually appealing titles or headers in the Aurora app.
 */




package com.example.aurora.utils;

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
