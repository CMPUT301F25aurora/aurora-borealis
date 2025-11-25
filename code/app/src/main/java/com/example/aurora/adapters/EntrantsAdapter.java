package com.example.aurora.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aurora.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for entrant cards on the organizer lottery screen.
 */
public class EntrantsAdapter extends RecyclerView.Adapter<EntrantsAdapter.EntrantViewHolder> {

    // ⭐ ADDED: Listener interface
    public interface OnSelectionChanged {
        void onChanged();
    }

    // ⭐ ADDED: Listener field
    private OnSelectionChanged selectionListener;

    public void setSelectionListener(OnSelectionChanged listener) {
        this.selectionListener = listener;
    }
    public static class EntrantItem {
        private final String name;
        private final String email;
        private final String status;
        private boolean checked;
        public EntrantItem(String name, String email, String status) {
            this.name = name;
            this.email = email;
            this.status = status;
            this.checked = false;
        }

        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getStatus() { return status; }
        public boolean isChecked() { return checked; }
        public void setChecked(boolean checked) { this.checked = checked; }
    }

    private final Context context;
    private final List<EntrantItem> items;

    public EntrantsAdapter(Context context, List<EntrantItem> initial) {
        this.context = context;
        this.items = initial != null ? initial : new ArrayList<>();
    }
    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_entrant, parent, false);
        return new EntrantViewHolder(v);
    }

    @NonNull
    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        EntrantItem item = items.get(position);

        holder.tvName.setText(item.getName());
        holder.tvEmail.setText(item.getEmail());
        holder.tvStatus.setText(item.getStatus());

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(item.isChecked());

        // Only notify on actual checkbox toggle
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setChecked(isChecked);

            if (selectionListener != null) {
                selectionListener.onChanged();   // correct
            }
        });

        // Simple status pill styling
        int badgeColor = ContextCompat.getColor(context, R.color.purple_500);
        holder.tvStatus.setBackgroundResource(R.drawable.bg_status_badge);
        holder.tvStatus.setTextColor(badgeColor);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void clearItems() {
        items.clear();
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onChanged();     // <--- HIGHLIGHTED FIX
        }
    }

    public void addItem(EntrantItem item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public List<EntrantItem> getSelectedEntrants() {
        List<EntrantItem> selected = new ArrayList<>();
        for (EntrantItem item : items) {
            if (item.isChecked()) {
                selected.add(item);
            }
        }
        return selected;
    }

    static class EntrantViewHolder extends RecyclerView.ViewHolder {

        CheckBox checkBox;
        TextView tvName;
        TextView tvEmail;
        TextView tvStatus;

        EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkSelect);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvStatus = itemView.findViewById(R.id.tvStatus);


        }
    }
}
