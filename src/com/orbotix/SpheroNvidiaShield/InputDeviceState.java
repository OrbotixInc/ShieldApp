package com.orbotix.SpheroNvidiaShield;

import android.util.SparseIntArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Hunter
 * Date: 7/15/13
 * Time: 6:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class InputDeviceState {

    private static final String TAG = "Sphero-Input-Device-State";
    private static final float ADDITIONAL_DEAD_ZONE_MODIFIER = 0.1f;

    private InputDevice mDevice;
    private int[] mAxes;
    private float[] mAxisValues;
    private SparseIntArray mKeys;

    static ArrayList<Integer> gameAxes;

    final static int X_AXIS_LEFT_STICK = MotionEvent.AXIS_X;
    final static int Y_AXIS_LEFT_STICK = MotionEvent.AXIS_Y;
    final static int X_AXIS_RIGHT_STICK = MotionEvent.AXIS_Z;
    final static int Y_AXIS_RIGHT_STICK = MotionEvent.AXIS_RZ;
    final static int DPAD_AXIS_Y = MotionEvent.AXIS_HAT_Y;
    final static int DPAD_AXIS_X = MotionEvent.AXIS_HAT_X;

    public InputDeviceState(InputDevice device) {

        gameAxes = new ArrayList<Integer>();
        gameAxes.add(X_AXIS_LEFT_STICK);
        gameAxes.add(X_AXIS_RIGHT_STICK);
        gameAxes.add(Y_AXIS_LEFT_STICK);
        gameAxes.add(Y_AXIS_RIGHT_STICK);

        mDevice = device;
        int numAxes = 0;
        for (InputDevice.MotionRange range : device.getMotionRanges()) {
            if ((range.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
                numAxes += 1;
            }
        }

        mAxes		= new int[numAxes];
        mAxisValues	= new float[numAxes];
        mKeys		= new SparseIntArray();

        int i = 0;
        for (InputDevice.MotionRange range : device.getMotionRanges()) {
            if ((range.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
                mAxes[i++] = range.getAxis();
            }
        }
    }

    public static float ProcessAxis(InputDevice.MotionRange range, float axisValue) {
        float absAxisValue = Math.abs(axisValue);
        float deadZone = range.getFlat() + ADDITIONAL_DEAD_ZONE_MODIFIER;
        if (absAxisValue <= deadZone) {
            return 0.0f;
        }
        if (axisValue < 0.0f) {
            return absAxisValue / range.getMin();
        }
        else {
            return absAxisValue / range.getMax();
        }
    }

    public static boolean isGameKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_X:
            case KeyEvent.KEYCODE_BUTTON_Y:
            case KeyEvent.KEYCODE_BUTTON_START:
                return true;
            default:
                return false;


        }
    }
    public boolean onKeyDown(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getRepeatCount() == 0) {
            if (isGameKey(keyCode)) {
                mKeys.put(keyCode, 1);
                return true;
            }
        }
        return false;
    }

    public boolean onKeyUp(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (isGameKey(keyCode)) {
            mKeys.put(keyCode, 0);
            return true;
        }
        return false;
    }

    public boolean onJoystickMotion(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) == 0) {
            return false;
        }
        for (int i = 0; i < mAxes.length; i++) {
            int axisId		= mAxes[i];
        }

        return true;
    }

    public InputDevice getDevice() {
        return mDevice;
    }
}

