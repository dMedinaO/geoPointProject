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
import org.osmdroid.views.Projection;
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

        GeoPoint startPoint = new GeoPoint(-33.447487, -70.673676);
        if(currentPosition != null){
            startPoint = currentPosition;
        }
        else if (question.getOptions().containsKey("center")){
            try {
                JSONObject centerJson = new JSONObject(question.getOptions().get("center").toString());
                startPoint.setLatitude(centerJson.getDouble("latitude"));
                startPoint.setLongitude(centerJson.getDouble("longitude"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        mMapController.setCenter(startPoint);

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
            mPointsOverlay.cleanPoints(mMap);
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
        Marker m;

        public DrawPointOverlay (MapView mapView) {
            super(mapView);
        }

        public void cleanPoints(MapView map){
            m.remove(map);
            m = null;
            pointSelected = null;
            map.invalidate();
        }

        public void addPoint(GeoPoint p){
            if(m!=null){
                m.remove(this.mMapView);
            }
            m = new Marker(this.mMapView);
            m.setPosition(p);
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            pointSelected = m.getPosition();
            this.mMapView.getOverlayManager().add(m);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e, MapView map) {
            Projection projection = map.getProjection();
            GeoPoint geoPoint = (GeoPoint) projection.fromPixels((int)e.getX(), (int)e.getY());
            addPoint(geoPoint);
            openQuestion();
            map.invalidate();
            return true;
        }

        @Override
        public boolean onLongPress(MotionEvent e, MapView map) {
            this.cleanPoints(map);
            return true;
        }
    }
}
