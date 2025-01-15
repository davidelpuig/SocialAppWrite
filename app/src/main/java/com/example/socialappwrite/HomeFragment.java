package com.example.socialappwrite;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;

import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.DocumentList;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;


public class HomeFragment extends Fragment {

    NavController navController;

    ImageView photoImageView;
    TextView displayNameTextView, emailTextView;

    Client client;
    Account account;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);  // <-----------------

        NavigationView navigationView = view.getRootView().findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);
        photoImageView = header.findViewById(R.id.imageView);
        displayNameTextView = header.findViewById(R.id.displayNameTextView);
        emailTextView = header.findViewById(R.id.emailTextView);

        client = new Client(requireContext())
                .setProject("678510c0002fc68abafc"); // Your project ID

        account = new Account(client);

        Handler mainHandler = new Handler(Looper.getMainLooper());

        try {
            account.get(new CoroutineCallback<>((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                    return;
                }

                mainHandler.post(() ->
                {
                    displayNameTextView.setText(result.getName().toString());
                    emailTextView.setText(result.getEmail().toString());
                    Glide.with(requireView()).load(R.drawable.user).into(photoImageView);

                    obtenerPosts();
                });

            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }

        view.findViewById(R.id.gotoNewPostFragmentButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.newPostFragment);
            }
        });
    }

    void obtenerPosts()
    {
        Databases databases = new Databases(client);

        try {
            databases.listDocuments(
                    "[TU_DATABASE_ID]",
                    "[TU_COLLECTION_ID]",
                    new CoroutineCallback<DocumentList>() {
                        @Override
                        public void onComplete(DocumentList result) {
                            // Procesar documentos
                            result.getDocuments().forEach(document -> {
                                System.out.println("Documento: " + document.getId());
                            });

                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            System.err.println("Error: " + throwable.getMessage());
                        }
                    }
            );

            // Incrementar el offset
        } catch (AppwriteException e) {
            e.printStackTrace();
        }
    }

    class PostViewHolder extends RecyclerView.ViewHolder{
        ImageView authorPhotoImageView;
        TextView authorTextView, contentTextView;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);

            authorPhotoImageView = itemView.findViewById(R.id.photoImageView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
        }
    }

    class PostsAdapter extends RecyclerView.Adapter<PostViewHolder> {

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PostViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_post, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            /*Glide.with(getContext()).load(post.authorPhotoUrl).circleCrop().into(holder.authorPhotoImageView);
            holder.authorTextView.setText(post.author);
            holder.contentTextView.setText(post.content);*/
        }

        @Override
        public int getItemCount() {
            return 0;
        }

        public void establecerLista()
        {

        }

    }
}