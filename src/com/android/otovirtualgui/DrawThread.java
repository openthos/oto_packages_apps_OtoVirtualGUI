package com.android.otovirtualgui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

class DrawThread extends Thread {
    final private static String TAG = "OtoVirtualGUI::DrawThread";
    final private static int SCANNING_INTERVAL = 1000 / 60; // 60Hz, 60 times per second.

    private boolean mRun;

    private SurfaceHolder mHolder;
    private int mWidth, mHeight;
    private String mImagePath;

    private FileChannel mChannel = null;

    public void setRunning(boolean run) {
        mRun = run;
    }

    public DrawThread(SurfaceHolder holder, String imagePath, int width, int height) {
        mHolder = holder;
        mWidth = width;
        mHeight = height;
        mImagePath = imagePath;
    }

    public MappedByteBuffer getMappedBuffer() {
        MappedByteBuffer byteBuffer = null;
        try {
            mChannel = (new FileInputStream(new File(mImagePath))).getChannel();
            byteBuffer = mChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int) mChannel.size());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            byteBuffer = null;
        } catch (IOException e) {
            e.printStackTrace();
            byteBuffer = null;
        } finally {
            if (mChannel != null) {
                try {
                    mChannel.close();
                    mChannel = null;
                } catch (IOException e) {
                }
            }
        }

        return byteBuffer;
    }

    private void sleepAWhile() {
        try {
            sleep(SCANNING_INTERVAL);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        MappedByteBuffer byteBuffer;

        do {
            byteBuffer = getMappedBuffer();
            sleepAWhile();
        } while (mRun && byteBuffer == null);

        while (mRun) {
            sleepAWhile();
            bitmap.copyPixelsFromBuffer(byteBuffer);
            byteBuffer.rewind();
            if (bitmap == null) {
                continue;
            }
            try {
                Canvas c = mHolder.lockCanvas();
                if (c != null) {
                    c.drawBitmap(bitmap, 0, 0, null);
                    mHolder.unlockCanvasAndPost(c);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mChannel != null) {
            try {
                mChannel.close();
            } catch (IOException e) {
            }
        }
    }
}
