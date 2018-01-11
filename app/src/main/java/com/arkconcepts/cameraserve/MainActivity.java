package com.arkconcepts.cameraserve;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


public class MainActivity extends FragmentActivity implements SurfaceHolder.Callback, Camera.PreviewCallback,
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener
        , TCPListener{

    private SurfaceHolder holder;
    private Camera camera;
    private boolean previewRunning = false;
    private int camId = 0;
    private ByteArrayOutputStream previewStream = new ByteArrayOutputStream();
    private int rotationSteps = 0;
    private boolean aboveLockScreen = true;

    private static SsdpAdvertiser ssdpAdvertiser = new SsdpAdvertiser();
    private static Thread ssdpThread = new Thread(ssdpAdvertiser);
    private static MjpegServer mjpegServer = new MjpegServer();
    private static Thread serverThread = new Thread(mjpegServer);
    private static HashMap<Integer, List<Camera.Size>> cameraSizes = new HashMap<>();
    private static ReentrantReadWriteLock frameLock = new ReentrantReadWriteLock();
    private static byte[] jpegFrame;

    GoogleApiClient myGoogleAPIClient;
    Location myLastLocation;
    Marker myCurrentLocation;
    LocationRequest myLocationRequest;
    private GoogleMap myMap;

    private TextView mTitle;
    private String informatiiServer;
    public String data[] = new String[2];

    public static byte[] getJpegFrame() {
        try {
            frameLock.readLock().lock();
            return jpegFrame;
        } finally {
            frameLock.readLock().unlock();
        }
    }

    public static HashMap<Integer, List<Camera.Size>> getCameraSizes() {
        return cameraSizes;
    }

    private static void setJpegFrame(ByteArrayOutputStream stream) {
        try {
            frameLock.writeLock().lock();
            jpegFrame = stream.toByteArray();
        } finally {
            frameLock.writeLock().unlock();
        }
    }

    private void cacheResolutions() {
        int cams = Camera.getNumberOfCameras();
        for (int i = 0; i < cams; i++) {
            Camera cam = Camera.open(i);
            Camera.Parameters params = cam.getParameters();
            cameraSizes.put(i, params.getSupportedPreviewSizes());
            cam.release();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        cacheResolutions();

        FloatingActionButton flipButton = (FloatingActionButton) findViewById(R.id.flipButton);
        flipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int cams = Camera.getNumberOfCameras();
                camId++;
                if (camId > cams - 1) camId = 0;
                if (previewRunning) stopPreview();
                if (camera != null) camera.release();
                camera = null;

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                preferences.edit().putString("cam", String.valueOf(camId)).apply();

                openCamAndPreview();

                Toast.makeText(MainActivity.this, "Cam " + (camId + 1),
                        Toast.LENGTH_SHORT).show();
            }
        });

        FloatingActionButton settingsButton = (FloatingActionButton) findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        SurfaceView cameraView = (SurfaceView) findViewById(R.id.surfaceView);
        holder = cameraView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //mTitle = (TextView) findViewById(R.id.titlu);
        TcpServiceHandler handler = new TcpServiceHandler(this,this);
        Thread th = new Thread(handler);
        th.start();
        Log.d("MAIN ACTIVITY", "AJUNGE AICI????!!!!???");



    }

    public String[] callCompleted(String source){
        Log.d("TCP", "Std parser " + source);
        informatiiServer = source;
        //mTitle.setText(source);
        //String data[] = new String[2];

        //if (source.matches("<MSG><N>.*</N><V>.*</V></MSG>")) {
        Document doc = null;
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = (Document) db.parse(new ByteArrayInputStream(source.getBytes()));
            NodeList n = doc.getElementsByTagName("N");
            Node nd = n.item(0);
            String msgName = nd.getFirstChild().getNodeValue();
            NodeList n1 = doc.getElementsByTagName("V");
            Node nd1 = n1.item(0);
            String tmpVal = nd1.getFirstChild().getNodeValue();
            data[0] = msgName;
            data[1] = tmpVal;
            Log.d("TCP", "Inside Std parser " + data[0] + " " + data[1]);
            actionOnData(data[0], data[1]);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        Log.d("TCP", "Just outside Std parser " + data[0] + " " + data[1]);
        return data;
        //} else Log.d("TCP", "Message in wrong format " + source);
        //mTitle.setText("Message in wrong format " + source);
        //return data;
    }

    public void actionOnData(String name, String value) {
        String tempName = name;
        String tempVal = value;
        //while (true) {
        if(tempName.equals("shiftDirection") && tempVal.equals("1")) {
            Log.d("TCP","in actionOnData " + data[0] + " " + data[1]);
            //mTitle.setText("Change to next higher gear");
            Intent myIntent = new Intent();
            myIntent.setClassName("com.example.android.TCPListen", "com.example.android.TCPListen.Images");
            //myIntent.putExtra("Change gear", "Shift to next gear!"); // key/value pair, where key needs current package prefix.
            startActivity(myIntent);
            try {
                wait(3000);
            } catch(InterruptedException e) {
                System.out.println("InterruptedException caught");
            }
        } else if(tempName.equals("vehicleSpeed") && tempVal.equals("120")) {
            Log.d("TCP","in actionOnData " + data[0] + " " + data[1]);
           //mTitle.setText("Drive like a man");
            //Intent myIntent = new Intent();
            //myIntent.setClassName("com.example.android.TCPListen", "com.example.android.TCPListen.Images");
            ////myIntent.putExtra("Change gear", "Shift to next gear!"); // key/value pair, where key needs current package prefix.
            //startActivity(myIntent);
        } else Log.d("TCP", "Just show an image");

        //}

    }

    @Override
    protected void onResume() {
        super.onResume();

        loadPreferences();

        openCamAndPreview();

        if (!ssdpThread.isAlive()) ssdpThread.start();
        if (!serverThread.isAlive()) serverThread.start();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        if (aboveLockScreen)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        this.finish();
        System.exit(0);
    }

    private void openCamAndPreview() {
        try {
            if (camera == null) camera = Camera.open(camId);
            startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        camId = Integer.parseInt(preferences.getString("cam", "0"));
        Integer rotDegrees = Integer.parseInt(preferences.getString("rotation", "0"));
        rotationSteps = rotDegrees / 90;
        Integer port = Integer.parseInt(preferences.getString("port", "8080"));
        MjpegServer.setPort(port);
        aboveLockScreen = preferences.getBoolean("above_lock_screen", aboveLockScreen);
        Boolean allIps = preferences.getBoolean("allow_all_ips", false);
        MjpegServer.setAllIpsAllowed(allIps);
    }

    private void startPreview() {
        if (previewRunning) stopPreview();

        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

        if (display.getRotation() == Surface.ROTATION_0) {
            camera.setDisplayOrientation(90);
        } else if (display.getRotation() == Surface.ROTATION_270) {
            camera.setDisplayOrientation(180);
        } else {
            camera.setDisplayOrientation(0);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String res = preferences.getString("resolution", "640x480");
        String[] resParts = res.split("x");

        Camera.Parameters p = camera.getParameters();
        p.setPreviewSize(Integer.parseInt(resParts[0]), Integer.parseInt(resParts[1]));
        camera.setParameters(p);

        try {
            camera.setPreviewDisplay(holder);
            camera.setPreviewCallback(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        camera.startPreview();

        holder.addCallback(this);

        previewRunning = true;
    }

    private void stopPreview() {
        if (!previewRunning) return;

        holder.removeCallback(this);
        camera.stopPreview();
        camera.setPreviewCallback(null);

        previewRunning = false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        stopPreview();
        if (camera != null) camera.release();
        camera = null;

        openCamAndPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        openCamAndPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        stopPreview();
        if (camera != null) camera.release();
        camera = null;
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        previewStream.reset();
        Camera.Parameters p = camera.getParameters();

        int previewHeight = p.getPreviewSize().height,
            previewWidth = p.getPreviewSize().width;

        switch(rotationSteps) {
            case 1:
                bytes = Rotator.rotateYUV420Degree90(bytes, previewWidth, previewHeight);
                break;
            case 2:
                bytes = Rotator.rotateYUV420Degree180(bytes, previewWidth, previewHeight);
                break;
            case 3:
                bytes = Rotator.rotateYUV420Degree270(bytes, previewWidth, previewHeight);
                break;
        }

        if (rotationSteps == 1 || rotationSteps == 3) {
            int tmp = previewHeight;
            previewHeight = previewWidth;
            previewWidth = tmp;
        }

        int format = p.getPreviewFormat();
        new YuvImage(bytes, format, previewWidth, previewHeight, null)
                .compressToJpeg(new Rect(0, 0, previewWidth, previewHeight),
                        100, previewStream);

        setJpegFrame(previewStream);
    }

    @Override
    public void onLocationChanged(Location location) {
        myLastLocation = location;
        if (myCurrentLocation != null) {
            myCurrentLocation.remove();
        }


        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        //Am folosit pt a testa, nu am semnal gps in casa
        //LatLng latLng = new LatLng(44,26);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);


        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String provider = locationManager.getBestProvider(new Criteria(), true);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location locations = locationManager.getLastKnownLocation(provider);
        List<String> providerList = locationManager.getAllProviders();
        if (null != locations && null != providerList && providerList.size() > 0) {
            double longitude = locations.getLongitude();
            double latitude = locations.getLatitude();
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            try {
                List<android.location.Address> listAddresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (null != listAddresses && listAddresses.size() > 0) {

                    String state = listAddresses.get(0).getAdminArea();
                    String country = listAddresses.get(0).getCountryName();
                    String subLocality = listAddresses.get(0).getSubLocality();
                    markerOptions.title("" + latLng + "," + subLocality + "," + state + "," + country);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        myCurrentLocation = myMap.addMarker(markerOptions);

        myMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        myMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        if (myGoogleAPIClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(myGoogleAPIClient, this);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        myLocationRequest = new LocationRequest();
        myLocationRequest.setInterval(1000);
        myLocationRequest.setFastestInterval(1000);
        myLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(myGoogleAPIClient, myLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        while(myGoogleAPIClient.isConnected() == false){
        String[] informatie = informatiiServer.split(" ");
        Double indiceStanga = Double.valueOf(informatie[0]);
        Double indiceDreapta = Double.valueOf(informatie[1]);
        if(indiceStanga != 0 || indiceDreapta != 0) {
            if (indiceDreapta - indiceStanga > 0.1) {
                myCurrentLocation.setRotation((float) (indiceDreapta - indiceStanga));
            }
            if (indiceStanga - indiceDreapta > 0.1) {
                myCurrentLocation.setRotation((float) (indiceStanga - indiceDreapta));
            }
            LatLng latLng = new LatLng(myLastLocation.getLatitude(), myLastLocation.getLongitude());
            LatLng newLatLng = new LatLng(latLng.latitude + 10*indiceStanga, latLng.longitude + 10*indiceDreapta);
            myCurrentLocation.setPosition(newLatLng);
        }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        myMap = googleMap;
        myMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        myMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient() {

        myGoogleAPIClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        myGoogleAPIClient.connect();
    }
}
