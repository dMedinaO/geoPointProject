package cl.esanhueza.map_david;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cl.esanhueza.map_david.models.Question;

public class PointPlusActivity extends QuestionActivity {
    Map<String, Class> questionTypeList = new HashMap<String, Class>();
    MapView mMap;
    IMapController mMapController;
    DrawPointOverlay mPointsOverlay;
    GeoPoint pointSelected;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setContent(){
        mMap = (MapView) findViewById(R.id.map);
        mMap.setTileSource(TileSourceFactory.MAPNIK); // MAPNIK = Openstreemap

        mMap.setBuiltInZoomControls(true);

        // initialize controller
        mMapController = mMap.getController();
        // set zoom and start position
        mMapController.setZoom(17.0);

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
            Toast.makeText(this, R.string.text_question_point_must_select_point, Toast.LENGTH_LONG).show();
            return;
        }

        if (response == null){
            Toast.makeText(this, R.string.text_question_point_must_anwser, Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent();
        try {
            response.put("latitude", pointSelected.getLatitude());
            response.put("longitude", pointSelected.getLongitude());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        intent.setData(Uri.parse(response.toString()));
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
        if (!PollActiveActivity.QUESTION_TYPE_LIST.containsKey(q.getType())){
            Toast.makeText(this, "El tipo de pregunta seleccionado no está implementado.", Toast.LENGTH_LONG).show();
            return;
        }

        java.lang.Class activity = (Class) PollActiveActivity.QUESTION_TYPE_LIST.get(q.getType());
        Intent intent = new Intent(this, activity);
        intent.putExtra("QUESTION", q.toJson());
        if (response != null){
            intent.putExtra("RESPONSE", response.toString());
        }
        startActivityForResult(intent, q.getNumber());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String returnedResult = data.getData().toString();
            try {
                response = new JSONObject(returnedResult);
                question.setState("Contestada");
                Toast.makeText(this, returnedResult, Toast.LENGTH_LONG).show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else{
            pointSelected = null;
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
            Map<String, Object> opts = question.getOptions();
            JSONArray arr;
            try {
                arr = (JSONArray) opts.get("points");
                for (int i = 0; i < arr.length(); i++){
                    Marker m = addMarker(mapView);
                    JSONObject obj = (JSONObject) arr.get(i);
                    GeoPoint gp = new GeoPoint(obj.getDouble("latitude"), obj.getDouble("longitude"));
                    m.setPosition(gp);
                }
                if (arr.length() > 0){
                    JSONObject obj = (JSONObject) arr.get(0);
                    GeoPoint gp = new GeoPoint(obj.getDouble("latitude"), obj.getDouble("longitude"));
                    mMapController.setCenter(gp);
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
