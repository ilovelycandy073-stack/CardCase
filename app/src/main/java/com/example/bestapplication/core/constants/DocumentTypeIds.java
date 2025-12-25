package com.example.bestapplication.core.constants;

/**
 * Stable identifiers for document types stored in the local database.
 */
public final class DocumentTypeIds {

    private DocumentTypeIds() {}

    public static final String IDCARD_CN = "IDCARD_CN";

    /**
     * Compatibility note:
     * The initial implementation reused "VEHICLE_LICENSE" as the bank card entry.
     * Keep the value to avoid breaking existing data.
     */
    public static final String BANK_CARD = "VEHICLE_LICENSE";

    public static final String DRIVER_LICENSE = "DRIVER_LICENSE";
    public static final String STUDENT_ID = "STUDENT_ID";
    public static final String WORK_ID = "WORK_ID";
}
