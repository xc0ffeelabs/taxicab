package com.xc0ffeelabs.taxicab.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.xc0ffeelabs.taxicab.R;
import com.xc0ffeelabs.taxicab.activities.TaxiCabApplication;

public class MapsFragment extends Fragment implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MapsFragment";

    public interface MapReady {
        void onMapReady(GoogleMap map, GoogleApiClient apiClient);
    }

    private MapReady mListener;

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 1;

    private TouchableMapFragment mMapFragment;
    private GoogleMap mMap;
    private GoogleApiClient mApiClient;
    private OnTouchEvent mTouchEventListener;

    public MapsFragment() {
    }

    public static MapsFragment newInstance() {
        MapsFragment fragment = new MapsFragment();
        return fragment;
    }

    public void setMapReadyListener(MapReady listener) {
        mListener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMapFragment = (TouchableMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        mMapFragment.setOnTouchListener(new TouchableMapFragment.OnTouchEvent() {
            @Override
            public void onMapTouchDown() {
                mTouchEventListener.onMapTouchDown();
            }

            @Override
            public void onMapTouchUp() {
                mTouchEventListener.onMapTouchUp();
            }
        });

        setupMap();
    }

    private void setupMap() {
        if (mMapFragment != null) {
            mMapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    loadMap(googleMap);
                }
            });
        } else {
            Toast.makeText(getActivity(), "Error: map fragment is null", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMap(GoogleMap googleMap) {
        mMap = googleMap;
        if (mMap == null) {
            Toast.makeText(getActivity(), "Map failed to load!", Toast.LENGTH_SHORT).show();
            return;
        } else {
            getMyLocation();
        }
    }

    private void getMyLocation() {
        try {
            mMap.setMyLocationEnabled(true);
            setupApiClient();
        } catch (SecurityException e) {
            throw e;
        }
    }

    private void setupApiClient() {
        if (mApiClient == null) {
            mApiClient = new GoogleApiClient.Builder(getActivity())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            connectClient();
        }
    }

    private void connectClient() {
        if (mApiClient != null && isGooglePlayServicesAvailable()) {
            mApiClient.connect();
        }
    }

    private void disconnectClient() {
        if (mApiClient != null) {
            mApiClient.disconnect();
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int result = apiAvailability.isGooglePlayServicesAvailable(getActivity());
        if (result != ConnectionResult.SUCCESS) {
            Dialog errorDialog = apiAvailability.getErrorDialog(getActivity(), result,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);
            errorDialog.show();
            return false;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    connectClient();
                    break;
                }
        }
    }

    @Override
    public void onStart() {
        connectClient();
        super.onStart();
    }

    @Override
    public void onStop() {
        disconnectClient();
        TaxiCabApplication.getNearbyDrivers().stopQueryDriverLocationUpdates();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mListener != null) {
            mListener.onMapReady(mMap, mApiClient);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (i == CAUSE_SERVICE_DISCONNECTED) {
            Toast.makeText(getContext(), "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
        } else if (i == CAUSE_NETWORK_LOST) {
            Toast.makeText(getContext(), "Network lost. Please re-connect.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(getActivity(),
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getContext(),
                    "Sorry. Location services not available to you", Toast.LENGTH_LONG).show();
        }
    }

    public interface OnTouchEvent {
        void onMapTouchDown();
        void onMapTouchUp();
    }

    public void setOnTouchListener(OnTouchEvent listener) {
        mTouchEventListener = listener;
    }
}
