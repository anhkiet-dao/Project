package com.example.do_an.search;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SearchApi {

    @POST("decision")
    Call<SearchResponse> searchStory(@Body SearchRequest request);
}

