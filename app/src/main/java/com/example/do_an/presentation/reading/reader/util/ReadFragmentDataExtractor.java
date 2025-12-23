package com.example.do_an.presentation.reading.reader.util;

import android.os.Bundle;

public class ReadFragmentDataExtractor {

    private String episodePdfLink;
    private String pdfPath;
    private String currentStoryId;
    private String mainStoryTitle;
    private String currentTitle;
    private String currentAuthor;
    private String currentImageUrl;

    public void extractFromBundle(Bundle args) {
        if (args == null)
            return;

        episodePdfLink = args.getString("PDF_LINK");
        pdfPath = args.getString("PDF_PATH");
        currentStoryId = args.getString("STORY_ID");
        mainStoryTitle = args.getString("STORY_TITLE");
        currentAuthor = args.getString("STORY_AUTHOR");
        currentImageUrl = args.getString("STORY_IMAGE_URL");

        String tapName = args.getString("TAP_TITLE");
        if (tapName == null || tapName.isEmpty()) {
            tapName = args.getString("TAP");
        }

        if (tapName != null && !tapName.isEmpty()) {
            currentTitle = tapName;
        } else {
            currentTitle = "Tập mới nhất";
        }
    }

    public String getEpisodePdfLink() {
        return episodePdfLink;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public String getCurrentStoryId() {
        return currentStoryId;
    }

    public String getMainStoryTitle() {
        return mainStoryTitle;
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    public String getCurrentAuthor() {
        return currentAuthor;
    }

    public String getCurrentImageUrl() {
        return currentImageUrl;
    }

    public void setCurrentStoryId(String currentStoryId) {
        this.currentStoryId = currentStoryId;
    }

    public void setMainStoryTitle(String mainStoryTitle) {
        this.mainStoryTitle = mainStoryTitle;
    }

    public void setCurrentTitle(String currentTitle) {
        this.currentTitle = currentTitle;
    }

    public void setCurrentAuthor(String currentAuthor) {
        this.currentAuthor = currentAuthor;
    }
}
