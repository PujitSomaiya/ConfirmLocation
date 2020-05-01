package com.pujit.confirmlocation.ui.view;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.pujit.confirmlocation.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private GoogleMap.OnCameraIdleListener onCameraIdleListener;
    private LocationRequest mLocationRequest;
    private Geocoder geocoder;
    private List<Address> addresses;
    private TextView tvCurrentLocation;
    private Context context = this;
    private PlacesClient placesClient;
    private EditText edSearch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        tvCurrentLocation = findViewById(R.id.tvCurrentLocation);
        if (!Places.isInitialized()) {
            Places.initialize(context, getResources().getString(R.string.google_maps_key));
            PlacesClient placesClient = Places.createClient(this);
        }
        placeAutoCompleteListener();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
        configureCameraIdle();

    }

    private void placeAutoCompleteListener() {
        final AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG));
        List<TypeFilter> typeFilters = new ArrayList<>(Arrays.asList(TypeFilter.values()));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                LatLng latLng = place.getLatLng();
                if (latLng != null) {
//                        configureCameraIdle();
                    mMap.clear();
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    tvCurrentLocation.setText("Location:\n" + place.getName());

                    Log.i("TAG", "Place: " + place.getName() + ", " + place.getId());
                } else {
                    Toast.makeText(context, "Latlag is null", Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Toast.makeText(context, String.valueOf(status), Toast.LENGTH_LONG).show();

            }
        });

    }

    //    private void searchField() {
//        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
//            @Override
//            public void onPlaceSelected(Place place) {
//
//            }
//
//            @Override
//            public void onError(Status status) {
//                Log.e("error",String.valueOf(status));
//            }
//        });
//    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(this, "permission denied",
                            Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        this, R.raw.mapstyle));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
                buildGoogleApiClient();
                mMap.setOnCameraIdleListener(onCameraIdleListener);
            }
        } else {
            mMap.setMyLocationEnabled(true);
            buildGoogleApiClient();
        }
    }

    @SuppressLint("MissingPermission")
    private void getAddress() {
        geocoder = new Geocoder(this, Locale.getDefault());
        try {
            LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            boolean network_enabled = locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Location location;
            if (network_enabled) {
                location = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    double longitude = location.getLongitude();
                    double latitude = location.getLatitude();
                    addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    String address = addresses.get(0).getAddressLine(0);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
                    tvCurrentLocation.setText("Location:\n" + address);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            Log.d("gps exception", String.valueOf(ex));
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            Log.d("network exception", String.valueOf(ex));
        }
        if (!network_enabled) {
            new AlertDialog.Builder(context)
                    .setMessage(R.string.internet_not_unabled)
                    .setPositiveButton(R.string.open_location_settings, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            context.startActivity(new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS));
                        }
                    }).setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            }).show();
            if (!gps_enabled) {
                new AlertDialog.Builder(context)
                        .setMessage(R.string.gps_network_not_enabled)
                        .setPositiveButton(R.string.open_location_settings, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                                context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                        }).setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                }).show();

            }
        } else {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
            getAddress();
        }

    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                    mLocationRequest, this);

        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
//        configureCameraIdle();
//        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//        LocationManager locationManager = (LocationManager)
//                getSystemService(Context.LOCATION_SERVICE);
//        String provider = locationManager.getBestProvider(new Criteria(), true);
//        if (ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
//                        != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        Location locations = locationManager.getLastKnownLocation(provider);
//        List<String> providerList = locationManager.getAllProviders();
//        if (null != locations && null != providerList && providerList.size() > 0) {
//            double longitude = locations.getLongitude();
//            double latitude = locations.getLatitude();
//            Geocoder geocoder = new Geocoder(getApplicationContext(),
//                    Locale.getDefault());
//            try {
//                List<Address> listAddresses = geocoder.getFromLocation(latitude,
//                        longitude, 1);
//                if (null != listAddresses && listAddresses.size() > 0) {
//                    String state = listAddresses.get(0).getAdminArea();
//                    String country = listAddresses.get(0).getCountryName();
//                    String subLocality = listAddresses.get(0).getSubLocality();
//
//                    String address=addresses.get(0).getAddressLine(0);
//                    String locality=addresses.get(0).getLocality();
//                    tvCurrentLocation.setText(address);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        if (mGoogleApiClient != null) {
//            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,
//                    this);
//        }
//

    }


    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    private void configureCameraIdle() {
        onCameraIdleListener = new GoogleMap.OnCameraIdleListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onCameraIdle() {
//                if (mCurrLocationMarker != null) {
//                    mCurrLocationMarker.remove();
//                }
                LatLng latLng = mMap.getCameraPosition().target;
                Geocoder geocoder = new Geocoder(MapsActivity.this);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                try {
                    List<Address> addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                    if (addressList != null && addressList.size() > 0) {
                        String address = addressList.get(0).getAddressLine(0);
                        String state = addressList.get(0).getAdminArea();
                        String country = addressList.get(0).getCountryName();
                        String subLocality = addressList.get(0).getSubLocality();
                        String locality = addressList.get(0).getAddressLine(0);
                        markerOptions.title("" + latLng + "," + subLocality + "," + state
                                + "," + country);
                        if (address != null) {
                            if (!address.isEmpty()) {
                                tvCurrentLocation.setText("Location:\n" + address);
                            }
                        } else {
                            Toast.makeText(MapsActivity.this, "No location found", Toast.LENGTH_SHORT).show();
                        }

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        };
    }

}
