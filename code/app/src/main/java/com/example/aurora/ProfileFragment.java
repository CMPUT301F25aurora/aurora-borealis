package com.example.aurora;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private FirebaseFirestore db;
    private DocumentReference userRef;

    private ImageView avatar;
    private TextView roleBadge, headerName, joinedCount, winsCount, editToggle;
    private EditText fullName, email, phone;
    private Button btnSave, btnEventHistory, btnNotifSettings, btnDelete;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        db = FirebaseFirestore.getInstance();

        avatar = v.findViewById(R.id.avatarCircle);
        roleBadge = v.findViewById(R.id.roleBadge);
        headerName = v.findViewById(R.id.headerName);
        joinedCount = v.findViewById(R.id.joinedCount);
        winsCount = v.findViewById(R.id.winsCount);
        editToggle = v.findViewById(R.id.editToggle);
        fullName = v.findViewById(R.id.inputFullName);
        email = v.findViewById(R.id.inputEmail);
        phone = v.findViewById(R.id.inputPhone);
        btnSave = v.findViewById(R.id.btnSave);
        btnEventHistory = v.findViewById(R.id.btnEventHistory);
        btnNotifSettings = v.findViewById(R.id.btnNotifSettings);
        btnDelete = v.findViewById(R.id.btnDeleteAccount);

        setEditing(false);
        resolveAndLoad();

        editToggle.setOnClickListener(x -> setEditing(true));
        btnSave.setOnClickListener(x -> saveProfile());
        btnEventHistory.setOnClickListener(x -> Toast.makeText(getContext(),"Event History coming soon",Toast.LENGTH_SHORT).show());
        btnNotifSettings.setOnClickListener(x -> Toast.makeText(getContext(),"Notification Settings coming soon",Toast.LENGTH_SHORT).show());
        btnDelete.setOnClickListener(x -> Toast.makeText(getContext(),"Delete Account coming soon",Toast.LENGTH_SHORT).show());
    }

    private void resolveAndLoad() {
        FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
        String em = fu != null ? fu.getEmail() : requireActivity().getSharedPreferences("aurora_prefs", getContext().MODE_PRIVATE).getString("user_email", null);
        Query q = db.collection("users").whereEqualTo("email", em).limit(1);
        q.get().addOnSuccessListener(snap -> {
            if (!snap.isEmpty()) {
                userRef = snap.getDocuments().get(0).getReference();
                loadProfile();
            } else {
                Map<String,Object> init = new HashMap<>();
                init.put("email", em);
                init.put("role","Entrant");
                init.put("joinedCount",0);
                init.put("winsCount",0);
                userRef = db.collection("users").document();
                userRef.set(init, SetOptions.merge()).addOnSuccessListener(v->loadProfile());
            }
        });
    }

    private void loadProfile() {
        userRef.get().addOnSuccessListener(doc -> {
            String n = doc.getString("name");
            String e = doc.getString("email");
            String p = doc.getString("phone");
            String role = doc.getString("role");
            Long j = doc.getLong("joinedCount");
            Long w = doc.getLong("winsCount");

            fullName.setText(n==null?"":n);
            email.setText(e==null?"":e);
            phone.setText(p==null?"":p);
            headerName.setText(TextUtils.isEmpty(n)?"Entrant":n);
            roleBadge.setText(TextUtils.isEmpty(role)?"Entrant":role);
            joinedCount.setText(String.valueOf(j==null?0:j));
            winsCount.setText(String.valueOf(w==null?0:w));
        });
    }

    private void saveProfile() {
        String n = fullName.getText().toString().trim();
        String e = email.getText().toString().trim();
        String p = phone.getText().toString().trim();

        Map<String,Object> upd = new HashMap<>();
        upd.put("name", n);
        upd.put("email", e);
        upd.put("phone", p);
        upd.put("role", "Entrant");

        userRef.set(upd, SetOptions.merge()).addOnSuccessListener(v -> {
            headerName.setText(TextUtils.isEmpty(n)?"Entrant":n);
            setEditing(false);
            Toast.makeText(getContext(),"Profile saved",Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e1 -> Toast.makeText(getContext(),"Save failed",Toast.LENGTH_SHORT).show());
    }

    private void setEditing(boolean editing) {
        fullName.setEnabled(editing);
        email.setEnabled(editing);
        phone.setEnabled(editing);
        btnSave.setVisibility(editing ? View.VISIBLE : View.GONE);
        editToggle.setVisibility(editing ? View.GONE : View.VISIBLE);
    }
}
