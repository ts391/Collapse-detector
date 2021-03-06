package com.aware.plugin.collapse_detector.UI;


import android.database.Cursor;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.aware.plugin.collapse_detector.AES;
import com.aware.plugin.collapse_detector.CollapseInfo;
import com.aware.plugin.collapse_detector.DatabaseHandler;
import com.aware.plugin.collapse_detector.R;
import com.aware.providers.Locations_Provider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.List;


public class MapScreen extends FragmentActivity {

    GoogleMap googleMap;
    String TAG = "collapse_detector";
    Marker marker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapscreen);

        SupportMapFragment supportMapFragment =(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.googleMap);
        googleMap = supportMapFragment.getMap();
        googleMap.setMyLocationEnabled(true);

        DatabaseHandler db = new DatabaseHandler(this);

//        Criteria criteria = new Criteria();
//
//        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//        String bestProvider = locationManager.getBestProvider(criteria, true);
//        Location location = locationManager.getLastKnownLocation(bestProvider);
//        if (location != null) {
//            onLocationChanged(location);
//        }
//        locationManager.requestLocationUpdates(bestProvider, 20000, 0, this);




//        double latitude = location.getLatitude();
//        double longitude = location.getLongitude();

        //get location data from aware database
        String [] selections = new String[2];
        selections[0] = "double_latitude";
        selections[1] = "double_longitude";
        Cursor gps_data = this.getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI, selections, null, null, "timestamp DESC");
        gps_data.moveToFirst();
        String sLatitude = gps_data.getString(gps_data.getColumnIndex("double_latitude"));
        String sLongitude = gps_data.getString(gps_data.getColumnIndex("double_longitude"));
        Log.d("test", "LOCATION: lat: "+sLatitude+" long: "+sLongitude);
        double latitude = Double.parseDouble(sLatitude);
        double longitude = Double.parseDouble(sLongitude);
        gps_data.close();
        LatLng myLocation = new LatLng(latitude, longitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation,10));

        // testing encryption & decryption
//        String test= "qwertyuiopoasdfghjk";
//        String enc= AES.encrypt(test);
//        Log.d("test", "ENCRYPTED "+enc);
//        String dec = AES.decrypt(enc);
//        Log.d("test", "DECRYPTED "+dec);

        List<CollapseInfo> collapses = db.getAllCollapses();
        for (CollapseInfo ci : collapses) {
            String info = ci.getInfo();
            Long timestamp = ci.getTimestamp();
            SimpleDateFormat sdf = new SimpleDateFormat("MM.dd.yyyy HH:mm ");
            String date = sdf.format(timestamp);

        String collapseDecrypted = AES.decrypt(info);
        InfoParser ip = new InfoParser(collapseDecrypted);

       // InfoParser ip = new InfoParser(info);

        LatLng collapseLocation = ip.getLatLng();

        int count = ip.getCount();
        marker = googleMap.addMarker(new MarkerOptions()
                .position(collapseLocation)
                .title("Building collapse")
                .snippet("Date: " + date +"\nNumber of phones: "+count));

        }
    }

    private class InfoParser{
        String receivedString;
        String removed = "[\"\\[\\],!\\u0000-\\u001f]";
        public void setReceivedString(String receivedString) {
            this.receivedString = receivedString;
        }

        InfoParser(String pString){
            this.receivedString=pString;
            Log.d("MAP SCREEN", "PARSING: "+receivedString);
        }

        String getId(){

            return receivedString.split(" ")[0].replaceAll(removed,"");
        }

        LatLng getLatLng(){


            double receivedLatitude = Double.parseDouble(receivedString.split(" ")[1].replaceAll(removed, ""));
            double receivedLongitude = Double.parseDouble(receivedString.split(" ")[2].replaceAll(removed, ""));
            return new LatLng(receivedLatitude, receivedLongitude);
        }
       int getCount(){
            return Integer.parseInt(receivedString.split(" ")[3].replaceAll(removed, ""));
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }



    private class GeocoderHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            String locationAddress;
            switch (message.what) {
                case 1:
                    Bundle bundle = message.getData();
                    locationAddress = bundle.getString("address");
                    break;
                default:
                    locationAddress = null;
            }
            TextView locationTv = (TextView) findViewById(R.id.latlongLocation);
            locationTv.setText(locationAddress);

        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }


}
