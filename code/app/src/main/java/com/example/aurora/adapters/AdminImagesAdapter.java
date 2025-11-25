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

public class AdminImagesAdapter extends RecyclerView.Adapter<AdminImagesAdapter.ImageHolder> {

    private List<AdminImage> images;
    private Context context;
    private OnDeleteClick listener;

    public interface OnDeleteClick {
        void onDelete(AdminImage img);
    }

    public AdminImagesAdapter(List<AdminImage> images, Context context, OnDeleteClick listener) {
        this.images = images;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_image, parent, false);
        return new ImageHolder(v);
    }

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


    @Override
    public int getItemCount() {
        return images.size();
    }

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
