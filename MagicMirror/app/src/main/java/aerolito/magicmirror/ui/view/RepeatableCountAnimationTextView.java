package aerolito.magicmirror.ui.view;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.TextView;

import java.text.DecimalFormat;

public class RepeatableCountAnimationTextView extends TextView {

    private static final long DEFAULT_DURATION = 1000;

    private boolean isAnimating = false;
    private ValueAnimator countAnimator;
    private CountAnimationListener countAnimationListener;

    private DecimalFormat decimalFormat;

    private int fromValue;
    private int toValue;

    public RepeatableCountAnimationTextView(Context context) {
        this(context, null, 0);
    }

    public RepeatableCountAnimationTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RepeatableCountAnimationTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setUpAnimator();
    }

    private void setUpAnimator() {
        countAnimator = new ValueAnimator();
        countAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                String value;
                if (decimalFormat == null) {
                    value = String.valueOf(animation.getAnimatedValue());
                } else {
                    value = decimalFormat.format(animation.getAnimatedValue());
                }
                RepeatableCountAnimationTextView.super.setText(value);
            }
        });

        countAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                isAnimating = true;

                if (countAnimationListener == null) return;
                countAnimationListener.onAnimationStart(countAnimator.getAnimatedValue());
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;

                if (countAnimationListener == null) return;
                countAnimationListener.onAnimationEnd(countAnimator.getAnimatedValue());
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                isAnimating = false;

                if (countAnimationListener == null) return;
                countAnimationListener.onAnimationRepeat(countAnimator.getAnimatedValue(), countAnimator.getRepeatCount());
            }
        });
        countAnimator.setDuration(DEFAULT_DURATION);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (countAnimator != null) {
            countAnimator.cancel();
        }
    }

    public RepeatableCountAnimationTextView setCountValues(int fromValue, int toValue) {
        this.fromValue = fromValue;
        this.toValue = toValue;
        countAnimator.setIntValues(this.fromValue, this.toValue);
        return this;
    }

    public int getFromValue() {
        return fromValue;
    }

    public int getToValue() {
        return toValue;
    }

    public void startCountAnimation() {
        if (isAnimating) return;
        countAnimator.start();
    }

    public RepeatableCountAnimationTextView setAnimationDuration(long duration) {
        countAnimator.setDuration(duration);
        return this;
    }

    public RepeatableCountAnimationTextView setInterpolator(@NonNull TimeInterpolator value) {
        countAnimator.setInterpolator(value);
        return this;
    }

    public RepeatableCountAnimationTextView setDecimalFormat(DecimalFormat decimalFormat) {
        this.decimalFormat = decimalFormat;
        return this;
    }

    public void clearDecimalFormat() {
        this.decimalFormat = null;
    }

    public RepeatableCountAnimationTextView setCountAnimationListener(CountAnimationListener countAnimationListener) {
        this.countAnimationListener = countAnimationListener;
        return this;
    }

    public RepeatableCountAnimationTextView setRepeatCount(int repeatCount) {
        countAnimator.setRepeatCount(repeatCount);
        return this;
    }

    public interface CountAnimationListener {

        void onAnimationStart(Object animatedValue);

        void onAnimationEnd(Object animatedValue);

        void onAnimationRepeat(Object animatedValue, int repeatCount);
    }
}
