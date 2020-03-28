package info.tinyservice.gps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info.tinyservice.gps.model.Device;
import info.tinyservice.gps.model.Position;
import info.tinyservice.gps.model.User;
import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

public class MapsActivity extends SupportMapFragment implements OnMapReadyCallback {
    private static final String TAG = "MapsActivity";

    private static final int REQUEST_FINE_LOCATION = 1;
    private LocationManager locationManager;
    private boolean isGPSEnabled = false;
    Location location;

    public static final int REQUEST_DEVICE = 1;
    public static final int RESULT_SUCCESS = 1;

    private GoogleMap map;
    private Handler handler = new Handler();

    private Map<Long, Device> devices = new HashMap<>();
    private Map<Long, Position> positions = new HashMap<>();
    private Map<Long, Marker> markers = new HashMap<>();
    private Socket mSocket = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getMapAsync(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_devices:
                startActivityForResult(new Intent(getContext(), DevicesActivity.class), REQUEST_DEVICE);
                return true;
            case R.id.action_logout:
                PreferenceManager.getDefaultSharedPreferences(getContext())
                        .edit().putBoolean(MainApplication.PREFERENCE_AUTHENTICATED, false).apply();
                ((MainApplication) getActivity().getApplication()).removeService();
                getActivity().finish();
                startActivity(new Intent(getContext(), LoginActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_DEVICE && resultCode == RESULT_SUCCESS) {
            long deviceId = data.getLongExtra(DevicesFragment.EXTRA_DEVICE_ID, 0);
            Position position = positions.get(deviceId);
            if (position != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(position.getLatitude(), position.getLongitude()), 15));
                markers.get(deviceId).showInfoWindow();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                View view = getActivity().getLayoutInflater().inflate(R.layout.view_info, null);
                ((TextView) view.findViewById(R.id.title)).setText(marker.getTitle());
                ((TextView) view.findViewById(R.id.details)).setText(marker.getSnippet());
                return view;
            }
        });

        enableLocation();
        createWebSocket();
    }

    private void enableLocation() {

//        if (mayRequestLocation()) {
//            //noinspection MissingPermission
//            map.setMyLocationEnabled(true);
//
//            locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
//            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER); // getting GPS status
//
//            if (!isGPSEnabled) {
//                Toast.makeText(getContext(), "Activa el GPS en tu dispositivo para mostrar tu ubicacion en el mapa", Toast.LENGTH_LONG).show();
//            } else {
//                location = null;
//                if (locationManager != null) {
//                    //noinspection MissingPermission
//                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//                    Log.d(TAG, "" + location.getLatitude() + " " + location.getLongitude());
//                    if (location != null) {
//                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
//                                new LatLng(location.getLatitude(), location.getLongitude()), 15));
//                    }
//                }
//            }
//        }
    }

    private boolean mayRequestLocation() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
        return false;
    }

    private String formatDetails(Position position) {
        final MainApplication application = (MainApplication) getContext().getApplicationContext();
        final User user = application.getUser();

        SimpleDateFormat dateFormat;
        if (user.getTwelveHourFormat()) {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
        } else {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }

        String speedUnit = getString(R.string.user_kn);
        double factor = 1;
        if (user.getSpeedunit() != null) {
            switch (user.getSpeedunit()) {
                case "kmh":
                    speedUnit = getString(R.string.user_kmh);
                    factor = 1.852;
                    break;
                case "mph":
                    speedUnit = getString(R.string.user_mph);
                    factor = 1.15078;
                    break;
                default:
                    speedUnit = getString(R.string.user_kmh);
                    factor = 1.852;
                    break;
            }
        }

        double speed = position.getSpeed() * factor;
        return new StringBuilder()
                .append(getString(R.string.position_time)).append(": ")
                .append(dateFormat.format(position.getFixTime())).append('\n')
                .append(getString(R.string.position_address)).append(": ")
                .append(position.getAddress()).append('\n')
                .append(getString(R.string.position_speed)).append(": ")
                .append(String.format("%.1f", speed)).append(' ')
                .append(speedUnit).append('\n')
                .toString();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "Pause");
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket = null;
            Log.i(TAG, "Socket disconnect");
        }

    }

    private void reconnectWebSocket() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null) {
                    Log.i(TAG, "trying reconnect socket");
                    if(mSocket == null){
                        createWebSocket();
                    }
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "destroy");
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket = null;
        }
    }

    private void createWebSocket() {
        Log.i(TAG, "createWebsocket");
        final MainApplication application = (MainApplication) getActivity().getApplication();
        application.getServiceAsync(new MainApplication.GetServiceCallback() {
            @Override
            public void onServiceReady(final OkHttpClient client, final Retrofit retrofit, WebService service) {
                User user = application.getUser();
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(13.789416, -88.8671058), user.getZoom()));

                service.getDevices().enqueue(new WebServiceCallback<List<Device>>(getContext()) {

                    @Override
                    public void onSuccess(retrofit2.Response<List<Device>> response) {
                        for (Device device : response.body()) {
                            if (device != null) {
                                devices.put(device.getId(), device);
                            }
                        }

                        String ds = MainApplication.DEFAULT_SERVER;
                        HttpUrl url = HttpUrl.parse(ds + ":3000/");
                        final StringBuilder sb = new StringBuilder();
                        for (Cookie c : client.cookieJar().loadForRequest(url)) {
                            sb.append(c.name()).append("=").append(c.value()).append(";");
                        }

                        try {
                            String urlSocket = ds + ":3001/";
                            mSocket = IO.socket(urlSocket);
                            mSocket.io().on(Manager.EVENT_TRANSPORT, new Emitter.Listener() {
                                @Override
                                public void call(Object... args) {
                                    Transport transport = (Transport) args[0];
                                    transport.on(Transport.EVENT_REQUEST_HEADERS, new Emitter.Listener() {
                                        @Override
                                        public void call(Object... args) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, List<String>> headers = (Map<String, List<String>>) args[0];
                                            // modify request headers
                                            headers.put("Cookie", Arrays.asList(sb.toString()));
                                        }
                                    });
                                }
                            });

                            mSocket.on(Socket.EVENT_CONNECT, onConnect);
                            mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
                            mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
//                            mSocket.on(Socket.EVENT_DISCONNECT, onConnectError);
                            mSocket.on("positions", onNewPositions);
                            mSocket.connect();
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }

            @Override
            public boolean onFailure() {
                return false;
            }
        });
    }


    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "socket connected");
                    }
                });
            }
        }
    };


    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSocket = null;
                        reconnectWebSocket();
                    }
                });
            }
        }
    };

    private Emitter.Listener onNewPositions = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONArray data = (JSONArray) args[0];
                        handlePosition(data);
                    }
                });
            }
        }

        private Date getFixTime(Object date) {
            if (date instanceof String) {
                try {
                    long timestamp = Long.parseLong((String) date);
                    return new Date(timestamp);
                } catch (NumberFormatException e) {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                    try {
                        Date dateResult = formatter.parse(((String) date).replaceAll("Z$", "+0000"));
                        return dateResult;
                    } catch (ParseException e2) {
                        return null;
                    }
                }
            } else {
                return null;
            }
        }

        private void handlePosition(JSONArray posArray) {
            if (posArray != null) {
                for (int i = 0; i < posArray.length(); i++) {
                    JSONObject row = null;
                    Position position = new Position();
                    try {
                        row = posArray.getJSONObject(i);
                        position.setDeviceId(row.getLong("deviceid"));
                        position.setLatitude(row.getDouble("latitude"));
                        position.setLongitude(row.getDouble("longitude"));
                        position.setSpeed(row.getDouble("speed"));
                        position.setAddress(row.getString("address"));
                        position.setFixTime(getFixTime(row.get("fixtime")));
                    } catch (JSONException e) {
                        if (row == null) continue;
                    }

                    long deviceId = position.getDeviceId();

                    if (devices.containsKey(deviceId)) {
                        LatLng location = new LatLng(position.getLatitude(), position.getLongitude());
                        Marker marker = markers.get(deviceId);
                        if (marker == null) {
                            marker = map.addMarker(new MarkerOptions()
                                    .title(devices.get(deviceId).getName())
                                    .icon(getImage(devices.get(deviceId).getImage()))
                                    .position(location));
                            markers.put(deviceId, marker);
                        } else {
                            marker.setPosition(location);
                        }

                        marker.setSnippet(formatDetails(position));
                        positions.put(deviceId, position);
                    }
                }
            }
        }

        private BitmapDescriptor getImage(String image) {
            switch (image) {
                case "001.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v001);
                case "002.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v002);
                case "003.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v003);
                case "004.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v004);
                case "005.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v005);
                case "006.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v006);
                case "007.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v007);
                case "008.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v008);
                case "009.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v009);
                case "010.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v010);
                case "011.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v011);
                case "012.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v012);
                case "013.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v013);
                case "016.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v016);
                case "017.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v017);
                case "021.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v021);
                case "023.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v023);
                case "024.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v024);
                case "025.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v025);
                case "026.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v026);
                case "027.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v027);
                case "041.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v041);
                case "042.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v042);
                case "043.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v043);
                case "044.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v044);
                case "045.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v045);
                case "046.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v046);
                case "047.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v047);
                case "048.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v048);
                case "049.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v049);
                case "050.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v050);
                case "053.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v053);
                case "054.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v054);
                case "055.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v055);
                case "058.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v058);
                case "061.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v061);
                case "062.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v062);
                case "070.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.v070);
                case "Circulo.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.vcirculo);
                case "dump_truck.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.vdump_truck);
                case "tanker.png":
                    return BitmapDescriptorFactory.fromResource(R.drawable.vtanker);
                default:
                    return BitmapDescriptorFactory.fromResource(R.drawable.vcirculo);
            }
        }
    };
}