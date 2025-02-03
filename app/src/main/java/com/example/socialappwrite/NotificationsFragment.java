package com.example.socialappwrite;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.Query;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import io.appwrite.models.User;
import io.appwrite.services.Databases;


public class NotificationsFragment extends Fragment {


    NotificationsAdapter adapter;
    Client client;
    AppViewModel appViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notifications, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        client = new Client(requireContext())
                .setProject(getString(R.string.APPWRITE_PROJECT_ID)); // Your project ID
        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.notificationsRecyclerView);

        adapter = new NotificationsAdapter();
        recyclerView.setAdapter(adapter);

        appViewModel.userAccount.observe(getViewLifecycleOwner(), new Observer<User<Map<String, Object>> >() {
            @Override
            public void onChanged(User<Map<String, Object>> mapUser) {
                // Obtenemos información de la cuenta del autor
                        obtenerNotificaciones(mapUser.getId());

            }
        });

    }

    void obtenerNotificaciones(String userId)
    {
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());

        try {
            databases.listDocuments(
                    getString(R.string.APPWRITE_DATABASE_ID), // databaseId
                    getString(R.string.APPWRITE_NOTIFICATIONS_COLLECTION_ID), // collectionId
                    Arrays.asList(Query.Companion.equal("receiverId", userId),  Query.Companion.orderDesc("timeStamp"), Query.Companion.limit(50)),
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error al obtener los posts: " + error.toString(), Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        System.out.println( result.toString() );

                        //Version simple
                        mainHandler.post(() -> adapter.establecerLista(result));

                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder{
        ImageView authorPhotoImageView;
        TextView contentTextView, timeTextView;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);

            authorPhotoImageView = itemView.findViewById(R.id.photoImageView);

            contentTextView = itemView.findViewById(R.id.contentTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);

        }
    }
    class NotificationsAdapter extends RecyclerView.Adapter<NotificationViewHolder> {

        DocumentList<Map<String,Object>> lista = null;

        public NotificationsAdapter()
        {
        }

        @NonNull
        @Override
        public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new NotificationViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_notification, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {

            Map<String,Object> post = lista.getDocuments().get(position).getData();

            if (post.get("authorPhotoUrl") == null)
            {
                holder.authorPhotoImageView.setImageResource(R.drawable.user);
            }
            else
            {
                Glide.with(getContext()).load(post.get("authorPhotoUrl").toString()).circleCrop().into(holder.authorPhotoImageView);
            }
            holder.contentTextView.setText(post.get("text").toString());

            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            Calendar calendar = Calendar.getInstance();
            if(post.get("timeStamp") != null)
                calendar.setTimeInMillis((long)post.get("timeStamp"));
            else
                calendar.setTimeInMillis(0);

            holder.timeTextView.setText( formatter.format(calendar.getTime()));

        }

        @Override
        public int getItemCount() {
            return lista == null ? 0 : lista.getDocuments().size();
        }

        public void establecerLista(DocumentList<Map<String,Object>> lista)
        {
            this.lista = lista;
            notifyDataSetChanged();
        }

    }

}