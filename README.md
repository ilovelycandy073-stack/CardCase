# BestApplication (AegisDocs)

This project is an Android (Java) app for managing document scans (ID card / bank card) with optional OCR.

## Local setup

1. Open the project in Android Studio.
2. Ensure `local.properties` contains your `sdk.dir` as usual.
3. (Optional) Configure Tencent OCR credentials (coursework only):

Add the following keys to `local.properties` (do **not** commit):

```
TC_SECRET_ID=...
TC_SECRET_KEY=...
TC_REGION=ap-guangzhou   # optional
```

You can also set them as environment variables with the same names.

## Security note

Do not commit any real cloud `SecretId/SecretKey` into git history.
For production, use a backend to mint short-lived credentials/tokens instead of shipping long-lived keys in the APK.

