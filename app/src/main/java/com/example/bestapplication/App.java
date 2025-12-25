package com.example.bestapplication;

import android.app.Application;

import com.example.bestapplication.core.concurrency.AppExecutors;
import com.example.bestapplication.core.constants.DocumentTypeIds;
import com.example.bestapplication.core.db.AppDatabase;
import com.example.bestapplication.core.db.entity.DocumentTypeEntity;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.Arrays;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // SQLCipher must load native libs.
        SQLiteDatabase.loadLibs(this);

        // Initialize default document types (idempotent).
        AppExecutors.io().execute(() -> {
            AppDatabase db = AppDatabase.get(this);
            db.dao().upsertTypes(Arrays.asList(
                    new DocumentTypeEntity(DocumentTypeIds.IDCARD_CN, "身份证（大陆）", true, "{}"),
                    new DocumentTypeEntity(DocumentTypeIds.DRIVER_LICENSE, "驾驶证", true, "{}"),
                    new DocumentTypeEntity(DocumentTypeIds.BANK_CARD, "银行卡", true, "{}"),
                    new DocumentTypeEntity(DocumentTypeIds.STUDENT_ID, "学生证", true, "{}"),
                    new DocumentTypeEntity(DocumentTypeIds.WORK_ID, "工作证", true, "{}")
            ));
        });
    }
}
