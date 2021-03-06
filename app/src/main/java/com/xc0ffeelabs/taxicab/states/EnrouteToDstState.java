package com.xc0ffeelabs.taxicab.states;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.xc0ffeelabs.taxicab.R;
import com.xc0ffeelabs.taxicab.activities.MapsActivity;
import com.xc0ffeelabs.taxicab.fragments.EnrouteToDestFragment;
import com.xc0ffeelabs.taxicab.models.Location;
import com.xc0ffeelabs.taxicab.models.User;
import com.xc0ffeelabs.taxicab.network.GMapV2Direction;

import org.parceler.Parcel;
import org.parceler.Parcels;
import org.w3c.dom.Document;

import java.util.ArrayList;

public class EnrouteToDstState implements State {

    private static final int MSG_REFRESH_LOCATION = 1;
    private static final int REFRESH_INTERVAL = 7;  // in sec
    private static final String TAG = EnrouteToDstState.class.getSimpleName();

    @Parcel
    public static class EnrouteToDstData {

        public static final String ENROUTE_DATA = "enrouteData";

        LatLng mUserLocation;
        String mDriverId;

        public EnrouteToDstData() {
        }

        public EnrouteToDstData(LatLng userLocation,
                                String driverId) {
            mUserLocation = userLocation;
            mDriverId = driverId;
        }
    }

    private MapsActivity mActivity;
    private GoogleMap mMap;
    private EnrouteToDstData mEnrouteData;
    private LatLng mUserLocation;
    private Marker mDstMarker;
    private static EnrouteToDstState mTaxiEnroute;
    private boolean mRefreshRequested;
    private LatLng mDriverLocation;
    private Marker mDriverMarker;
    private User mDriver;
    private Handler mHandler = new MyHandler(Looper.getMainLooper());
    private LatLng mDest = new LatLng(37.4810289, -122.1565179);
    Polyline mLine;

    public static EnrouteToDstState getInstance() {
        if (mTaxiEnroute == null) {
            mTaxiEnroute = new EnrouteToDstState();
        }
        return mTaxiEnroute;
    }

    private EnrouteToDstState() {
    }

    @Override
    public void enterState(MapsActivity activity, Bundle data) {
        mActivity = activity;
        mEnrouteData = Parcels.unwrap(
                data.getParcelable(EnrouteToDstData.ENROUTE_DATA));
        mUserLocation = mEnrouteData.mUserLocation;
        if (mEnrouteData == null) {
            throw new IllegalArgumentException("Why? Why? are you sending null data. I'm tired");
        }
        initialize();
    }

    private void initialize() {
        mMap = mActivity.getMap();
        FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fm_controls, EnrouteToDestFragment.getInstance(), "enroute");
        ft.commit();
        fetchDriverDetails();

        User user = (User) ParseUser.getCurrentUser();
        try {
            Location destLocation = user.getDestLocation();
            if (destLocation != null) {
                destLocation.fetchIfNeeded();
                mDest = new LatLng(destLocation.getLatitude(), destLocation.getLongitude());
            }
            addUserMarker();
        } catch (Exception e) {
            Log.e("NAYAN", "Failed to get latest info!");
        }

    }

    private void fetchDriverDetails() {
        ParseQuery<User> query = ParseQuery.getQuery(User.class);
        query.getInBackground(mEnrouteData.mDriverId, new GetCallback<User>() {
            @Override
            public void done(User object, ParseException e) {
                addDriverMarker(object);
            }
        });
    }

    private void addDriverMarker(User driver) {
        mDriver = driver;
        mDriverLocation = new LatLng(mDriver.getLocation().getLatitude(),
                mDriver.getLocation().getLongitude());
        MarkerOptions markerOptions = new MarkerOptions().title(driver.getName())
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car_pin))
                .position(mDriverLocation);
        mDriverMarker = mMap.addMarker(markerOptions);
        zoomCamera();
    }

    private void zoomCamera() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(mUserLocation);

        /* Dummy end location. Let's go to golden gate bridge. Bright sunny day */
        LatLng endLocation = mDest;
        builder.include(mUserLocation);
        builder.include(endLocation);
        LatLngBounds bounds = builder.build();
        int padding  = (int) mActivity.getResources().getDimension(R.dimen.map_offset);
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.moveCamera(cu);
        showRoute(mUserLocation, endLocation);
        updateDriverLocation();
    }

    private void updateDriverLocation() {
        mRefreshRequested = true;
        mHandler.sendEmptyMessage(MSG_REFRESH_LOCATION);
    }

    private void showRoute(LatLng src, LatLng dest) {
        new GMapV2Direction(src, dest, new GMapV2Direction.RouteReady() {
            @Override
            public void onRouteReady(Document document) {
                drawRoute(document);
            }
        }).execute();
    }

    private void drawRoute(Document doc) {
        if (doc != null) {
            ArrayList<LatLng> directionPoint = GMapV2Direction.getDirection(doc);
            PolylineOptions rectLine = new PolylineOptions().width(10).color(Color.parseColor("#4285f4"));

            for (int i = 0; i < directionPoint.size(); i++) {
                rectLine.add(directionPoint.get(i));
            }
            mLine = mMap.addPolyline(rectLine);
        }
    }

    private void addUserMarker() {
        if (mDstMarker == null) {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(mDest)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.pin_green))
                    .title("Destination");
            mDstMarker = mMap.addMarker(markerOptions);
        } else {
            mDstMarker.setPosition(mDest);
        }
    }

    @Override
    public void exitState() {
        if (mDstMarker != null) mDstMarker.remove();
        if (mLine != null) mLine.remove();
    }

    private class MyHandler extends Handler {

        MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REFRESH_LOCATION:
                    if (mRefreshRequested) {
                        fetchDriverPosition();
                        sendEmptyMessageDelayed(MSG_REFRESH_LOCATION, REFRESH_INTERVAL * 1000);
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("Can't handle");
            }
        }
    }

    private void fetchDriverPosition() {
        mDriver.fetchInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if (e == null) {
                    updateDriverMarker();
                } else {
                    Log.e(TAG, "Unable to update driver location");
                }
            }
        });
    }

    private void updateDriverMarker() {
        mDriverLocation = new LatLng(mDriver.getLocation().getLatitude(),
                mDriver.getLocation().getLongitude());
        mDriverMarker.setPosition(mDriverLocation);
    }

    @Override
    public StateManager.States getState() {
        return StateManager.States.DestEnroute;
    }

    @Override
    public void onTouchUp() {

    }

    @Override
    public void onTouchDown() {

    }
}
