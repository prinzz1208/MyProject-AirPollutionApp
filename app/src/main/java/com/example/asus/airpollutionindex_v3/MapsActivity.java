package com.example.asus.airpollutionindex_v3;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    //constants
    private static final String TAG = "MainActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 10;

    //widgets
    private ImageView mGps, mInfo;
    private EditText mSearchText;

    //vars
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap mMap;
    private Boolean mLocationPermissionGranted = false;
    private String apiKey = "Your Google API Key";
    private Marker mMarker;
    private LatLng geo;
    private Location currentLocation = null;
    String APoI = " ", searchString, APItoken = "Your Weather Token";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        //widget initialization
        isGPSOn();
        mSearchText = findViewById(R.id.input_search);
        mGps = findViewById(R.id.gps);
        mInfo = findViewById(R.id.info);
        getLocationPermission();
        init();
    }


    public class DownloadTask extends AsyncTask<String, String, String> {
        private String result = "", stationName = "";
        private boolean placeFound = true;

        public String extractData(String s) {

            try {
//                Log.d(TAG, "extractData: Before Extracted Data:"+s);

                JSONObject jsonObject = new JSONObject(s);
                if (jsonObject.getString("status").equals("error")) {
                    placeFound = false;
                }
                if (placeFound) {
                    JSONObject data = jsonObject.getJSONObject("data").getJSONObject("iaqi");
                    Iterator<String> jsonIterator = data.keys();

                    while (jsonIterator.hasNext()) {
                        String t = jsonIterator.next();
                        JSONObject innerData = (JSONObject) data.get(t);
                        //                    Log.d(TAG, "extractData: t:"+t);

                        switch (t) {
                            case "pm25":
                                result += "PM25: " + innerData.getString("v") + "\r\n";
                                break;
                            case "pm10":
                                result += "PM10: " + innerData.getString("v") + "\r\n";
                                break;
                            case "o3":
                                result += "O3: " + innerData.getString("v") + "\r\n";
                                break;
                            case "no2":
                                result += "NO2: " + innerData.getString("v") + "\r\n";
                                break;
                            case "so2":
                                result += "SO2: " + innerData.getString("v") + "\r\n";
                                break;
                            case "co":
                                result += "CO: " + innerData.getString("v") + "\r\n";
                                break;
                        }
                    }
                    Log.d(TAG, "extractData: Extracted Data:" + result);
                    return result;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d(TAG, "extractData: Error");
            }
            return null;
        }

        public Double[] extractGEO(String s) {
            try {
                JSONObject jsonObject = new JSONObject(s);
                if (jsonObject.getString("status").equals("error")) {
                    placeFound = false;
                }
                if (placeFound) {
                    JSONObject name = jsonObject.getJSONObject("data").getJSONObject("city");
                    JSONArray data = name.getJSONArray("geo");
                    stationName = name.getString("name");
                    Double[] latlng = new Double[2];
                    for (int i = 0; i < data.length(); i++) {
                        latlng[i] = data.getDouble(i);
                    }
                    return latlng;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected String doInBackground(String... urls) {
            String result = " ";
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                InputStream in = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                while (data != -1) {
                    result += (char) data;
                    data = reader.read();
                }
                Log.d(TAG, "doInBackground: Result:" + result);
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            APoI = extractData(s);
            Double[] ll = extractGEO(s);
            if (placeFound) {
                if (ll != null) {
                    geo = new LatLng(ll[0], ll[1]);
                }
                searchString = mSearchText.getText().toString();
                geoLocate(searchString);
                if (!stationName.equals("")) {
                    moveCamera(geo, DEFAULT_ZOOM, stationName);
                } else {
                    moveCamera(geo, DEFAULT_ZOOM, searchString);
                }
            } else {
                Toast.makeText(MapsActivity.this, "No data for the place", Toast.LENGTH_SHORT).show();
            }
//            mMap.addMarker(new MarkerOptions().position(geo));
//            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(geo,1));
        }
    }

    private void init() {
        Log.d(TAG, "init: initialising");
        isGPSOn();
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
                        || event.getAction() == KeyEvent.ACTION_DOWN
                        || event.getAction() == KeyEvent.KEYCODE_ENTER
                        ) {
                    if (isInternetOn()) {
                        searchString = mSearchText.getText().toString();
                        DownloadTask task = new DownloadTask();
                        try {
                            task.execute("https://api.waqi.info/feed/" + searchString + "/?token=" + APItoken);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "No Internet!!", Toast.LENGTH_SHORT).show();
                    }

                }
                return false;
            }
        });
        hideSoftKeyboard();
        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInternetOn()) {
                    Log.d(TAG, "onClick: clicked gps icon");
                    getDeviceLocation();
                    if (currentLocation != null) {

                        DownloadTask task = new DownloadTask();
                        try {
                            task.execute("https://api.waqi.info/feed/geo:" + currentLocation.getLatitude() + ";" + currentLocation.getLongitude() + "/?token=" + APItoken);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(MapsActivity.this, "Current Location is NULL", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "No Internet!!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInternetOn()) {
                    Log.d(TAG, "onClick: Clicked Place Listener");
                    try {
                        if (mMarker.isInfoWindowShown()) {
                            mMarker.hideInfoWindow();
                        } else {
                            Log.d(TAG, "onClick: place info:" + mMarker.getTitle());
                            mMarker.showInfoWindow();

                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MapsActivity.this, "No Internet!!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void initMap() {

        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }

    private void geoLocate(String searchString) {
        Log.d(TAG, "geoLocate: geolocating..");

        //Download Task

        Geocoder geocoder = new Geocoder(MapsActivity.this);   //
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1); //give list of addresses
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (list.size() > 0) {
            Address address = list.get(0);
            Log.d(TAG, "geoLocate: found a location" + address.toString());
//            moveCamera(new LatLng(address.getLatitude(),address.getLongitude()),DEFAULT_ZOOM,address.getAddressLine(0));
        }
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting device's current location");
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (mLocationPermissionGranted) {
                final Task location = fusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location");
                            currentLocation = (Location) task.getResult();
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My Location");
                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapsActivity.this, "Unable to get the Location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void moveCamera(LatLng latlng, float zoom, String title) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom));
        mMap.clear();
        mMap.setInfoWindowAdapter(new CustomWindowInfoAdapter(MapsActivity.this));
        MarkerOptions options;
        if (!title.equals("My Location")) {
            if (!APoI.equals(" ")) {
                Log.d(TAG, "moveCamera: APoI:" + APoI);
                options = new MarkerOptions().position(latlng).title(title).snippet(APoI);
            } else {
                Log.d(TAG, "moveCamera: APoI:" + APoI);

                options = new MarkerOptions().position(latlng).title(title);
            }
            mMarker = mMap.addMarker(options);
        } else {
            options = new MarkerOptions().position(latlng).title("My Location");
            mMarker = mMap.addMarker(options);
        }

        Log.d(TAG, "moveCamera: moving camera to lat: " + latlng.latitude + " long: " + latlng.longitude);
        hideSoftKeyboard();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mLocationPermissionGranted) {
            getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting");
        String[] permissions = {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        };
        if ((ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION)) == PackageManager.PERMISSION_GRANTED) {
            if ((ContextCompat.checkSelfPermission(this.getApplicationContext(), COARSE_LOCATION)) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_REQUEST_CODE);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: Requestiong Permission Result");
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionGranted = true;
                    //initialize our map
                    initMap();
                }
            }
        }
    }

    private void hideSoftKeyboard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }

    private boolean isInternetOn() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isWifiConn = false;
        boolean isMobileConn = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (Network network : connMgr.getAllNetworks()) {
                NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    isWifiConn |= networkInfo.isConnected();
                }
                if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    isMobileConn |= networkInfo.isConnected();
                }
            }
        } else {
            Toast.makeText(this, "Android Version is lower than required", Toast.LENGTH_SHORT).show();
        }
        return isWifiConn | isMobileConn;
    }
    private void isGPSOn() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
    }
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
}


