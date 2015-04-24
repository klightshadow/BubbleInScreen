package com.lightshadow.bubbleinscreen.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.AnimationDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.LocationSource;
import com.lightshadow.bubbleinscreen.R;
import com.lightshadow.bubbleinscreen.util.XMLParser;
import com.lightshadow.bubbleinscreen.views.HandActivity;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class BubbleInScreen extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationSource.OnLocationChangedListener, LocationListener {

    private WindowManager windowManager;
    private ImageView bubbleView, deleteView, handFirst;
    private WindowManager.LayoutParams params;
    Display display;
    private Handler handler;
    private Runnable runnable;
    private static int screenWidth;
    private static int screenHeight;
    private AnimationDrawable animationDrawable;
    private RelativeLayout bubbleLayout, handLayout;
    private RelativeLayout notifyLayout, notify;
    private PopupWindow popupWindow;

    private static boolean isHandShow = false;
    private static boolean isTextShow = false;
    WindowManager.LayoutParams handFirstParams;
    WindowManager.LayoutParams notifyTextParams;
    TextView notifyText;
    private boolean isLeft = true;

    private GoogleApiClient googleApiClient;
    private Location location;
    private LocationRequest locationRequest;
    private static  List<Address> address;
    String xml;
    Document document;
    private static final String LOCATIONS = "locations";
    private static final String LOCATIONS_NAME = "locationsName";
    private static final String LOCATION = "location";

    public BubbleInScreen() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        locationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        CheckGooglePlayService();
        //LayoutInflater inflater = (LayoutInflater)getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        bubbleLayout = new RelativeLayout(this);//(RelativeLayout)inflater.inflate(R.layout.activity_bubble, null);

        //bubbleView = (ImageView)bubbleLayout.findViewById(R.id.iv_bubbleView);
        windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        bubbleView = new ImageView(this);
        deleteView = new ImageView(this);
        handFirst = new ImageView(this);
        //notifyText = new TextView(this);

        bubbleView.setId(R.id.bubbleid);
        bubbleView.setImageResource(R.drawable.pet);
        handFirst.setImageResource(R.drawable.ic_launcher);
        //handFirst.setVisibility(View.GONE);

        deleteView.setImageResource(R.drawable.ic_launcher);

//        notifyText.setText("test");
//        notifyText.setTextColor(getResources().getColor(android.R.color.black));
//        notifyText.setBackgroundColor(getResources().getColor(android.R.color.white));

        handFirst.setClickable(true);

        setImageAction();

        /* add bubbleView   | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE*/
        final RelativeLayout.LayoutParams bubbleParams = new RelativeLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        bubbleParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

        handFirstParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        handFirstParams.gravity = Gravity.CENTER;
        //handFirstParams.addRule(RelativeLayout.CENTER_IN_PARENT, R.id.iv_handFirst);
        windowManager.addView(handFirst, handFirstParams);

        bubbleLayout.addView(bubbleView, bubbleParams);

        notifyLayout = (RelativeLayout)LayoutInflater.from(this).inflate(R.layout.layout_notify, null);
        notifyText = (TextView)notifyLayout.findViewById(R.id.tv_notify);
        notifyText.setBackgroundColor(getResources().getColor(android.R.color.white));
        notifyText.setTextColor(getResources().getColor(android.R.color.black));
        notifyText.setTextSize(20);
        notify = (RelativeLayout)notifyLayout.findViewById(R.id.rl_notify);

        notifyTextParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        notifyTextParams.gravity = Gravity.TOP | Gravity.LEFT;

        notifyLayout.setVisibility(View.GONE);
        windowManager.addView(notifyLayout, notifyTextParams);
//        notifyTextParams.addRule(RelativeLayout.RIGHT_OF, R.id.bubbleid);
//
//        bubbleLayout.addView(notifyText, notifyTextParams);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;
        //params.windowAnimations = android.R.style.Animation_Translucent;

        display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        windowManager.addView(bubbleLayout, params);

        animationDrawable = (AnimationDrawable)this.getResources().getDrawable(R.drawable.cat_walk);
        bubbleView.setImageDrawable(animationDrawable);
        /*animation = new TranslateAnimation(0, 980, 100, 100);
        animation.setDuration(5000);
        animation.setRepeatCount(0);
        bubbleView.startAnimation(animation);*/

        //animation.startNow();

        /*add deleteView*/
        WindowManager.LayoutParams deleteParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        deleteParams.gravity = Gravity.BOTTOM | Gravity.CENTER;

        windowManager.addView(deleteView, deleteParams);
        animImage();
        moveImage();

        deleteView.setVisibility(View.GONE);

    }

    private void CheckGooglePlayService() {
        googleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        int isGooglePlayServiceAvilable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(isGooglePlayServiceAvilable == ConnectionResult.SUCCESS) {
            googleApiClient.connect();
        } else {
            String errorText = GooglePlayServicesUtil.getErrorString(isGooglePlayServiceAvilable);
            Toast.makeText(this, errorText, Toast.LENGTH_SHORT).show();
        }
    }

    private void animImage() {
        Handler mHandler = new Handler();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                animationDrawable.start();
            }
        });
    }

    private void moveImage() {

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if(params.y <= 0) {
                    params.x += 10;
                }
                if(params.x > screenWidth * 0.8) {
                    isLeft = true;
                    params.y += 10;
                }
                if(params.y > screenHeight * 0.8) {
                    params.x -= 10;
                }
                if(params.x <= 0) {
                    isLeft = false;
                    params.y -= 10;
                }
                try {
                    windowManager.updateViewLayout(bubbleLayout, params);
                    moveImage();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }

            }
        };
        handler.postDelayed(runnable, 100);

    }

    private void setImageAction() {

        bubbleLayout.setOnTouchListener(new View.OnTouchListener() {

            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        handler.removeCallbacks(runnable);
                        animationDrawable.stop();
                        if(notifyLayout != null) {
                            notifyLayout.setVisibility(View.GONE);
                            textHandler.removeCallbacks(textRunnable);
                        }
                        bubbleView.setImageResource(R.drawable.pet);

                        //animation.cancel();
                        //bubbleView.setAnimation(null);
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        //Toast.makeText(BubbleInScreen.this, "Move start", Toast.LENGTH_SHORT).show();
                        deleteView.setVisibility(View.VISIBLE);
                        return false;
                    case MotionEvent.ACTION_UP:
                       if(params.y > screenHeight * 0.7 && screenWidth * 0.4 < params.x && params.x < screenWidth * 0.6) {
                            bubbleView.setVisibility(View.GONE);
                            deleteView.setVisibility(View.GONE);
                            Toast.makeText(BubbleInScreen.this, "Remove icon", Toast.LENGTH_SHORT).show();
                            Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                            vibrator.vibrate(50);
                            stopSelf();
                       } else if(params.y < screenHeight * 0.2 && params.x > screenWidth * 0.9) {
                           switch (handFirst.getVisibility()) {
                               case View.VISIBLE:
                                   //popupWindow.dismiss();
                                   //handFirst.setVisibility(View.GONE);
                                   break;
                               case View.GONE:
                                   handFirst.setVisibility(View.VISIBLE);
                                   break;
                           }
                            deleteView.setVisibility(View.GONE);
                       } else {

                            if(params.x > screenWidth / 2) {
                                params.x = screenWidth;
                                windowManager.updateViewLayout(bubbleLayout, params);
                            } else if(params.x <= screenWidth /2){
                                params.x = 0;
                                windowManager.updateViewLayout(bubbleLayout, params);
                            }
                            bubbleView.setImageDrawable(animationDrawable);
                            handler.postDelayed(runnable, 3000);


                            //bubbleView.startAnimation(animation);
                            deleteView.setVisibility(View.GONE);
                            //Log.e("x", String.valueOf(params.x));
                            //Log.e("y", String.valueOf(params.y));
                       }
                        double distance = Math.sqrt(Math.abs(params.x-initialX)*Math.abs(params.x-initialX)+Math.abs(params.y-initialY)*Math.abs(params.y-initialY));
                        if(distance < 15) {
                            Log.e("bubbleView", "click");
//                            if(isHandShow) {
//                                HandActivity.handActivity.finish();
//                                isHandShow = false;
//                            } else if(isTextShow) {
//                                isTextShow = false;
//                                notifyLayout.setVisibility(View.GONE);
//                                notifyText.setText("");
//                                windowManager.updateViewLayout(notifyLayout, notifyTextParams);
//                                handler.postDelayed(runnable, 1000);
//                            }
                            return false;
                        } else {

                            return  true;
                        }

                    case MotionEvent.ACTION_MOVE:
                        //bubbleView.setVisibility(View.GONE);

                        params.x = initialX + (int)(event.getRawX() - initialTouchX);
                        params.y = initialY + (int)(event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(bubbleLayout, params);
                        break;//return  true;
                }
                return false;
            }
        });

        handFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //initiatePopup();
//
                try {
                    String DistName = address.get(0).getLocality();
                    String CityName = address.get(0).getAdminArea();
                    showNotify(CityName + "\n" + DistName);
                    //getNowWeather(CityName, DistName);
                    Intent it = new Intent(BubbleInScreen.this, HandActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(it);
                    isHandShow = true;
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }


            }
        });
    }

    private void getNowWeather(String cityName, String distName) {
        String[] urlName = getResources().getStringArray(R.array.locationName);
        String[] urlArray = getResources().getStringArray(R.array.location);
        String url = "";
        for(int i = 0; i < urlName.length; i++) {
            if(cityName.equals(urlName[i]) ) {
                Log.e("", cityName + ", " + urlArray[i]);
                url = urlArray[i];
            }
        }
        final XMLParser xmlParser = new XMLParser();
        final String finalUrl = url;

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                xml = xmlParser.getWeatherXML(finalUrl);
//                document = xmlParser.getDomElement(xml);
//                NodeList nodeList = document.getElementsByTagName("cwbopendata");
//
//        for(int i = 0; i < nodeList.getLength(); i++) {
//            Element e = (Element)nodeList.item(i);
//            temperture = xmlParser.getValue(e, distName);
//        }
//                Log.e("temp", String.valueOf(nodeList.getLength()));
//            }
//        }).start();
//
//        cityName = cityName.replace("台", "臺");
//
//        String temperture = "", descript;

    }

//    private void initiatePopup() {
//        Point size = new Point();
//        //display.getSize(size);
//        int width = size.x;
//        int height = size.y;
//        handLayout = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.layout_hand, null);
//
//        popupWindow = new PopupWindow(this);
//        popupWindow.setContentView(handLayout);
//        popupWindow.setWidth(RelativeLayout.LayoutParams.WRAP_CONTENT);
//        popupWindow.setHeight(RelativeLayout.LayoutParams.WRAP_CONTENT);
//        popupWindow.setOutsideTouchable(false);
//        popupWindow.setFocusable(true);
//        popupWindow.showAsDropDown(bubbleView);
//
//        ImageView handFirst = (ImageView)handLayout.findViewById(R.id.iv_finger1);
//        handFirst.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(BubbleInScreen.this, "handFirst Click", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }

    @Override
    public void onDestroy() {

        if (bubbleView != null && deleteView != null) {
            windowManager.removeView(bubbleLayout);
            windowManager.removeView(handFirst);
            windowManager.removeView(deleteView);
        }
        googleApiClient.disconnect();
        super.onDestroy();
    }

    Handler textHandler = new Handler();
    Runnable textRunnable = new Runnable() {
        @Override
        public void run() {
            if(notifyLayout != null) {
                notifyLayout.setVisibility(View.GONE);
            }
        }
    };

    private void showNotify(String notify) {
        if(notifyLayout != null && bubbleLayout != null) {
            isTextShow = true;
            handler.removeCallbacks(runnable);
            bubbleView.setImageResource(R.drawable.pet);
            notifyText.setText(notify);

            WindowManager.LayoutParams bubbleParams = (WindowManager.LayoutParams)bubbleLayout.getLayoutParams();
            WindowManager.LayoutParams notifyParams = (WindowManager.LayoutParams)notifyLayout.getLayoutParams();

            this.notify.getLayoutParams().height = bubbleLayout.getHeight();
            this.notify.getLayoutParams().width = WindowManager.LayoutParams.WRAP_CONTENT;

            if(!isLeft) {
                notifyParams.x = bubbleParams.x + bubbleView.getWidth();
                notifyParams.y = bubbleParams.y;

                this.notify.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            } else {
                notifyParams.x = bubbleParams.x - notifyText.getWidth();
                notifyParams.y = bubbleParams.y;

                this.notify.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            }

            notifyLayout.setVisibility(View.VISIBLE);
            windowManager.updateViewLayout(notifyLayout, notifyParams);

            textHandler.postDelayed(textRunnable, 5000);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if(location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, (com.google.android.gms.location.LocationListener) this);
        } else {
            handleNewLocation(location);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            // Start an Activity that tries to resolve the error
            //connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            Toast.makeText(this, "Google play service connect...", Toast.LENGTH_SHORT).show();
        } else {
            Log.e("Google play service", "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    private void handleNewLocation(Location location) {
        //
        final Location mLocation = location;
        new Thread(new Runnable() {
            @Override
            public void run() {Log.e("handleNewLocation", mLocation.toString());
                Geocoder geocoder = new Geocoder(BubbleInScreen.this, Locale.getDefault());
                try {
                    address = geocoder.getFromLocation(mLocation.getLatitude(), mLocation.getLongitude(), 1);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
}
