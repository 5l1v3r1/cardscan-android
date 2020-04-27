package com.getbouncer.cardscan.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.MappedByteBuffer;

public class NameExtractionModelDetection {

    private static NameExtractionModel nameExtractionModel = null;

    static void configure(@NotNull Context applicationContext) {
        try {
            MappedByteBuffer model = new LocalNameExtractionModelFactory().loadModelFile(applicationContext);
            nameExtractionModel = new NameExtractionModel(model);
        } catch (IOException e) {
            Log.w("Bouncer", "Unable to load local UXModel");
        }
    }

    public static float[] predict(Bitmap bitmap, Context context) {
        if (nameExtractionModel == null) {
            configure(context);
        }
        return nameExtractionModel.predict(bitmap, context);
    }

    public static String predictCharacter(Bitmap bitmap, Context context) {
        float[] prediction = predict(bitmap, context);
        int index = getIndexOfLargest(prediction);
        String result = "";
        if (prediction[index] > 0.7) {
            if (index == 0) {
                Log.d("PREDICTION", "BACKGROUND: " + prediction[index]);
                result += ' ';
            } else {
                int c = 'A' - (char) 1 + (char) index;
                char ch = (char) c;
                Log.d("PREDICTION", "" + ch + ": " + prediction[index]);
                result += ch;
            }
        }
        return result;
    }

    private static int getIndexOfLargest( float[] array )
    {
        if ( array == null || array.length == 0 ) return -1; // null or empty

        int largest = 0;
        for ( int i = 1; i < array.length; i++ )
        {
            if ( array[i] > array[largest] ) largest = i;
        }
        return largest; // position of the first largest found
    }
}
