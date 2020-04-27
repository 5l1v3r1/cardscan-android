package com.getbouncer.cardscan.base;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.getbouncer.cardscan.base.ssd.DetectedSSDBox;

import java.util.List;

public interface OnCardholderNameListener {

    public void onLegalNamePrediction(
            @Nullable final String number,
            @Nullable final Expiry expiry,
            @NonNull final Bitmap ocrDetectionBitmap,
            @Nullable final List<DetectedBox> digitBoxes,
            @Nullable final DetectedBox expiryBox,
            @Nullable final List<DetectedSSDBox> boxes,
            @Nullable final String legalName,
            @NonNull final Bitmap objectDetectionBitmap,
            @Nullable final Bitmap screenDetectionBitmap,
            final long frameAddedTimeMs
    );

    void onFatalError();
}
