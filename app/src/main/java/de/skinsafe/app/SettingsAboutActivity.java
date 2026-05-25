package de.skinsafe.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class SettingsAboutActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_about);

        // Donate Button öffnen
        MaterialButton donateButton = findViewById(R.id.buttonDonate);
        donateButton.setOnClickListener(v -> {
            String url = "https://www.paypal.com/ncp/payment/WY7DJ55SGCU6E";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });

        //Get Build Version Name for About SkinSafe page
        TextView tvVersion = findViewById(R.id.textVersion);
        if (tvVersion != null) {
            try {
                String versionName = getPackageManager()
                        .getPackageInfo(getPackageName(), 0).versionName;

                tvVersion.setText(
                        getString(R.string.app_vers, versionName)
                );

            } catch (PackageManager.NameNotFoundException e) {
                tvVersion.setText("Version unbekannt");
            }
        }

        //Activate Hyperlink at About Checkmate Screen
        setupHyperlink();
    }

    private void setupHyperlink() {
        TextView linkTextView = findViewById(R.id.linkToCheckInset);
        linkTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
