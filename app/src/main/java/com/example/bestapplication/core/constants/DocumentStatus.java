package com.example.bestapplication.core.constants;

/**
 * Common status values stored on {@code DocumentItemEntity.status}.
 */
public final class DocumentStatus {

    private DocumentStatus() {}

    public static final String CREATED = "CREATED";
    public static final String OCR_PROCESSING = "OCR_PROCESSING";

    // Bank card specific intermediate states
    public static final String FRONT_OCR_DONE = "FRONT_OCR_DONE";
    public static final String BACK_SAVED = "BACK_SAVED";

    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";
}
