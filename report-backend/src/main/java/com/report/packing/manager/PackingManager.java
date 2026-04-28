package com.report.packing.manager;

public interface PackingManager {
    void executePacking();
    boolean checkAndWaitForConsumption();
    void triggerNextPacking();
}