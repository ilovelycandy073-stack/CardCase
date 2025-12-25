package com.example.bestapplication.core.scan;

import android.content.Context;

import com.example.bestapplication.core.constants.DocumentStatus;
import com.example.bestapplication.core.db.AppDatabase;
import com.example.bestapplication.core.db.VaultDao;
import com.example.bestapplication.core.db.entity.BlobRefEntity;
import com.example.bestapplication.core.db.entity.DocumentItemEntity;
import com.example.bestapplication.core.filestore.EncryptedFileStore;
import com.example.bestapplication.core.network.tencent.TencentSecrets;
import com.example.bestapplication.core.util.JsonUtils;

import java.util.Map;

/**
 * Generic scan pipeline runner.
 *
 * Run this from a worker thread.
 */
public final class ScanPipeline {

    private ScanPipeline() {}

    public static DocumentItemEntity run(
            Context ctx,
            ScanSpec spec,
            ScanSessionParams params,
            Map<String, byte[]> captures
    ) throws Exception {

        DocumentItemEntity item = null;
        VaultDao dao = null;
        try {
            if (spec.requiresSecrets(params)) {
                if (JsonUtils.isEmpty(TencentSecrets.SECRET_ID) || JsonUtils.isEmpty(TencentSecrets.SECRET_KEY)) {
                    throw new IllegalStateException("请在 local.properties 或环境变量中配置 TC_SECRET_ID / TC_SECRET_KEY");
                }
            }

            AppDatabase db = AppDatabase.get(ctx);
            dao = db.dao();
            item = spec.resolveOrCreateItem(dao, params);
            spec.beforeProcess(dao, item, params);

            EncryptedFileStore store = new EncryptedFileStore(ctx);
            ScanOutcome outcome = spec.process(ctx, dao, store, params, item, captures);

            if (outcome != null) {
                // 1) Persist blobs
                for (BlobToSave b : outcome.blobs) {
                    if (b == null || b.bytes == null) continue;
                    store.saveBytes(b.relPath, b.bytes);
                    dao.upsertBlob(new BlobRefEntity(
                            b.blobRefId,
                            item.itemId,
                            b.slot,
                            b.relPath,
                            b.mimeType,
                            System.currentTimeMillis()
                    ));
                }

                // 2) Update item
                if (outcome.newInfoJson != null) {
                    item.infoJson = outcome.newInfoJson;
                }
                if (outcome.newStatus != null) {
                    item.status = outcome.newStatus;
                }
                item.updatedAt = System.currentTimeMillis();
                dao.upsertItem(item);
            }

            return item;

        } catch (Exception e) {
            // Best-effort mark failed.
            if (item != null) {
                try {
                    item.status = DocumentStatus.FAILED;
                    item.updatedAt = System.currentTimeMillis();
                    if (dao == null) {
                        AppDatabase db = AppDatabase.get(ctx);
                        db.dao().upsertItem(item);
                    } else {
                        dao.upsertItem(item);
                    }
                } catch (Exception ignored) {}
            }
            throw e;
        }
    }
}
