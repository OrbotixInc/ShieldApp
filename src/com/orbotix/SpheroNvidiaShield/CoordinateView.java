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
public class CoordinateView extends RelativeLayout {

    private TextView mXvalue;
    private TextView mYvalue;
    private TextView mZvalue;

    public CoordinateView(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(context, R.layout.coordinate_view, this);

        mXvalue = (TextView)findViewById(R.id.x_value);
        mYvalue = (TextView)findViewById(R.id.y_value);
        mZvalue = (TextView)findViewById(R.id.z_val);
    }

    public void setX(String x){
        mXvalue.setText(x);
    }

    public void setY(String y){
        mYvalue.setText(y);
    }

    public void setZ(String z){
        mZvalue.setText(z);
    }
}
