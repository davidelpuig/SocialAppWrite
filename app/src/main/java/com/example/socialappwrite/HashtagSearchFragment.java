package com.example.socialappwrite;

import java.util.Arrays;
import java.util.List;

import io.appwrite.Query;

public class HashtagSearchFragment extends HomeFragment {

    @Override
    protected List<String> postsQuery()
    {
        return Arrays.asList(Query.Companion.contains("hashtags", appViewModel.currentHashtag),  Query.Companion.orderDesc("timeStamp"), Query.Companion.limit(50));
    }
}
