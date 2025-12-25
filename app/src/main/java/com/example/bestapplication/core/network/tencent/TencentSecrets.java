package com.example.bestapplication.core.network.tencent;

import com.example.bestapplication.BuildConfig;

/**
 * OCR credentials.
 *
 * Security note:
 * - Do NOT commit real SecretId/SecretKey to git.
 * - For real products, do not ship cloud credentials in the APK; use a backend to mint short-lived tokens.
 */
public final class TencentSecrets {
    private TencentSecrets() {}

    /**
     * Configure in local.properties or environment variables (see app/build.gradle).
     */
    public static final String SECRET_ID = BuildConfig.TC_SECRET_ID;
    public static final String SECRET_KEY = BuildConfig.TC_SECRET_KEY;

    /**
     * Region is optional for OCR. Empty string means "not set".
     */
    public static final String REGION = BuildConfig.TC_REGION;
}
