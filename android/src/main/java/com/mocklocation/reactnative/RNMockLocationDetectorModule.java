package com.mocklocation.reactnative;

import android.widget.Toast;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import java.util.Map;
import java.util.HashMap;
import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

public class RNMockLocationDetectorModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    public static Promise promise;

    public RNMockLocationDetectorModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNMockLocationDetector";
    }

    @ReactMethod
    public void checkMockLocationProvider(final Promise promise) {
      this.promise = promise;
        if (ActivityCompat.checkSelfPermission(getCurrentActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(getCurrentActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        FusedLocationProviderClient mFusedLocationClient;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getCurrentActivity());
        mFusedLocationClient.getLastLocation().addOnSuccessListener(getCurrentActivity(),
            new OnSuccessListener<Location> () {
                @Override
                public void onSuccess(Location location) {
                    if (location == null) {
                        RNMockLocationDetectorModule.promise.resolve(false);
                    } else if (areThereMockPermissionApps(getCurrentActivity())) {
                        RNMockLocationDetectorModule.promise.resolve(true);
                    } else if (isLocationFromMockProvider(getCurrentActivity(), location)) {
                        RNMockLocationDetectorModule.promise.resolve(true);
                    } else{
                        RNMockLocationDetectorModule.promise.resolve(false);
                    }
                }
            });
    }

    public boolean isLocationFromMockProvider(Context context, Location location) {
        if (android.os.Build.VERSION.SDK_INT >= 18) {
            if (location.isFromMockProvider()) {
                return true;
            }
        }

        if (Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION).equals("0")) {
            return false;
        }

        return true;
    }

    public boolean areThereMockPermissionApps(Context context) {
        int count = 0;

        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages =
            pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo applicationInfo : packages) {
            try {
                PackageInfo packageInfo = pm.getPackageInfo(applicationInfo.packageName,
                                                            PackageManager.GET_PERMISSIONS);

                // Get Permissions
                String[] requestedPermissions = packageInfo.requestedPermissions;

                if (requestedPermissions != null) {
                    for (int i = 0; i < requestedPermissions.length; i++) {
                        if (requestedPermissions[i]
                            .equals("android.permission.ACCESS_MOCK_LOCATION")
                            && !applicationInfo.packageName.equals(context.getPackageName())) {
                            count++;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("Got exception " , e.getMessage());
            }
        }

        if (count > 0) return true;

        return false;
    }
}