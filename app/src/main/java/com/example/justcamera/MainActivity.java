package com.example.justcamera;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    public static final  String APP_TAG = MainActivity.class.getSimpleName();
    private Disposable disposable;

    Button btnTakePhoto;
    ImageView imageViewPhoto ;

    String  cameraFilePath ;
    String image_name_on_phone;

    public String getImage_name_on_phone() {
        return image_name_on_phone;
    }

    public void setImage_name_on_phone(String image_name_on_phone) {

        String[] x = image_name_on_phone.split("/");

        this.image_name_on_phone = x[(x.length - 1)];
    }

    private View.OnClickListener mCaptureImageButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                startImageCapture();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final RxPermissions rxPermissions = new RxPermissions(MainActivity.this);
        rxPermissions.setLogging(true);
        setContentView(R.layout.activity_main);
        btnTakePhoto = findViewById(R.id.btn_take_img);
        imageViewPhoto =findViewById(R.id.iv_img);

        disposable = RxView.clicks(this.findViewById(R.id.btn_take_img))
                .compose(rxPermissions.ensureEach(Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE))
                .subscribe(new Consumer<Permission>() {

                               @Override
                               public void accept(Permission permission) throws Exception {
                                   Timber.i(APP_TAG, "Permission result%s ", permission);
                                   if (permission.granted) {
                                       btnTakePhoto.setOnClickListener(mCaptureImageButtonClickListener);
                                   } else if (permission.shouldShowRequestPermissionRationale) {
                                       // Denied permission without ask never again
                                       Toast.makeText(MainActivity.this,
                                               "Denied permission without ask never again",
                                               Toast.LENGTH_SHORT).show();
                                   } else {
                                       // Denied permission with ask never again
                                       // Need to go to the settings
                                       Toast.makeText(MainActivity.this,
                                               "Permission denied, can't enable the camera",
                                               Toast.LENGTH_SHORT).show();
                                   }
                               }
                           }, new Consumer<Throwable>() {
                               @Override
                               public void accept(Throwable t) {
                                   Timber.e(t, APP_TAG, "onError%s");
                               }
                           },
                        new Action() {
                            @Override
                            public void run() {
                                Timber.i(APP_TAG, "OnComplete");
                            }
                        });



    }

    private void startImageCapture() throws IOException {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", createImageFile()));
        startActivityForResult(intent, 1);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String imageFileName = "image_" + timeStamp;
        //This is the directory in which the file will be created. This is the default location of Camera photos
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".png",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for using again
        cameraFilePath = "file://" + image.getAbsolutePath();

        setImage_name_on_phone(cameraFilePath);

        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK){
            if (requestCode == 1) {
                File f = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM), "Camera/" + getImage_name_on_phone());
                try {
                    Bitmap bitmap;
                    BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                    bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(),
                            bitmapOptions);
                    imageViewPhoto.setImageBitmap(bitmap);
                    OutputStream outFile = null;
                    try {
                        outFile = new FileOutputStream(f);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 85, outFile);
                        outFile.flush();
                        outFile.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Timber.d("Error");
            }

        }
    }
}
