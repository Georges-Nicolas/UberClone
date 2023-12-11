package com.example.myapplication;

import static com.example.myapplication.BuildConfig.MAPS_API_KEY;

import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener {

    private String rideId,currentUserId,customerId,driverId,userDriverOrCustomer;

    private TextView locationRide;
    private TextView distanceRide;
    private TextView dateRide;
    private TextView userName;
    private TextView userPhone;
    private TextView rideCost;
    private TextView mService;

    private RatingBar mRatingBar;

    private ImageView userImage;

    private DatabaseReference historyRideInfoDb;

    private LatLng destinationLatLng,pickupLatLng;

    private String distance;

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);
        polylines = new ArrayList<>();

        rideId = getIntent().getExtras().getString("rideId");

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        locationRide = findViewById(R.id.rideLocation);
        distanceRide = findViewById(R.id.rideDistance);
        dateRide = findViewById(R.id.rideDate);
        userName = findViewById(R.id.userName);
        userPhone = findViewById(R.id.userPhone);
        rideCost = findViewById(R.id.rideCost);
        mService = findViewById(R.id.service);




        mRatingBar = findViewById(R.id.ratingBar);

        userImage = findViewById(R.id.userImage);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        historyRideInfoDb = FirebaseDatabase.getInstance().getReference().child("history").child(rideId);




        getRideInformation();
    }

    private void getRideInformation() {

        historyRideInfoDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    for (DataSnapshot child : snapshot.getChildren()){
                        if (child.getKey().equals("customer")){
                            customerId = child.getValue().toString();
                            if(!customerId.equals(currentUserId)){
                                userDriverOrCustomer = "Drivers";
                                getUerInformation("Customers",customerId);
                            }
                        }
                        if (child.getKey().equals("driver")){
                           driverId = child.getValue().toString();
                            if(!driverId.equals(currentUserId)){
                                userDriverOrCustomer = "Customers";
                                getUerInformation("Drivers",driverId);
                                displayCustomerRealtedObjects();
                            }
                        }
                        if (child.getKey().equals("timestamp")){
                            dateRide.setText(getDate(Long.valueOf(child.getValue().toString())));
                        }
                        if (child.getKey().equals("rating")){
                            mRatingBar.setRating(Integer.valueOf(child.getValue().toString()));
                        }
                        if (child.getKey().equals("distance")){
                            distance = child.getValue().toString();
                            distanceRide.setText(distance.substring(0,Math.min(distance.length(),5))+" km");

                        }
                        if (child.getKey().equals("destination")){
                            locationRide.setText(child.getValue().toString());

                        }
                        if (child.getKey().equals("location")){
                            pickupLatLng = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()),Double.valueOf(child.child("from").child("lng").getValue().toString()));
                            destinationLatLng = new LatLng(Double.valueOf(child.child("to").child("lat").getValue().toString()),Double.valueOf(child.child("to").child("lng").getValue().toString()));
                            if(destinationLatLng != new LatLng(0,0)){
                                getRouteToMarker();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
    private void getRideCost(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        DatabaseReference serviceRef = ref.child("Users").child("Drivers").child(driverId).child("service");
        serviceRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String dataString = dataSnapshot.getValue(String.class);
                            if (dataString.equals("UberX")){
                                mService.setText("Service: UberX");
                                double dividedDistance = Double.parseDouble(distance) / 8;
                                String formattedDistance = String.format("%.4s", dividedDistance);
                                rideCost.setText(formattedDistance + "$");
                            }
                            else if (dataString.equals("UberBlack")){
                                mService.setText("Service: UberBlack");
                                double dividedDistance = Double.parseDouble(distance) / 4;
                                String formattedDistance = String.format("%.4s", dividedDistance);
                                rideCost.setText(formattedDistance + "$");
                            }
                            else if(dataString.equals("UberXl")){
                                mService.setText("Service: UberXl");
                                double dividedDistance = Double.parseDouble(distance) / 2;
                                String formattedDistance = String.format("%.4s", dividedDistance);
                                rideCost.setText(formattedDistance + "$");
                            }
                        }

                    }

        @Override
        public void onCancelled(DatabaseError databaseError) {
        }
    });
    }

    private void displayCustomerRealtedObjects() {
        mRatingBar.setVisibility(View.VISIBLE);
        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                historyRideInfoDb.child("rating").setValue(rating);
                DatabaseReference mDriverRatingDb = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("rating");
                mDriverRatingDb.child(rideId).setValue(rating);
                Toast.makeText(HistorySingleActivity.this, driverId, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getUerInformation(String otherUserDriverOrCustomer, String otherUserID) {

        DatabaseReference mOtherUserDB = FirebaseDatabase.getInstance().getReference().child("Users").child(otherUserDriverOrCustomer).child(otherUserID);
        mOtherUserDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()){
                    Map<String,Object> map = (Map<String,Object>) snapshot.getValue();
                    if(map.get("name") != null){
                        userName.setText(map.get("name").toString());
                    }

                    if(map.get("phone") != null){
                        userPhone.setText(map.get("phone").toString());
                    }

                    if(map.get("profileImageUrl") != null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(userImage);
                    }


                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private String getDate(Long timeStamp) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timeStamp * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault());
        String date = sdf.format(cal.getTime());
        return date;
    }

    private void getRouteToMarker() {
        Routing routing = new Routing.Builder()
                .key(MAPS_API_KEY)
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(pickupLatLng,destinationLatLng)
                .build();
        routing.execute();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
    }
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{com.google.android.material.R.color.primary_dark_material_light};

    @Override
    public void onRoutingFailure(RouteException e) {
        if(e !=null){
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(pickupLatLng)
                .include(destinationLatLng);
        LatLngBounds bounds  = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int padding  = (int) (width*0.2);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

        mMap.animateCamera(cameraUpdate);

        mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("pickup location"));
        mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("destination"));

        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }
        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {
            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;
            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);
            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }
        getRideCost();
    }

    @Override
    public void onRoutingCancelled() {

    }
    private void errasePolyline(){
        for (Polyline line: polylines){
            line.remove();
        }
        polylines.clear();
    }
}