package com.linkcapture.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LinkAdapter extends RecyclerView.Adapter<LinkAdapter.ViewHolder> {
    public static class LinkItem {
        public String url, source;
        public long timestamp;
        public LinkItem(String url, String source, long timestamp) {
            this.url = url; this.source = source; this.timestamp = timestamp;
        }
    }

    private final List<LinkItem> items = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener { void onItemClick(LinkItem item); }
    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

    public void setItems(List<LinkItem> newItems) {
        items.clear(); items.addAll(newItems); notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_link, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        LinkItem item = items.get(pos);
        h.tvUrl.setText(item.url);
        h.tvSource.setText(item.source);
        h.tvTime.setText(new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(item.timestamp)));
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onItemClick(item); });
    }

    @Override public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUrl, tvSource, tvTime;
        ViewHolder(View v) {
            super(v);
            tvUrl = v.findViewById(R.id.tvUrl);
            tvSource = v.findViewById(R.id.tvSource);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }
}