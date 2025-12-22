package com.example.do_an.data.library.remote;

import com.example.do_an.data.library.remote.dto.SearchRequest;
import com.example.do_an.data.library.remote.dto.SearchResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SearchApi {

    @POST("decision")
    Call<SearchResponse> searchStory(@Body SearchRequest request);
}

