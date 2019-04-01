package cl.esanhueza.map_david;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
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
import java.util.List;

public class PointActivity extends QuestionActivity {
    MapView mMap;
    IMapController mMapController;
    DrawPointOverlay mPointsOverlay;

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
        if (question.getOptions().containsKey("zoom")){
            mMapController.setZoom(Double.valueOf(question.getOptions().get("zoom").toString()));
        }

        mMapController.setCenter(startPoint);

        mPointsOverlay = new DrawPointOverlay(mMap);
        mMap.getOverlays().add(mPointsOverlay);

        if(response != null){
            try {
                JSONArray array = response.getJSONArray("value");
                for (int i=0; i<array.length(); i++){
                    JSONObject obj = array.getJSONObject(i);
                    GeoPoint p = new GeoPoint(obj.getDouble("latitude"), obj.getDouble("longitude"));
                    mPointsOverlay.addPoint(p);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public int getContentViewId(){
        return R.layout.activity_point;
    };

    public void cleanMap(View view){
        this.mPointsOverlay.cleanPoints(mMap);
    }

    public void saveResponse(View view){
        Intent intent = new Intent();


        if (mPointsOverlay.points.size() == 0){
            Toast.makeText(this, R.string.text_question_point_editor_must_add_points, Toast.LENGTH_LONG).show();
            return;
        }

        JSONObject result = new JSONObject();
        JSONArray array = new JSONArray();
        try {
            for (int i=0; i<mPointsOverlay.points.size(); i++){
                JSONObject obj = new JSONObject();
                obj.put("latitude", mPointsOverlay.points.get(i).getPosition().getLatitude());
                obj.put("longitude", mPointsOverlay.points.get(i).getPosition().getLongitude());
                array.put(obj);
            }
            result.put("value", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        intent.setData(Uri.parse(result.toString()));
        setResult(Activity.RESULT_OK, intent);
        finish();
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
        ArrayList<Marker> points;

        public DrawPointOverlay (MapView mapView) {
            super(mapView);
            points = new ArrayList<>();
        }

        public void deletePoint(Marker m){
            points.remove(m);
            m.remove(mMapView);
            mMap.invalidate();
        }

        public void cleanPoints(MapView map){
            for (Marker m : points){
                m.remove(map);
            }
            points.clear();
            map.invalidate();
        }

        public void addPoint(GeoPoint p){
            Marker m = new Marker(this.mMapView);
            m.setPosition(p);
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            m.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {

                @Override
                public boolean onMarkerClick(Marker marker, MapView mapView) {
                    deletePoint(marker);
                    return false;
                }
            });

            points.add(m);
            this.mMapView.getOverlayManager().add(m);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e, MapView map) {
            Projection projection = map.getProjection();
            GeoPoint geoPoint = (GeoPoint) projection.fromPixels((int)e.getX(), (int)e.getY());
            addPoint(geoPoint);
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
