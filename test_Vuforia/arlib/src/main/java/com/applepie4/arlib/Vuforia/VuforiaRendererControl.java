package com.applepie4.arlib.Vuforia;

import com.vuforia.Matrix44F;
import com.vuforia.State;

public interface VuforiaRendererControl {
    void renderFrame(State state, float[] projectionMatrix, Matrix44F vbProjectionMatrix);
}
