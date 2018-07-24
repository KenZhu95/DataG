package com.example.ken_z.datag;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BaiduNaviManagerFactory;
import com.baidu.navisdk.adapter.IBNRouteGuideManager;


import java.util.List;


public class GuideActivity extends AppCompatActivity {

    private static final String TAG = GuideActivity.class.getName();

    private BNRoutePlanNode mBNRoutePlanNode = null;
    private List<BNRoutePlanNode> mBNRoutePlanNodes;

    private IBNRouteGuideManager mRouteGuideManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        createHandler();

        mRouteGuideManager = BaiduNaviManagerFactory.getRouteGuideManager();
        View view = mRouteGuideManager.onCreate(this, mOnNavigationListener);
        //setContentView(R.layout.activity_guide);
        if (view != null) {
            setContentView(view);
        }
        //getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);


        Intent intent = getIntent();
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                mBNRoutePlanNode = (BNRoutePlanNode) bundle.getSerializable(MainActivity.ROUTE_PLAN_NODE);
                mBNRoutePlanNodes = (List<BNRoutePlanNode>) bundle.getSerializable(MainActivity.ROUTE_PLAN_NODES);
            }
        }

        routeGuideEvent();
    }


    private void routeGuideEvent() {
        com.example.ken_z.datag.EventHandler.getInstance().getDialog(this);
        com.example.ken_z.datag.EventHandler.getInstance().showDialog();

        BaiduNaviManagerFactory.getRouteGuideManager().setRouteGuideEventListener(
                new IBNRouteGuideManager.IRouteGuideEventListener() {
                    @Override
                    public void onCommonEventCall(int what, int arg1, int arg2, Bundle bundle) {
                        com.example.ken_z.datag.EventHandler.getInstance().handleNaviEvent(what, arg1, arg2, bundle);
                    }
                }
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Event Bus register
        mRouteGuideManager.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Event Bus unregister
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRouteGuideManager.onResume();
        // 自定义图层
        //showOverlay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }



    private static final int MSG_RESET_NODE = 3;

    private Handler hd = null;

    private void createHandler() {
        if (hd == null) {
            hd = new Handler(getMainLooper()) {
                public void handleMessage(Message msg) {
                    if (msg.what == MSG_RESET_NODE) {
                        //
                    }
                }
            };
        }

    }

    private IBNRouteGuideManager.OnNavigationListener mOnNavigationListener =
            new IBNRouteGuideManager.OnNavigationListener() {
                @Override
                public void onNaviGuideEnd() {
                    //exit navigation
                    finish();
                }

                @Override
                public void notifyOtherAction(int actionType, int i1, int i2, Object o) {
                    if (actionType == 0) {
                        //get destination, exit automatically
                        Log.i(TAG, "notifyOtherAction actionType = " + actionType + ",导航到达目的地！");
                        mRouteGuideManager.forceQuitNaviWithoutDialog();
                    }
                }
            };

}
