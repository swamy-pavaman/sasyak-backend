package com.kapilagro.sasyak.model;
public  class BatchPresignedUrlRequest {
    private String[] fileNames;
    private int expiryHours = 1;

    public String[] getFileNames() {
        return fileNames;
    }

    public void setFileNames(String[] fileNames) {
        this.fileNames = fileNames;
    }

    public int getExpiryHours() {
        return expiryHours;
    }

    public void setExpiryHours(int expiryHours) {
        this.expiryHours = expiryHours;
    }
}