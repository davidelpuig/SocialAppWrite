package com.example.socialappwrite;

import android.os.Bundle;
import android.view.View;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import java.util.Arrays;
import java.util.List;

import io.appwrite.Query;

public class HashtagSearchFragment extends HomeFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.getRootView().findViewById(R.id.toolbar);

        toolbar.setTitle("Hashtag: #"+appViewModel.currentHashtag);
    }

    @Override
    protected List<String> postsQuery()
    {
        return Arrays.asList(Query.Companion.contains("hashtags", appViewModel.currentHashtag),  Query.Companion.orderDesc("timeStamp"), Query.Companion.limit(50));
    }
}
