package com.example.do_an.data.library.remote.dto;

import java.util.List;
import java.util.Map;

public class SearchResponse {
    public String decision_type;
    public String story;
    public int confidence;
    public String intent;
    public List<String> stories;
    public Map<String, List<String>> data;
}

