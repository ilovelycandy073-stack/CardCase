package com.example.bestapplication.core.scan;

/**
 * Parameters for a scan session. Keep this small and extensible.
 */
public class ScanSessionParams {
    public String itemId;
    public String mode;

    public ScanSessionParams() {}

    public ScanSessionParams(String itemId, String mode) {
        this.itemId = itemId;
        this.mode = mode;
    }
}
