package com.example.aurora.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.aurora.R;
import com.example.aurora.models.AdminImage;

import java.util.List;
/**
 * Adapter used in the Admin panel to display event poster images.
 * Shows: event title, event ID, organizer email, and poster thumbnail.
 * Provides a delete button for each item via a callback.
 */
public class AdminImagesAdapter extends RecyclerView.Adapter<AdminImagesAdapter.ImageHolder> {
    private List<AdminImage> images;
    private Context context;
    private OnDeleteClick listener;
    public interface OnDeleteClick {
        void onDelete(AdminImage img);
    }

    /**
     * Creates an adapter for showing event posters in the admin list.
     *
     * @param images   List of AdminImage objects to display.
     * @param context  Context for inflating layouts and loading images.
     * @param listener Callback for delete button actions.
     */
    public AdminImagesAdapter(List<AdminImage> images, Context context, OnDeleteClick listener) {
        this.images = images;
        this.context = context;
        this.listener = listener;
    }

    /**
     * Inflates the layout for a single admin image card.
     */
    @NonNull
    @Override
    public ImageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_image, parent, false);
        return new ImageHolder(v);
    }

    /**
     * Binds event poster information to the ViewHolder.
     *
     * @param holder   The ViewHolder for the current item.
     * @param position Position of the item in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull ImageHolder holder, int position) {
        AdminImage img = images.get(position);

        holder.title.setText(img.eventTitle);
        holder.eventIdTv.setText("ID: " + img.eventId);
        holder.organizerTv.setText("Organizer: " + img.organizerEmail);

        Glide.with(context)
                .load(img.posterUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.thumbnail);

        holder.removeBtn.setOnClickListener(v -> listener.onDelete(img));
    }

    /**
     * @return Number of images in the admin list.
     */
    @Override
    public int getItemCount() {
        return images.size();
    }

    /**
     * ViewHolder representing a single admin image row.
     * Holds poster thumbnail, event title, event ID, organizer email,
     * and the remove button.
     */
    public class ImageHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView eventIdTv;
        TextView organizerTv;
        Button removeBtn;
        public ImageHolder(@NonNull View itemView) {
            super(itemView);

            thumbnail = itemView.findViewById(R.id.adminPosterThumb);
            title = itemView.findViewById(R.id.adminImageEventTitle);
            eventIdTv = itemView.findViewById(R.id.adminImageEventId);
            organizerTv = itemView.findViewById(R.id.adminImageOrganizer);
            removeBtn = itemView.findViewById(R.id.adminDeleteImageBtn);
        }
    }
}
