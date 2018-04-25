package kgu.ykita.virtualmarathonbasic1_1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;

    TextView distance, time;
    Button button;

    Timer mTimer = null;            //onClickメソッドでインスタンス生成
    Handler mHandler = new Handler();   //UI Threadへのpost用ハンドラ
    float mLaptime = 0.0f;

    boolean onFlag = false;

    private CourseManager mc;
    private ArrayList<LatLng> courseLocations;

    private Location currentLocation = null;
    private static Location lastLocation = null;
    private static float totalDistance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        distance = (TextView) findViewById(R.id.distance);
        time = (TextView) findViewById(R.id.time);
        button = (Button) findViewById(R.id.button);

        mc = new CourseManager(this);
        courseLocations = mc.getCourse();

        // Fine か Coarseのいずれかのパーミッションが得られているかチェックする
        // 本来なら、Android6.0以上かそうでないかで実装を分ける必要がある
        if (ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION)  != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            /** fine location のリクエストコード（値は他のパーミッションと被らなければ、なんでも良い）*/
            final int requestCode = 1;

            // いずれも得られていない場合はパーミッションのリクエストを要求する
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode );
            return;
        }

        // 位置情報を管理している LocationManager のインスタンスを生成する
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        String locationProvider = null;

        // GPSが利用可能になっているかどうかをチェック
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationProvider = LocationManager.GPS_PROVIDER;
        }
        // GPSプロバイダーが有効になっていない場合は基地局情報が利用可能になっているかをチェック
        else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationProvider = LocationManager.NETWORK_PROVIDER;
        }
        // いずれも利用可能でない場合は、GPSを設定する画面に遷移する
        else {
            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(settingsIntent);
            return;
        }

        /** 位置情報の通知するための最小時間間隔（ミリ秒） */
        final long minTime = 500;
        /** 位置情報を通知するための最小距離間隔（メートル）*/
        final long minDistance = 1;

        // 利用可能なロケーションプロバイダによる位置情報の取得の開始
        // FIXME 本来であれば、リスナが複数回登録されないようにチェックする必要がある
        //locationManager.requestLocationUpdates(locationProvider, minTime, minDistance, this);

        // GPS_PROVIDERだけだと室内で使えないので，NETWORK_PROVIDERも利用する．
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, this);

        // 最新の位置情報
        Location location = locationManager.getLastKnownLocation(locationProvider);

        if (location != null) {
            Toast.makeText(this, "READY:"+locationProvider, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        mMap.moveCamera(CameraUpdateFactory.zoomTo(16));
        PolylineOptions course = new PolylineOptions();
        for (int i = 0; i < courseLocations.size(); i++)
            course.add(courseLocations.get(i));
        course.color(Color.RED);
        mMap.addPolyline(course);

        button.setText(R.string.start);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (button.getText().equals("START")) {
                    //lm.start();
                    onFlag = true;
                    button.setText(R.string.stop);

                    mTimer = new Timer(true);
                    mTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // mHandlerを通じてUI Threadへ処理をキューイング
                            mHandler.post(new Runnable() {
                                public void run() {

                                    //実行間隔分を加算処理
                                    mLaptime += 0.1d;

                                    //計算にゆらぎがあるので小数点第1位で丸める
                                    BigDecimal bi = new BigDecimal(mLaptime);
                                    float outputValue = bi.setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();

                                    //現在のLapTime
                                    time.setText(String.format("%.1f", outputValue));
                                }
                            });
                        }
                    }, 100, 100);
                } else if (button.getText().equals("STOP")) {
                    //lm.stop();
                    onFlag = false;
                    button.setText(R.string.start);

                    if (mTimer != null) {
                        mTimer.cancel();
                        mTimer = null;
                    }
                }
            }
        });

        showLocation(0);
    }

    public void showLocation(float d) {

        distance.setText(String.format("%.2f", d));

        Location loc = mc.getLocation(d);

        LatLng here = new LatLng(loc.getLatitude(), loc.getLongitude());
        mMap.addMarker(new MarkerOptions().position(here).title("Marker here"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(here));
    }

    //位置情報が通知されるたびにコールバックされるメソッド
    @Override
    public void onLocationChanged(Location location){

        Toast.makeText(this, "LocationChanged", Toast.LENGTH_LONG).show();

        if(!onFlag) return;

        currentLocation = location;

        if (lastLocation == null) {
            lastLocation = currentLocation;
            return;
        }
        float[] results = new float[3];
        Location.distanceBetween(lastLocation.getLatitude(), lastLocation.getLongitude(), currentLocation.getLatitude(), currentLocation.getLongitude(), results);
        totalDistance += results[0];
        showLocation(totalDistance);

    }

    //ロケーションプロバイダが利用不可能になるとコールバックされるメソッド
    @Override
    public void onProviderDisabled(String provider) {
        //ロケーションプロバイダーが使われなくなったらリムーブする必要がある
        Toast.makeText(this, "OnProviderDisabled", Toast.LENGTH_LONG).show();
    }

    //ロケーションプロバイダが利用可能になるとコールバックされるメソッド
    @Override
    public void onProviderEnabled(String provider) {
        //プロバイダが利用可能になったら呼ばれる
        Toast.makeText(this, "OnProviderEnabled", Toast.LENGTH_LONG).show();
    }

    //ロケーションステータスが変わるとコールバックされるメソッド
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // 利用可能なプロバイダの利用状態が変化したときに呼ばれる
        Toast.makeText(this, "OnStatusChanged", Toast.LENGTH_LONG).show();
    }
}
