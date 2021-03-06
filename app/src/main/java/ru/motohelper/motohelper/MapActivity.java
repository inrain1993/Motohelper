package ru.motohelper.motohelper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.SeekBar;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import ru.motohelper.motohelper.Fragments.FragmentSettings;

import static android.R.attr.x;


public class MapActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener, OnMapReadyCallback, ServerUtilityGetMarkers.OnRefreshed {

    MapFragment mapFragment;
    GoogleMap mMap;

    int markerRefreshTimeout;

    Map<String, MyMarker> markersCollection = new HashMap<String, MyMarker>();
    EditText markerShortDescription;
    EditText markerLongDescription;

    FragmentSettings fragmentSettings;

    TextView navUserLogin;
    TextView navUserFamNam;
    TextView textViewMilSecondsDisplay;
    TextView markerOwner;

    EditText shortDesc;
    EditText longDesc;
    EditText phone;


    ImageView markerImage;

    Button btnDial;
    Button btnSMS;
    Button btnDeleteMarker;
    Button btnSubmitMapMarkerSettings;
    Button btnDiscardMapMarkerSettings;
    Button btnFilterSave;
    Button btnFilterCancel;

    RadioButton markerTypeCorrupt;
    RadioButton markerTypeLookFriends;
    RadioButton markerTypeAccident;

    CheckBox checkBoxAllowAutoRefresh;
    CheckBox checkBoxShowZoomButtons;
    CheckBox checkBoxShowCompass;
    CheckBox checkBoxShowMyLocation;
    CheckBox checkBoxOnlyMyMarkers;
    CheckBox checkBoxAccidentMarkers;
    CheckBox checkBoxLookFriendsMarkers;
    CheckBox checkBoxCorruptMarkers;

    SeekBar seekBarMilliSeconds;

    Button buttonCreateMarker;
    Button buttonCancelCreateMarker;

    Dialog modalAddMarker;
    Dialog modalOnView;
    Dialog modalMapAndMarkersSetting;
    Dialog modalFilteringSettings;

    LatLng positionToAddMarker;

    SettingsHolder appSettings;

    LocationManager locationManager;


    MyMarker selectedMarker;

    User currentUser;

    ServerUtilityGetMarkers getMarkersAuto;

    boolean autoRefresh;

    Timer timer;
    TimerTask doAsynchronousTask;
    ArrayList<MyMarker> oldState;
    Location currentLocation;
    Location lastKnownLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        initializeMap();
        appSettings = new SettingsHolder(this);
        currentUser = appSettings.getCurrentUser();
        prepareToolbar();
        fragmentSettings = new FragmentSettings();
        autoRefresh = appSettings.getGetAllowAutoRefreshSetting();

        try {
            markerRefreshTimeout = Integer.parseInt(appSettings.getRefreshTimeout());
        } catch (Exception e) {
            markerRefreshTimeout = 20 * 1000;
        }

        /**
         *
         * Слушатель GPS
         *
         */
        LocationListener listener = new LocationListener() {
            public void onLocationChanged(Location argLocation) {
                currentLocation = argLocation;
            }

            @Override
            public void onProviderDisabled(String provider) {
                currentLocation = null;
            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }
        };

        /**
         *
         * Слушатель интернет
         *
         */

        LocationListener listenerInternet = new LocationListener() {
            public void onLocationChanged(Location argLocation) {
                currentLocation = argLocation;
            }

            @Override
            public void onProviderDisabled(String provider) {
                currentLocation = null;
            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }
        };


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
                0, listener);
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 0, 0, listenerInternet);
        lastKnownLocation = locationManager
                .getLastKnownLocation(LocationManager.GPS_PROVIDER);


    }


    private void initializeMap() {
        mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);
    }


    @Override
    // Set up Map behavour
    public void onMapReady(GoogleMap map) {
        map.setIndoorEnabled(true);
        map.setBuildingsEnabled(true);
        reloadMap(map);
        map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                return false;
            }
        });
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                onCreateModalToAddMarker();
                positionToAddMarker = latLng;
            }
        });
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return false;
            }
        });
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                onCreateModalToViewMarker(marker);
                return false;
            }
        });


        mMap = map;

        getMarkersByTimer();

    }

    void onCreateModalFilteringSettings() {
        modalFilteringSettings = new Dialog(MapActivity.this);
        modalFilteringSettings.setContentView(R.layout.modal_filtering_settings);

        btnFilterSave = (Button) modalFilteringSettings.findViewById(R.id.button_save_filters);
        btnFilterCancel = (Button) modalFilteringSettings.findViewById(R.id.button_discard_filters);

        checkBoxOnlyMyMarkers = (CheckBox) modalFilteringSettings.findViewById(R.id.checkBoxOnlyMyMarkers);
        checkBoxAccidentMarkers = (CheckBox) modalFilteringSettings.findViewById(R.id.checkBoxFilterAccident);
        checkBoxCorruptMarkers = (CheckBox) modalFilteringSettings.findViewById(R.id.checkBoxFilterCorrupt);
        checkBoxLookFriendsMarkers = (CheckBox) modalFilteringSettings.findViewById(R.id.checkBoxFilterLookFriends);

        checkBoxOnlyMyMarkers.setChecked(appSettings.getShowOnlyMyMarkers());
        checkBoxAccidentMarkers.setChecked(appSettings.getShowOnlyAccidents());
        checkBoxCorruptMarkers.setChecked(appSettings.getShowOnlyCorrupts());
        checkBoxLookFriendsMarkers.setChecked(appSettings.getShowOnlyLookFriends());

        btnFilterSave.setOnClickListener(this);
        btnFilterCancel.setOnClickListener(this);

        modalFilteringSettings.show();
    }

    void onCreateModalMapAndMarkers() {
        modalMapAndMarkersSetting = new Dialog(MapActivity.this);
        modalMapAndMarkersSetting.setTitle(getResources().getString(R.string.MapAndMarkers));
        modalMapAndMarkersSetting.setContentView(R.layout.map_and_markers);

        btnSubmitMapMarkerSettings = (Button) modalMapAndMarkersSetting.findViewById(R.id.button_submit_map_marker_settings);
        btnDiscardMapMarkerSettings = (Button) modalMapAndMarkersSetting.findViewById(R.id.button_discard_map_marker_settings);

        checkBoxAllowAutoRefresh = (CheckBox) modalMapAndMarkersSetting.findViewById(R.id.checkBoxAllowAutoRefresh);
        checkBoxShowCompass = (CheckBox) modalMapAndMarkersSetting.findViewById(R.id.checkboxShowCompass);
        checkBoxShowMyLocation = (CheckBox) modalMapAndMarkersSetting.findViewById(R.id.checkboxShowMyLocationBtn);
        checkBoxShowZoomButtons = (CheckBox) modalMapAndMarkersSetting.findViewById(R.id.checkboxShowZoomBtn);

        seekBarMilliSeconds = (SeekBar) modalMapAndMarkersSetting.findViewById(R.id.seekBarMilSecs);

        textViewMilSecondsDisplay = (TextView) modalMapAndMarkersSetting.findViewById(R.id.milSecndsDisplay);

        checkBoxAllowAutoRefresh.setOnClickListener(this);

        btnSubmitMapMarkerSettings.setOnClickListener(this);
        btnDiscardMapMarkerSettings.setOnClickListener(this);


        seekBarMilliSeconds.setMax(120);

        // Инициализируем данные в окне из настроек
        try {
            seekBarMilliSeconds.setProgress(Integer.parseInt(appSettings.getRefreshTimeout()) / 1000);
        } catch (Exception e) {
            seekBarMilliSeconds.setProgress(10);
        }
        textViewMilSecondsDisplay.setText(Integer.toString(seekBarMilliSeconds.getProgress()));


        boolean b1 = appSettings.getGetAllowAutoRefreshSetting();
        boolean b2 = appSettings.getGetShowZoomButtonSetting();
        boolean b3 = appSettings.getGetShowCompassButtonSetting();
        boolean b4 = appSettings.getGetShowMyLocationButtonSetting();
        checkBoxAllowAutoRefresh.setChecked(b1);
        checkBoxShowZoomButtons.setChecked(b2);
        checkBoxShowCompass.setChecked(b3);
        checkBoxShowMyLocation.setChecked(b4);
        setSeekBarRefreshTimeoutVisible(checkBoxAllowAutoRefresh.isChecked());


        seekBarMilliSeconds.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                textViewMilSecondsDisplay.setText(Integer.toString(seekBar.getProgress()) + getResources().getString(R.string.Sec));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                appSettings.setRefreshTimeOut(seekBar.getProgress());
            }
        });

        modalMapAndMarkersSetting.show();

    }

    public void onCreateModalToAddMarker() {

        modalAddMarker = new Dialog(MapActivity.this);
        modalAddMarker.setTitle(R.string.addMarker);
        modalAddMarker.setContentView(R.layout.modal_add_marker_r);

        markerShortDescription = (EditText) modalAddMarker.findViewById(R.id.shortDesc);
        markerLongDescription = (EditText) modalAddMarker.findViewById(R.id.longDesc);

        buttonCreateMarker = (Button) modalAddMarker.findViewById(R.id.buttonAddMarker);
        buttonCancelCreateMarker = (Button) modalAddMarker.findViewById(R.id.buttonAddMarkerCancel);

        markerTypeCorrupt = (RadioButton) modalAddMarker.findViewById(R.id.corr);
        markerTypeLookFriends = (RadioButton) modalAddMarker.findViewById(R.id.lookf);
        markerTypeAccident = (RadioButton) modalAddMarker.findViewById(R.id.accident);

        markerTypeCorrupt.setOnClickListener(MapActivity.this);
        markerTypeLookFriends.setOnClickListener(MapActivity.this);
        markerTypeAccident.setOnClickListener(MapActivity.this);

        buttonCreateMarker.setOnClickListener(MapActivity.this);
        buttonCancelCreateMarker.setOnClickListener(MapActivity.this);


        modalAddMarker.show();
    }

    // создает модальное окно и наполняет его данными
    public void onCreateModalToViewMarker(Marker marker) {
        modalOnView = new Dialog(MapActivity.this);
        modalOnView.setTitle(markersCollection.get(marker.getId()).getUserLogin());
        modalOnView.setContentView(R.layout.modal_view_marker_r);

        shortDesc = (EditText) modalOnView.findViewById(R.id.shortDescView);
        longDesc = (EditText) modalOnView.findViewById(R.id.longDescView);
        phone = (EditText) modalOnView.findViewById(R.id.phoneView);

        markerOwner = (TextView) modalOnView.findViewById(R.id.textView_markerOwner);

        markerImage = (ImageView) modalOnView.findViewById(R.id.imageView_markerImage);

        btnDial = (Button) modalOnView.findViewById(R.id.buttonDial);
        btnSMS = (Button) modalOnView.findViewById(R.id.buttonSMS);
        btnDeleteMarker = (Button) modalOnView.findViewById(R.id.buttonDeleteMarker);

        String markerUserLogin = markersCollection.get(marker.getId()).getUserLogin();
        String currentUserLogin = currentUser.getLogin();


        selectedMarker = markersCollection.get(marker.getId());
        String markerOwnerName = getResources().getString(R.string.OwnerOfMarkerIs)+" "+ selectedMarker.getUserName() +" "+ selectedMarker.getUserSecondName();
        markerOwner.setText(markerOwnerName);
        if (markerUserLogin.equals(currentUserLogin)) {
            btnDeleteMarker.setVisibility(View.VISIBLE);
        } else {
            btnDeleteMarker.setVisibility(View.INVISIBLE);
        }


        btnDial.setOnClickListener(this);
        btnSMS.setOnClickListener(this);
        btnDeleteMarker.setOnClickListener(this);

        shortDesc.setEnabled(false);
        longDesc.setEnabled(false);
        phone.setEnabled(false);

        // наполнение данными
        try {

            shortDesc.setText(markersCollection.get(marker.getId()).getShortDescription());
            longDesc.setText(markersCollection.get(marker.getId()).getDescription());
            phone.setText(markersCollection.get(marker.getId()).getPhone());
            markerImage.setBackgroundResource(selectedMarker.getBitmap());

        } catch (Exception e) {
            e.printStackTrace();
        }

        modalOnView.show();
    }


    /**
     * Implementations of Navigation Drawer
     */

    void prepareToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.accept, R.string.btnCancel);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View navHeaderView = navigationView.getHeaderView(0);


        navUserFamNam = (TextView) navHeaderView.findViewById(R.id.navUserFamNam);
        navUserFamNam.setText(currentUser.getFirstName() + " " + currentUser.getSecondName());

        navUserLogin = (TextView) navHeaderView.findViewById(R.id.navUserLogin);
        navUserLogin.setText(currentUser.getLogin());

    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Обработчики нажатий на элементы бокового меню
     */

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

        switch (item.getItemId()) {
            case R.id.nav_map:
                onCreateModalMapAndMarkers();
                break;
            case R.id.nav_settings:
                //fragmentTransaction.replace(R.id.content_main, fragmentSettings);
                break;
            case R.id.nav_filtering:
                onCreateModalFilteringSettings();
                break;
            case R.id.nav_vk:
                Uri adress = Uri.parse("https://vk.com/motohelper_official");
                Intent openLink = new Intent(Intent.ACTION_VIEW, adress);
                startActivity(openLink);
                break;
            case R.id.nav_youtube:
                Uri adress2 = Uri.parse("https://www.youtube.com/channel/UC2FyacbcsACH-OZZ9Xi4yYg");
                Intent openLink2 = new Intent(Intent.ACTION_VIEW, adress2);
                startActivity(openLink2);
                break;
            case R.id.nav_refresh:
                refreshMarkers(false);
                break;
        }
        fragmentTransaction.commit();


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    /**
     * Обработчики нажатия на кнопки в активности
     */


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonAddMarkerCancel:
                modalAddMarker.dismiss();
                break;
            case R.id.buttonAddMarker:

                if(!markerTypeAccident.isChecked() && !markerTypeCorrupt.isChecked() && !markerTypeLookFriends.isChecked())
                {
                    Toast.makeText(MapActivity.this,getResources().getText(R.string.chooseMarkerType),Toast.LENGTH_SHORT).show();
                    break;
                }

                if(markerShortDescription.getText().toString().length()==0){
                    Toast.makeText(MapActivity.this,getResources().getText(R.string.ShortDescriptionRequired),Toast.LENGTH_SHORT).show();
                    break;
                }

                //тип и инициализация
                int type = 1;
                if (markerTypeAccident.isChecked()) type = 1;
                if (markerTypeCorrupt.isChecked()) type = 2;
                if (markerTypeLookFriends.isChecked()) type = 3;

                MyMarker m = new MyMarker(positionToAddMarker, markerShortDescription.getText().toString(), markerLongDescription.getText().toString(), currentUser.getPhone(), type, true, currentUser.getLogin());
                ServerUtilityAddMarker addMarker = new ServerUtilityAddMarker(this, m, currentUser);
                addMarker.execute();
                modalAddMarker.dismiss();
                break;
            case R.id.buttonDial:
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:" + phone.getText().toString()));
                startActivity(dialIntent);
                break;
            case R.id.buttonSMS:
                Intent smsIntent = new Intent(Intent.ACTION_VIEW, Uri.fromParts(
                        "sms", phone.getText().toString(), null));
                startActivity(smsIntent);
                break;

            case R.id.buttonDeleteMarker:
                ServerUtilityRemoveMarker removeMarker = new ServerUtilityRemoveMarker(MapActivity.this, selectedMarker, appSettings.getIpAddress());
                removeMarker.execute();
                modalOnView.cancel();
                break;

            /**
             *
             * МАРКЕРЫ И КАРТА
             *
             */
            case R.id.checkBoxAllowAutoRefresh:
                setSeekBarRefreshTimeoutVisible(checkBoxAllowAutoRefresh.isChecked());
                break;

            case R.id.button_submit_map_marker_settings:
                appSettings.setAllowAutoRefresh(checkBoxAllowAutoRefresh.isChecked());
                setSeekBarRefreshTimeoutVisible(checkBoxAllowAutoRefresh.isChecked());
                appSettings.setShowCompassButton(checkBoxShowCompass.isChecked());
                appSettings.setShowMyLocationButton(checkBoxShowMyLocation.isChecked());
                appSettings.setShowZoomButton(checkBoxShowZoomButtons.isChecked());
                modalMapAndMarkersSetting.dismiss();
                markerRefreshTimeout = seekBarMilliSeconds.getProgress() * 1000;
                appSettings.setRefreshTimeOut(markerRefreshTimeout);
                reloadMap(mMap);

                try {
                    refreshMarkers(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case R.id.button_discard_map_marker_settings:
                modalMapAndMarkersSetting.dismiss();
                break;
            /**
             *
             * ФИЛЬТРАЦИЯ
             *
             */
            case R.id.button_save_filters:
                appSettings.setShowOnlyMyMarkers(checkBoxOnlyMyMarkers.isChecked());
                appSettings.setShowOnlyAccidents(checkBoxAccidentMarkers.isChecked());
                appSettings.setShowOnlyCorrupts(checkBoxCorruptMarkers.isChecked());
                appSettings.setShowOnlyLookFriends(checkBoxLookFriendsMarkers.isChecked());
                refreshMarkers(true);
                modalFilteringSettings.dismiss();
                break;
            case R.id.button_discard_filters:
                modalFilteringSettings.dismiss();
                break;
        }


    }

    private void reloadMap(GoogleMap m) {
        m.getUiSettings().setCompassEnabled(appSettings.getGetShowCompassButtonSetting());
        m.getUiSettings().setMyLocationButtonEnabled(appSettings.getGetShowMyLocationButtonSetting());
        m.getUiSettings().setZoomControlsEnabled(appSettings.getGetShowZoomButtonSetting());
        autoRefresh = appSettings.getGetAllowAutoRefreshSetting();

    }

    public void setSeekBarRefreshTimeoutVisible(boolean b) {
        if (b) {
            seekBarMilliSeconds.setVisibility(View.VISIBLE);
            textViewMilSecondsDisplay.setVisibility(View.VISIBLE);
        } else {
            seekBarMilliSeconds.setVisibility(View.INVISIBLE);
            textViewMilSecondsDisplay.setVisibility(View.INVISIBLE);
        }

    }


    //Рефреш маркеров по таймеру
    void getMarkersByTimer() {
        mMap.clear();
        final Handler handler;
        handler = new Handler();
        timer = new Timer();
        doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            MapActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    getMarkersAuto = new ServerUtilityGetMarkers(MapActivity.this);
                                    getMarkersAuto.setDoDialog(false);
                                    getMarkersAuto.setOnRefreshed(MapActivity.this);
                                    if (appSettings.getGetAllowAutoRefreshSetting()) {
                                        try {
                                            getMarkersAuto.execute();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                    }
                                }

                            });
                        } catch (Exception e) {

                        }
                    }
                });
            }
        };


        timer.schedule(doAsynchronousTask, 50, markerRefreshTimeout);
    }

    @Override
    public void onRefreshCompleted() throws ExecutionException, InterruptedException {
        ArrayList<MyMarker> mMarkers = getMarkersAuto.getMarkers();
        markersCollection.clear();

        if (!(currentLocation == null)) {

            NotifManager notif = new NotifManager(MapActivity.this);
            notif.setCurrentLocation(currentLocation);
            notif.setCurrentUser(currentUser);
            notif.setRadius(5000);
            notif.notify(oldState, mMarkers);
        }


        mMap.clear();
        if (mMarkers.size() > 0)
            redrawMarkers(mMarkers);
        oldState = mMarkers;
    }

    private void redrawMarkers(ArrayList<MyMarker> mMarkers) {
        /**
         *
         * Если выбран флаг только своих маркеров
         *
         */

        if (appSettings.getShowOnlyMyMarkers()) {
            /**
             *  ДТП
             */
            if (appSettings.getShowOnlyAccidents()) {
                for (int i = 0; i < mMarkers.size(); i++) {
                    if (mMarkers.get(i).getType() == 1 && mMarkers.get(i).getUserLogin().equals(currentUser.getLogin())) {
                        mMarkers.get(i).addMarker(mMap);
                        markersCollection.put(mMarkers.get(i).getMarker().getId(), mMarkers.get(i));
                    }
                }

            }
            /**
             * Ищу попутчиков
             */
            if (appSettings.getShowOnlyLookFriends()) {
                for (int i = 0; i < mMarkers.size(); i++) {
                    if (mMarkers.get(i).getType() == 3 && mMarkers.get(i).getUserLogin().equals(currentUser.getLogin())) {
                        mMarkers.get(i).addMarker(mMap);
                        markersCollection.put(mMarkers.get(i).getMarker().getId(), mMarkers.get(i));
                    }
                }

            }
            if (appSettings.getShowOnlyCorrupts()) {
                for (int i = 0; i < mMarkers.size(); i++) {
                    if (mMarkers.get(i).getType() == 2 && mMarkers.get(i).getUserLogin().equals(currentUser.getLogin())) {
                        mMarkers.get(i).addMarker(mMap);
                        markersCollection.put(mMarkers.get(i).getMarker().getId(), mMarkers.get(i));
                    }
                }

            }

        }
        /**
         *
         * Если НЕ выбран флаг только своих маркеров
         *
         */
        if (!appSettings.getShowOnlyMyMarkers()) {
            /**
             *  ДТП
             */
            if (appSettings.getShowOnlyAccidents()) {
                for (int i = 0; i < mMarkers.size(); i++) {
                    if (mMarkers.get(i).getType() == 1) {
                        mMarkers.get(i).addMarker(mMap);
                        markersCollection.put(mMarkers.get(i).getMarker().getId(), mMarkers.get(i));
                    }
                }

            }
            /**
             * Ищу попутчиков
             */
            if (appSettings.getShowOnlyLookFriends()) {
                for (int i = 0; i < mMarkers.size(); i++) {
                    if (mMarkers.get(i).getType() == 3) {
                        mMarkers.get(i).addMarker(mMap);
                        markersCollection.put(mMarkers.get(i).getMarker().getId(), mMarkers.get(i));
                    }
                }

            }
            if (appSettings.getShowOnlyCorrupts()) {
                for (int i = 0; i < mMarkers.size(); i++) {
                    if (mMarkers.get(i).getType() == 2) {
                        mMarkers.get(i).addMarker(mMap);
                        markersCollection.put(mMarkers.get(i).getMarker().getId(), mMarkers.get(i));
                    }
                }

            }

        }

    }

    private void refreshMarkers(boolean doDialog) {
        getMarkersAuto = new ServerUtilityGetMarkers(MapActivity.this);
        getMarkersAuto.setDoDialog(doDialog);
        getMarkersAuto.setOnRefreshed(MapActivity.this);
        getMarkersAuto.execute();
        //После выполнения - идем на колбэк onRefreshCompleted()
    }

    private void openQuitDialog() {
        AlertDialog.Builder quitDialog = new AlertDialog.Builder(
                MapActivity.this);
        quitDialog.setTitle("Выход");
        quitDialog
                .setMessage("Внимание! Если вы закроете приложение, то уведомления работать не будут! Для работы уведомлений сверните приложение кнопкой 'HOME'.");
        quitDialog.setCancelable(true);

        quitDialog.setPositiveButton("Закрыть",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        onDestroyActivity();

                    }
                });

        quitDialog.setNegativeButton("Отмена",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        quitDialog.show();
    }

    public void onDestroyActivity() {
        super.onBackPressed();
    }
}

