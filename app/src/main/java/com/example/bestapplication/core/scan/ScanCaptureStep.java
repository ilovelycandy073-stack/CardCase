package com.example.bestapplication.core.scan;

/**
 * One capture step in a scan session (e.g. FRONT/BACK).
 */
public class ScanCaptureStep {
    /** A stable identifier for the capture (e.g. "FRONT", "BACK"). */
    public final String stepId;

    public ScanCaptureStep(String stepId) {
        this.stepId = stepId;
    }
}
