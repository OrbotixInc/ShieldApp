package com.orbotix.SpheroNvidiaShield;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created with IntelliJ IDEA.
 * User: Hunter
 * Date: 7/17/13
 */
public class SpeedView extends RelativeLayout {


    private TextView mTitle;
    private TextView mSpeedMultiplier;

    public SpeedView(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(context, R.layout.speed, this);

        mTitle = (TextView)findViewById(R.id.speed_label);
        if (mTitle == null) {
            Log.v("Nullity", "mTitle is null!");
        }
        mSpeedMultiplier = (TextView)findViewById(R.id.speed_value);
    }

    public void setSpeedMultiplierLabel(String text) {
        mSpeedMultiplier.setText(text);
    }

}