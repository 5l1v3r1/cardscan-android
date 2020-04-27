package com.getbouncer.cardscan.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import com.getbouncer.cardscan.base.GlobalConfig;
import com.getbouncer.cardscan.base.ImageClassifier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

class NameExtractionModel extends ImageClassifier {
    @Nullable
    private File modelFile;
    @Nullable private MappedByteBuffer modelByteBuffer;

    /** the model takes a 224x224 sample images as input */
    private static final int LOAD_SIZE = 224;

    /** model returns whether or not there is a screen present */
    static final int NUM_CLASSES = 27;

    @NotNull
    public float[][] screenPresentEnergies;

    NameExtractionModel(@NotNull File modelFile) {
        this.modelFile = modelFile;
        screenPresentEnergies = new float[1][NUM_CLASSES];
    }

    NameExtractionModel(@NotNull MappedByteBuffer modelByteBuffer) {
        this.modelFile = null;
        this.modelByteBuffer = modelByteBuffer;
        screenPresentEnergies = new float[1][NUM_CLASSES];
    }

    @NotNull
    public synchronized float[] predict(@NotNull Bitmap image, @NotNull Context context) {
        long predictStart = SystemClock.uptimeMillis();
        final int NUM_THREADS = 4;
        try {

            try {
                if(tflite == null) {
                    init(context);
                    setNumThreads(NUM_THREADS);
                }
            } catch (Error | Exception e) {
                Log.e("NameExtractionModel", "Couldn't load NameExtractionModel", e);

            }

            try {
                return runModel(image);
            } catch (Error | Exception e) {
                Log.i("NameExtractionModel", "runModel exception, retry NameExtractionModel", e);
                init(context);
                return runModel(image);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("Performance Diagnostics", "UXModel inference time " + (SystemClock.uptimeMillis() - predictStart));

        float[] r = new float[27];

        return r;
    }

    @NotNull
    private float[] runModel(@NotNull Bitmap image) {
        classifyFrame(image);
        float[] r = new float[27];
        for (int i = 0; i < 27; i++) {
            r[i] = screenPresentEnergies[0][i];
        }
        return r;
    }


    @NotNull
    @Override
    protected MappedByteBuffer loadModelFile(@NotNull Context context) throws IOException {
        if (this.modelByteBuffer != null) {
            return this.modelByteBuffer;
        }

        if (this.modelFile == null) {
            throw new RuntimeException("Cannot load UX model");
        }

        FileInputStream inputStream = new FileInputStream(modelFile);
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = 0;
        long declaredLength = modelFile.length();
        MappedByteBuffer result = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset,
                declaredLength);
        inputStream.close();
        this.modelByteBuffer = result;
        return result;
    }

    @Override
    protected int getImageSizeX() {
        return LOAD_SIZE;
    }

    @Override
    protected int getImageSizeY() {
        return LOAD_SIZE;
    }

    @Override
    protected int getNumBytesPerChannel() {
        return 4;
    }

    @Override
    protected void addPixelValue(int pixelValue) {
        imgData.putFloat(((float) ((pixelValue >> 16) & 0xFF)) / 255);
        imgData.putFloat(((float) ((pixelValue >> 8) & 0xFF)) / 255);
        imgData.putFloat(((float) (pixelValue & 0xFF)) / 255);

    }

    @Override
    protected void runInference() {
        final long startTime = SystemClock.uptimeMillis();
        tflite.run(imgData, screenPresentEnergies);
        if (GlobalConfig.PRINT_TIMING) {
            Log.d("UX model", "Inference time: " + Long.toString(SystemClock.uptimeMillis() - startTime));
        }
    }
}
