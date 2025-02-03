package com.example.socialappwrite;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.Map;

import io.appwrite.models.Session;
import io.appwrite.models.User;
import io.appwrite.services.Account;

public class AppViewModel extends AndroidViewModel {
    public static class Media {
        public Uri uri;
        public String tipo;
        public Media(Uri uri, String tipo) {
            this.uri = uri;
            this.tipo = tipo;
        }
    }
    public MutableLiveData<Map<String,Object>> postSeleccionado = new
            MutableLiveData<>();
    public MutableLiveData<Media> mediaSeleccionado = new
            MutableLiveData<>();

    public String parentPostId, parentPostAuthorId;
    public String currentHashtag;
    public MutableLiveData<User<Map<String,Object>>> userAccount = new MutableLiveData<>();
    public MutableLiveData<Map<String, Object>> userProfile = new MutableLiveData<>();

    public AppViewModel(@NonNull Application application) {
        super(application);
    }
    public void setMediaSeleccionado(Uri uri, String type) {
        mediaSeleccionado.setValue(new Media(uri, type));
    }
}