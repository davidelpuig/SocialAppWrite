package com.example.socialappwrite;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.Document;
import io.appwrite.models.InputFile;
import io.appwrite.models.User;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;
import io.appwrite.services.Storage;


public class ProfileFragment extends Fragment {

    NavController navController;
    ImageView photoImageView;
    TextView displayNameTextView, emailTextView;
    Button changePictureButton;
    AppViewModel appViewModel;

    Client client;
    String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);  // <-----------------
        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        photoImageView = view.findViewById(R.id.photoImageView);
        displayNameTextView = view.findViewById(R.id.displayNameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        changePictureButton = view.findViewById(R.id.changePictureButton);

        changePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seleccionarImagen();
            }
        });

        client = new Client(requireContext())
                .setProject(getString(R.string.APPWRITE_PROJECT_ID)); // Your project ID

        appViewModel.userAccount.observe(getViewLifecycleOwner(), new Observer<User<Map<String, Object>>>() {
            @Override
            public void onChanged(User<Map<String, Object>> mapUser) {
                userId = mapUser.getId();
                displayNameTextView.setText(mapUser.getName().toString());
                emailTextView.setText(mapUser.getEmail().toString());
            }
        });

        appViewModel.userProfile.observe(getViewLifecycleOwner(), new Observer<Map<String, Object>>() {
            @Override
            public void onChanged(Map<String, Object> stringObjectMap) {
                String photo = null;
                if(stringObjectMap.get("photoUrl") != null)
                    photo = stringObjectMap.get("photoUrl").toString();

                Glide.with(requireView()).load(photo == null ? R.drawable.user : photo).into(photoImageView);
            }
        });

    }

    private final ActivityResultLauncher<String> galeria =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        // Cambiar foto
                        Storage storage = new Storage(client);
                        Handler mainHandler = new Handler(Looper.getMainLooper());

                        File tempFile = null;
                        try {
                            tempFile = getFileFromUri(getContext(), uri);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        storage.createFile(
                                getString(R.string.APPWRITE_STORAGE_BUCKET_ID), // bucketId
                                "unique()", // fileId
                                InputFile.Companion.fromFile(tempFile), // file
                                new ArrayList<>(), // permissions (optional)
                                new CoroutineCallback<>((result, error) -> {
                                    if (error != null) {
                                        System.err.println("Error subiendo el archivo:" + error.getMessage() );
                                        return;
                                    }

                                    String downloadUrl = "https://cloud.appwrite.io/v1/storage/buckets/" + getString(R.string.APPWRITE_STORAGE_BUCKET_ID) + "/files/" + result.getId() + "/view?project=" + getString(R.string.APPWRITE_PROJECT_ID) + "&project=" + getString(R.string.APPWRITE_PROJECT_ID) + "&mode=admin";
                                    mainHandler.post(() ->
                                    {
                                        actualizarPerfil(downloadUrl);
                                    });
                                })
                        );
                    });

    private void seleccionarImagen() {
        galeria.launch("image/*");
    }

    void actualizarPerfil(String photoUrl)
    {
        Databases databases = new Databases(client);

        Map<String, Object> data = new HashMap<>();
        data.put("photoUrl", photoUrl);

        try {
            databases.updateDocument(
                    getString(R.string.APPWRITE_DATABASE_ID), // databaseId
                    getString(R.string.APPWRITE_PROFILES_COLLECTION_ID), // collectionId
                    userId, // documentId
                    data, // data (optional)
                    new ArrayList<>(), // permissions (optional)
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            error.printStackTrace();
                            return;
                        }

                        System.out.println("Perfil actualizado:" + result.toString());

                        appViewModel.userProfile.postValue(result.getData());

                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

    public File getFileFromUri(Context context, Uri uri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);

        if (inputStream == null) {
            throw new FileNotFoundException("No se pudo abrir el URI: " + uri);
        }

        String fileName = getFileName(context, uri);
        File tempFile = new File(context.getCacheDir(), fileName);

        FileOutputStream outputStream = new FileOutputStream(tempFile);
        byte[] buffer = new byte[1024];
        int length;

        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        outputStream.close();
        inputStream.close();

        return tempFile;
    }

    private String getFileName(Context context, Uri uri) {
        String fileName = "temp_file";
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
            }
        }
        return fileName;
    }
}