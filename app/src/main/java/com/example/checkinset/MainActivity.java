package com.example.checkinset;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.*;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import com.example.checkinset.model.DataModel;
import com.example.checkinset.model.ImageModel;
import com.example.checkinset.model.PointModel;
import com.example.checkinset.utils.DataStorage;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import android.animation.ValueAnimator;
import org.json.JSONException;

public class MainActivity extends AppCompatActivity implements ImageManager.ImageResultCallback {

    private LinearLayout imageContainer;
    //private FloatingActionButton addPointButton;
    private ImageButton addPointButton;
    private boolean isAddingPoint = false;
    private boolean isDeletingImage = false;

    private DataModel dataModel;
    private DataStorage dataStorage;

    private ActivityResultLauncher<String> saveFileLauncher;
    private ActivityResultLauncher<String> openFileLauncher;

    // Map: Ordnet jedem CustomImageLayout das zugehörige ImageModel zu
    private final Map<CustomImageLayout, ImageModel> layoutToImageMap = new HashMap<>();

    private ImageManager imageManager;
    private String currentImageTitle;

    private TextView tvCircleNumber;

    private TextInputEditText pointNotes, tvTimestamp, tvCoordinateX, tvCoordinateY;

    MaterialButton ratingBtnGreen, ratingBtnYellow, ratingBtnRed;

    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;

    // Global gespeicherter aktuell ausgewählter Punkt und zugehöriges Layout
    private PointModel currentPoint;
    private CustomImageLayout currentLayout;


    private DataIOManager dataIOManager;

    private boolean protectedViewOn = true; // default: an
    private boolean historyViewOn = false;

    private WavePulseAnimator waveAnimator;

    private static final String PREFS_NAME = "donation_prefs";
    private static final String KEY_LAST_SHOWN = "donation_last_shown";

    private UpdateChecker checker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Check for updates
        checker = new UpdateChecker(this, this.getString(R.string.app_vers));
        checker.checkForUpdate(false);

        // Toolbar initialisieren
        Toolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        topAppBar.bringToFront();
        ViewCompat.setElevation(topAppBar, getResources().getDisplayMetrics().density * 12); // 12dp in px
        topAppBar.invalidate();

        addPointButton = findViewById(R.id.addPointButton);
        imageContainer = findViewById(R.id.imageContainer);

        //BottomSheet initialisieren
        // Bottom Sheet Komponenten
        LinearLayout bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheet.setBackground(ContextCompat.getDrawable(this, R.drawable.bottom_sheet_background));

        tvCircleNumber = findViewById(R.id.tvCircleNumber);
        tvTimestamp = findViewById(R.id.tvTimestamp);
        tvCoordinateX = findViewById(R.id.tvCoordinateX);
        tvCoordinateY = findViewById(R.id.tvCoordinateY);
        ratingBtnGreen = findViewById(R.id.ratingBtnGreen);
        ratingBtnYellow = findViewById(R.id.ratingBtnYellow);
        ratingBtnRed = findViewById(R.id.ratingBtnRed);
        pointNotes = findViewById(R.id.pointNotes);
        AppCompatImageButton btnDeletePoint = findViewById(R.id.btnDeletePoint);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        bottomSheet.post(() -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN));
        bottomSheetBehavior.setPeekHeight(bottomSheet.getHeight());


        //BottomSheet Date/Clock
        tvTimestamp.setOnClickListener(v -> {
            // Versuche das vorhandene Datum/Uhrzeit zu parsen, ansonsten aktuelles Datum/Uhrzeit verwenden
            Calendar calendar = Calendar.getInstance();
            if (currentPoint != null && currentPoint.timestamp != null) {
                // Erwartetes Format: "yyyy-MM-dd HH:mm:ss"
                String timestamp = currentPoint.timestamp;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = null;
                try {
                    date = sdf.parse(timestamp);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                if (date != null)
                    calendar.setTime(date);
            }

            // Öffne den DatePickerDialog
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    MainActivity.this,
                    (datePicker, year, month, dayOfMonth) -> {
                        // Datum wurde ausgewählt, jetzt den Kalender aktualisieren
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        // Öffne anschließend den TimePickerDialog
                        TimePickerDialog timePickerDialog = new TimePickerDialog(
                                MainActivity.this,
                                (timePicker, hourOfDay, minute) -> {
                                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                    calendar.set(Calendar.MINUTE, minute);
                                    // Neues Datum und Uhrzeit kombinieren und formatieren
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                    String newDateTime = sdf.format(calendar.getTime());
                                    tvTimestamp.setText(newDateTime);
                                    // Aktualisiere das Datum im aktuellen PointModel
                                    if (currentPoint != null) {
                                        currentPoint.timestamp = newDateTime;
                                        refreshAllPoints();
                                        DataStorage.saveData(MainActivity.this, dataModel);
                                    }
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true // 24-Stunden-Format
                        );
                        timePickerDialog.show();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();

        });

        // Für X-Koordinate
        //TextView tvCoordinateX = findViewById(R.id.tvCoordinateX);
        tvCoordinateX.setOnClickListener(v -> {
            // Aktuellen X-Wert holen (hier als float, z.B. aus dem aktuellen PointModel)
            float currentX = currentPoint != null ? currentPoint.xPercent * currentLayout.getWidth() : 0f;
            showCoordinateEditDialog("X", currentX, newValue -> {
                // Aktualisiere den aktuellen PointModel-Wert (hier als Prozentwert)
                if (currentPoint != null && currentLayout != null) {
                    currentPoint.xPercent = newValue / (float) currentLayout.getWidth();
                    // Aktualisiere die Anzeige
                    tvCoordinateX.setText(String.format(Locale.getDefault(), "%.2f", newValue));
                    loadUIFromDataModel();
                    DataStorage.saveData(MainActivity.this, dataModel);
                }
            });
        });

        // Für Y-Koordinate
        //TextView tvCoordinateY = findViewById(R.id.tvCoordinateY);
        tvCoordinateY.setOnClickListener(v -> {
            float currentY = currentPoint != null ? currentPoint.yPercent * currentLayout.getHeight() : 0f;
            showCoordinateEditDialog("Y", currentY, newValue -> {
                if (currentPoint != null && currentLayout != null) {
                    currentPoint.yPercent = newValue / (float) currentLayout.getHeight();
                    tvCoordinateY.setText(String.format(Locale.getDefault(), "%.2f", newValue));
                    loadUIFromDataModel();
                    DataStorage.saveData(MainActivity.this, dataModel);
                }
            });
        });

        ratingBtnGreen.setOnClickListener(v -> selectRating(1,true, ratingBtnGreen, R.drawable.ic_circle_selected_green, R.color.circleBackgroundGreen, ratingBtnYellow, ratingBtnRed));
        ratingBtnYellow.setOnClickListener(v -> selectRating(2,true, ratingBtnYellow, R.drawable.ic_circle_selected_yellow, R.color.circleBackgroundYellow, ratingBtnGreen, ratingBtnRed));
        ratingBtnRed.setOnClickListener(v -> selectRating(3,true, ratingBtnRed, R.drawable.ic_circle_selected_red, R.color.circleBackgroundRed, ratingBtnGreen, ratingBtnYellow));

        pointNotes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Keine Aktion erforderlich
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Text im aktuellen PointModel speichern
                if (currentPoint != null) {
                    currentPoint.notes = s.toString();
                    DataStorage.saveData(MainActivity.this, dataModel); // Datenmodell speichern
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
                // Keine Aktion erforderlich
            }
        });


        // Aktion des Lösch-Buttons im Bottom Sheet
        btnDeletePoint.setOnClickListener(v -> {
            if (currentPoint != null && currentLayout != null) {
                ImageModel imgModel = layoutToImageMap.get(currentLayout);
                if (imgModel != null) {
                    imgModel.points.remove(currentPoint);
                    // UI neu aufbauen (alternativ: gezielt den Punkt entfernen)
                    loadUIFromDataModel();
                    DataStorage.saveData(MainActivity.this, dataModel);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    Toast.makeText(MainActivity.this, "Point deleted.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        addPointButton.setOnClickListener(v -> {
            isAddingPoint = true;
            addPointButton.setEnabled(false);
            Toast.makeText(MainActivity.this, "Tap on a picture to set a point.", Toast.LENGTH_SHORT).show();
        });

        // Datenmodell laden
        dataModel = DataStorage.loadData(this);

        // UI aus dem Datenmodell aufbauen
        loadUIFromDataModel();

        // ImageManager initialisieren
        imageManager = new ImageManager(this, this, this);

        dataIOManager = new DataIOManager(this, dataModel, dataStorage, this);

        saveFileLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/zip"),
                uri -> {
                    if (uri != null) {
                        try {
                            dataIOManager.exportDataToZip(uri);
                        } catch (IOException e) {
                            Toast.makeText(this, "Error when exporting the data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Export canceled.", Toast.LENGTH_SHORT).show();
                    }
                });
        openFileLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            dataIOManager.importDataFromZip(uri);
                        } catch (IOException | JSONException e) {
                            Toast.makeText(this, "Fehler beim Importieren der Daten: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Import abgebrochen.", Toast.LENGTH_SHORT).show();
                    }
                });

        //Add image of white owl to toolbar
        try {
            getSupportActionBar().setIcon(R.drawable.iconsowl_vector_purple);
        } catch (Exception e) {
            Log.e("MainActivity", "onCreate: Error setting toolbar icon", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        getMenuInflater().inflate(R.menu.top_app_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }else if (id == R.id.action_capture_image) {
            // Vor der Kameraaufnahme evtl. Titel abfragen
            showTitleInputDialog(true);
            return true;
        } else if (id == R.id.action_pick_image) {
            showTitleInputDialog(false);
            return true;
        } else if (id == R.id.action_delete_image) {
            isDeletingImage = true;
            Toast.makeText(this, "Now tap on the picture you want to delete. (Incl. points)", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_export_data) {
            dataIOManager.exportData(saveFileLauncher);
            return true;
        } else if (id == R.id.action_import_data) {
            importData();
            return true;
        } else if (item.getItemId() == R.id.action_toggle_history) {
            historyViewOn = !historyViewOn; //Toggle protected view
            item.setIcon(historyViewOn ? R.drawable.outline_history_24 : R.drawable.outline_history_toggle_off_24);
            loadUIFromDataModel();
            return true;
        } else if (item.getItemId() == R.id.action_toggle_protected_images) {
            protectedViewOn = !protectedViewOn; //Toggle protected view
            item.setIcon(protectedViewOn ? R.drawable.ic_wappen_on : R.drawable.ic_wappen_off);
            loadUIFromDataModel();
            return true;
        } else if (item.getItemId() == R.id.check_for_updates) {
            checker.checkForUpdate(true);
            return true;
        } else if (item.getItemId() == R.id.action_aboutSkinSafe) {
            Intent intent_settings_about = new Intent(this, SettingsAboutActivity.class);
            startActivity(intent_settings_about);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    private void importData() {
        openFileLauncher.launch("*/*");
    }

    private interface CoordinateUpdateListener {
        void onCoordinateUpdated(float newValue);
    }

    private void showCoordinateEditDialog(String label, float currentValue, CoordinateUpdateListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(label + " Koordinate bearbeiten");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.getDefault(), "%.2f", currentValue));
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            try {
                float newValue = Float.parseFloat(input.getText().toString());
                listener.onCoordinateUpdated(newValue);
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, "Invalid value", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Abort", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showTitleInputDialog(boolean fromCamera) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter heading");

        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(editText);

        builder.setPositiveButton("OK", (dialog, which) -> {
            currentImageTitle = editText.getText().toString();
            imageManager.setCurrentImageTitle(currentImageTitle);
            if (fromCamera) {
                imageManager.checkCameraPermissionAndOpenCamera();
            } else {
                imageManager.openGallery();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Baut die komplette UI aus dem Datenmodell auf.
     */
    public void loadUIFromDataModel() {
        imageContainer.removeAllViews();
        layoutToImageMap.clear();

        for (ImageModel img : dataModel.images) {
            addImageToUI(img);
        }

        showPointDetails(currentPoint, currentLayout);
    }

    public void updateDataModel(DataModel newModel) {
        this.dataModel = newModel;
        loadUIFromDataModel();
    }

    /**
     * Zeigt ein Bild samt Überschrift und Punkten.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void addImageToUI(ImageModel imageModel) {
        final long[] lastTapTime = {0}; // Array, um eine mutable Variable zu haben
        String imagePath = protectedViewOn ? imageModel.cartoonImagePath : imageModel.originalImagePath ;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap != null) {
            try {
                ExifInterface exif = new ExifInterface(imagePath);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int rotationDegrees = exifToDegrees(orientation);
                if (rotationDegrees != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotationDegrees);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }
            } catch (IOException e) {
                Log.e("MainActivity", "addImageToUI: Error creating the image.", e);
            }
        }

        CustomImageLayout customLayout = new CustomImageLayout(this);
        customLayout.setImageBitmap(bitmap);

        // TouchListener für das CustomImageLayout:
        customLayout.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Falls das Bild gelöscht werden soll:
                if (isDeletingImage) {
                    removeImage(customLayout);
                    isDeletingImage = false;
                    return true;
                }
                // Falls ein neuer Punkt gesetzt werden soll:
                if (isAddingPoint) {
                    float xPercent = event.getX() / customLayout.getWidth();
                    float yPercent = event.getY() / customLayout.getHeight();
                    createPoint(customLayout, xPercent, yPercent);
                    isAddingPoint = false;
                    addPointButton.setEnabled(true);
                    return true;
                }
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastTapTime[0] < 300) { // 300 ms als Schwellwert
                    // Doppelklick erkannt: Ermittle den am nächsten liegenden Punkt
                    float xPercent = event.getX() / customLayout.getWidth();
                    float yPercent = event.getY() / customLayout.getHeight();
                    ImageModel imgModel = layoutToImageMap.get(customLayout);
                    if (imgModel != null && !imgModel.points.isEmpty()) {
                        PointModel nearestPoint = getClosestPoint(imgModel, xPercent, yPercent);
                        if (nearestPoint != null) {
                            currentPoint = nearestPoint;
                            currentLayout = customLayout;
                            showPointDetails(nearestPoint, customLayout);
                        }
                    }
                }
                lastTapTime[0] = currentTime;
                return true;
            }
            return false;
        });

        TextView titleView = new TextView(this, null, 0, R.style.ImageTitle);
        titleView.setText(imageModel.title);
        titleView.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_corners_image_title));
        titleView.setPadding(16, 16, 16, 16); // Padding für besseren Abstand

        // Layout-Parameter für die Positionierung der Überschrift
        FrameLayout.LayoutParams titleLayoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        titleLayoutParams.gravity = Gravity.TOP | Gravity.START; // Links oben positionieren
        titleView.setLayoutParams(titleLayoutParams);

        // Überschrift direkt zum CustomImageLayout hinzufügen
        customLayout.addView(titleView);

        // CustomImageLayout in den Container einfügen
        LinearLayout containerLayout = new LinearLayout(this);
        containerLayout.setOrientation(LinearLayout.VERTICAL);
        containerLayout.setPadding(0, 16, 0, 16);
        containerLayout.addView(customLayout, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        imageContainer.addView(containerLayout);

        customLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                customLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                for (PointModel p : imageModel.points) {
                    addPointView(customLayout, p.xPercent, p.yPercent, p.color, p);
                }
                refreshAllPoints();
            }
        });
        layoutToImageMap.put(customLayout, imageModel);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        imageManager.handleActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onImageCaptured(String cartoonImagePath, String originalImagePath, String imageTitle) {
        ImageModel newImg = new ImageModel();
        newImg.originalImagePath = originalImagePath;
        newImg.cartoonImagePath = cartoonImagePath;
        newImg.title = imageTitle;
        dataModel.images.add(newImg);
        addImageToUI(newImg);
        DataStorage.saveData(this, dataModel);
    }

    @Override
    public void onError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAllPoints();

        // Wähle ein View als Parent, z. B. die Root-Layout ID
        View coordinator = findViewById(R.id.coordinatorLayout);
        showCoffeeDonationSnackbar(coordinator);
    }

    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            // Deine alte Logik
            int state = bottomSheetBehavior.getState();
            if (state == BottomSheetBehavior.STATE_EXPANDED
                    || state == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                refreshAllPoints();
                return;
                // hier nicht weiterleiten -> Back wurde behandelt
            } else {
                // Nicht behandelt: temporär deaktivieren und System die Back-Action ausführen lassen
                setEnabled(false);
                MainActivity.this.getOnBackPressedDispatcher().onBackPressed();
                // optional wieder aktivieren (Lifecycle macht das meist automatisch)
                setEnabled(true);
            }
        }
    };

    private void createPoint(CustomImageLayout layout, float xPercent, float yPercent) {
        ImageModel imgModel = layoutToImageMap.get(layout);
        if (imgModel == null) return;

        PointModel p = new PointModel();
        p.xPercent = xPercent;
        p.yPercent = yPercent;

        p.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        p.color = 0;

        imgModel.points.add(p);
        addPointView(layout, xPercent, yPercent, p.color, p);
        refreshAllPoints();
        DataStorage.saveData(this, dataModel);
    }

    private void addPointView(CustomImageLayout layout, float xPercent, float yPercent, int color, PointModel point) {
        int size = (int) (20 * getResources().getDisplayMetrics().density);
        int actualX = (int) (layout.getWidth() * xPercent) - size / 2;
        int actualY = (int) (layout.getHeight() * yPercent) - size / 2;

        // 1) Container anlegen und mit unserem PointModel taggen
        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setTag(point);
        FrameLayout.LayoutParams wrapperLp = new FrameLayout.LayoutParams(size, size);
        wrapperLp.leftMargin = actualX;
        wrapperLp.topMargin  = actualY;
        layout.addView(wrapper, wrapperLp);

        // 2) Kreis‑View
        View circle = new View(this);
        circle.setId(R.id.point_circle);
        setCircleBackground(circle, color, 255, 0);


        wrapper.addView(circle, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // 3) Label‑View
        TextView overlay = new TextView(this);
        overlay.setId(R.id.point_label);
        overlay.setGravity(Gravity.CENTER);
        overlay.setTypeface(Typeface.DEFAULT_BOLD);
        overlay.setTextSize(10);
        overlay.setText(String.valueOf(getDaysDifference(point.timestamp)));
        wrapper.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }


    private long getDaysDifference(String timestamp) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date pointDate = df.parse(timestamp);
            if (pointDate == null) return 0;
                long diffMillis = new Date().getTime() - pointDate.getTime();
            return diffMillis / (1000L * 60 * 60 * 24); // Ganzzahlige Tage
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private PointModel getClosestPoint(ImageModel imageModel, float xPercent, float yPercent) {
        PointModel closestPoint = null;
        double minDistance = Double.MAX_VALUE;
        int labelFadedDays = SettingsManager.getlabelFadedDaysOff(this);; // Ab wieviel Tagen soll das Label ausgeblendet werden?
        long daysDifference;

        for (PointModel point : imageModel.points) {
            daysDifference = getDaysDifference(point.timestamp);
            if ((daysDifference <= labelFadedDays) || historyViewOn) { // Nur Punkte berücksichtigen, deren Label sichtbar ist
                double dx = xPercent - point.xPercent;
                double dy = yPercent - point.yPercent;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestPoint = point;
                }
            }
        }
        return closestPoint;
    }

    //Refresh and Show Bottom Sheet details of selected point
    private void showPointDetails(PointModel point, CustomImageLayout layout) {
        if (point == null) {
            Log.e("MainActivity", "showPointDetails: point ist null – kein Datenobjekt vorhanden");
            return;
        }

        // Zuerst alle Punkte auf den Standardradius zurücksetzen
        refreshAllPoints();

        if (bottomSheetBehavior == null) {
            Log.e("BottomSheet", "❌ Error: BottomSheetBehavior ist null!");
            return;
        }

        //Set Days old
        tvCircleNumber.setText(String.valueOf(getDaysDifference(point.timestamp)));

        // Color top Point in bottom sheet
        GradientDrawable bgDrawable = (GradientDrawable) tvCircleNumber.getBackground();
        if (point.mark <= 1) {
            bgDrawable.setColor(ContextCompat.getColor(this, R.color.circleBackgroundGreen));
            tvCircleNumber.setTextColor(ContextCompat.getColor(this, R.color.circleFontGreen));
            selectRating(1,false, ratingBtnGreen, R.drawable.ic_circle_selected_green, R.color.circleBackgroundGreen, ratingBtnYellow, ratingBtnRed);
        } else if (point.mark == 2) {
            bgDrawable.setColor(ContextCompat.getColor(this, R.color.circleBackgroundYellow));
            tvCircleNumber.setTextColor(ContextCompat.getColor(this, R.color.circleFontYellow));
            selectRating(2,false, ratingBtnYellow, R.drawable.ic_circle_selected_yellow, R.color.circleBackgroundYellow, ratingBtnGreen, ratingBtnRed);
        } else {
            bgDrawable.setColor(ContextCompat.getColor(this, R.color.circleBackgroundRed));
            tvCircleNumber.setTextColor(ContextCompat.getColor(this, R.color.circleFontRed));
            selectRating(3,false, ratingBtnRed, R.drawable.ic_circle_selected_red, R.color.circleBackgroundRed, ratingBtnGreen, ratingBtnYellow);
        }

        // Setze Text für den ausgewählten Punkt
        tvTimestamp.setText(DateUtils.convertDate(point.timestamp));

        int absX = (int) (layout.getWidth() * point.xPercent);
        int absY = (int) (layout.getHeight() * point.yPercent);
        tvCoordinateX.setText(String.valueOf(absX));
        tvCoordinateY.setText(String.valueOf(absY));
        pointNotes.setText(currentPoint.notes);

        // Bottom Sheet anzeigen
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        // Neuer aktueller Punkt
        currentPoint = point;
        currentLayout = layout;

        View wrapper = layout.findViewWithTag(currentPoint);
        if (!(wrapper == null)) {
            // Find circle view
            View circle = wrapper.findViewById(R.id.point_circle);
            int strokeWidthDp = 2;
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            float density = getResources().getDisplayMetrics().density;
            int strokeWidthPx = (int) (strokeWidthDp * density + 0.5f);
            gd.setStroke(strokeWidthPx, ContextCompat.getColor(this, R.color.white));
            //Get background color of circle
            gd.setColor(bgDrawable.getColor());
            circle.setBackground(gd);
        }

        if (waveAnimator != null) {
            waveAnimator.stop();
        }

        int sizePx = (int) (25 * getResources().getDisplayMetrics().density);
        int actualX = (int) (layout.getWidth() * point.xPercent) - sizePx / 2;
        int actualY = (int) (layout.getHeight() * point.yPercent) - sizePx / 2;

        // Wellen-Animator initialisieren und starten
        waveAnimator = new WavePulseAnimator(
                layout, // oder das passende Container-View
                actualX,
                actualY,
                sizePx,
                Color.WHITE,
                0.8f,
                3f
        );
        waveAnimator.start();
    }

    //Modifiy design of all points on images (NOT Bottom Sheet)
    private void refreshAllPoints() {

        if (waveAnimator != null) {
            waveAnimator.stop();
        }

        int strokeColor = android.R.color.transparent;
        float strokeWidthDp = 0;
        int alphaHistory = 255;
        int labelFadedDays = SettingsManager.getlabelFadedDaysOff(this);; // Ab wieviel Tagen soll das Label ausgeblendet werden?
        int labelOffDays = SettingsManager.getDaysForLabelOff(this);
        int maxPoints = SettingsManager.getMaxPointsHistory(this);

        // Über alle Bilder gehen
        for (Map.Entry<CustomImageLayout, ImageModel> e : layoutToImageMap.entrySet()) {
            CustomImageLayout layout = e.getKey();

            // Über alle Punkte in diesem Bild gehen
            for (PointModel p : e.getValue().points) {
                // Container finden
                View wrapper = (View) layout.findViewWithTag(p);
                if (wrapper == null) continue;

                // Find circle view
                View circle = wrapper.findViewById(R.id.point_circle);

                // Label updaten
                TextView label = wrapper.findViewById(R.id.point_label);
                long daysDifference = getDaysDifference(p.timestamp);

                // History-View!! Alle Punkte anzeigen, keine Zahlen, nur Kreise
                if (historyViewOn) {
                    // ✅ Alle Punkte behalten, keine Zahl anzeigen
                    label.setText("");

                    //Add alpha to color
                    alphaHistory = 120; // =75% Transparent 255=Not transparent
                    label.setTextColor(Color.TRANSPARENT); // optional, sonst leer

                    //Set Stroke width for history graph
                    strokeWidthDp = 1.5f;

                    // Kreis
                    int baseSize = (int) (16 * getResources().getDisplayMetrics().density);
                    int newSize = (int) (baseSize * 1f); //1 = keine Kreis Skalierung

                    // Kreisgröße setzen
                    ViewGroup.LayoutParams params = circle.getLayoutParams();
                    params.width = newSize;
                    params.height = newSize;
                    circle.setLayoutParams(params);

                    // Wrapper-Layout neu positionieren, damit der Kreis mittig bleibt
                    FrameLayout.LayoutParams wrapperLp = (FrameLayout.LayoutParams) wrapper.getLayoutParams();
                    int actualX = (int) (layout.getWidth() * p.xPercent) - newSize / 2;
                    int actualY = (int) (layout.getHeight() * p.yPercent) - newSize / 2;
                    wrapperLp.width = newSize;
                    wrapperLp.height = newSize;
                    wrapperLp.leftMargin = actualX;
                    wrapperLp.topMargin = actualY;
                    wrapper.setLayoutParams(wrapperLp);

                } else {

                    // Label anzeigen
                    if (daysDifference <= labelOffDays) {
                        label.setText(String.valueOf(daysDifference));

                        label.setTypeface(label.getTypeface(), Typeface.BOLD);


                    } else {

                        // Labels nachdem die Tage überschritten sind, ausblenden und Kreis verkleinern
                        if (daysDifference <= labelFadedDays) {
                            label.setText(""); // Kein Label anzeigen

                            int baseSize = (int) (16 * getResources().getDisplayMetrics().density);

                            // Linearer Skalierungsfaktor von 1.0 (100%) bis 0.25 (25%)
                            float t = (float) (daysDifference - labelOffDays) / (labelFadedDays - labelOffDays);
                            t = Math.max(0f, Math.min(1f, t)); // Clamp zwischen 0 und 1
                            float scale = 1.0f - t * 0.75f; // 1.0 -> 0.25

                            int newSize = (int) (baseSize * scale);

                            // Kreisgröße setzen
                            ViewGroup.LayoutParams params = circle.getLayoutParams();
                            params.width = newSize;
                            params.height = newSize;
                            circle.setLayoutParams(params);

                            // Wrapper-Layout neu positionieren, damit der Kreis mittig bleibt
                            FrameLayout.LayoutParams wrapperLp = (FrameLayout.LayoutParams) wrapper.getLayoutParams();
                            int actualX = (int) (layout.getWidth() * p.xPercent) - newSize / 2;
                            int actualY = (int) (layout.getHeight() * p.yPercent) - newSize / 2;
                            wrapperLp.width = newSize;
                            wrapperLp.height = newSize;
                            wrapperLp.leftMargin = actualX;
                            wrapperLp.topMargin = actualY;
                            wrapper.setLayoutParams(wrapperLp);

                        } else {
                            // Punkt aus dem Layout entfernen
                            layout.removeView(wrapper);
                            continue;

                        }
                    }
                }

                // Kreisfarbe
                if (p.mark <= 1) {
                    setCircleBackground(circle, ContextCompat.getColor(this, R.color.circleBackgroundGreen), alphaHistory, strokeWidthDp);
                    label.setTextColor(ContextCompat.getColor(this, R.color.circleFontGreen));
                } else if (p.mark == 2) {
                    setCircleBackground(circle, ContextCompat.getColor(this, R.color.circleBackgroundYellow), alphaHistory, strokeWidthDp);
                    label.setTextColor(ContextCompat.getColor(this, R.color.circleFontYellow));
                } else if (p.mark > 2) {
                    setCircleBackground(circle, ContextCompat.getColor(this, R.color.circleBackgroundRed), alphaHistory, strokeWidthDp);
                    label.setTextColor(ContextCompat.getColor(this, R.color.circleFontRed));
                }

            }
        }
    }

    private void setCircleBackground(View view, int fillColor, int alphaHistory, float strokeWidthDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);

        //Transparenz nur auf die Füllfarbe anwenden
        int fillWithAlpha = ColorUtils.setAlphaComponent(fillColor, alphaHistory);
        gd.setColor(fillWithAlpha);

        //Stroke bleibt deckend (keine Alpha-Reduktion)
        if (strokeWidthDp > 0) {
            float density = getResources().getDisplayMetrics().density;
            int strokeWidthPx = (int) (strokeWidthDp * density + 0.5f);
            gd.setStroke(strokeWidthPx, fillColor);
        }

        view.setBackground(gd);
    }

    private void removeImage(CustomImageLayout layout) {
        ImageModel imageModel = layoutToImageMap.get(layout);
        if (imageModel == null) return;
        dataModel.images.remove(imageModel);
        View containerLayout = (View) layout.getParent();
        if (containerLayout != null && containerLayout.getParent() instanceof ViewGroup) {
            ((ViewGroup) containerLayout.getParent()).removeView(containerLayout);
        }
        layoutToImageMap.remove(layout);
        DataStorage.saveData(this, dataModel);
        refreshAllPoints();
        Toast.makeText(this, "Image deleted.", Toast.LENGTH_SHORT).show();
    }

    private int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) return 90;
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) return 180;
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) return 270;
        return 0;
    }

    private void showCoffeeDonationSnackbar(View parentView) {
        if (!shouldShowDonationSnackbar()) return;

        String message = "Enjoying the app? Donate me a coffee to support its growth! ☕";

        Snackbar snackbar = Snackbar.make(parentView, message, Snackbar.LENGTH_INDEFINITE);

        // Snackbar an den Button binden
        //snackbar.setAnchorView(anchorView);

        setLastShownSnackbar(); // optional

        snackbar.setAction("DONATE 5$", v -> {
            String url = "https://www.paypal.com/donate?hosted_button_id=CF3AHXTKNARRL";
            parentView.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            snackbar.dismiss();
        });

        snackbar.setActionTextColor(Color.parseColor("#2A9D8F"));
        snackbar.show();
    }

    // Prüfen, ob die Erinnerung angezeigt werden soll
    private boolean shouldShowDonationSnackbar() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long lastShown = prefs.getLong(KEY_LAST_SHOWN, 0);
        long now = System.currentTimeMillis();
        long threeMonthsMillis = 1000L * 60; // * 60 * 24 * 30 * 3; // ca. 3 Monate
        return now - lastShown > threeMonthsMillis;
    }

    // Zeitstempel speichern
    private void setLastShownSnackbar() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_SHOWN, System.currentTimeMillis()).apply();
    }

    // Set rating buttons drawable circle (See on Click listener)
    private void selectRating(int mark, boolean saveData, MaterialButton selectedButton, int selectedDrawable, int selectedColor, MaterialButton... otherButtons) {
        if (currentPoint == null) return; // Absturz verhindern
        if (currentLayout == null) return; // Absturz verhindern

        // Set Selected Button
        selectedButton.setIconTint(null);
        selectedButton.setIcon(getDrawable(selectedDrawable));
        currentPoint.mark = mark;

        if (saveData) {
            // Save data
            showPointDetails(currentPoint, currentLayout);
            DataStorage.saveData(MainActivity.this, dataModel);
        }

        // Reset other buttons
        for (MaterialButton btn : otherButtons) {
            btn.setIcon(getDrawable(R.drawable.ic_circle));

            // Farbe basierend auf Button ID setzen
            int color;
            if (btn.getId() == R.id.ratingBtnGreen) {
                color = R.color.circleBackgroundGreen;
            } else if (btn.getId() == R.id.ratingBtnYellow) {
                color = R.color.circleBackgroundYellow;
            } else { // ratingBtnRed
                color = R.color.circleBackgroundRed;
            }

            btn.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, color)));
        }
    }

}
