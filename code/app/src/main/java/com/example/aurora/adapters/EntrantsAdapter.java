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

/**
 * Adapter used in organizer entrant lists.
 * Supports:
 *  Checkbox selection (Waiting / Selected / Cancelled tabs)
 *  Delete button for Selected tab
 *  Hiding checkbox for Final tab
 */
public class EntrantsAdapter extends RecyclerView.Adapter<EntrantsAdapter.EntrantViewHolder> {

    /**
     * Callback invoked when any checkbox state changes.
     * Used by the parent screen to update the Notify button label.
     */
    public interface OnSelectionChanged {
        void onChanged();
    }

    /**
     * Callback invoked when the delete button is pressed on a "Selected" entrant.
     */
    public interface OnDeleteClickListener {
        void onDelete(String email);
    }

    private OnSelectionChanged selectionListener;
    private OnDeleteClickListener deleteListener;

    /**
     * Assigns the selection change listener.
     */
    public void setSelectionListener(OnSelectionChanged listener) {
        this.selectionListener = listener;
    }

    /**
     * Assigns the delete button listener.
     */
    public void setDeleteListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    /**
     * Represents a single entrant row (name, email, status, checkbox state).
     */
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

    /**
     * Constructs the adapter with an initial list of entrants.
     */
    public EntrantsAdapter(Context context, List<EntrantItem> initial) {
        this.context = context;
        this.items = initial != null ? initial : new ArrayList<>();
    }

    /**
     * Inflates the entrant row layout.
     */
    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_entrant, parent, false);
        return new EntrantViewHolder(v);
    }

    /**
     * Binds name, email, checkbox, and delete button to each row.
     */
    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        EntrantItem item = items.get(position);

        holder.tvName.setText(item.getName());
        holder.tvEmail.setText(item.getEmail());

        if (item.getStatus().equals("Final")) {
            holder.checkBox.setVisibility(View.GONE);
        } else {
            holder.checkBox.setVisibility(View.VISIBLE);

            // Normal checkbox behavior for other statuses
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(item.isChecked());
            holder.checkBox.setOnCheckedChangeListener((btn, checked) -> {
                item.setChecked(checked);
                if (selectionListener != null) selectionListener.onChanged();
            });
        }

        if (item.getStatus().equals("Selected")) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) deleteListener.onDelete(item.getEmail());
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    /**
     * @return number of entrant rows.
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Clears the whole list and refreshes UI.
     */
    public void clearItems() {
        items.clear();
        notifyDataSetChanged();
        if (selectionListener != null) selectionListener.onChanged();
    }

    /**
     * Adds a new entrant row to the list.
     */
    public void addItem(EntrantItem item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    /**
     * Removes a row by its email (used by delete button).
     */
    public void removeByEmail(String email) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getEmail().equals(email)) {
                items.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public List<EntrantItem> getSelectedEntrants() {
        List<EntrantItem> list = new ArrayList<>();
        for (EntrantItem i : items) if (i.isChecked()) list.add(i);
        return list;
    }

    /**
     * ViewHolder for a single entrant row.
     * Holds name, email, checkbox, and delete button.
     */
    static class EntrantViewHolder extends RecyclerView.ViewHolder {

        CheckBox checkBox;
        TextView tvName, tvEmail;
        ImageButton btnDelete;

        public EntrantViewHolder(@NonNull View itemView) {
            super(itemView);

            checkBox = itemView.findViewById(R.id.checkSelect);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);

            btnDelete = itemView.findViewById(R.id.btnDeleteEntrant);
        }
    }
}
