package de.skinsafe.app.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Repräsentiert ein Bild + Metadaten (Titel, Liste von Punkten).
 */
public class ImageModel {
    public String originalImagePath;   // z.B. currentPhotoPath
    public String cartoonImagePath;
    public String title;  // Überschrift
    public List<PointModel> points = new ArrayList<>();
}

