package com.linkcapture.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private LinkAdapter adapter;
    private static final Uri CONTENT_URI = Uri.parse("content://com.linkcapture.app.provider/links");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvStatus = findViewById(R.id.tvStatus);
        tvStatus.setText(isModuleActive() ? "Module: ACTIVE" : "Module: NOT ACTIVE (enable in LSPosed)");

        RecyclerView rv = findViewById(R.id.rvLinks);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LinkAdapter();
        rv.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("url", item.url));
            Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show();
        });

        Button btnClear = findViewById(R.id.btnClear);
        btnClear.setOnClickListener(v -> {
            getContentResolver().delete(CONTENT_URI, null, null);
            loadLinks();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLinks();
    }

    private void loadLinks() {
        List<LinkAdapter.LinkItem> list = new ArrayList<>();
        try (Cursor c = getContentResolver().query(CONTENT_URI, null, null, null, null)) {
            if (c != null) {
                while (c.moveToNext()) {
                    list.add(new LinkAdapter.LinkItem(
                        c.getString(c.getColumnIndexOrThrow("url")),
                        c.getString(c.getColumnIndexOrThrow("source")),
                        c.getLong(c.getColumnIndexOrThrow("timestamp"))
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        adapter.setItems(list);
    }

    private boolean isModuleActive() { return false; }
}