package com.example.checkinset;

import android.content.Intent;
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
            String url = "https://www.paypal.com/donate?hosted_button_id=CF3AHXTKNARRL";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });

        //Activate Hyperlink at About Checkmate Screen
        setupHyperlink();
    }

    private void setupHyperlink() {
        TextView linkTextView = findViewById(R.id.linkToCheckInset);
        linkTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
