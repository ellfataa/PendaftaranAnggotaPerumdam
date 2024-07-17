package com.example.formregistrasi;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.net.DatagramPacket;

import javax.xml.transform.Result;

public class RegistrasiActivity extends AppCompatActivity {

    Button btnPickImage;
    ImageView fotoKTP;

    ActivityResultLauncher<Intent> resultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrasi);

        btnPickImage = findViewById(R.id.btnPickImage);
        fotoKTP = findViewById(R.id.fotoKTP);
        registerResult();

        btnPickImage.setOnClickListener(view -> pickImage());
        Intent intent;
        resultLauncher.launch(intent);
    }

    private void pickImage(){
        Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
    }

    private void registerResult(){
        resultLauncher = registerForActivityResult(
                new ActivityResultContract.StartActivityForResult(),
                new ActivityResultCallback<Result>() {
                    @Override
                    public void onActivityResult(Result o) {
                        try{
                            DatagramPacket result;
                            Uri imageUri = result.getData().getData();
                            fotoKTP.setImageURI(imageUri);
                        }catch (Exception e){
                            Toast.makeText(RegistrasiActivity.this, "Tidak ada gambar yang terpilih", Toast.LENGTH_SHORT).show();
                        }
                    }

                    

                }
        );
    }
}