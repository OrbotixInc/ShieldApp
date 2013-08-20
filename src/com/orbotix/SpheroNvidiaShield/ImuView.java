package com.orbotix.SpheroNvidiaShield;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**

 * Date: 4/30/12
 *
 * @author Adam Williams
 */
public class ImuView extends RelativeLayout {

    private TextView mTitle;
    private TextView mRollValue;
    private TextView mPitchValue;
    private TextView mYawValue;

    public ImuView(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(context, R.layout.coordinate_view, this);

        mTitle = (TextView)findViewById(R.id.title_text);
        mRollValue = (TextView)findViewById(R.id.x_value);
        mPitchValue = (TextView)findViewById(R.id.y_value);
        mYawValue = (TextView)findViewById(R.id.z_val);

        ((TextView)findViewById(R.id.x_label)).setText("Roll:");
        ((TextView)findViewById(R.id.y_label)).setText("Pitch:");
        ((TextView)findViewById(R.id.z_label)).setText("Yaw:");
    }

    public void setRoll(String x){
        mRollValue.setText(x);
    }

    public void setPitch(String y){
        mPitchValue.setText(y);
    }

    public void setYaw(String z){
        mYawValue.setText(z);
    }
}
