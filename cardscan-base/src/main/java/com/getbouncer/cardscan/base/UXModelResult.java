package com.getbouncer.cardscan.base;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UXModelResult {

    public final float noCardScore;
    public final float panSideScore;
    public final float noPanSideScore;
    private float[] modelOutput;
    private float maxScore;
    @Nullable private UXModelEnum uxModelEnum;

    public UXModelResult(@NonNull float[] modelOutput) {
        noCardScore = -20;
        panSideScore = modelOutput[0];
        noPanSideScore = modelOutput[1];
        this.modelOutput = modelOutput;

        this.calculateResult();
    }

    public enum UXModelEnum {
        NO_PAN_SIDE,
        NO_CARD,
        PAN_SIDE
    }

    @Nullable
    public UXModelEnum getResult() {
        return this.uxModelEnum;
    }

    public float getMaxScore() {
        return this.maxScore;
    }

    private void calculateResult() {
        if (panSideScore >= noPanSideScore) {
            maxScore = panSideScore;
            uxModelEnum = UXModelEnum.PAN_SIDE;
        } else {
            maxScore = panSideScore;
            uxModelEnum = UXModelEnum.NO_PAN_SIDE;
        }
    }
}
