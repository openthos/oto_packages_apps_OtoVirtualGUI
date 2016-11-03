package com.android.otovirtualgui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import com.android.otovirtualgui.WestonProto;
import com.android.otovirtualgui.WestonProto.InputEventProto;

public class MainActivity extends Activity {
    final private static String TAG = "OtoVirtualGUI::MainActivity";

    final public static String TMP_DIR_MESSAGE = "TMP_DIR_MESSAGE";
    final public static String WIDTH_MESSAGE = "WIDTH_MESSAGE";
    final public static String HEIGHT_MESSAGE = "HEIGHT_MESSAGE";

    final private static int BTN_LEFT = 0x110;
    final private static int BTN_RIGHT = 0x111;

    final private static int GUI_DEFAULT_WIDTH = 1920;
    final private static int GUI_DEFAULT_HEIGHT = 1080;
    final private static String GUI_DEFAULT_PATH = "/io";

    final private static String GUI_IO_CONFIG = "/config";
    final private static String GUI_IO_IMAGE = "/image.bin";
    final private static String GUI_IO_SOCKET = "/weston_socket";

    private WestonView mWestonView;
    private SocketThread mSocketThread;
    private BlockingQueue<InputEventProto> mEventQueue;
    private int mButtonState = BTN_LEFT;

    private String mConfigPath;
    private String mImagePath;
    private String mSocketPath;

    private int mWidth;
    private int mHeight;

    private void setDefaultSize() {
        mWidth = GUI_DEFAULT_WIDTH;
        mHeight = GUI_DEFAULT_HEIGHT;
    }

    void launchWestonIfNeeded(Intent intent) {
        String height = intent.getStringExtra(HEIGHT_MESSAGE);
        String width = intent.getStringExtra(WIDTH_MESSAGE);
        String tmpDir = intent.getStringExtra(TMP_DIR_MESSAGE);
        String filesPath = getFilesDir().getAbsolutePath();

        if (tmpDir == null) {
            tmpDir = GUI_DEFAULT_PATH;
        }
        mConfigPath = filesPath + tmpDir + GUI_IO_CONFIG;
        mImagePath = filesPath + tmpDir + GUI_IO_IMAGE;
        mSocketPath = filesPath + tmpDir + GUI_IO_SOCKET;

        if ((width == null) || (height == null)) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(
                                                new FileInputStream(new File(mConfigPath))));
                String size = reader.readLine();
                if (size != null) {
                    try {
                        Scanner scanner = new Scanner(size);
                        scanner.useDelimiter("x");
                        width = scanner.next();
                        height = scanner.next();
                    } catch (NoSuchElementException e) {
                        Log.e(TAG, "size sould be ?x?, invalid format size: " + size);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "invalid content in file: " + mConfigPath);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        if ((width != null) && (height != null)) {
            try {
                mWidth = Integer.parseInt(width);
                mHeight = Integer.parseInt(height);
            } catch (NumberFormatException e) {
                Log.e(TAG, "size sould be ?x?, invalid format size: " + width + "x" + height);
                setDefaultSize();
            }
        } else {
            setDefaultSize();
        }

        Log.i(TAG, "size: " + mWidth + "x" + mHeight);
        Log.i(TAG, "config path: " + mConfigPath);
        Log.i(TAG, "image  path: " + mImagePath);
        Log.i(TAG, "socket path: " + mSocketPath);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        launchWestonIfNeeded(intent);

        mEventQueue = new LinkedBlockingDeque();
        mSocketThread = new SocketThread(mEventQueue, mSocketPath);
        mSocketThread.start();
        mWestonView = new WestonView(this, mImagePath, mWidth, mHeight);
        setContentView(mWestonView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocketThread.stopRun();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        InputEventProto eventProto = new InputEventProto();
        eventProto.type = InputEventProto.KeyEventType;
        eventProto.time = event.getEventTime();
        eventProto.keyEvent = new WestonProto.KeyEvent();
        eventProto.keyEvent.key = event.getScanCode();

        switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:
            case KeyEvent.ACTION_UP:
                eventProto.keyEvent.actionType = event.getAction();
                break;
            default:
                return super.dispatchKeyEvent(event);
        }

        try {
            mEventQueue.put(eventProto);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        int[] location = new int[2] ;
        mWestonView.getLocationOnScreen(location);

        InputEventProto eventProto = new InputEventProto();
        eventProto.type = InputEventProto.MotionEventType;
        eventProto.time = event.getEventTime();
        eventProto.motionEvent = new WestonProto.MotionEvent();
        eventProto.motionEvent.x = Math.round(event.getRawX() - location[0]);
        eventProto.motionEvent.y = Math.round(event.getRawY() - location[1]);

        switch (event.getAction()) {
            case MotionEvent.ACTION_HOVER_MOVE:
            //case MotionEvent.ACTION_BUTTON_PRESS:
            //case MotionEvent.ACTION_BUTTON_RELEASE:
                eventProto.motionEvent.actionType = event.getAction();
                break;
            case MotionEvent.ACTION_SCROLL:
                eventProto.motionEvent.actionType = event.getAction();
                eventProto.motionEvent.axis = 0 - event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                break;
            default:
                return false;
        }

        try {
            mEventQueue.put(eventProto);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return super.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int[] location = new int[2] ;
        mWestonView.getLocationOnScreen(location);

        InputEventProto eventProto = new InputEventProto();
        eventProto.type = InputEventProto.MotionEventType;
        eventProto.time = event.getEventTime();
        eventProto.motionEvent = new WestonProto.MotionEvent();
        eventProto.motionEvent.x = Math.round(event.getRawX() - location[0]);
        eventProto.motionEvent.y = Math.round(event.getRawY() - location[1]);

        switch (event.getAction()) {
            case MotionEvent.ACTION_HOVER_MOVE:
            case WestonProto.MotionEvent.ACTION_BUTTON_PRESS:
            case WestonProto.MotionEvent.ACTION_BUTTON_RELEASE:
                eventProto.motionEvent.actionType = event.getAction();
                break;
            case MotionEvent.ACTION_DOWN:
                eventProto.motionEvent.actionType = WestonProto.MotionEvent.ACTION_BUTTON_PRESS;
                if (event.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                    mButtonState = BTN_RIGHT;
                } else {
                    if (event.getButtonState() != MotionEvent.BUTTON_PRIMARY) {
                        // still regard as left button
                        Log.e(TAG, "unknown button state " + event.getButtonState());
                    }
                    mButtonState = BTN_LEFT;
                }
                eventProto.motionEvent.button = mButtonState;
                break;
            case MotionEvent.ACTION_UP:
                eventProto.motionEvent.actionType = WestonProto.MotionEvent.ACTION_BUTTON_RELEASE;
                eventProto.motionEvent.button = mButtonState;
                break;
            case MotionEvent.ACTION_MOVE:
                eventProto.motionEvent.actionType = MotionEvent.ACTION_HOVER_MOVE;
                break;
            case MotionEvent.ACTION_SCROLL:
                eventProto.motionEvent.actionType = event.getAction();
                eventProto.motionEvent.axis = 0 - event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                break;
            default:
                return false;
        }

        try {
            mEventQueue.put(eventProto);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return super.dispatchTouchEvent(event);
    }
}
