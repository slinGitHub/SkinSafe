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
    private UpdateChecker checker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_about);

        // Check for Updates Button
        MaterialButton updateButton = findViewById(R.id.buttonCheckForUpdates);
        updateButton.setOnClickListener(v -> {
            checker = new UpdateChecker(this, this.getString(R.string.app_vers));
            checker.checkForUpdate(true);
        });

        // Init Donate Buttons
        MaterialButton donateButton1 = findViewById(R.id.buttonDonate1);
        donateButton1.setOnClickListener(v -> {
            String url = "https://www.paypal.com/ncp/payment/FMARMMKGYQDMC";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });

        MaterialButton donateButton2 = findViewById(R.id.buttonDonate2);
        donateButton2.setOnClickListener(v -> {
            String url = "https://www.paypal.com/ncp/payment/ZTPMB56LVKHGQ";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });

        MaterialButton donateButton3 = findViewById(R.id.buttonDonate3);
        donateButton3.setOnClickListener(v -> {
            String url = "https://www.paypal.com/ncp/payment/WXCHCL4VA53B8";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });


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

        // Welcome Screen Button (JETZT INNERHALB ONCREATE)
        TextView btnWelcome = findViewById(R.id.showWelcomeScreen);
        if (btnWelcome != null) {
            btnWelcome.setOnClickListener(v -> {
                // Aufruf der neuen Utility-Klasse
                UIUtils.showWelcomeDialog(this);
            });
        }
    }

    private void setupHyperlink() {
        TextView linkWeb = findViewById(R.id.linkToCheckInset);
        if (linkWeb != null) {
            linkWeb.setMovementMethod(LinkMovementMethod.getInstance());
        }

        TextView linkSource = findViewById(R.id.hyperSkinSafeSourceCode);
        if (linkSource != null) {
            linkSource.setMovementMethod(LinkMovementMethod.getInstance());
        }

        TextView linkPrivacy = findViewById(R.id.hyperlinkPrivacyPolicy);
        if (linkPrivacy != null) {
            linkPrivacy.setMovementMethod(LinkMovementMethod.getInstance());
        }

    }

}
