package com.example.bestapplication.feature.idcard.scan;

import com.example.bestapplication.R;
import com.example.bestapplication.core.scan.ScanSessionParams;
import com.example.bestapplication.core.scan.ScanSpec;
import com.example.bestapplication.feature.idcard.IdCardDetailActivity;
import com.example.bestapplication.feature.scan.BaseScanActivity;

/**
 * ID card scanning UI backed by the generic scan pipeline.
 */
public class IdCardScanActivity extends BaseScanActivity {

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_idcard_scan;
    }

    @Override
    protected ScanSpec provideSpec() {
        return new IdCardScanSpec();
    }

    @Override
    protected ScanSessionParams provideParams() {
        ScanSessionParams p = new ScanSessionParams();
        p.itemId = getIntent().getStringExtra(IdCardDetailActivity.EXTRA_ITEM_ID);
        return p;
    }
}
