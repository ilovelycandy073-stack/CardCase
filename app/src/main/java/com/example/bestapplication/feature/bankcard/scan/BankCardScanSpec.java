package com.example.bestapplication.feature.bankcard.scan;

import android.util.Base64;

import com.example.bestapplication.core.constants.BlobSlots;
import com.example.bestapplication.core.constants.DocumentStatus;
import com.example.bestapplication.core.constants.DocumentTypeIds;
import com.example.bestapplication.core.db.VaultDao;
import com.example.bestapplication.core.db.entity.DocumentItemEntity;
import com.example.bestapplication.core.filestore.EncryptedFileStore;
import com.example.bestapplication.core.network.dto.TencentBankCardOcrResponse;
import com.example.bestapplication.core.network.tencent.TencentOcrBankCardClient;
import com.example.bestapplication.core.network.tencent.TencentSecrets;
import com.example.bestapplication.core.scan.BlobToSave;
import com.example.bestapplication.core.scan.ScanCaptureStep;
import com.example.bestapplication.core.scan.ScanOutcome;
import com.example.bestapplication.core.scan.ScanSessionParams;
import com.example.bestapplication.core.scan.ScanSpec;
import com.example.bestapplication.core.util.JsonUtils;
import com.example.bestapplication.feature.bankcard.BankCardDetailActivity;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ScanSpec for bank card.
 */
public class BankCardScanSpec implements ScanSpec {

    public static final String STEP_FRONT = "FRONT";
    public static final String STEP_BACK = "BACK";

    @Override
    public List<ScanCaptureStep> getCaptureSteps(ScanSessionParams params) {
        List<ScanCaptureStep> steps = new ArrayList<>();
        String mode = params == null ? null : params.mode;
        if (BankCardDetailActivity.MODE_BACK_ONLY.equals(mode)) {
            steps.add(new ScanCaptureStep(STEP_BACK));
        } else {
            steps.add(new ScanCaptureStep(STEP_FRONT));
        }
        return steps;
    }

    @Override
    public String getTitle(ScanSessionParams params) {
        return "扫描银行卡";
    }

    @Override
    public String getTipForStep(ScanSessionParams params, int stepIndex) {
        String mode = params == null ? null : params.mode;
        if (BankCardDetailActivity.MODE_BACK_ONLY.equals(mode)) {
            return "拍摄反面（仅保存图片）";
        }
        return "拍摄正面（识别并保存）";
    }

    @Override
    public boolean requiresSecrets(ScanSessionParams params) {
        // Only required when doing FRONT OCR.
        String mode = params == null ? null : params.mode;
        return !BankCardDetailActivity.MODE_BACK_ONLY.equals(mode);
    }

    @Override
    public DocumentItemEntity resolveOrCreateItem(VaultDao dao, ScanSessionParams params) throws Exception {
        String itemId = params == null ? null : params.itemId;
        if (JsonUtils.isEmpty(itemId)) {
            throw new IllegalStateException("参数错误：缺少 itemId");
        }
        DocumentItemEntity item = dao.findItem(itemId);
        if (item == null) {
            // Defensive: create a placeholder row.
            item = new DocumentItemEntity(itemId, DocumentTypeIds.BANK_CARD, "{}", System.currentTimeMillis(), DocumentStatus.CREATED);
        }
        return item;
    }

    @Override
    public void beforeProcess(VaultDao dao, DocumentItemEntity item, ScanSessionParams params) {
        String mode = params == null ? null : params.mode;
        if (!BankCardDetailActivity.MODE_BACK_ONLY.equals(mode)) {
            item.status = DocumentStatus.OCR_PROCESSING;
            item.updatedAt = System.currentTimeMillis();
        }
        dao.upsertItem(item);
    }

    @Override
    public ScanOutcome process(android.content.Context ctx, VaultDao dao, EncryptedFileStore store, ScanSessionParams params, DocumentItemEntity item, Map<String, byte[]> captures) throws Exception {
        String mode = params == null ? null : params.mode;

        if (BankCardDetailActivity.MODE_BACK_ONLY.equals(mode)) {
            byte[] backJpeg = captures.get(STEP_BACK);
            if (backJpeg == null || backJpeg.length == 0) {
                throw new IllegalStateException("image empty");
            }
            String backPath = "bankcard/" + item.itemId + "/back.jpg.enc";
            List<BlobToSave> blobs = new ArrayList<>();
            blobs.add(new BlobToSave(
                    item.itemId + "_BANKCARD_BACK",
                    BlobSlots.BANKCARD_BACK,
                    backPath,
                    "image/jpeg",
                    backJpeg
            ));

            String status = DocumentStatus.BACK_SAVED;
            if (dao.findBlobBySlot(item.itemId, BlobSlots.BANKCARD_FRONT) != null) {
                status = DocumentStatus.COMPLETED;
            }
            return new ScanOutcome(status, null, blobs);
        }

        // FRONT OCR
        byte[] frontCaptured = captures.get(STEP_FRONT);
        if (frontCaptured == null || frontCaptured.length == 0) {
            throw new IllegalStateException("image empty");
        }

        TencentOcrBankCardClient client = new TencentOcrBankCardClient(
                TencentSecrets.SECRET_ID,
                TencentSecrets.SECRET_KEY,
                TencentSecrets.REGION
        );

        long ts = System.currentTimeMillis() / 1000L;
        TencentBankCardOcrResponse resp = client.bankCardOcr(frontCaptured, ts);
        if (resp == null || resp.Response == null) throw new RuntimeException("Empty response");
        if (resp.Response.Error != null) {
            throw new RuntimeException(resp.Response.Error.Code + ": " + resp.Response.Error.Message);
        }

        // Prefer cropped border-cut image.
        byte[] frontToSave = frontCaptured;
        if (!JsonUtils.isEmpty(resp.Response.BorderCutImage)) {
            try {
                frontToSave = Base64.decode(resp.Response.BorderCutImage, Base64.DEFAULT);
            } catch (Exception ignored) {
                frontToSave = frontCaptured;
            }
        }

        String frontPath = "bankcard/" + item.itemId + "/front.jpg.enc";
        List<BlobToSave> blobs = new ArrayList<>();
        blobs.add(new BlobToSave(
                item.itemId + "_BANKCARD_FRONT",
                BlobSlots.BANKCARD_FRONT,
                frontPath,
                "image/jpeg",
                frontToSave
        ));

        // Merge infoJson (preserve existing)
        JsonObject obj = JsonUtils.safeParseObject(item.infoJson);
        obj.addProperty("CardNo", JsonUtils.nvl(resp.Response.CardNo));
        obj.addProperty("BankInfo", JsonUtils.nvl(resp.Response.BankInfo));
        obj.addProperty("ValidDate", JsonUtils.nvl(resp.Response.ValidDate));
        obj.addProperty("CardType", JsonUtils.nvl(resp.Response.CardType));
        obj.addProperty("CardName", JsonUtils.nvl(resp.Response.CardName));
        obj.addProperty("CardCategory", JsonUtils.nvl(resp.Response.CardCategory));
        obj.addProperty("QualityValue", resp.Response.QualityValue == null ? "" : String.valueOf(resp.Response.QualityValue));
        obj.addProperty("WarningCode", joinWarning(resp.Response.WarningCode));
        obj.addProperty("RequestId", JsonUtils.nvl(resp.Response.RequestId));

        String cardNo = JsonUtils.nvl(resp.Response.CardNo);
        obj.addProperty("CardNoMasked", maskCardNo(cardNo));

        String status = DocumentStatus.FRONT_OCR_DONE;
        if (dao.findBlobBySlot(item.itemId, BlobSlots.BANKCARD_BACK) != null) {
            status = DocumentStatus.COMPLETED;
        }

        return new ScanOutcome(status, new Gson().toJson(obj), blobs);
    }

    private static String joinWarning(List<Integer> codes) {
        if (codes == null || codes.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < codes.size(); i++) {
            sb.append(codes.get(i));
            if (i != codes.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    private static String maskCardNo(String cardNo) {
        if (cardNo == null) return "";
        String s = cardNo.trim();
        if (s.length() <= 8) return "****";
        return s.substring(0, 4) + " **** **** " + s.substring(s.length() - 4);
    }
}
