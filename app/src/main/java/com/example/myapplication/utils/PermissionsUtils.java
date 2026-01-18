package com.example.myapplication.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionsUtils {
    public static final int REQUEST_CAMERA_PERMISSION = 1;

    public static void requestPermission(Activity context, PermissionListener listener) {
        List<String> permissionsToRequest = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO);
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (permissionsToRequest.isEmpty()) {
            listener.onPermission(true);
        } else {
            ActivityCompat.requestPermissions(context,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_CAMERA_PERMISSION);
            listener.onPermission(false);
        }
    }
}
