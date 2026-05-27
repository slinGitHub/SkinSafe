package de.skinsafe.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class UIUtils {

    // Der bekannte Welcome-Dialog
    public static void showWelcomeDialog(Context context) {
        String welcomeMessage = "Track your catheter positions and protect your skin with ease.\n\n" +
                "• Maximum Data Privacy: All your data and photos stay securely on your phone.\n\n" +
                "• No cloud, no data sharing: The integrated TFLite GAN (Generative Adversarial Network) model " +
                "cartoonifies your images directly on your device.\n\n" +
                "Explore all features like History tracking and position visualization in our guide.";

        showStyledDialog(context, "Welcome to SkinSafe!", welcomeMessage, "Visit Guide", "https://slingithub.github.io/SkinSafeWeb/");
    }

    // NEU: Der Update-Dialog im gleichen Design
    public static void showUpdateDialog(Context context, String newVersion, String downloadUrl) {
        String updateMessage = "SkinSafe " + newVersion + "\n\n" +
                "Click 'Download Now' to get the latest features and improvements.";

        showStyledDialog(context, "Update Available", updateMessage, "Download Now", downloadUrl);
    }

    // Hilfsmethode, um den Code trocken (DRY) zu halten
    private static void showStyledDialog(Context context, String title, String message, String positiveBtnText, String url) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveBtnText, (dialog, which) -> {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                })
                .setNegativeButton("Later", (dialog, which) -> dialog.dismiss())
                .show();
    }
}