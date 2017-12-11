package com.user.streamingpush;

import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by user0 on 2017/11/10.
 */

public class Utils {
    /**
     * 将YUV420SP数据顺时针旋转90度
     *
     * @param data        要旋转的数据
     * @param imageWidth  要旋转的图片宽度
     * @param imageHeight 要旋转的图片高度
     * @return 旋转后的数据
     */
    public static byte[] rotateNV21Degree90(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i--;
            }
        }
        return yuv;
    }

    /**
     * 将YUV420SP数据逆时针旋转90度
     *
     * @param src      要旋转的数据
     * @param srcWidth 要旋转的图片宽度
     * @param height   要旋转的图片高度
     * @return 旋转后的数据
     */
    public static byte[] rotateNV21Negative90(byte[] src, int srcWidth, int height) {
        byte[] dst = new byte[srcWidth * height * 3 / 2];
        int nWidth = 0, nHeight = 0;
        int wh = 0;
        int uvHeight = 0;
        if (srcWidth != nWidth || height != nHeight) {
            nWidth = srcWidth;
            nHeight = height;
            wh = srcWidth * height;
            uvHeight = height >> 1;//uvHeight = height / 2
        }

        //旋转Y
        int k = 0;
        for (int i = 0; i < srcWidth; i++) {
            int nPos = srcWidth - 1;
            for (int j = 0; j < height; j++) {
                dst[k] = src[nPos - i];
                k++;
                nPos += srcWidth;
            }
        }

        for (int i = 0; i < srcWidth; i += 2) {
            int nPos = wh + srcWidth - 1;
            for (int j = 0; j < uvHeight; j++) {
                dst[k] = src[nPos - i - 1];
                dst[k + 1] = src[nPos - i];
                k += 2;
                nPos += srcWidth;
            }
        }
        return dst;
    }

    /**
     * 将YUV420SP数据顺时针旋转90度
     *
     * @param src       要旋转的数据
     * @param srcWidth  要旋转的图片宽度
     * @param srcHeight 要旋转的图片高度
     * @return 旋转后的数据
     */
    public static byte[] rotateNV21Positive90(byte[] src, int srcWidth, int srcHeight) {
        byte[] dst = new byte[srcWidth * srcHeight * 3 / 2];
        int nWidth = 0, nHeight = 0;
        int wh = 0;
        int uvHeight = 0;
        if (srcWidth != nWidth || srcHeight != nHeight) {
            nWidth = srcWidth;
            nHeight = srcHeight;
            wh = srcWidth * srcHeight;
            uvHeight = srcHeight >> 1;//uvHeight = height / 2
        }

        //旋转Y
        int k = 0;
        for (int i = 0; i < srcWidth; i++) {
            int nPos = 0;
            for (int j = 0; j < srcHeight; j++) {
                dst[k] = src[nPos + i];
                k++;
                nPos += srcWidth;
            }
        }

        for (int i = 0; i < srcWidth; i += 2) {
            int nPos = wh;
            for (int j = 0; j < uvHeight; j++) {
                dst[k] = src[nPos + i];
                dst[k + 1] = src[nPos + i + 1];
                k += 2;
                nPos += srcWidth;
            }
        }
        return dst;
    }

    public static byte[] rotateYUV420Degree180(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int i = 0;
        int count = 0;

        for (i = imageWidth * imageHeight - 1; i >= 0; i--) {
            yuv[count] = data[i];
            count++;
        }

        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (i = imageWidth * imageHeight * 3 / 2 - 1; i >= imageWidth
                * imageHeight; i -= 2) {
            yuv[count++] = data[i - 1];
            yuv[count++] = data[i];
        }
        return yuv;
    }

    public static void convert(byte[] data, ByteBuffer buffer, int width, int height,
                               int mYPadding, boolean mPlanar, boolean mPanesReversed) {
        byte[] result = convert(data, width, height, mYPadding, mPlanar, mPanesReversed);
        int min = buffer.capacity() < data.length ? buffer.capacity() : data.length;
        buffer.put(result, 0, min);
    }

    public static byte[] convert(byte[] data, int width, int height,
                                 int mYPadding, boolean mPlanar, boolean mPanesReversed) {
        // A buffer large enough for every case
        byte[] mBuffer = new byte[3 * height * width / 2];
        int mSize = width * height;
        if (mBuffer == null || mBuffer.length != 3 * width * height / 2 + mYPadding) {
            mBuffer = new byte[3 * height * width / 2 + mYPadding];
        }

        if (!mPlanar) {
            // Swaps U and V
            if (!mPanesReversed) {
                for (int i = mSize; i < mSize + mSize / 2; i += 2) {
                    mBuffer[0] = data[i + 1];
                    data[i + 1] = data[i];
                    data[i] = mBuffer[0];
                }
            }
            if (mYPadding > 0) {
                System.arraycopy(data, 0, mBuffer, 0, mSize);
                System.arraycopy(data, mSize, mBuffer, mSize + mYPadding, mSize / 2);
                return mBuffer;
            }
            return data;
        } else {
            // De-interleave U and V
            if (!mPanesReversed) {
                for (int i = 0; i < mSize / 4; i += 1) {
                    mBuffer[i] = data[mSize + 2 * i + 1];
                    mBuffer[mSize / 4 + i] = data[mSize + 2 * i];
                }
            } else {
                for (int i = 0; i < mSize / 4; i += 1) {
                    mBuffer[i] = data[mSize + 2 * i];
                    mBuffer[mSize / 4 + i] = data[mSize + 2 * i + 1];
                }
            }
            if (mYPadding == 0) {
                System.arraycopy(mBuffer, 0, data, mSize, mSize / 2);
            } else {
                System.arraycopy(data, 0, mBuffer, 0, mSize);
                System.arraycopy(mBuffer, 0, mBuffer, mSize + mYPadding, mSize / 2);
                return mBuffer;
            }
            return data;
        }
    }

    public static void dumpOutputBuffer(byte[] buffer, int offset, int length, String path, boolean append) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path, append);
            fos.write(buffer, offset, length);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}