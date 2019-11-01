package com.example.imageresizer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int GALERY_CODE_OUTPUT = 1002;
    private static final int PERMISION_CODE = 1000;
    private static final int CAMERA_CODE_OUTPUT = 1001;


    ImageView imageView, imageResize;
    TextView oriSize, resizedSize, oriScale, resizedScale;
    Button take;

    Uri image_uri;

    Bitmap bitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.image_result);
        imageResize = findViewById(R.id.image_resize);
        oriSize = findViewById(R.id.tvOriginal_size);
        resizedSize = findViewById(R.id.tvResize_size);
        oriScale = findViewById(R.id.tvOriginal_scale);
        resizedScale = findViewById(R.id.tvResize_scale);
        take = findViewById(R.id.button_take);

        take.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPictureDialog();
            }
        });


    }

    private void showPictureDialog(){

        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this);
        pictureDialog.setTitle("Select Action");
        String[] pictureDialogItems = {
                "Select photo from gallery",
                "Capture photo from camera" };
        pictureDialog.setItems(pictureDialogItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                photoFromGallery();
                                break;
                            case 1:
                                takeFromCamera();
                                break;
                        }
                    }
                });
        pictureDialog.show();

    }


    private void photoFromGallery() {
        Intent intent=new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES,mimeTypes);
        startActivityForResult(intent,GALERY_CODE_OUTPUT);
    }


    private void takeFromCamera() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {

                String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

                requestPermissions(permission, PERMISION_CODE);
            } else {
                openCamera();

            }

        } else {
            openCamera();
        }
    }

    private void openCamera(){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "from camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, CAMERA_CODE_OUTPUT);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Permission denied..", Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode){
                case GALERY_CODE_OUTPUT:
                    if(data!=null){
                        try {
                            image_uri = data.getData();
                            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        loadImage();
                    }

                case CAMERA_CODE_OUTPUT:
                    if(data!=null){
                        try {
                            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        loadImage();
                    }

            }

    }

    private void loadImage() {
        try {
            setResize();
            showData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showData(){
        oriScale.setText(bitmap.getHeight() + "*" + bitmap.getWidth());
        imageView.setImageBitmap(bitmap);
        imageView.getLayoutParams().height = ActionBar.LayoutParams.WRAP_CONTENT;
        imageView.getLayoutParams().width = ActionBar.LayoutParams.WRAP_CONTENT;
        oriSize.setText("Original Size: " +(getImageLength(getAbsPath(image_uri)))/1000  + " kb");
    }

    public long getImageLength(String absFileName)
    {
        File file = new File(absFileName);
        return file.length();
    }

    private String getAbsPath(Uri uri)
    {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);

        //managedQuery replacement, must use minSdk 26
    /*    String res = null;
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection,null ,null );
        if(cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);

        }
        cursor.close();
        return res;*/
    }

    private void setResize() throws IOException {
        Bitmap b = BitmapFactory.decodeFile(getAbsPath(image_uri));

        //original size
        int origWidth = b.getWidth();
        int origHeight = b.getHeight();

        if(origWidth > origHeight){
            final int destWidth = 500;

            if(origWidth > destWidth){
                double destHeight;
                destHeight = origHeight/ ((double) origWidth/destWidth);
                Bitmap b2 = Bitmap.createScaledBitmap(b, destWidth, (int) destHeight, false);

                Uri resize_uri = getImageUri(this, b2);

                imageResize.setImageBitmap(b2);
                imageResize.getLayoutParams().height = ActionBar.LayoutParams.WRAP_CONTENT;
                imageResize.getLayoutParams().width = ActionBar.LayoutParams.WRAP_CONTENT;
                resizedSize.setText("Resized Size: " +(getImageLength(getAbsPath(resize_uri)))/1000  + " kb");
                resizedScale.setText(b2.getHeight() + "*" + b2.getWidth());


            }

        } else if (origHeight > origWidth){

            final int destHeight = 500;

            if(origHeight > destHeight) {
                double destWidth = origWidth / ((double) origHeight / destHeight);
                Bitmap b2 = Bitmap.createScaledBitmap(b, (int) destWidth, destHeight,false);

                Uri resize_uri = getImageUri(this, b2);

                imageResize.setImageBitmap(b2);
                imageResize.getLayoutParams().height = ActionBar.LayoutParams.WRAP_CONTENT;
                imageResize.getLayoutParams().width = ActionBar.LayoutParams.WRAP_CONTENT;
                resizedSize.setText("Resized Size: " + (getImageLength(getAbsPath(resize_uri))) / 1000 + " kb");
                resizedScale.setText(b2.getHeight() + "*" + b2.getWidth());
            }
        }

    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

}
