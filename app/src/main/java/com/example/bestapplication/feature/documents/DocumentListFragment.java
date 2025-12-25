package com.example.bestapplication.feature.documents;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bestapplication.R;
import com.example.bestapplication.core.concurrency.AppExecutors;
import com.example.bestapplication.core.constants.DocumentTypeIds;
import com.example.bestapplication.core.db.AppDatabase;
import com.example.bestapplication.core.db.entity.DocumentTypeEntity;
import com.example.bestapplication.feature.bankcard.BankCardListActivity;
import com.example.bestapplication.feature.idcard.IdCardListActivity;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DocumentListFragment extends Fragment {

    private static final String SP_NAME = "ui_prefs";
    private static final String KEY_ORDER = "doc_type_order_json";

    public DocumentListFragment() {
        super(R.layout.fragment_document_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RecyclerView rv = view.findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // Use applicationContext for persistence/db; avoids requireContext() crashes if fragment detaches.
        Context appCtx = requireContext().getApplicationContext();

        AppExecutors.io().execute(() -> {
            List<DocumentTypeEntity> raw = AppDatabase.get(appCtx).dao().listTypes();
            final List<DocumentTypeEntity> types = new ArrayList<>(raw == null ? new ArrayList<>() : raw);

            applySavedOrderInPlace(appCtx, types);

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                DocumentTypeAdapter ad = new DocumentTypeAdapter(types, type -> {
                    if (DocumentTypeIds.IDCARD_CN.equals(type.typeId)) {
                        startActivity(new Intent(getContext(), IdCardListActivity.class));
                    } else if (DocumentTypeIds.BANK_CARD.equals(type.typeId)) {
                        startActivity(new Intent(getContext(), BankCardListActivity.class));
                    } else {
                        Toast.makeText(getContext(), "暂未开放", Toast.LENGTH_SHORT).show();
                    }
                });

                rv.setAdapter(ad);
                attachDrag(rv, ad, appCtx);
            });
        });
    }

    private void attachDrag(RecyclerView rv, DocumentTypeAdapter ad, Context appCtx) {
        ItemTouchHelper.SimpleCallback cb = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                0
        ) {
            @Override
            public boolean isLongPressDragEnabled() {
                return true; // long press to drag
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {

                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                ad.move(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // no swipe
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                saveOrder(appCtx, ad.getItems());
            }
        };

        new ItemTouchHelper(cb).attachToRecyclerView(rv);
    }

    private void applySavedOrderInPlace(Context ctx, List<DocumentTypeEntity> types) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(SP_NAME, 0);
            String json = sp.getString(KEY_ORDER, null);
            if (json == null || json.trim().isEmpty()) return;

            JSONArray arr = new JSONArray(json);
            HashMap<String, Integer> order = new HashMap<>();
            for (int i = 0; i < arr.length(); i++) {
                order.put(arr.getString(i), i);
            }

            // New types not in order will be pushed to the end.
            types.sort((a, b) -> {
                int ia = order.containsKey(a.typeId) ? order.get(a.typeId) : 1_000_000;
                int ib = order.containsKey(b.typeId) ? order.get(b.typeId) : 1_000_000;
                return Integer.compare(ia, ib);
            });
        } catch (Exception ignored) {
        }
    }

    private void saveOrder(Context ctx, List<DocumentTypeEntity> types) {
        try {
            JSONArray arr = new JSONArray();
            for (DocumentTypeEntity t : types) arr.put(t.typeId);

            ctx.getSharedPreferences(SP_NAME, 0)
                    .edit()
                    .putString(KEY_ORDER, arr.toString())
                    .apply();
        } catch (Exception ignored) {
        }
    }
}
