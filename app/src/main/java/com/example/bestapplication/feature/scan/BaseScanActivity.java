package com.example.bestapplication.feature.scan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.bestapplication.R;
import com.example.bestapplication.core.concurrency.AppExecutors;
import com.example.bestapplication.core.db.entity.DocumentItemEntity;
import com.example.bestapplication.core.scan.ScanCaptureStep;
import com.example.bestapplication.core.scan.ScanPipeline;
import com.example.bestapplication.core.scan.ScanSessionParams;
import com.example.bestapplication.core.scan.ScanSpec;
import com.example.bestapplication.feature.idcard.scan.ImageProxyBytes;
import com.example.bestapplication.feature.idcard.scan.ScanOverlayView;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Generic CameraX capture UI that drives a configurable scan pipeline.
 *
 * Subclasses only provide layout + spec + params.
 */
public abstract class BaseScanActivity extends AppCompatActivity {

    private static final String TAG = "BaseScanActivity";

    protected PreviewView previewView;
    protected ScanOverlayView overlayView;
    protected TextView tipView;

    private ImageCapture imageCapture;
    private Executor mainExecutor;

    private ScanSpec spec;
    private ScanSessionParams params;
    private List<ScanCaptureStep> steps;
    private int stepIndex = 0;
    private final Map<String, byte[]> captures = new HashMap<>();
    private volatile boolean busy = false;

    private final ActivityResultLauncher<String> camPerm =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera();
                else {
                    Toast.makeText(this, "未获得相机权限，无法扫描证件", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @LayoutRes
    protected abstract int getLayoutResId();

    @NonNull
    protected abstract ScanSpec provideSpec();

    @NonNull
    protected abstract ScanSessionParams provideParams();

    /** Called on pipeline success. Default: toast + finish. */
    protected void onPipelineSuccess(DocumentItemEntity item) {
        Toast.makeText(this, "已完成保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    /** Called on pipeline failure. Default: show error + reset session. */
    protected void onPipelineFailure(String message) {
        Toast.makeText(this, "处理失败：" + message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());

        this.spec = provideSpec();
        this.params = provideParams();
        if (isFinishing()) {
            return;
        }
        this.steps = spec.getCaptureSteps(params);
        if (steps == null || steps.isEmpty()) {
            Toast.makeText(this, "扫描配置错误：无拍摄步骤", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        previewView = findViewById(R.id.preview);
        overlayView = findViewById(R.id.overlay);
        tipView = findViewById(R.id.tip);
        mainExecutor = ContextCompat.getMainExecutor(this);

        // Title
        TextView title = findViewById(R.id.tv_title);
        if (title != null) {
            String t = safe(spec.getTitle(params));
            if (!t.isEmpty()) title.setText(t);
        }

        findViewById(R.id.btn_shot).setOnClickListener(v -> shoot());

        render();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            camPerm.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (overlayView != null && !busy) overlayView.start();
    }

    @Override
    protected void onPause() {
        if (overlayView != null) overlayView.stop();
        super.onPause();
    }

    private void render() {
        if (tipView != null) {
            if (busy) {
                tipView.setText("处理中，请稍候…");
            } else {
                tipView.setText(safe(spec.getTipForStep(params, stepIndex)));
            }
        }
        if (overlayView != null) {
            if (busy) overlayView.stop();
            else overlayView.start();
        }
    }

    private void setUiBusy(boolean b) {
        busy = b;
        findViewById(R.id.btn_shot).setEnabled(!b);
        render();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                provider.unbindAll();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setJpegQuality(92)
                        .build();

                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
            } catch (Exception e) {
                Log.e(TAG, "camera init failed", e);
                Toast.makeText(this, "相机初始化失败", Toast.LENGTH_LONG).show();
                finish();
            }
        }, mainExecutor);
    }

    private void shoot() {
        if (imageCapture == null) return;
        if (busy) return;

        imageCapture.takePicture(mainExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                try {
                    byte[] jpeg = ImageProxyBytes.toJpegBytes(image);
                    ScanCaptureStep step = steps.get(stepIndex);
                    captures.put(step.stepId, jpeg);

                    if (stepIndex < steps.size() - 1) {
                        stepIndex++;
                        render();
                        Toast.makeText(BaseScanActivity.this, "已拍摄，请继续", Toast.LENGTH_SHORT).show();
                    } else {
                        setUiBusy(true);
                        runPipeline();
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "process photo failed", ex);
                    Toast.makeText(BaseScanActivity.this, "处理照片失败，请重试", Toast.LENGTH_LONG).show();
                } finally {
                    image.close();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "takePicture error", exception);
                Toast.makeText(BaseScanActivity.this, "拍照失败，请重试", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void runPipeline() {
        AppExecutors.io().execute(() -> {
            try {
                DocumentItemEntity item = ScanPipeline.run(this, spec, params, captures);
                runOnUiThread(() -> onPipelineSuccess(item));
            } catch (Exception e) {
                Log.e(TAG, "pipeline failed", e);
                String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                runOnUiThread(() -> {
                    setUiBusy(false);
                    resetSession();
                    onPipelineFailure(msg);
                });
            }
        });
    }

    private void resetSession() {
        stepIndex = 0;
        captures.clear();
        render();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
