package com.example.bestapplication.core.scan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result of a scan pipeline processing step.
 */
public class ScanOutcome {
    /** If non-null, replaces item.status. */
    public final String newStatus;

    /** If non-null, replaces item.infoJson. */
    public final String newInfoJson;

    /** Blobs to persist (encrypted) and upsert into blob_ref. */
    public final List<BlobToSave> blobs;

    public ScanOutcome(String newStatus, String newInfoJson, List<BlobToSave> blobs) {
        this.newStatus = newStatus;
        this.newInfoJson = newInfoJson;
        if (blobs == null) {
            this.blobs = Collections.emptyList();
        } else {
            this.blobs = Collections.unmodifiableList(new ArrayList<>(blobs));
        }
    }
}
