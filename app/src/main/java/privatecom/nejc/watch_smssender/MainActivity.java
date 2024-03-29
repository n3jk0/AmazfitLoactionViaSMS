package privatecom.nejc.watch_smssender;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import privatecom.nejc.watch_smssender.domain.MyLocation;

public class MainActivity extends AppCompatActivity {
    TextView statusBar;
    LocationManager locationManager;
    Location lastLocation;
    volatile boolean locationChanged = false;

    private static final Map<String, MyLocation> LOCATION_MAP = new HashMap<>();
    private static final Map<String, String> ADRESS_NUMBERS = new HashMap<>();

    static {
//        TODO: add some from Ljubljana - Novo mesto
        LOCATION_MAP.put("Maribor", new MyLocation(46.554649, 15.645881));
        LOCATION_MAP.put("Fram", new MyLocation(46.453872, 15.638675));
        LOCATION_MAP.put("Slovenska Bistrica", new MyLocation(46.391991, 15.572810));
        LOCATION_MAP.put("Slovenske Konjice", new MyLocation(46.337307, 15.422497));
        LOCATION_MAP.put("Celje", new MyLocation(46.231022, 15.260290));
        LOCATION_MAP.put("Vransko", new MyLocation(46.245659, 14.952250));
        LOCATION_MAP.put("Trojane", new MyLocation(46.188221, 14.886122));
        LOCATION_MAP.put("Lukovica", new MyLocation(46.168750, 14.691093));
        LOCATION_MAP.put("Domžale", new MyLocation(46.137878, 14.593840));
        LOCATION_MAP.put("Ljubljana", new MyLocation(46.056946, 14.505752));

//        Maybe with +386 ...
        ADRESS_NUMBERS.put("Ljubezen moja", "041692228");
        ADRESS_NUMBERS.put("Nejc", "070701664");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        statusBar = findViewById(R.id.statusBar);
        setContentView(R.layout.activity_main);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    public void getLocationAndSendSMS(View view) {
        locationChanged = false;
        updateStatusBar(getString(R.string.statusUpdatingLocation));
        updateLocation();
        new WaitForLocation().execute();
    }

    public void updateLocation() {
        MyLocationListener locationListener = new MyLocationListener();

        if (isLocationEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                System.out.println("No permission!");
                return;
            }
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000 * 10, 10, locationListener);
        } else {
            updateStatusBar("GPS not available");
        }
    }

    private void updateStatusBar(String status) {
        if (statusBar == null) {
            statusBar = findViewById(R.id.statusBar);
        }
        System.out.println(status);
        statusBar.setText(status);
    }

    private String printLocation(Location location) {
        if (location == null) {
            return "";
        }
        String nearestCity = findNearestCity(location);
        return String.format("(long: %.3f lat: %.3f)\n", location.getLongitude(), location.getLatitude(), nearestCity);
    }

    private String findNearestCity(Location location) {
        double currLat = location.getLatitude();
        double currLong = location.getLongitude();
        String nearestCity = "";
        double minDistance = Double.MAX_VALUE;
        for (Map.Entry<String, MyLocation> entry : LOCATION_MAP.entrySet()) {
            double latDist = Math.abs(currLat - entry.getValue().getLatitude());
            double longDist = Math.abs(currLong - entry.getValue().getLongitude());

            double currDistance = Math.hypot(latDist, longDist);
            if (currDistance < minDistance) {
                nearestCity = entry.getKey();
                minDistance = currDistance;
            }
        }
        return nearestCity;
    }

    private boolean isLocationEnabled() {
        System.out.println("GPS: " + locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private class WaitForLocation extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            waitForLocation();
            return null;
        }

        private void waitForLocation() {
            long start = System.currentTimeMillis();
            long curr = System.currentTimeMillis();
            // Max wait 1min
            while (!locationChanged && (curr - start) < 60000) {
                curr = System.currentTimeMillis();
            }
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!locationChanged) {
                        updateStatusBar(getString(R.string.locationNotFound));
                        return;
                    }
                    updateStatusBar("Final location: \n" + printLocation(lastLocation));
                }
            });
        }
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if (location.getAccuracy() < 10) {
                lastLocation = location;
                locationChanged = true;
                locationManager.removeUpdates(this);
            } else {
                updateStatusBar(String.format("Not good enough: %.3f", location.getAccuracy()));
            }

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }
}
