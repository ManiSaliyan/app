package com.example.myserviceapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.location.Location;
import android.location.LocationManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.provider.Settings;



public class MyForegroundService extends Service {
    FusedLocationProviderClient mFusedLocationClient;
    int PERMISSION_ID = 44;
    double lolat,lolon;
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private Handler handler;
    private HandlerThread handlerThread;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("My Service")
                .setContentText("The service is running in the foreground")
                .setSmallIcon(R.drawable.ic_service_icon)
                .build();

        startForeground(1, notification);

        // Start a background thread for tasks
        handlerThread = new HandlerThread("ServiceThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Run a task periodically
        handler.post(new Runnable() {
            @Override
            public void run() {
                getLocation();
                handler.postDelayed(this, 5000); // Re-run every 5 seconds
            }
        });
        return START_STICKY;
    }
 
    private void getLocation(){
          mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if (location == null) {
                            requestNewLocationData();
                        } else {
                            lolat = location.getLatitude();
                            lolon = location.getLongitude();
                            sendLoc(lolat,lolon);
                        }
                    }
                });
    }
    private void sendLoc(double longi,double lat) throws IOException {
		String post_url = "https://w-safety-chi.vercel.app/update/"+lat+"/"+longi;
		URL obj = new URL(post_url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		int responseCode = con.getResponseCode();
		System.out.println("GET Response Code :: " + responseCode);
		if (responseCode == HttpURLConnection.HTTP_OK) { // success
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// print result
			System.out.println(response.toString());
		} else {
			System.out.println("GET request did not work.");
		}

    }
    private void requestNewLocationData() {

        // Initializing LocationRequest
        // object with appropriate methods
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        // setting LocationRequest
        // on FusedLocationClient
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            lolat = mLastLocation.getLatitude();
            lolon = mLastLocation.getLongitude();
            sendLoc(lolat,lolon);
        }
    };
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handlerThread != null) {
            handlerThread.quit();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // This is not a bound service, so return null
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
