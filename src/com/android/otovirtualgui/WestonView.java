package com.android.otovirtualgui;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

class WestonView extends SurfaceView implements SurfaceHolder.Callback {

    final private static String TAG = "OtoVirtualGUI::WestonView";

    private DrawThread myThread;

    private String mImagePath;
    private int mWidth, mHeight;

    public WestonView(Context context, String imagePath, int width, int height) {
        super(context);

        mImagePath = imagePath;
        mWidth = width;
        mHeight = height;

        getHolder().addCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (!myThread.isAlive()) {
            //myThread.mWidth = width;
            //myThread.mHeight = height;
            myThread.setRunning(true);
            myThread.start();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        myThread = new DrawThread(holder, mImagePath, mWidth, mHeight);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        myThread.setRunning(false);
        myThread = null;
    }
}
