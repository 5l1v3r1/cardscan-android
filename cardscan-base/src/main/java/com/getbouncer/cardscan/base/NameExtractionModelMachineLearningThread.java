package com.getbouncer.cardscan.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.getbouncer.cardscan.base.ssd.DetectedSSDBox;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;


public class NameExtractionModelMachineLearningThread extends MachineLearningThread {

    synchronized public void post(
            @androidx.annotation.Nullable byte[] frameBytes,
            int width,
            int height,
            int format,
            int sensorOrientation,
            @androidx.annotation.Nullable OnObjectListener objectListener,
            @androidx.annotation.Nullable OnCardholderNameListener cardholderlNameListener,
            @NonNull Context context,
            @androidx.annotation.Nullable File objectDetectFile,
            float roiCenterYRatio
    ) {

        RunArguments args = new RunArguments(frameBytes , width, height, format, sensorOrientation,
                objectListener, cardholderlNameListener, context, objectDetectFile, roiCenterYRatio);
        queue.push(args);
        notify();
    }

    private void runObjectModel(
            final Bitmap bitmapForObjectDetection,
            final RunArguments args,
            final Bitmap bitmapForScreenDetection,
            final long frameAddedTimeMs
    ) {
        final ObjectDetect detect = new ObjectDetect(args.mObjectDetectFile);
        detect.predictOnCpu(bitmapForObjectDetection, args.mContext);

        boolean shouldRunNameExtraction = false;
        RectF nameRect = null;

        // check to see if we detected a legal name. If so, run our name extraction model, serially for now
        for (DetectedSSDBox box:detect.objectBoxes) {
            if (box.label == 10) {
               shouldRunNameExtraction = true;
               nameRect = box.rect;
               break;
            }
        }
        final String name;
        if (shouldRunNameExtraction) {
            nameRect.height();
            int x = (int) (nameRect.left * bitmapForObjectDetection.getWidth());
            int y = (int) (nameRect.top * bitmapForObjectDetection.getHeight());
            int width = (int) (nameRect.width() * bitmapForObjectDetection.getWidth());
            int height = (int) (nameRect.height() * bitmapForObjectDetection.getHeight());

            x = (int) nameRect.left;
            y = (int) nameRect.top;
            width = (int) nameRect.width();
            height = (int) nameRect.height();
            Log.d("STEVEN", "raw name x: " + nameRect.left);
            Log.d("STEVEN", "raw name y: " + nameRect.top);
            Log.d("STEVEN", "raw name width: " + nameRect.width());
            Log.d("STEVEN", "raw name height: " + nameRect.height());

            Log.d("STEVEN", "name x: " + x);
            Log.d("STEVEN", "name y: " + x);
            Log.d("STEVEN", "name width: " + width);
            Log.d("STEVEN", "name height: " + height);

            Bitmap nameBitmap = Bitmap.createBitmap(bitmapForObjectDetection, x, y, width, height);

            int charWidth = height;
            int begin = 0;
            StringBuffer nameBuffer = new StringBuffer();
            int backgroundCount = 0;
            int repeatCharCount = 0;
            String curChar = null;
            while (begin < width - charWidth) {
                Bitmap firstLetter = Bitmap.createBitmap(nameBitmap, begin, 0, charWidth, charWidth);
                String prediction = NameExtractionModelDetection.predictCharacter(firstLetter, args.mContext);
                if (prediction.equals(" ")) {
                    backgroundCount += 1;
                    if (backgroundCount == 3) {
                        nameBuffer.append(' ');
                    }
                } else {
                    backgroundCount = 0;
                    if (prediction.equals(curChar)) {
                        repeatCharCount += 1;
                    } else {
                        repeatCharCount = 0;
                    }
                    if (repeatCharCount == 1) {
                        // pass
                    } else {
                        curChar = prediction;
                        nameBuffer.append(prediction);
                    }
                }
                begin += charWidth / 4;
            }
            name = nameBuffer.toString();
        } else {
            name = null;
        }
        if (name != null) {
            Log.d("PREDICTION", "name: " + name);
        }

        // hit handler for legal name
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                try {
                    //Log.d("STEVEN", "valid object detection file " + args.mObjectListener);
                    if (args.mCardholderNameListener != null) {
                        if (detect.hadUnrecoverableException) {
                            //args.mCardholderNameListener.onObjectFatalError();
                        } else {
                            Log.d("STEVEN", "posting cardholder thing");
                            args.mCardholderNameListener.onLegalNamePrediction(
                                    "",
                                    null,
                                    bitmapForObjectDetection,
                                    null,
                                    null,
                                    detect.objectBoxes,
                                    name,
                                    bitmapForObjectDetection,
                                    bitmapForScreenDetection,
                                    frameAddedTimeMs
                            );
                        }
                    }
                    bitmapForObjectDetection.recycle();
                    if (bitmapForScreenDetection != null) {
                        bitmapForScreenDetection.recycle();
                    }
                } catch (Error | Exception e) {
                    // prevent callbacks from crashing the app, swallow it
                    e.printStackTrace();
                }
            }
        });
    }

    private void runModel() {
        final RunArguments args = getNextImage();

        Bitmap objectDetect;
        @Nullable Bitmap screenDetect;
        if (args.mFrameBytes != null) {
            Bitmaps bitmaps = getBitmaps(
                    args.mFrameBytes,
                    args.mWidth,
                    args.mHeight,
                    args.mFormat,
                    args.mSensorOrientation,
                    args.mRoiCenterYRatio,
                    args.mIsOcr
            );
            objectDetect = bitmaps.objectDetect;
            screenDetect = bitmaps.screenDetect;
        } else if (args.mBitmap != null) {
            objectDetect = args.mBitmap;
            screenDetect = null;
        } else {

            objectDetect = Bitmap.createBitmap(480, 480, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(objectDetect);
            Paint paint = new Paint();
            paint.setColor(Color.GRAY);
            canvas.drawRect(0.0f, 0.0f, 480.0f, 480.0f, paint);
            screenDetect = null;
        }

        runObjectModel(objectDetect, args, screenDetect, args.frameAddedTimeMs);

    }

    @Override
    public void run() {
        while (true) {
            try {
                runModel();
            } catch (Error | Exception e) {
                // center field exception handling, make sure that the ml thread keeps running
                e.printStackTrace();
            }
        }
    }
}
