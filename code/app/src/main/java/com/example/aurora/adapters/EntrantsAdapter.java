package com.example.aurora.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aurora.R;

import java.util.ArrayList;
import java.util.List;

public class EntrantsAdapter extends RecyclerView.Adapter<EntrantsAdapter.EntrantViewHolder> {

    // Listener for checkbox changes
    public interface OnSelectionChanged {
        void onChanged();
    }

    // Listener for DELETE button
    public interface OnDeleteClickListener {
        void onDelete(String email);
    }

    private OnSelectionChanged selectionListener;
    private OnDeleteClickListener deleteListener;

    public void setSelectionListener(OnSelectionChanged listener) {
        this.selectionListener = listener;
    }

    public void setDeleteListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    // Model
    public static class EntrantItem {
        private final String name;
        private final String email;
        private boolean checked;

        public EntrantItem(String name, String email) {
            this.name = name;
            this.email = email;
            this.checked = false;
        }

        public String getName() { return name; }
        public String getEmail() { return email; }
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

    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        EntrantItem item = items.get(position);

        holder.tvName.setText(item.getName());
        holder.tvEmail.setText(item.getEmail());

        // Checkbox
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(item.isChecked());
        holder.checkBox.setOnCheckedChangeListener((button, isChecked) -> {
            item.setChecked(isChecked);
            if (selectionListener != null) selectionListener.onChanged();
        });

        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(item.getEmail());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void removeByEmail(String email) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getEmail().equals(email)) {
                items.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public void clearItems() {
        items.clear();
        notifyDataSetChanged();
        if (selectionListener != null) selectionListener.onChanged();
    }

    public void addItem(EntrantItem item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public List<EntrantItem> getSelectedEntrants() {
        List<EntrantItem> out = new ArrayList<>();
        for (EntrantItem i : items) if (i.isChecked()) out.add(i);
        return out;
    }

    static class EntrantViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView tvName, tvEmail;
        ImageButton btnDelete;

        EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkSelect);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            btnDelete = itemView.findViewById(R.id.btnDeleteEntrant);
        }
    }
}