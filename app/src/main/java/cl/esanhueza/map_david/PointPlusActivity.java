package cl.esanhueza.map_david;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cl.esanhueza.map_david.models.Question;

public class PointPlusActivity extends QuestionActivity {
    Map<String, Class> questionTypeList = new HashMap<String, Class>();
    MapView mMap;
    IMapController mMapController;
    ArrayList<OverlayItem> mItems;
    DrawPointOverlay mPointsOverlay;
    ItemizedOverlayWithFocus<OverlayItem> mDrawingOverlay;
    String response;
    GeoPoint pointSelected;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setContent(){
        mMap = (MapView) findViewById(R.id.map);
        mMap.setTileSource(TileSourceFactory.MAPNIK); // MAPNIK = Openstreemap

        mMap.setBuiltInZoomControls(false);
        mMap.setMultiTouchControls(true);

        // initialize controller
        mMapController = mMap.getController();
        // set zoom and start position
        mMapController.setZoom(13.0);
        GeoPoint startPoint = new GeoPoint(-33.447487, -70.673676);
        mMapController.setCenter(startPoint);

        // initialize array to store icons.
        mItems = new ArrayList<OverlayItem>();

        mItems.add(new OverlayItem("Title", "Description", new GeoPoint(-33.447487, -70.673676)));

        mDrawingOverlay = new ItemizedOverlayWithFocus<OverlayItem>(this, mItems,
            new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                @Override
                public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                    //do something
                    return true;
                }
                @Override
                public boolean onItemLongPress(final int index, final OverlayItem item) {
                    return false;
                }
            });

        mPointsOverlay = new DrawPointOverlay(mMap);
        mMap.getOverlays().add(mPointsOverlay);

        ((LinearLayout) findViewById(R.id.toolbar)).removeViewAt(0);
    }

    public int getContentViewId(){
        return R.layout.activity_point;
    };

    public void cleanMap(View view){
        this.mPointsOverlay.cleanPoints(mMap);
    }

    public void saveResponse(View view){
        _saveResponse();
    }

    public void _saveResponse(){
        if (pointSelected == null){
            Toast.makeText(this, "Debe seleccionar un punto.", Toast.LENGTH_LONG).show();
            return;
        }

        if (response == ""){
            Toast.makeText(this, "Debe responder la pregunta antes de continuar. Seleccionar un punto.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent();
        String text = "";
        text += "{\"response\": " + response + ",";
        text += "\"latitude\":" + String.valueOf(pointSelected.getLatitude());
        text += ",\"longitude\":" + String.valueOf(pointSelected.getLongitude()) + "}";
        intent.setData(Uri.parse(text));
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    public void openQuestion(){
        JSONObject obj = (JSONObject) question.getOptions().get("question");
        try {
            obj.put("n", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Question q = new Question(obj);
        // se revisa si el tipo de pregunta esta previamente implementado.
        if (!PollActivity.QUESTION_TYPE_LIST.containsKey(q.getType())){
            Toast.makeText(this, "El tipo de pregunta seleccionado no está implementado.", Toast.LENGTH_LONG).show();
            return;
        }

        java.lang.Class activity = (Class) PollActivity.QUESTION_TYPE_LIST.get(q.getType());
        Intent intent = new Intent(this, activity);
        intent.putExtra("QUESTION", q.toJson());
        startActivityForResult(intent, q.getNumber());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String returnedResult = data.getData().toString();
            Log.d("Result", "Pregunta contestada, respuesta: " + returnedResult);
            response = returnedResult;
            question.setState("Contestada");
            Toast.makeText(this, returnedResult, Toast.LENGTH_LONG).show();
        }
        else{
            pointSelected = null;
            Log.d("Result", "Pregunta cerrada sin terminar de contestar.");
        }
    }

    public void onResume(){
        super.onResume();
        mMap.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    public void onPause(){
        super.onPause();
        mMap.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    class DrawPointOverlay extends MyLocationNewOverlay{


        public DrawPointOverlay (MapView mapView) {
            super(mapView);
            HashMap<String, Object> opts = question.getOptions();
            JSONArray arr;
            try {
                arr = (JSONArray) opts.get("points");
                Log.d("JSONArray: ", String.valueOf(arr));
                for (int i = 0; i < arr.length(); i++){
                    Marker m = addMarker(mapView);
                    JSONObject obj = (JSONObject) arr.get(i);
                    GeoPoint gp = new GeoPoint(obj.getDouble("latitude"), obj.getDouble("longitude"));
                    m.setPosition(gp);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public Marker addMarker(MapView mapview){
            Marker marker = new Marker(mapview);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker, MapView mapView) {
                    pointSelected = marker.getPosition();
                    openQuestion();
                    return false;
                }
            });
            mapview.getOverlayManager().add(marker);
            return marker;
        }

        public void cleanPoints(MapView map){
            map.getOverlayManager().clear();
            map.invalidate();
        }

        @Override
        public boolean onLongPress(MotionEvent e, MapView map) {
            //this.cleanPoints(map);
            return true;
        }
    }
}
