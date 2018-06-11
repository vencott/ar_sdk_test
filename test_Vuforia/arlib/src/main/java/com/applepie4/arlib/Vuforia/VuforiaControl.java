package com.applepie4.arlib.Vuforia;

import com.vuforia.State;

public interface VuforiaControl {
    boolean doInitTrackers();

    boolean doLoadTrackersData();

    boolean doStartTrackers();

    boolean doStopTrackers();

    boolean doUnloadTrackersData();

    boolean doDeinitTrackers();

    void onInitARDone(VuforiaException e);

    void onVuforiaUpdate(State state);
}
