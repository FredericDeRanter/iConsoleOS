package eu.le_tian.iConsoleOS.levelpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import eu.le_tian.iConsoleOS.R;

public class LevelPicker extends ConstraintLayout {

    private TextView levelText;


    public LevelPicker(Context context) {
        this(context, null);
    }

    public LevelPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LevelPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        LayoutInflater.from(context).inflate(R.layout.levelpicker, this, true);

        //vSeconds = findViewById(R.id.np_s);
        //vMinutes = findViewById(R.id.np_m);

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CustomTimePicker, 0, 0);
            //setTotalSeconds(a.getInt(R.styleable.CustomTimePicker_initialTotalSecondsValue, 120));
            if (a.getBoolean(R.styleable.CustomTimePicker_ThreeDigitMinutes, true)) {
                //    vMinutes.setMaxValue(999);
            } else {
                //   vMinutes.setMaxValue(99);
            }
            a.recycle();
        }

    }

}
