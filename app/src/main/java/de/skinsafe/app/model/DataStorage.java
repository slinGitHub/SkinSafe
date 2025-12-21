package de.skinsafe.app.model;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class DataStorage {

    private static final String FILENAME = "data.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Lädt die gespeicherten Daten aus data.json (falls vorhanden).
     */
    public static DataModel loadData(Context context) {
        File file = new File(context.getFilesDir(), FILENAME);
        if (!file.exists()) {
            return new DataModel(); // Noch keine Daten vorhanden
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            return gson.fromJson(br, DataModel.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new DataModel();
        }
    }

    /**
     * Speichert die Daten in data.json.
     */
    public static void saveData(Context context, DataModel data) {
        File file = new File(context.getFilesDir(), FILENAME);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

