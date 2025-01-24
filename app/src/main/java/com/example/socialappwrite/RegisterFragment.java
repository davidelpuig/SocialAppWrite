package com.example.socialappwrite;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;


public class RegisterFragment extends Fragment {

   NavController navController;
    private EditText usernameEditText, emailEditText, passwordEditText;
    private Button registerButton;

    Client client;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);  // <-----------------

        usernameEditText = view.findViewById(R.id.usernameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);

        registerButton = view.findViewById(R.id.registerButton);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                crearCuenta();
            }
        });
    }

    void crearCuenta()
    {
        if (!validarFormulario()) {
            return;
        }

        registerButton.setEnabled(false);

        client = new Client(requireActivity().getApplicationContext());
        client.setProject(getString(R.string.APPWRITE_PROJECT_ID));

        Account account = new Account(client);

        Handler mainHandler = new Handler(Looper.getMainLooper());

        try {
            account.create(
                    "unique()", // userId
                    emailEditText.getText().toString(), // email
                    passwordEditText.getText().toString(), // password
                    usernameEditText.getText().toString(), // name (optional)
                    new CoroutineCallback<>((result, error) -> {

                        mainHandler.post(() ->  registerButton.setEnabled(true));

                        if (error != null) {
                            Snackbar.make(requireView(), "Error: " + error.toString(), Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        mainHandler.post(() ->  createUserProfile(result.getId()));
                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

    private void createUserProfile(String currentUser)
    {
        // Crear instancia del servicio Databases
        Databases databases = new Databases(client);

        // Datos del documento
        Map<String, Object> data = new HashMap<>();
        data.put("userId", currentUser);
        data.put("photoUrl", null);

        Handler mainHandler = new Handler(Looper.getMainLooper());

        // Crear el documento
        try {
            databases.createDocument(
                    getString(R.string.APPWRITE_DATABASE_ID), // databaseId
                    getString(R.string.APPWRITE_PROFILES_COLLECTION_ID), // collectionId
                    currentUser,
                    data,
                    new ArrayList<>(), // Permisos opcionales, como ["role:all"]
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error: " + error.toString(), Snackbar.LENGTH_LONG).show();
                        }
                        else
                        {
                            System.out.println("Perfil creado:" + result.toString());
                            mainHandler.post(() ->
                            {
                                actualizarUI(currentUser);
                            });
                        }

                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

    private void actualizarUI(String currentUser) {
        if(currentUser != null){
            //navController.navigate(R.id.homeFragment);
            navController.popBackStack();
        }
    }

    private boolean validarFormulario() {
        boolean valid = true;

        if (TextUtils.isEmpty(emailEditText.getText().toString())) {
            emailEditText.setError("Required.");
            valid = false;
        } else {
            emailEditText.setError(null);
        }

        if (TextUtils.isEmpty(passwordEditText.getText().toString())) {
            passwordEditText.setError("Required.");
            valid = false;
        } else {
            passwordEditText.setError(null);
        }

        return valid;
    }
}