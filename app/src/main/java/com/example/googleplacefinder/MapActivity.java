package com.example.googleplacefinder;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;
import com.skyfishjy.library.RippleBackground;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    /*
        All Variables
     */
    private GoogleMap myGMap;
    //fetching the current location from device
    private FusedLocationProviderClient myFusedLocationProviderClient;
    //Suggesting places
    private PlacesClient myPlacesClient;
    private List<AutocompletePrediction> predictionList;

    private Location myLastLocation;
    private LocationCallback myLocationCallback;

    private MaterialSearchBar myMaterialSearchBar;
    private View mapView;
    private Button findButton;
    private RippleBackground rippleBackground;

    private final float DEFAULT_ZOOM = 18;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        myMaterialSearchBar = findViewById(R.id.searchBar);
        findButton = findViewById(R.id.find_near);
        rippleBackground = findViewById(R.id.ripple_marker);


        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_view);
        supportMapFragment.getMapAsync(this);

        mapView = supportMapFragment.getView();

        myFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapActivity.this);
        Places.initialize(MapActivity.this, "AIzaSyCD0oJlRgAgMlUYVwUszQRg6cBUdUE6es0");
        myPlacesClient = Places.createClient(this);
        final AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        myMaterialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {

            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                startSearch(text.toString(), true, null, true);
            }

            @Override
            public void onButtonClicked(int buttonCode) {
                if (buttonCode == MaterialSearchBar.BUTTON_NAVIGATION) {

                }else if(buttonCode == MaterialSearchBar.BUTTON_BACK){
                    myMaterialSearchBar.disableSearch();
                }
            }
        });

        myMaterialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                FindAutocompletePredictionsRequest predictionsRequest = FindAutocompletePredictionsRequest.builder()
                        .setCountry("LK")
                        .setTypeFilter(TypeFilter.ADDRESS)
                        .setSessionToken(token)
                        .setQuery(s.toString())
                        .build();
                myPlacesClient.findAutocompletePredictions(predictionsRequest).addOnCompleteListener(
                        new OnCompleteListener<FindAutocompletePredictionsResponse>() {
                            @Override
                            public void onComplete(@NonNull Task<FindAutocompletePredictionsResponse> task) {
                                if (task.isSuccessful()){
                                    FindAutocompletePredictionsResponse predictionsResponse = task.getResult();
                                    if (predictionsResponse != null){
                                        predictionList = predictionsResponse.getAutocompletePredictions();
                                        List<String> suggestionList = new ArrayList<>();
                                        for (int i = 0; i < predictionList.size() ; i++) {
                                            AutocompletePrediction prediction = predictionList.get(i);
                                            suggestionList.add(prediction.getFullText(null).toString());
                                        }
                                        myMaterialSearchBar.updateLastSuggestions(suggestionList);
                                        if (myMaterialSearchBar.isSuggestionsVisible()){
                                            myMaterialSearchBar.showSuggestionsList();
                                        }
                                    }
                                }else {
                                    Log.d("myTAG", "prediction fetching task unsuccessful");
                                }
                            }
                        }
                );
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        myMaterialSearchBar.setSuggestionsClickListener(new SuggestionsAdapter.OnItemViewClickListener() {
            @Override
            public void OnItemClickListener(int position, View v) {
                if (position >= predictionList.size())
                    return;

                AutocompletePrediction selectedPrediction = predictionList.get(position);
                String suggestion = myMaterialSearchBar.getLastSuggestions().toString();
                myMaterialSearchBar.setText(suggestion);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        myMaterialSearchBar.clearSuggestions();
                    }
                }, 1000);

                myMaterialSearchBar.clearSuggestions();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

                if(imm != null)
                    imm.hideSoftInputFromWindow(myMaterialSearchBar.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);

                String placeid = selectedPrediction.getPlaceId();
                List<Place.Field> placeFields = Arrays.asList(Place.Field.LAT_LNG);

                FetchPlaceRequest fetchPlaceRequest = FetchPlaceRequest.builder(placeid, placeFields).build();
                myPlacesClient.fetchPlace(fetchPlaceRequest).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                    @Override
                    public void onSuccess(FetchPlaceResponse fetchPlaceResponse) {
                        Place place = fetchPlaceResponse.getPlace();
                        Log.i("myTAG", "Place found" + place.getName());

                        LatLng latLngPlace = place.getLatLng();
                        if (latLngPlace != null){
                            myGMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngPlace, DEFAULT_ZOOM));
                        }
                    }

                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof ApiException){
                            ApiException apiException = (ApiException) e;
                            apiException.printStackTrace();
                            int statusCode = apiException.getStatusCode();
                            Log.d("myTAG", "Place not found " + e.getMessage());
                            Log.i("myTAG", "Status Code" + statusCode);
                        }
                    }
                });
            }

            @Override
            public void OnItemDeleteListener(int position, View v) {

            }
        });

        findButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LatLng currentMarkerLocation = myGMap.getCameraPosition().target;
                rippleBackground.startRippleAnimation();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        rippleBackground.stopRippleAnimation();
                        startActivity(new Intent(MapActivity.this, BusStopActivity.class));
                        finish();
                    }
                }, 3000);
            }
        });

    }

    /**
     * This method will ready the map for all functions
     * ( zoom, move , etc. )
     * @param googleMap
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        myGMap = googleMap;
        myGMap.setMyLocationEnabled(true);
        myGMap.getUiSettings().setMyLocationButtonEnabled(true);

        if (mapView != null && mapView.findViewById(Integer.parseInt("1")) != null){
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent())
                    .findViewById(Integer.parseInt("2"));

            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();

            //Removing the default location button from the top and set to bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 40, 180);
        }

        //Check if GPS is enable or not and tell user to allow it
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(MapActivity.this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        /**
         * When GPS is enable then this function will call
         */
        task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                getDeviceLocation();
            }
        });

        /**
         * When GPS is not enable then this function will call
         */
        task.addOnFailureListener(MapActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException){
                    ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                    try {
                        resolvableApiException.startResolutionForResult(MapActivity.this, 51);
                    } catch (IntentSender.SendIntentException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });

        myGMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                if (myMaterialSearchBar.isSuggestionsVisible())
                    myMaterialSearchBar.clearSuggestions();
                if (myMaterialSearchBar.isSearchEnabled())
                    myMaterialSearchBar.disableSearch();
                return false;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 51 && resultCode == RESULT_OK) {
            getDeviceLocation();
        }
    }

    /**
     * This method will fetch the current location of the device
     */
    @SuppressLint("MissingPermission")
    private void getDeviceLocation() {
        myFusedLocationProviderClient.getLastLocation()
                .addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                         if (task.isSuccessful()){
                             myLastLocation = task.getResult();
                             if (myLastLocation != null){
                                 myGMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                         new LatLng(myLastLocation.getLatitude(),
                                                    myLastLocation.getLongitude()),
                                                 DEFAULT_ZOOM));
                             }else {
                                 final LocationRequest locationRequest = LocationRequest.create();
                                 locationRequest.setInterval(10000);
                                 locationRequest.setFastestInterval(5000);
                                 locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                                 myLocationCallback = new LocationCallback(){
                                     @Override
                                     public void onLocationResult(LocationResult locationResult) {
                                         super.onLocationResult(locationResult);
                                         if (locationResult == null)
                                             return;
                                         myLastLocation = locationResult.getLastLocation();
                                         myGMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                                 new LatLng(myLastLocation.getLatitude(),
                                                         myLastLocation.getLongitude()),
                                                 DEFAULT_ZOOM));
                                         myFusedLocationProviderClient.removeLocationUpdates(myLocationCallback);
                                     }
                                 };
                                 myFusedLocationProviderClient.requestLocationUpdates(locationRequest, myLocationCallback, null);
                             }
                         }else {
                             Toast.makeText(MapActivity.this, "unable to get last location", Toast.LENGTH_SHORT).show();
                         }
                    }
                });

    }
}
