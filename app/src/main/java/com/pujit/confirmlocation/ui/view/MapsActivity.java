package com.pujit.confirmlocation.ui.view;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
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
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.pujit.confirmlocation.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, View.OnClickListener {
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private GoogleMap.OnCameraIdleListener onCameraIdleListener;
    private LocationRequest mLocationRequest;
    private Geocoder geocoder;
    private List<Address> addresses;
    private TextView tvCurrentLocation;
    private Context context = this;
    private CheckBox chkFav;
    private Intent searchIntent;
    private MaterialAutoCompleteTextView svMaps;
    private SupportMapFragment mapFragment;
    private PlaceAutocompleteAdapter placeAutocompleteAdapter;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40, -168), new LatLng(71, 136));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        initControls();

    }

    private void initControls() {
        initListeners();
        mapFragment = getSupportMapFragment();
        mapFragment.getMapAsync(this);
//        placeAutoCompleteListener();
        changeMyLocationButton(mapFragment);
        configureCameraIdle();
//        searchView();
    }

//    private void searchView() {
//        svMaps.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @SuppressLint("SetTextI18n")
//            @Override
//            public boolean onQueryTextSubmit(String s) {
//                String location=svMaps.getQuery().toString();
//                addresses=null;
//                if (!location.equals("")){
//                    geocoder=new Geocoder(MapsActivity.this);
//                    try {
//                        addresses=geocoder.getFromLocationName(location,1);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    Address address=addresses.get(0);
//                    LatLng latLng=new LatLng(address.getLatitude(),address.getLongitude());
//                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
//                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
//                    tvCurrentLocation.setText("Location:\n" + address.getAddressLine(0));
////                    Log.i("TAG", "Place: " + place.getName() + ", " + place.getId());
//                }
//
//                return false;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String s) {
//                return false;
//            }
//        });
//        mapFragment.getMapAsync(this);
//    }

    private SupportMapFragment getSupportMapFragment() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        return (SupportMapFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.map);
    }

    private void changeMyLocationButton(SupportMapFragment mapFragment) {
        @SuppressLint("ResourceType") View locationButton = mapFragment.getView().findViewById(2);
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
    }

    private void initListeners() {
        tvCurrentLocation = findViewById(R.id.tvCurrentLocation);
        chkFav = findViewById(R.id.chkFav);
        svMaps = findViewById(R.id.svMaps);
        placeAutocompleteAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, LAT_LNG_BOUNDS, null);
        svMaps.setAdapter(placeAutocompleteAdapter);
        svMaps.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER) {

                    //execute our method for searching
                    geoLocate();
                }

                return false;
            }
        });

    }

    @SuppressLint("SetTextI18n")
    private void geoLocate() {
        String searchString = svMaps.getText().toString();

        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            Log.e("TAG", "geoLocate: IOException: " + e.getMessage());
        }

        if (list.size() > 0) {
            Address address = list.get(0);

            Log.d("TAG", "geoLocate: found a location: " + address.toString());
            //Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();

            LatLng latLng = new LatLng(list.get(0).getLatitude(), list.get(0).getLongitude());
            mMap.clear();
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            tvCurrentLocation.setText("Location:\n" + list.get(0).getAddressLine(0));

        }
    }

//    private void placeAutoCompleteListener() {
//        Places.initialize(context, getResources().getString(R.string.google_maps_key));
//        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
//        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG));
//        autocompleteFragment.setTypeFilter(TypeFilter.CITIES);
////        List<Place.Field> fields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
////        searchIntent = new Autocomplete.IntentBuilder(
////                AutocompleteActivityMode.OVERLAY, fields)
////                .build(this);
////        startActivityForResult(searchIntent, 11);
////        cvSearchBarl.setOnClickListener(this);
//        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
//            @Override
//            public void onPlaceSelected(@NonNull Place place) {
//                LatLng latLng = place.getLatLng();
//                if (latLng != null) {
//                    configureCameraIdle();
//                    mMap.clear();
//                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
//                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
//                    if (chkFav.isChecked()) {
//                        String savedLocation = place.getId();
//                    }
//                    tvCurrentLocation.setText("Location:\n" + place.getName());
//                    Log.i("TAG", "Place: " + place.getName() + ", " + place.getId());
//                } else {
//                    Toast.makeText(context, "Latlag is null", Toast.LENGTH_LONG).show();
//                }
//
//            }
//
//            @Override
//            public void onError(Status status) {
//                // TODO: Handle the error.
//                Toast.makeText(context, String.valueOf(status), Toast.LENGTH_LONG).show();
//
//            }
//        });
//
//    }

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
        if (requestCode == 11) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                LatLng latLng = place.getLatLng();
                if (latLng != null) {
                    mMap.clear();
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

                } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                    // TODO: Handle the error.
                    Status status = Autocomplete.getStatusFromIntent(data);
                    Log.i("TAG", status.getStatusMessage());
                } else if (resultCode == RESULT_CANCELED) {
                    // The user canceled the operation.
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
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
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setPadding(0, 0, 0, 0);
        mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        this, R.raw.mapstyle));
//        mMap.getUiSettings().setZoomControlsEnabled(true);
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
                    .enableAutoManage(this, this)
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
            @SuppressLint({"MissingPermission", "SetTextI18n"})
            @Override
            public void onCameraIdle() {
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

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.cvSearchBarl) {
            startActivityForResult(searchIntent, 11);
        }
    }
}
