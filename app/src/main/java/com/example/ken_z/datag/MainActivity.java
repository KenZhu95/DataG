package com.example.ken_z.datag;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BaiduNaviManagerFactory;
import com.baidu.navisdk.adapter.IBNRouteGuideManager;
import com.baidu.navisdk.adapter.IBNRoutePlanManager;
import com.baidu.navisdk.adapter.IBNTTSManager;
import com.baidu.navisdk.adapter.IBaiduNaviManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    static final String ROUTE_PLAN_NODE = "routePlanNode";
    static final String ROUTE_PLAN_NODES = "routePlanNodes";
    static final String NAV_ABORT = "navigationAbort";
    private static final String APP_FOLDER_NAME = "datag";

    private String toastMessage = "None";

    private MapView myMapView = null;
    private BaiduMap mBaiduMap;
//    private boolean if_start_navi = false;
//    private boolean if_abort_navi = false;

    public MyOrientationListener myOrientationListener;
    boolean isFirst = true; //whether to set location for the first time
    public BitmapDescriptor mCurrentMaker;
    private MyLocationConfiguration.LocationMode mCurrentMode;
    private float mCurrentX;

    private static final int accuracyCircleFillColor = 0xAAFFFF88;
    private static final int accuracyCircleStrokeColor = 0xAA00FF00;

    //location module
    private LocationClient locationClient;
    public MyLocationListener myLocationListener = new MyLocationListener();

    //navigation module
    RoutePlanSearch mSearch = null;
    String authinfo;
    private boolean if_init_success = false;
    private BNRoutePlanNode mStartNode = null;

    private BNRoutePlanNode mBNRoutePlanNode = null;
    private List<BNRoutePlanNode> mBNRoutePlanNodes = new ArrayList<>();

    Button button_navi, button_toast;

    //to launch the guide activity after some time
    private Timer mOffTimer;
    private Handler mOffHandler;

    private boolean if_bundle = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        MyApplication.getInstance().addActivity(this);

        button_navi = findViewById(R.id.button_navi);
        button_toast = findViewById(R.id.button_toast);
        myMapView = findViewById(R.id.bmapView);
        mBaiduMap = myMapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);

        button_navi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startNavigation();
            }
        });

        button_toast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startToast();
            }
        });

        checkPerm();

        initLocation();
        initNavigation();

    }

    private void checkPerm() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int permission = ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if(permission!= PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,new String[]
                            {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.LOCATION_HARDWARE,Manifest.permission.READ_PHONE_STATE,
                                    Manifest.permission.WRITE_SETTINGS,Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_CONTACTS},0x0010);
                }

                int permission1 = ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION);
                int permission2 = ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

                if(permission1 != PackageManager.PERMISSION_GRANTED || permission2 != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,new String[] {
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},0x0010);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("TTT", "Access GPS");
                } else {
                    Log.d("TTTT", "Access Denied");
                } break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        mBaiduMap.setMyLocationEnabled(true);
        if (!locationClient.isStarted()) {
            locationClient.start();
            myOrientationListener.start();
        }
        Intent intent = getIntent();
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                //toastMessage = bundle.getString("title");
                if_bundle = true;
                int abort_nav = bundle.getInt(NAV_ABORT);
                if (abort_nav == 0) {
                    mBNRoutePlanNode = (BNRoutePlanNode) bundle.getSerializable(ROUTE_PLAN_NODE);
                    mBNRoutePlanNodes = (List<BNRoutePlanNode>) bundle.getSerializable(ROUTE_PLAN_NODES);
                    launchNavigation();
                } else {
                    //if_abort_navi = true;
                    //to exit App
                    mOffHandler = new Handler() {
                        public void handleMessage(Message msg) {
                            if (msg.what > 0) {
                            } else {
                                MyApplication.getInstance().exit();
                                mOffTimer.cancel();
                            }
                            super.handleMessage(msg);
                        }
                    };
                    //count down timer
                    mOffTimer = new Timer(true);
                    TimerTask tt = new TimerTask() {
                        int countTime = 3;
                        @Override
                        public void run() {
                            if (countTime > 0) {
                                countTime--;
                            }
                            Message msg = new Message();
                            msg.what = countTime;
                            mOffHandler.sendMessage(msg);
                        }
                    };
                    mOffTimer.schedule(tt, 1000, 1000);
                }

            }
        }

        if((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0){
            finish();
            return;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mBaiduMap.setMyLocationEnabled(false);
        locationClient.stop();
        myOrientationListener.stop();
    }

    private void initLocation() {
        mCurrentMode = MyLocationConfiguration.LocationMode.COMPASS;
        mCurrentMaker = null;
        mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                mCurrentMode, true, mCurrentMaker, accuracyCircleFillColor, accuracyCircleStrokeColor
        ));
        locationClient = new LocationClient(getApplicationContext());
        locationClient.registerLocationListener(myLocationListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);
        option.setLocationNotify(true);
        option.setNeedDeviceDirect(true);
        option.setCoorType("bd09ll");
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setScanSpan(1000);
        option.setIsNeedAddress(true);
        option.disableCache(true);
        locationClient.setLocOption(option);
        myOrientationListener = new MyOrientationListener(this);
        myOrientationListener.setOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                mCurrentX = x;
            }
        });

    }

    private void initNavigation() {
        mSearch = RoutePlanSearch.newInstance();
        OnGetRoutePlanResultListener nvListener = new OnGetRoutePlanResultListener() {
            @Override
            public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {

            }

            @Override
            public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {

            }

            @Override
            public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

            }

            @Override
            public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {

            }

            @Override
            public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

            }

            @Override
            public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

            }
        };
        mSearch.setOnGetRoutePlanResultListener(nvListener);

        BaiduNaviManagerFactory.getBaiduNaviManager().init(this, Environment.getExternalStorageDirectory().toString(),
                APP_FOLDER_NAME, new IBaiduNaviManager.INaviInitListener() {
                    @Override
                    public void onAuthResult(int i, String s) {
                        if (0 == i) {
                            authinfo = "key check success";
                        } else {
                            authinfo = "key check fail" + s;
                        }
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, authinfo, Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void initStart() {
                        Toast.makeText(MainActivity.this, "navigation initialization begin", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void initSuccess() {
                        Toast.makeText(MainActivity.this, "navigation initialization success", Toast.LENGTH_SHORT).show();
                        if_init_success = true;
                        initSetting();
                        initTTS();
                    }

                    @Override
                    public void initFailed() {
                        Toast.makeText(MainActivity.this, "navigation initialization fail", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initSetting() {
    }

    private String getSDcardDir() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }

    private IBNTTSManager.IBNOuterTTSPlayerCallback mTTSCallback = new IBNTTSManager.IBNOuterTTSPlayerCallback() {

        @Override
        public int getTTSState() {
//            /** 播放器空闲 */
//            int PLAYER_STATE_IDLE = 1;
//            /** 播放器正在播报 */
//            int PLAYER_STATE_PLAYING = 2;
            return PLAYER_STATE_IDLE;
        }

        @Override
        public int playTTSText(String text, String s1, int i, String s2) {
            Log.e("BNSDKDemo", "playTTSText:" + text);
            return 0;
        }

        @Override
        public void stopTTS() {
            Log.e("BNSDKDemo", "stopTTS");
        }
    };

    private void initTTS() {
        //apply internal TTS
        BaiduNaviManagerFactory.getTTSManager().initTTS(getApplicationContext(),
                getSDcardDir(), APP_FOLDER_NAME, NormalUtils.getTTSAppID());

        //同步内置TTS状态callback
        BaiduNaviManagerFactory.getTTSManager().setOnTTSStateChangedListener(new IBNTTSManager.IOnTTSPlayStateChangedListener() {
            @Override
            public void onPlayStart() {
                Log.e("datag", "ttsCallback.onPlayStart");
            }

            @Override
            public void onPlayEnd(String s) {
                Log.e("datag", "ttsCallback.onPlayEnd");
            }

            @Override
            public void onPlayError(int i, String s) {
                Log.e("datag", "ttsCallback.onPlayError");
            }
        });

        //注册异步状态消息
        BaiduNaviManagerFactory.getTTSManager().setOnTTSStateChangedHandler(
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        Log.e("datag", "ttsHandler.msg.what=" + msg.what);
                    }
                }
        );
    }

    private void startNavigation() {
        if (!if_init_success) {
            Toast.makeText(MainActivity.this, "还未初始化!", Toast.LENGTH_SHORT).show();
        }
        MyLocationData currentLoc = mBaiduMap.getLocationData();
        BNRoutePlanNode sNode = new BNRoutePlanNode(currentLoc.longitude, currentLoc.latitude, "Start", "Start", BNRoutePlanNode.CoordinateType.BD09LL);
        final List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
        if (mBNRoutePlanNodes.size() > 0) {
            mStartNode = mBNRoutePlanNodes.get(0);
            //mStartNode = sNode;
            //list.add(sNode);
            for (BNRoutePlanNode node : mBNRoutePlanNodes) {
                list.add(node);
            }
        } else {
            //BNRoutePlanNode sNode = new BNRoutePlanNode(currentLoc.longitude, currentLoc.latitude, "Start", "Start", BNRoutePlanNode.CoordinateType.BD09LL);
            BNRoutePlanNode node_1 = new BNRoutePlanNode(currentLoc.longitude + 0.05, currentLoc.latitude, "node 1", "node 1", BNRoutePlanNode.CoordinateType.BD09LL);
            BNRoutePlanNode node_2 = new BNRoutePlanNode(currentLoc.longitude + 0.1, currentLoc.latitude + 0.05, "node 2", "node 2", BNRoutePlanNode.CoordinateType.BD09LL);
            BNRoutePlanNode eNode = new BNRoutePlanNode(currentLoc.longitude - 0.2, currentLoc.latitude - 0.2, "End", "End", BNRoutePlanNode.CoordinateType.BD09LL);

            mStartNode = sNode;

            list.add(sNode);
            list.add(node_1);
            list.add(node_2);
            list.add(eNode);
        }


        BaiduNaviManagerFactory.getRoutePlanManager().routeplanToNavi(
                list,
                IBNRoutePlanManager.RoutePlanPreference.ROUTE_PLAN_PREFERENCE_DEFAULT,
                null,
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_START:
                                Toast.makeText(MainActivity.this, "算路开始", Toast.LENGTH_SHORT)
                                        .show();
                                break;
                            case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_SUCCESS:
                                Toast.makeText(MainActivity.this, "算路成功", Toast.LENGTH_SHORT)
                                        .show();
                                break;
                            case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_FAILED:
                                Toast.makeText(MainActivity.this, "算路失败", Toast.LENGTH_SHORT)
                                        .show();
                                //if_start_navi = false;
                                break;
                            case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_TO_NAVI:
                                Toast.makeText(MainActivity.this, "算路成功准备进入导航", Toast.LENGTH_SHORT)
                                        .show();
                                Intent intent = new Intent(MainActivity.this,
                                        GuideActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putInt(NAV_ABORT, 0);
                                bundle.putSerializable(ROUTE_PLAN_NODE, mStartNode);
                                bundle.putSerializable(ROUTE_PLAN_NODES, (Serializable)list);
                                intent.putExtras(bundle);
                                startActivity(intent);
                                break;
                            default:
                                // nothing
                                break;
                        }
                    }
                }
        );
    }

    private void startToast() {
        Toast.makeText(MainActivity.this, toastMessage, Toast.LENGTH_LONG).show();
    }


    public class MyLocationListener extends BDAbstractLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            //do not process new data after MapView is destroyed
            if (location == null || myMapView == null) {
                return;
            }

            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    .direction(mCurrentX)
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();

            mBaiduMap.setMyLocationData(locData); //set location data
            int code = location.getLocType();
            System.out.print(code);
            if (isFirst) {
                isFirst = false;
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                //define map state
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(latLng)
                        .zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        }

        public void onReceivePoi(BDLocation poiLocation) {

        }
    }

    @Override
    protected void onDestroy() {
        //locationClient.stop();
        //mBaiduMap.setMyLocationEnabled(false);
        myMapView.onDestroy();
        myMapView = null;
        super.onDestroy();

    }

    @Override
    protected void onResume() {
        myMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        myMapView.onPause();
        super.onPause();
    }


    private void launchNavigation() {
        mOffHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what <= 0) {
                    startNavigation();
                    mOffTimer.cancel();
                }
                super.handleMessage(msg);
            }
        };

        //count down timer
        mOffTimer = new Timer(true);
        TimerTask tt = new TimerTask() {
            int countTime = 10;
            @Override
            public void run() {
                if (countTime > 0) {
                    countTime--;
                }
                Message msg = new Message();
                msg.what = countTime;
                mOffHandler.sendMessage(msg);
            }
        };
        mOffTimer.schedule(tt, 1000, 1000);
    }
}