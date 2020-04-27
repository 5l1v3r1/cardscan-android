package com.getbouncer.cardscan.base;

import android.content.Context;


import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.MappedByteBuffer;

public class LocalNameExtractionModelFactory extends ModelFactory {

    @Override
    @NotNull
    public MappedByteBuffer loadModelFile(@NotNull Context context) throws IOException {
        return loadModelFromResource(context, R.raw.char_v4_05_0_98_16);
    }

}
