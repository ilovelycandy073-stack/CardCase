package com.example.bestapplication.feature.idcard.scan;

import android.content.Context;

import com.example.bestapplication.core.constants.BlobSlots;
import com.example.bestapplication.core.constants.DocumentStatus;
import com.example.bestapplication.core.constants.DocumentTypeIds;
import com.example.bestapplication.core.db.VaultDao;
import com.example.bestapplication.core.db.entity.DocumentItemEntity;
import com.example.bestapplication.core.pdf.IdCardPdfService;
import com.example.bestapplication.core.scan.BlobToSave;
import com.example.bestapplication.core.scan.ScanCaptureStep;
import com.example.bestapplication.core.scan.ScanOutcome;
import com.example.bestapplication.core.scan.ScanSessionParams;
import com.example.bestapplication.core.scan.ScanSpec;
import com.example.bestapplication.core.filestore.EncryptedFileStore;
import com.example.bestapplication.core.network.dto.TencentIdCardOcrResponse;
import com.example.bestapplication.core.network.tencent.TencentAdvancedInfoParser;
import com.example.bestapplication.core.network.tencent.TencentOcrIdCardClient;
import com.example.bestapplication.core.network.tencent.TencentSecrets;
import com.example.bestapplication.core.util.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ScanSpec for Chinese ID card.
 */
public class IdCardScanSpec implements ScanSpec {

    public static final String STEP_FRONT = "FRONT";
    public static final String STEP_BACK = "BACK";

    @Override
    public List<ScanCaptureStep> getCaptureSteps(ScanSessionParams params) {
        List<ScanCaptureStep> steps = new ArrayList<>();
        steps.add(new ScanCaptureStep(STEP_FRONT));
        steps.add(new ScanCaptureStep(STEP_BACK));
        return steps;
    }

    @Override
    public String getTitle(ScanSessionParams params) {
        return "扫描身份证";
    }

    @Override
    public String getTipForStep(ScanSessionParams params, int stepIndex) {
        return stepIndex == 0 ? "拍摄人像面" : "拍摄国徽面";
    }

    @Override
    public boolean requiresSecrets(ScanSessionParams params) {
        return true;
    }

    @Override
    public DocumentItemEntity resolveOrCreateItem(VaultDao dao, ScanSessionParams params) {
        String itemId = params == null ? null : params.itemId;
        String targetId = itemId == null ? "" : itemId.trim();

        DocumentItemEntity item;
        if (!targetId.isEmpty()) {
            item = dao.findItem(targetId);
            if (item == null) {
                item = new DocumentItemEntity(
                        targetId,
                        DocumentTypeIds.IDCARD_CN,
                        "{}",
                        System.currentTimeMillis(),
                        DocumentStatus.CREATED
                );
            }
        } else {
            item = dao.findFirstItemByType(DocumentTypeIds.IDCARD_CN);
            if (item == null) {
                item = new DocumentItemEntity(
                        UUID.randomUUID().toString(),
                        DocumentTypeIds.IDCARD_CN,
                        "{}",
                        System.currentTimeMillis(),
                        DocumentStatus.CREATED
                );
            }
        }
        return item;
    }

    @Override
    public void beforeProcess(VaultDao dao, DocumentItemEntity item, ScanSessionParams params) {
        item.status = DocumentStatus.OCR_PROCESSING;
        item.updatedAt = System.currentTimeMillis();
        dao.upsertItem(item);
    }

    @Override
    public ScanOutcome process(Context ctx, VaultDao dao, EncryptedFileStore store, ScanSessionParams params, DocumentItemEntity item, Map<String, byte[]> captures) throws Exception {
        byte[] frontJpeg = captures.get(STEP_FRONT);
        byte[] backJpeg = captures.get(STEP_BACK);
        if (frontJpeg == null || backJpeg == null) {
            throw new IllegalStateException("front/back image missing");
        }

        // Preserve custom title.
        String preservedCustomTitle = "";
        try {
            JsonObject old = JsonUtils.safeParseObject(item.infoJson);
            preservedCustomTitle = JsonUtils.getString(old, "customTitle");
        } catch (Exception ignored) {}

        TencentOcrIdCardClient client = new TencentOcrIdCardClient(
                TencentSecrets.SECRET_ID,
                TencentSecrets.SECRET_KEY,
                TencentSecrets.REGION
        );

        long ts1 = System.currentTimeMillis() / 1000L;
        TencentIdCardOcrResponse front = client.idCardOcr(frontJpeg, "FRONT", ts1);
        if (front == null || front.Response == null) throw new RuntimeException("Empty response(front)");
        if (front.Response.Error != null) {
            throw new RuntimeException(front.Response.Error.Code + ": " + front.Response.Error.Message);
        }

        long ts2 = System.currentTimeMillis() / 1000L;
        TencentIdCardOcrResponse back = client.idCardOcr(backJpeg, "BACK", ts2);
        if (back == null || back.Response == null) throw new RuntimeException("Empty response(back)");
        if (back.Response.Error != null) {
            throw new RuntimeException(back.Response.Error.Code + ": " + back.Response.Error.Message);
        }

        // Try cropped image from AdvancedInfo; fallback to original.
        byte[] frontCrop = TencentAdvancedInfoParser.extractCroppedIdCardJpeg(front.Response.AdvancedInfo);
        byte[] backCrop = TencentAdvancedInfoParser.extractCroppedIdCardJpeg(back.Response.AdvancedInfo);

        byte[] frontToSave = (frontCrop != null) ? frontCrop : frontJpeg;
        byte[] backToSave = (backCrop != null) ? backCrop : backJpeg;

        // Compose infoJson
        Map<String, Object> info = new HashMap<>();
        info.put("name", front.Response.Name);
        info.put("sex", front.Response.Sex);
        info.put("nation", front.Response.Nation);
        info.put("birth", front.Response.Birth);
        info.put("address", front.Response.Address);
        info.put("idNum", front.Response.IdNum);
        info.put("idNumMasked", maskId(front.Response.IdNum));
        info.put("authority", back.Response.Authority);
        info.put("validDate", back.Response.ValidDate);

        info.put("advancedFront", front.Response.AdvancedInfo);
        info.put("advancedBack", back.Response.AdvancedInfo);
        info.put("requestIdFront", front.Response.RequestId);
        info.put("requestIdBack", back.Response.RequestId);

        if (!JsonUtils.isEmpty(preservedCustomTitle)) {
            info.put("customTitle", preservedCustomTitle.trim());
        }

        // Build PDF (two pages)
        IdCardPdfService pdfService = new IdCardPdfService();
        byte[] pdfBytes;
        try (InputStream fIn = new ByteArrayInputStream(frontToSave);
             InputStream bIn = new ByteArrayInputStream(backToSave)) {
            pdfBytes = pdfService.buildPdfBytes(fIn, bIn, info);
        }

        String base = "idcard/" + item.itemId + "/";
        String frontPath = base + "front.jpg.enc";
        String backPath = base + "back.jpg.enc";
        String pdfPath = base + "idcard.pdf.enc";

        List<BlobToSave> blobs = new ArrayList<>();
        blobs.add(new BlobToSave(
                item.itemId + "_IDCARD_FRONT",
                BlobSlots.IDCARD_FRONT,
                frontPath,
                "image/jpeg",
                frontToSave
        ));
        blobs.add(new BlobToSave(
                item.itemId + "_IDCARD_BACK",
                BlobSlots.IDCARD_BACK,
                backPath,
                "image/jpeg",
                backToSave
        ));
        blobs.add(new BlobToSave(
                item.itemId + "_IDCARD_PDF",
                BlobSlots.IDCARD_PDF,
                pdfPath,
                "application/pdf",
                pdfBytes
        ));

        return new ScanOutcome(
                DocumentStatus.COMPLETED,
                new Gson().toJson(info),
                blobs
        );
    }

    private static String maskId(String id) {
        if (id == null) return "";
        String s = id.trim();
        if (s.length() < 8) return "****";
        return s.substring(0, 4) + "**********" + s.substring(s.length() - 4);
    }
}
