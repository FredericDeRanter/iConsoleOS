package eu.le_tian.iConsoleOS.customtimepicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.michaelmuenzer.android.scrollablennumberpicker.ScrollableNumberPickerListener;

import eu.le_tian.iConsoleOS.R;


public class CustomTimePicker2 extends ConstraintLayout {

    private static final String SUPERSTATE = "superState";
    private static final String TOTALSECONDS = "totalSeconds";
    private onCTValueChangeListener listener;

    private com.michaelmuenzer.android.scrollablennumberpicker.ScrollableNumberPicker vSeconds;
    private com.michaelmuenzer.android.scrollablennumberpicker.ScrollableNumberPicker vMinutes;

    public CustomTimePicker2(Context context) {
        this(context, null);
    }

    public CustomTimePicker2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomTimePicker2(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        LayoutInflater.from(context).inflate(R.layout.customtimepicker2, this, true);

        vSeconds = findViewById(R.id.np_s);
        vMinutes = findViewById(R.id.np_m);

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CustomTimePicker, 0, 0);
            setTotalSeconds(a.getInt(R.styleable.CustomTimePicker_initialTotalSecondsValue, 120));
            if (a.getBoolean(R.styleable.CustomTimePicker_ThreeDigitMinutes, true)) {
                vMinutes.setMaxValue(999);
            } else {
                vMinutes.setMaxValue(99);
            }
            a.recycle();
        }

        vSeconds.setListener(value -> {
            int totalSeconds = value +
                    (vMinutes.getValue() * 60);
            if (listener != null) {
                listener.onValueChange(totalSeconds);
            }
        });

        vMinutes.setListener(value -> {
            int totalSeconds = vSeconds.getValue() +
                    (value * 60);
            if (listener != null) {
                listener.onValueChange(totalSeconds);
            }
        });
    }

    public void setTotalSeconds(int iTotalSeconds) {
        int iMinutes = Math.floorDiv(iTotalSeconds, 60);
        int iSeconds = iTotalSeconds - (iMinutes * 60);
        setMinutesSeconds(iMinutes, iSeconds);
    }

    public void setMinutesSeconds(int iMinutes, int iSeconds) {
        vSeconds.setValue(iSeconds);
        vMinutes.setValue(iMinutes);
    }

    public int getTotalSeconds() {
        int totalSeconds = vSeconds.getValue() +
                (vMinutes.getValue() * 60);
        return totalSeconds;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putParcelable(SUPERSTATE, super.onSaveInstanceState());
        state.putInt(TOTALSECONDS, getTotalSeconds());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable ss) {
        Bundle state = (Bundle) ss;
        super.onRestoreInstanceState(state.getParcelable(SUPERSTATE));
        setTotalSeconds(state.getInt(TOTALSECONDS));
    }

    public void setOnValueChangeListener(onCTValueChangeListener listener) {
        this.listener = listener;
    }

    public interface onCTValueChangeListener {
        void onValueChange(int totalSeconds);

    }

}
