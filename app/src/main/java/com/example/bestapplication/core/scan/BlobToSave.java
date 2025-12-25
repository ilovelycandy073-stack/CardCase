package com.example.bestapplication.core.scan;

/**
 * A request to persist an encrypted blob and its DB reference.
 */
public class BlobToSave {
    public final String blobRefId;
    public final String slot;
    public final String relPath;
    public final String mimeType;
    public final byte[] bytes;

    public BlobToSave(String blobRefId, String slot, String relPath, String mimeType, byte[] bytes) {
        this.blobRefId = blobRefId;
        this.slot = slot;
        this.relPath = relPath;
        this.mimeType = mimeType;
        this.bytes = bytes;
    }
}
