package com.orbotix.SpheroNvidiaShield;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import orbotix.robot.app.ColorPickerActivity;
import orbotix.robot.base.*;
import orbotix.robot.sensor.AccelerometerData;
import orbotix.robot.sensor.AttitudeSensor;
import orbotix.robot.sensor.DeviceSensorsData;
import orbotix.robot.widgets.HSBColorPickerView;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.SensorFlag;
import orbotix.sphero.SensorListener;
import orbotix.sphero.Sphero;
import orbotix.view.calibration.CalibrationView;
import orbotix.view.connection.SpheroConnectionView;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Hunter
 * Date: 7/15/13
 */
public class RootActivity extends Activity {

    private static final String TAG = "OBX-NVIDIA-MAIN";
    private static final long ROTATE_OR_DRIVE_DELAY = 50;
    private static final long START_SLEEP_DELAY = 3000;

    private Sphero mSphero = null;
    private PowerManager.WakeLock mWakeLock;
    private ConnectionListener mConnectionListener;
    private ColorPickerActivity.OnColorChangedListener mColorChangedListener;
    private boolean LEDIsOn = false;
    private Handler mHandler;

    private InputDeviceState controllerInput;

    private SpheroConnectionView mSpheroConnectionView;
    private ImuView mImuView;
    private CoordinateView mAccelerometerFilteredView;
    private CalibrationView mCalibrationView;
    private SpeedView mSpeedView;
    private HSBColorPickerView mColorPicker;

    private static float degreeHeading = 0.0f;
    private static float speed = 0.0f;
    private static ArrayList<Float> speedMultipliers;
    private static float currentSpeedMultiplier;
    private static boolean isConnectingToSphero;
    private static boolean calibrating;
    private static float lastCalibrationAngle;
    private static boolean holdingStartKey;
    private static KeyEvent holdingEvent;
    private static boolean boosting;
    private static int previousColor;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setupFields();
        setupViews();
        prepareListener();
    }

    @Override
    protected void onResume() {

        super.onResume();
        mWakeLock.acquire();
        mSpheroConnectionView.startDiscovery();
    }

    @Override
    protected void onPause() {

        super.onPause();
        mWakeLock.release();
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        if (mSphero != null) {
            // make sure to remove the streaming listener!
            mSphero.getSensorControl().removeSensorListener(mSensorListener);
            mSphero.disconnect(); // Disconnect Robot properly
        }
        // Remove all things on the handler
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onStop() {

        super.onStop();
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        if (mSphero != null) {
            // make sure to remove the streaming listener!
            mSphero.getSensorControl().removeSensorListener(mSensorListener);
            mSphero.disconnect(); // Disconnect Robot properly
        }
        // Remove all things on the handler
        mHandler.removeCallbacksAndMessages(null);
    }

    private void prepareListener() {

        // This event listener will notify you when these events occur, it is up to you what you want to do during them
        mConnectionListener = new ConnectionListener() {
            @Override
            public void onConnected(Robot sphero) {
                // Hide the connection view. Comment this code if you want to connect to multiple robots in other apps.
                // Currently streaming doesn't support multiple robots.
                mSpheroConnectionView.setVisibility(View.INVISIBLE);
                mSphero = (Sphero) sphero;
                mSphero.setBackLEDBrightness(1.0f);
                LEDIsOn = true;
                mSphero.setColor(180, 0, 255);
                mSphero.enableStabilization(true);  // disable
                mSphero.getSensorControl().setRate(10  /*Hz*/);
                mSphero.getSensorControl().addSensorListener(mSensorListener, SensorFlag.ACCELEROMETER_NORMALIZED, SensorFlag.ATTITUDE);
                isConnectingToSphero = false;
                setupCalibration();
                mColorPicker.setVisibility(View.VISIBLE);
                mColorPicker.invalidate();
            }

            @Override
            public void onConnectionFailed(Robot sphero) {
                isConnectingToSphero = true;
                // let the SpheroConnectionView handle or hide it and do something here...
            }

            @Override
            public void onDisconnected(Robot sphero) {
                isConnectingToSphero = true;
                mSpheroConnectionView.startDiscovery();
            }
        };
        mSpheroConnectionView.addConnectionListener(mConnectionListener);
    }

    private void setupViews() {

        setContentView(R.layout.main);
        mCalibrationView = (CalibrationView) findViewById(R.id.calibration_widget);
        mImuView = (ImuView) findViewById(R.id.imu_values);
        mAccelerometerFilteredView = (CoordinateView) findViewById(R.id.accelerometer_filtered_coordinates);
        mSpeedView = (SpeedView) findViewById(R.id.speed_view);
        mSpeedView.setSpeedMultiplierLabel(Float.toString(currentSpeedMultiplier));

        // Find Sphero Connection View from layout file
        mSpheroConnectionView = (SpheroConnectionView) findViewById(R.id.sphero_connection_view);

        mColorPicker = (HSBColorPickerView)findViewById(R.id.color_picker);
        mColorPicker.setNewColor(0xFFFFFF);
        mColorChangedListener = new ColorPickerActivity.OnColorChangedListener() {
            @Override
            public void OnColorChanged(int newColor) {
                if (mSphero != null) {
                    previousColor = newColor;
                    int r = (newColor >> 16) & 0xFF;
                    int g = (newColor >> 8) & 0xFF;
                    int b = (newColor) & 0xFF;
                    mSphero.setColor(r, g, b);
                }
            }
        };
        mColorPicker.setOnColorChangedListener(mColorChangedListener);
    }

    private void setupFields() {

        controllerInput = null;
        mHandler = new Handler();
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "StayInControl");
        isConnectingToSphero = true;

        currentSpeedMultiplier = 1.0f;
        speedMultipliers = new ArrayList<Float>();
        for (int i = 0; i < 11; ++i) {
            speedMultipliers.add(0.1f * i);
        }

        calibrating = false;

        holdingStartKey = false;
    }


    private final SensorListener mSensorListener = new SensorListener() {

        @Override
        public void sensorUpdated(DeviceSensorsData datum) {
            //Show attitude data
            AttitudeSensor attitude = datum.getAttitudeData();
            if (attitude != null) {
                mImuView.setPitch(String.format("%+3d", attitude.pitch));
                mImuView.setRoll(String.format("%+3d", attitude.roll));
                mImuView.setYaw(String.format("%+3d", attitude.yaw));
            }

            //Show accelerometer data
            AccelerometerData accel = datum.getAccelerometerData();
            if (attitude != null) {
                mAccelerometerFilteredView.setX(String.format("%+.4f", accel.getFilteredAcceleration().x));
                mAccelerometerFilteredView.setY(String.format("%+.4f", accel.getFilteredAcceleration().y));
                mAccelerometerFilteredView.setZ(String.format("%+.4f", accel.getFilteredAcceleration().z));
            }
        }
    };

    private void setupCalibration() {
        mCalibrationView.setRobot(mSphero);
        mCalibrationView.setColor(Color.GREEN);
        mCalibrationView.setCircleColor(Color.GREEN);
        mCalibrationView.enable();
    }

    private InputDeviceState getInputDeviceState(InputEvent event) {
        InputDevice device = event.getDevice();
        if (device == null) {
            return null;
        }
        if (controllerInput == null) {
            controllerInput = new InputDeviceState(device);
        }

        if (controllerInput.getDevice() == device) {
            return controllerInput;
        }

        return null;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (isConnectingToSphero) {
            return super.dispatchKeyEvent(event);
        }

        InputDeviceState state = getInputDeviceState(event);

        if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_A || event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_B) {
            return super.dispatchKeyEvent(event);
        }

        if (state == null) {
            return super.dispatchKeyEvent(event);
        }
        switch (event.getAction()) {

            case KeyEvent.ACTION_DOWN:
                Log.v(TAG, "Action Down on key: " + KeyEvent.keyCodeToString(event.getKeyCode()));
                if (state.onKeyDown(event)) {
                    handleGameKey(event);
                    return true;
                }
                break;

            case KeyEvent.ACTION_UP:
                Log.v(TAG, "Action Up on key: " + KeyEvent.keyCodeToString(event.getKeyCode()));
                if (state.onKeyUp(event)) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_START) {
                        holdingStartKey = false;
                        mHandler.removeCallbacks(retryHold);
                    }
                    return true;
                }
                break;
        }

        return super.dispatchKeyEvent(event);
    }

    private void handleHold(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BUTTON_START:
                // Sleep robot until shaken
                sleepRobot();
        }
    }

    private void sleepRobot() {
        Toast.makeText(RootActivity.this, "Sleeping Connected Robot", Toast.LENGTH_LONG);
        mHandler.removeCallbacksAndMessages(null);
        mSphero.sleep(0);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {

        if (isConnectingToSphero) {
            return super.dispatchGenericMotionEvent(event);
        }
        if (((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) == 0)
                || (event.getAction() != MotionEvent.ACTION_MOVE)) {
            return super.dispatchGenericMotionEvent(event);
        }

        InputDeviceState state = getInputDeviceState(event);
        if (state == null) {
            return super.dispatchGenericMotionEvent(event);
        }

        if (event.getAxisValue(MotionEvent.AXIS_HAT_Y) == 1.0f) {
            decreaseSpeedMultiplier();
            return true;
        } else if (event.getAxisValue(MotionEvent.AXIS_HAT_Y) == -1.0f) {
            increaseSpeedMultiplier();
            return true;
        }

        if (event.getAxisValue(MotionEvent.AXIS_LTRIGGER) == 1.0f) {
            boosting = true;
            previousColor = mSphero.getColor();
            Log.v(TAG, "Setting color: " + previousColor);
            mSphero.setColor(0x00, 0xFF, 0x00);
            RollCommand.sendCommand(mSphero, RollCommand.getCurrentHeading(), 1.0f);
            mHandler.post(rotateOrDrive);
            return true;

        }
        else {
            boosting = false;
            int r = (previousColor >> 16) & 0xFF;
            int g = (previousColor >> 8) & 0xFF;
            int b = (previousColor) & 0xFF;
            mSphero.setColor(r, g, b);

        }

        if (state.onJoystickMotion(event)) {

            int historySize = event.getHistorySize();

            for (int i = 0; i < historySize; i++) {
                processJoystickInput(mSphero, state.getDevice(), event, i);
            }

            processJoystickInput(mSphero, state.getDevice(), event, -1);
            return true;
        }


        return super.dispatchGenericMotionEvent(event);

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        if (isConnectingToSphero) {
            return super.dispatchTouchEvent(event);
        }
        mCalibrationView.interpretMotionEvent(event);

        event.offsetLocation(-mColorPicker.getLeft(), -mColorPicker.getTop());

        mColorPicker.onTouchEvent(event);
        return true;
    }

    private void handleGameKey(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BUTTON_X:
                if (LEDIsOn) {
                    LEDIsOn = false;
                    disableLED();
                } else {
                    LEDIsOn = true;
                    enableLED();
                }
                break;
            case KeyEvent.KEYCODE_BUTTON_Y:
                setRandomColor();
                break;

            case KeyEvent.KEYCODE_BUTTON_THUMBL:
            case KeyEvent.KEYCODE_BUTTON_THUMBR:

                RollCommand.sendCommand(mSphero, RollCommand.getCurrentHeading(), 0.0f, true);
                // Set the heading to 0
                SetHeadingCommand.sendCommand(mSphero, 0.0f);
            case KeyEvent.KEYCODE_BUTTON_START:
                detectHold(event, START_SLEEP_DELAY);

        }

    }

    private void setRandomColor() {
        if (mSphero != null) {
            Random rng = new Random();
            int red = rng.nextInt() % 256;
            int green = rng.nextInt() % 256;
            int blue = rng.nextInt() % 256;
            mSphero.setColor(red, green, blue);

            previousColor = RGBToInt(red, green, blue);
        }
    }

    private int RGBToInt(int red, int green, int blue) {
        return ((red & 0xFF) << 16 | ((green & 0xFF) << 8) | (blue & 0xFF));
    }

    private void detectHold(KeyEvent event, long delay) {
        if (holdingStartKey && holdingEvent != null) {
            holdingStartKey = false;
            holdingEvent = null;
            handleHold(event);
        } else {
            holdingStartKey = true;
            holdingEvent = event;
            mHandler.postDelayed(retryHold, delay);

        }

    }

    private Runnable retryHold = new Runnable() {
        @Override
        public void run() {
            detectHold(holdingEvent, 0);
        }
    };

    private void processJoystickInput(Robot sphero, InputDevice device, MotionEvent event, int historyPos) {
        if (sphero == null) {
            return;
        }

        float normalizedXLeftStick = normalizeAxis(device, MotionEvent.AXIS_X, event, historyPos);
        float normalizedYLeftStick = normalizeAxis(device, MotionEvent.AXIS_Y, event, historyPos);
        float normalizedXRightStick = normalizeAxis(device, MotionEvent.AXIS_Z, event, historyPos);
        float normalizedYRightStick = normalizeAxis(device, MotionEvent.AXIS_RZ, event, historyPos);

        //mHandler.removeCallbacksAndMessages(rotateOrDrive);

        if (determineMagnitude(normalizedXRightStick, normalizedYRightStick) > 0.9f) {
            calibrate(normalizedXRightStick, normalizedYRightStick);

        }
        if (determineMagnitude(normalizedXRightStick, normalizedYRightStick) < 0.9f) {
            stopCalibration();
        }

        mHandler.removeCallbacksAndMessages(null);
        speed = determineMagnitude(normalizedXLeftStick, normalizedYLeftStick);
        degreeHeading = determineDegreeHeading(normalizedXLeftStick, normalizedYLeftStick);

        if (speed == 0.0f) {
            mSphero.stop();
        } else {
            if (calibrating) {
                mSphero.stop();
            } else {
                mSphero.drive(degreeHeading, speed);
                mHandler.post(rotateOrDrive);
            }

        }

    }

    private void calibrate(float x, float y) {
        float angle = determineDegreeHeading(x, y);
        mHandler.removeCallbacksAndMessages(null);
        if (mSphero != null) {
            calibrating = true;
            RollCommand.sendCommand(mSphero, angle, 0.0f, false);
            lastCalibrationAngle = angle;
        }
    }

    private void stopCalibration() {
        if (!calibrating) return;
        mHandler.removeCallbacksAndMessages(null);
        RollCommand.sendCommand(mSphero, lastCalibrationAngle, 0.0f, true);
        SetHeadingCommand.sendCommand(mSphero, 0.0f);
        calibrating = false;

    }

    private Runnable rotateOrDrive = new Runnable() {
        @Override
        public void run() {
            if (calibrating) return;
            if (boosting) {
                RollCommand.sendCommand(mSphero, RollCommand.getCurrentHeading(), 1.0f);
            }
            else {
                RollCommand.sendCommand(mSphero, degreeHeading, speed);
            }
            //Post another instance on the handler
            mHandler.postDelayed(rotateOrDrive, ROTATE_OR_DRIVE_DELAY);
        }
    };


    private float normalizeAxis(InputDevice device, int axis, MotionEvent event, int historyPos) {
        InputDevice.MotionRange xRange = device.getMotionRange(axis, event.getSource());
        if (xRange != null) {
            float axisValue;
            if (historyPos >= 0) {
                axisValue = event.getHistoricalAxisValue(axis, historyPos);
            } else {
                axisValue = event.getAxisValue(axis);
            }
            return InputDeviceState.ProcessAxis(xRange, axisValue);
        }
        return -1.0f;
    }

    private void decreaseSpeedMultiplier() {
        int currentMultiplerIndex = speedMultipliers.indexOf(currentSpeedMultiplier);
        // If the object is found
        if (currentMultiplerIndex != -1) {
            // And it's not the end of the array
            if (currentMultiplerIndex > 0) {
                // Go to the next one
                currentSpeedMultiplier = speedMultipliers.get(currentMultiplerIndex - 1);
                // Set the label on the view
                mSpeedView.setSpeedMultiplierLabel(Float.toString(currentSpeedMultiplier));
            }
        }
    }

    private void increaseSpeedMultiplier() {
        int currentMultiplerIndex = speedMultipliers.indexOf(currentSpeedMultiplier);
        // If the object is found
        if (currentMultiplerIndex != -1) {
            // And it's not the end of the array
            if (currentMultiplerIndex < speedMultipliers.size() - 1) {
                // Go to the next one
                currentSpeedMultiplier = speedMultipliers.get(currentMultiplerIndex + 1);
                // Set the label on the view
                mSpeedView.setSpeedMultiplierLabel(Float.toString(currentSpeedMultiplier));
            }
        }
    }

    private void enableLED() {
        if (mSphero != null) {
            BackLEDOutputCommand.sendCommand(mSphero, 1.0f);
        }
    }

    private void disableLED() {
        if (mSphero != null) {
            BackLEDOutputCommand.sendCommand(mSphero, 0.0f);
        }
    }


    private float determineDegreeHeading(float x, float y) {
        float angle = 90.0f + (float) Math.toDegrees(Math.atan2(y, x));
        if (angle > 359) {
            angle -= 360;
        } else if (angle < 0) {
            angle += 360;
        }

        return angle;
    }


    private float determineMagnitude(float x, float y) {
        float speed = currentSpeedMultiplier * (float) Math.sqrt(x * x + y * y);
        if (speed > 1.0f) {
            return 1.0f;
        } else {
            return speed;
        }
    }
}