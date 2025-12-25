package com.example.bestapplication.core.scan;

import android.content.Context;

import com.example.bestapplication.core.db.VaultDao;
import com.example.bestapplication.core.db.entity.DocumentItemEntity;
import com.example.bestapplication.core.filestore.EncryptedFileStore;

import java.util.List;
import java.util.Map;

/**
 * A document-specific spec that plugs into the generic scan pipeline.
 */
public interface ScanSpec {

    /**
     * Capture steps in order. The generic UI uses this to drive multi-shot flows.
     */
    List<ScanCaptureStep> getCaptureSteps(ScanSessionParams params);

    /** Title text displayed at the top (optional; return empty to leave as-is). */
    String getTitle(ScanSessionParams params);

    /** Tip text for current step or processing state. */
    String getTipForStep(ScanSessionParams params, int stepIndex);

    /** Whether Tencent OCR secrets are required for this session. */
    boolean requiresSecrets(ScanSessionParams params);

    /** Resolve the target item or create if needed. */
    DocumentItemEntity resolveOrCreateItem(VaultDao dao, ScanSessionParams params) throws Exception;

    /** Called before processing; implementations may update status and upsert item. */
    void beforeProcess(VaultDao dao, DocumentItemEntity item, ScanSessionParams params);

    /**
     * Document-specific processing. Build blobs + infoJson + final status.
     *
     * @param captures map from stepId to captured JPEG bytes.
     */
    ScanOutcome process(
            Context ctx,
            VaultDao dao,
            EncryptedFileStore store,
            ScanSessionParams params,
            DocumentItemEntity item,
            Map<String, byte[]> captures
    ) throws Exception;
}
