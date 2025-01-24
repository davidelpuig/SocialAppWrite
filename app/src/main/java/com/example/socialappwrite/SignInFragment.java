package com.example.socialappwrite;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;

public class SignInFragment extends Fragment {

    NavController navController;
    AppViewModel appViewModel;

    Client client;
    Account account;
    private EditText emailEditText, passwordEditText;
    private Button emailSignInButton;
    private LinearLayout signInForm;
    private ProgressBar signInProgressBar;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sign_in, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        client = new Client(requireActivity().getApplicationContext());
        client.setProject(getString(R.string.APPWRITE_PROJECT_ID));

        account = new Account(client);
        /*account.deleteSession(
                "current", // sessionId
                new CoroutineCallback<>((result, error) -> {
                    if (error != null) {
                        error.printStackTrace();
                        return;
                    }

                    //Log.d("Appwrite", result.toString());
                })
        );*/


        navController = Navigation.findNavController(view);  // <-----------------
        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        emailSignInButton = view.findViewById(R.id.emailSignInButton);
        signInForm = view.findViewById(R.id.signInForm);
        signInProgressBar = view.findViewById(R.id.signInProgressBar);

        view.findViewById(R.id.gotoCreateAccountTextView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navController.navigate(R.id.registerFragment);
            }
        });

        emailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accederConEmail();
            }
        });

        Handler mainHandler = new Handler(Looper.getMainLooper());
        account.getSession(
                "current", // sessionId
                new CoroutineCallback<>((result, error) -> {
                    if (error != null) {
                        error.printStackTrace();
                        return;
                    }

                    // Si ya estamos logeados, pasamos a Home
                    if(result != null)
                        mainHandler.post(() -> {
                            obtenerAccount(account);
                        });
                })
        );

    }

    private void obtenerAccount(Account account)
    {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        try {
            account.get(new CoroutineCallback<>((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                    return;
                }

                mainHandler.post(() ->
                {
                    appViewModel.userAccount.postValue(result);
                    obtenerPerfil(result.getId());
                });

            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

    private void obtenerPerfil(String userId)
    {
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());

        try {
            databases.getDocument(
                    getString(R.string.APPWRITE_DATABASE_ID), // databaseId
                    getString(R.string.APPWRITE_PROFILES_COLLECTION_ID), // collectionId
                    userId, // queries (optional)
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error al obtener el perfil: " + error.toString(), Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        System.out.println( result.toString() );

                        mainHandler.post(()-> {

                            appViewModel.userProfile.postValue(result.getData());
                            actualizarUI(userId);
                        });
                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

    private void accederConEmail() {
        signInForm.setVisibility(View.GONE);
        signInProgressBar.setVisibility(View.VISIBLE);

        Account account = new Account(client);

        Handler mainHandler = new Handler(Looper.getMainLooper());

        account.createEmailPasswordSession(
                emailEditText.getText().toString(), // email
                passwordEditText.getText().toString(), // password
                new CoroutineCallback<>((result, error) -> {
                    if (error != null) {
                        Snackbar.make(requireView(), "Error: " + error.toString(), Snackbar.LENGTH_LONG).show();
                    }
                    else
                    {
                        System.out.println("Sesión creada para el usuario:" + result.toString());
                        obtenerAccount(account);

                    }
                    mainHandler.post(() -> {
                        signInForm.setVisibility(View.VISIBLE);
                        signInProgressBar.setVisibility(View.GONE);
                    });
                })
        );

    }

    private void actualizarUI(String currentUser) {
        if(currentUser != null){
            navController.navigate(R.id.homeFragment);
        }
    }
}