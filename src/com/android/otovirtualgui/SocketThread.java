package com.android.otovirtualgui;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.BlockingQueue;

import com.android.otovirtualgui.WestonProto.InputEventProto;

class SocketThread extends Thread
{
    final private static String TAG = "OtoVirtualGUI::ServerSocketThread";
    final private static String SERVER_HELLO = "ServerHello";
    final private static String CLIENT_HELLO = "ClientHello";

    private boolean mRunning = true;

    private LocalSocket mSocket;
    private BlockingQueue mBlockingQueue;
    private String mSocketPath;

    public SocketThread(BlockingQueue blockingQueue, String socketPath) {
        mBlockingQueue = blockingQueue;
        mSocketPath = socketPath;
    }

    public void stopRun() {
        mRunning = false;
    }

    private byte[] lengthToByte(int length) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.BIG_ENDIAN); // optional, the initial order is always BIG_ENDIAN.
        b.putInt(length);
        return b.array();
    }

    private byte[] readLen(InputStream inputStream, int dataLen) throws IOException {
        byte[] buf = new byte[dataLen];
        int toBeRead = dataLen, totalRead = 0, readBytes = 0;
        while (toBeRead > 0 && (readBytes = inputStream.read(buf, totalRead, toBeRead)) != -1) {
            toBeRead -= readBytes;
            totalRead += readBytes;
        }

        if (toBeRead == 0) {
            return buf;
        }
        throw new IOException("Data too short");
    }

    private byte[] readMsg() throws IOException {
        InputStream inputStream = mSocket.getInputStream();
        ByteBuffer b = ByteBuffer.wrap(readLen(inputStream, 4));
        //b.order(ByteOrder.BIG_ENDIAN);
        int dataLen = b.getInt();
        byte[] data = readLen(inputStream, dataLen);
        return data;
    }

    private void eventLoop() throws IOException {
        OutputStream outputStream = mSocket.getOutputStream();
        while (mRunning) {
            try {
                WestonProto.InputEventProto event =
                                             (WestonProto.InputEventProto) mBlockingQueue.take();
                byte[] body = WestonProto.InputEventProto.toByteArray(event);
                outputStream.write(lengthToByte(body.length));
                outputStream.write(body);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    @Override
    public void run() {
        try {
            mSocket = new LocalSocket();
            mSocket.connect(new LocalSocketAddress(mSocketPath,
                                                   LocalSocketAddress.Namespace.FILESYSTEM));
            mSocket.getOutputStream().write(lengthToByte(CLIENT_HELLO.length()));
            mSocket.getOutputStream().write(CLIENT_HELLO.getBytes());

            String msg = new String(readMsg());
            if (msg.equals(SERVER_HELLO)) {
                eventLoop();
            } else {
                Log.e(TAG, "Server hand shake FAILURE, wrong msg: " + msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
            mRunning = false;
        } finally {
            if (mSocket != null) {
                try {
                    mSocket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
