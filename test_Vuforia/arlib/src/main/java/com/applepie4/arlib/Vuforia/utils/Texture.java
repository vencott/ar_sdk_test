package com.applepie4.arlib.Vuforia.utils;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.vuforia.Image;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Texture {
    private static final String LOGTAG = "Vuforia_Texture";

    public int mWidth;
    public int mHeight;
    public int mChannels;
    public ByteBuffer mData;
    public int[] mTextureID = new int[1];
    public boolean mSuccess = false;

    public static Texture loadTextureFromBitmap(Bitmap bitMap) {
        int[] data = new int[bitMap.getWidth() * bitMap.getHeight()];
        bitMap.getPixels(data, 0, bitMap.getWidth(), 0, 0, bitMap.getWidth(), bitMap.getHeight());

        return loadTextureFromIntBuffer(data, bitMap.getWidth(), bitMap.getHeight());
    }

    public static Texture loadTextureFromApk(String fileName,
                                             AssetManager assets) {
        InputStream inputStream = null;
        try {
            inputStream = assets.open(fileName, AssetManager.ACCESS_BUFFER);

            BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
            Bitmap bitMap = BitmapFactory.decodeStream(bufferedStream);

            int[] data = new int[bitMap.getWidth() * bitMap.getHeight()];
            bitMap.getPixels(data, 0, bitMap.getWidth(), 0, 0, bitMap.getWidth(), bitMap.getHeight());

            return loadTextureFromIntBuffer(data, bitMap.getWidth(), bitMap.getHeight());
        } catch (IOException e) {
            Log.e(LOGTAG, "Failed to log texture '" + fileName + "' from APK");
            Log.i(LOGTAG, e.getMessage());
            return null;
        }
    }

    public static Texture loadTextureFromIntBuffer(int[] data, int width,
                                                   int height) {
        int numPixels = width * height;
        byte[] dataBytes = new byte[numPixels * 4];

        for (int p = 0; p < numPixels; ++p) {
            int colour = data[p];
            dataBytes[p * 4] = (byte) (colour >>> 16);
            dataBytes[p * 4 + 1] = (byte) (colour >>> 8);
            dataBytes[p * 4 + 2] = (byte) colour;
            dataBytes[p * 4 + 3] = (byte) (colour >>> 24);
        }

        Texture texture = new Texture();
        texture.mWidth = width;
        texture.mHeight = height;
        texture.mChannels = 4;

        texture.mData = ByteBuffer.allocateDirect(dataBytes.length).order(ByteOrder.nativeOrder());
        int rowSize = texture.mWidth * texture.mChannels;
        for (int r = 0; r < texture.mHeight; r++)
            texture.mData.put(dataBytes, rowSize * (texture.mHeight - 1 - r), rowSize);

        texture.mData.rewind();

        dataBytes = null;
        data = null;

        texture.mSuccess = true;
        return texture;
    }

    public static Texture loadTextureFromImage(Image image) {
        Texture texture = new Texture();
        texture.mWidth = image.getWidth();
        texture.mHeight = image.getHeight();
        texture.mChannels = 4;
        texture.mData = image.getPixels();

        texture.mData.rewind();

        texture.mSuccess = true;

        return texture;
    }
}
