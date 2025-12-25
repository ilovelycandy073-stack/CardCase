package com.example.bestapplication.feature.bankcard.scan;

import android.widget.Toast;

import com.example.bestapplication.R;
import com.example.bestapplication.core.scan.ScanSessionParams;
import com.example.bestapplication.core.scan.ScanSpec;
import com.example.bestapplication.core.util.JsonUtils;
import com.example.bestapplication.feature.bankcard.BankCardDetailActivity;
import com.example.bestapplication.feature.scan.BaseScanActivity;

/**
 * Bank card scanning UI backed by the generic scan pipeline.
 */
public class BankCardScanActivity extends BaseScanActivity {

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_bankcard_scan;
    }

    @Override
    protected ScanSpec provideSpec() {
        return new BankCardScanSpec();
    }

    @Override
    protected ScanSessionParams provideParams() {
        ScanSessionParams p = new ScanSessionParams();
        p.itemId = getIntent().getStringExtra(BankCardDetailActivity.EXTRA_ITEM_ID);
        p.mode = getIntent().getStringExtra(BankCardDetailActivity.EXTRA_MODE);
        if (JsonUtils.isEmpty(p.mode)) {
            p.mode = BankCardDetailActivity.MODE_FRONT_OCR;
        }
        if (JsonUtils.isEmpty(p.itemId)) {
            Toast.makeText(this, "参数错误：缺少 itemId", Toast.LENGTH_SHORT).show();
            finish();
        }
        return p;
    }
}
