package com.example.socialappwrite;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.User;
import io.appwrite.models.InputFile;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;
import io.appwrite.services.Storage;


public class NewPostFragment extends Fragment {

    Button publishButton;
    EditText postConentEditText;
    NavController navController;

    AppViewModel appViewModel;

    Client client;
    Account account;

    Uri mediaUri;
    String mediaTipo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        client = new Client(requireContext())
                .setProject(getString(R.string.APPWRITE_PROJECT_ID)); // Sustituye con tu Project ID


        publishButton = view.findViewById(R.id.publishButton);
        postConentEditText = view.findViewById(R.id.postContentEditText);

        publishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publicar();
            }
        });

        view.findViewById(R.id.camara_fotos).setOnClickListener(v ->
                tomarFoto());
        view.findViewById(R.id.camara_video).setOnClickListener(v ->
                tomarVideo());
        view.findViewById(R.id.grabar_audio).setOnClickListener(v ->
                grabarAudio());
        view.findViewById(R.id.imagen_galeria).setOnClickListener(v ->
                seleccionarImagen());
        view.findViewById(R.id.video_galeria).setOnClickListener(v ->
                seleccionarVideo());
        view.findViewById(R.id.audio_galeria).setOnClickListener(v ->
                seleccionarAudio());
        appViewModel.mediaSeleccionado.observe(getViewLifecycleOwner(), media ->
        {
            this.mediaUri = media.uri;
            this.mediaTipo = media.tipo;
            Glide.with(this).load(media.uri).into((ImageView) view.findViewById(R.id.previsualizacion));
        });
    }

    void publicar()
    {
        String postContent = postConentEditText.getText().toString();
        account = new Account(client);

        if(TextUtils.isEmpty(postContent)){
            postConentEditText.setError("Required");
            return;
        }

        publishButton.setEnabled(false);

        // Obtenemos información de la cuenta del autor
        try {
            account.get(new CoroutineCallback<>((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                    return;
                }

                if (mediaTipo == null) {
                    guardarEnAppWrite(result, postContent, null);
                }
                else
                {
                    pujaIguardarEnAppWrite(result, postContent);
                }

            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }

    }

    void guardarEnAppWrite(User<Map<String, Object>> user, String content, String mediaUrl)
    {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        // Crear instancia del servicio Databases
        Databases databases = new Databases(client);

        // Datos del documento
        Map<String, Object> data = new HashMap<>();
        data.put("uid", user.getId().toString());
        data.put("author", user.getName().toString());
        data.put("authorPhotoUrl", null);
        data.put("content", content);
        data.put("mediaUrl", mediaUrl);

        // Crear el documento
        try {
            databases.createDocument(
                    getString(R.string.APPWRITE_DATABASE_ID), // databaseId
                    getString(R.string.APPWRITE_POSTS_COLLECTION_ID), // collectionId
                    "unique()", // Usa 'unique()' para generar un ID único automáticamente
                    data,
                    new ArrayList<>(), // Permisos opcionales, como ["role:all"]
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error: " + error.toString(), Snackbar.LENGTH_LONG).show();
                        }
                        else
                        {
                            System.out.println("Post creado:" + result.toString());
                            mainHandler.post(() ->
                            {
                                navController.popBackStack();
                                appViewModel.setMediaSeleccionado(null, null);
                            });
                        }

                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

    private void pujaIguardarEnAppWrite(User<Map<String, Object>> user, final String postText)
    {
        Storage storage = new Storage(client);

        storage.createFile(
                getString(R.string.APPWRITE_STORAGE_BUCKET_ID), // bucketId
                "unique()", // fileId
                InputFile.Companion.fromFile(new File(mediaUri.toString())), // file
                new ArrayList<>(), // permissions (optional)
                new CoroutineCallback<>((result, error) -> {
                    if (error != null) {
                        System.err.println("Error subiendo el archivo:" + error.getMessage() );
                        return;
                    }

                    System.out.println( result.toString() );
                })
        );
    }

    private final ActivityResultLauncher<String> galeria =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        appViewModel.setMediaSeleccionado(uri, mediaTipo);
                    });
    private final ActivityResultLauncher<Uri> camaraFotos =
            registerForActivityResult(new ActivityResultContracts.TakePicture(),
                    isSuccess -> {
                        appViewModel.setMediaSeleccionado(mediaUri, "image");
                    });
    private final ActivityResultLauncher<Uri> camaraVideos =
            registerForActivityResult(new ActivityResultContracts.TakeVideo(),
                    isSuccess -> {
                        appViewModel.setMediaSeleccionado(mediaUri, "video");
                    });
    private final ActivityResultLauncher<Intent> grabadoraAudio =
            registerForActivityResult(new
                    ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    appViewModel.setMediaSeleccionado(result.getData().getData(),
                            "audio");
                }
            });
    private void seleccionarImagen() {
        mediaTipo = "image";
        galeria.launch("image/*");
    }
    private void seleccionarVideo() {
        mediaTipo = "video";
        galeria.launch("video/*");
    }
    private void seleccionarAudio() {
        mediaTipo = "audio";
        galeria.launch("audio/*");
    }
    private void tomarFoto() {
        try {
            mediaUri = FileProvider.getUriForFile(requireContext(),
                    "com.example.socialappwrite" + ".fileprovider",
                    File.createTempFile("img", ".jpg",
                            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES))
            );
            camaraFotos.launch(mediaUri);
        } catch (IOException e) {}
    }
    private void tomarVideo() {
        try {
            mediaUri = FileProvider.getUriForFile(requireContext(),
                    "com.example.socialappwrite" + ".fileprovider",
                    File.createTempFile("vid", ".mp4",
                            requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)));
            camaraVideos.launch(mediaUri);
        } catch (IOException e) {}
    }
    private void grabarAudio() {
        grabadoraAudio.launch(new
                Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION));
    }
}