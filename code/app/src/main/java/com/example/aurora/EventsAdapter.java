package com.example.aurora;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private final Context context;
    private final List<Event> events;
    private final FirebaseFirestore db;
    private final String uid;

    public EventsAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
        this.db = FirebaseFirestore.getInstance();

        // If you have UserSession.java:
        this.uid = UserSession.getUserId(context);

        // If you DON'T use UserSession, comment the line above and use:
        // this.uid = Settings.Secure.getString(
        //         context.getContentResolver(),
        //         Settings.Secure.ANDROID_ID
        // );
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event e = events.get(position);

        holder.eventTitle.setText(e.getTitle());
        holder.eventDate.setText(e.getDate());
        holder.eventLocation.setText(e.getLocation());

        // No details screen right now, so hide the button (or you can remove it from XML later)
        holder.btnViewDetails.setVisibility(View.GONE);
        holder.btnViewDetails.setOnClickListener(null);

        // Join waiting list
        holder.btnJoin.setOnClickListener(v ->
                db.collection("events")
                        .document(e.getEventId())
                        .update("waitingList", FieldValue.arrayUnion(uid))
        );
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {

        TextView eventTitle, eventDate, eventLocation;
        Button btnViewDetails, btnJoin;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventTitle = itemView.findViewById(R.id.eventTitle);
            eventDate = itemView.findViewById(R.id.eventDate);
            eventLocation = itemView.findViewById(R.id.eventLocation);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnJoin = itemView.findViewById(R.id.btnJoin);
        }
    }
}
