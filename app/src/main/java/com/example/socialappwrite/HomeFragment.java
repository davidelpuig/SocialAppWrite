package com.example.socialappwrite;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.opengl.Visibility;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
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
import io.appwrite.models.Session;
import io.appwrite.models.User;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;


public class HomeFragment extends Fragment {

    NavController navController;
    AppViewModel appViewModel;

    ImageView photoImageView;
    TextView displayNameTextView, emailTextView;

    Client client;
    Account account;

    String userId;

    PostsAdapter adapter;

    HashMap<String, DocumentList<Map<String,Object>>> listaPosts;

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
        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        NavigationView navigationView = view.getRootView().findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);
        photoImageView = header.findViewById(R.id.imageView);
        displayNameTextView = header.findViewById(R.id.displayNameTextView);
        emailTextView = header.findViewById(R.id.emailTextView);

        RecyclerView postsRecyclerView = view.findViewById(R.id.postsRecyclerView);
        adapter = new PostsAdapter(null, 0);
        postsRecyclerView.setAdapter(adapter);

        view.findViewById(R.id.gotoNewPostFragmentButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appViewModel.parentPostId = null;
                navController.navigate(R.id.newPostFragment);
            }
        });

        client = new Client(requireContext())
                .setProject(getString(R.string.APPWRITE_PROJECT_ID)); // Your project ID

        account = new Account(client);

        //Handler mainHandler = new Handler(Looper.getMainLooper());

        appViewModel.userAccount.observe(getViewLifecycleOwner(), new Observer<User<Map<String,Object>>>() {
                    @Override
                    public void onChanged(User<Map<String, Object>> mapUser) {
                        userId = mapUser.getId();
                        displayNameTextView.setText(mapUser.getName());
                        emailTextView.setText(mapUser.getEmail());


                        listaPosts = new HashMap<>();
                        obtenerPosts();
                    }
                }
        );

        appViewModel.userProfile.observe(getViewLifecycleOwner(), new Observer<Map<String, Object>>() {
            @Override
            public void onChanged(Map<String, Object> stringObjectMap) {
                if(stringObjectMap.get("photoUrl") != null)
                    Glide.with(requireView()).load(stringObjectMap.get("photoUrl").toString()).into(photoImageView);
                else
                    Glide.with(requireView()).load(R.drawable.user).into(photoImageView);
            }
        });

    }


    void obtenerPosts()
    {
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());

        try {
            databases.listDocuments(
                    getString(R.string.APPWRITE_DATABASE_ID), // databaseId
                    getString(R.string.APPWRITE_POSTS_COLLECTION_ID), // collectionId
                    Arrays.asList(/*Query.Companion.isNull("parentPost"),*/  Query.Companion.orderDesc("timeStamp"), Query.Companion.limit(50)),
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error al obtener los posts: " + error.toString(), Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        System.out.println( result.toString() );

                        listaPosts = new HashMap<>();

                        for(int i = 0; i < result.getDocuments().size(); i++)
                        {
                            Document<Map<String, Object>> document = result.getDocuments().get(i);
                            String parentId = (document.getData().get("parentPost") == null ? null : document.getData().get("parentPost").toString());
                            if (listaPosts.get(parentId) == null)
                            {
                                listaPosts.put(parentId, new DocumentList<>(0, new ArrayList<>()));
                            }
                            DocumentList<Map<String,Object>> subLista = listaPosts.get(parentId);
                            subLista.getDocuments().add(document);
                            listaPosts.replace(parentId, subLista);
                        }

                        //Version simple
                        mainHandler.post(() -> adapter.establecerLista(listaPosts.get(null)));

                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }


    class PostViewHolder extends RecyclerView.ViewHolder{
        ImageView authorPhotoImageView, authorSmallPhotoImageView, likeImageView, mediaImageView;
        TextView authorTextView, contentTextView, numLikesTextView, timeTextView, replyButton;
        RecyclerView commentsRecyclerView;

        PostsAdapter commentsAdapter;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);

            authorPhotoImageView = itemView.findViewById(R.id.photoImageView);
            authorSmallPhotoImageView = itemView.findViewById(R.id.smallPhotoImageView);
            likeImageView = itemView.findViewById(R.id.likeImageView);
            mediaImageView = itemView.findViewById(R.id.mediaImage);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            numLikesTextView = itemView.findViewById(R.id.numLikesTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            commentsRecyclerView = itemView.findViewById(R.id.commentsRecyclerView);
            replyButton = itemView.findViewById(R.id.replyButton);


            commentsRecyclerView.setAdapter(commentsAdapter);

        }
    }

    class PostsAdapter extends RecyclerView.Adapter<PostViewHolder> {

        DocumentList<Map<String,Object>> lista = null;
        PostsAdapter parentAdapter;
        int posInParent;

        public PostsAdapter(PostsAdapter parentAdapter, int posInParent)
        {
            this.parentAdapter = parentAdapter;
            this.posInParent = posInParent;
        }

        void notifyElementUpdate()
        {
            if(parentAdapter != null)
            {
                parentAdapter.notifyItemChanged(posInParent);
                parentAdapter.notifyElementUpdate();
            }
        }

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PostViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_post, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {

            Map<String,Object> post = lista.getDocuments().get(position).getData();

            if(post.get("parentPost") != null)
            {
                holder.authorPhotoImageView.setVisibility(GONE);
                holder.authorSmallPhotoImageView.setVisibility(VISIBLE);
            }
            else
            {
                holder.authorPhotoImageView.setVisibility(VISIBLE);
                holder.authorSmallPhotoImageView.setVisibility(GONE);
            }

            if (post.get("authorPhotoUrl") == null)
            {
                holder.authorPhotoImageView.setImageResource(R.drawable.user);
                holder.authorSmallPhotoImageView.setImageResource(R.drawable.user);
            }
            else
            {
                Glide.with(getContext()).load(post.get("authorPhotoUrl").toString()).circleCrop().into(holder.authorPhotoImageView);
                Glide.with(getContext()).load(post.get("authorPhotoUrl").toString()).circleCrop().into(holder.authorSmallPhotoImageView);
            }
            holder.authorTextView.setText(post.get("author").toString());
            holder.contentTextView.setText(post.get("content").toString());

            //Fecha y hora
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            Calendar calendar = Calendar.getInstance();
            if(post.get("timeStamp") != null)
                calendar.setTimeInMillis((long)post.get("timeStamp"));
            else
                calendar.setTimeInMillis(0);

            holder.timeTextView.setText( formatter.format(calendar.getTime()));


            // Gestion de likes
            List<String> likes = (List<String>) post.get("likes");
            if(likes.contains(userId))
                holder.likeImageView.setImageResource(R.drawable.like_on);
            else
                holder.likeImageView.setImageResource(R.drawable.like_off);

            holder.numLikesTextView.setText(String.valueOf(likes.size()));

            holder.likeImageView.setOnClickListener(view -> {

                Databases databases = new Databases(client);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                List<String> nuevosLikes = likes;

                if(nuevosLikes.contains(userId))
                    nuevosLikes.remove(userId);
                else
                    nuevosLikes.add(userId);

                Map<String, Object> data = new HashMap<>();
                data.put("likes", nuevosLikes);

                try {
                    databases.updateDocument(
                            getString(R.string.APPWRITE_DATABASE_ID), // databaseId
                            getString(R.string.APPWRITE_POSTS_COLLECTION_ID), // collectionId
                            post.get("$id").toString(), // documentId
                            data, // data (optional)
                            new ArrayList<>(), // permissions (optional)
                            new CoroutineCallback<>((result, error) -> {
                                if (error != null) {
                                    error.printStackTrace();
                                    return;
                                }

                                System.out.println("Likes actualizados:" + result.toString());

                                mainHandler.post(() -> {
                                    adapter.notifyItemChanged(position);
                                    notifyElementUpdate();
                                });
                            })
                    );
                } catch (AppwriteException e) {
                    throw new RuntimeException(e);
                }

            });

            // Miniatura de media
            if (post.get("mediaUrl") != null) {
                holder.mediaImageView.setVisibility(VISIBLE);
                if ("audio".equals(post.get("mediaType").toString())) {
                    Glide.with(requireView()).load(R.drawable.audio).centerCrop().into(holder.mediaImageView);
                } else {
                    Glide.with(requireView()).load(post.get("mediaUrl").toString()).centerCrop().into(holder.mediaImageView);
                }
                holder.mediaImageView.setOnClickListener(view -> {
                    appViewModel.postSeleccionado.setValue(post);
                    navController.navigate(R.id.mediaFragment);
                });
            } else {
                holder.mediaImageView.setVisibility(GONE);
            }

            //Comments
            holder.replyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    appViewModel.parentPostId = lista.getDocuments().get(holder.getAdapterPosition()).getId();
                    navController.navigate(R.id.newPostFragment);
                }
            });

            PostsAdapter commentsAdapter = new PostsAdapter(this, position);
            DocumentList<Map<String,Object>> subLista = listaPosts.get(lista.getDocuments().get(position).getId());
            if(subLista != null)
                commentsAdapter.establecerLista(subLista);
            holder.commentsRecyclerView.setAdapter(commentsAdapter);


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