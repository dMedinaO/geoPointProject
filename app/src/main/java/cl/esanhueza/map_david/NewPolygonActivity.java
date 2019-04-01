package cl.esanhueza.map_david;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
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
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

import cl.esanhueza.map_david.Util.CustomPaintingSurface;
import cl.esanhueza.map_david.Util.LockableScrollView;

import static cl.esanhueza.map_david.R.id.fragment_map;

public class NewPolygonActivity extends QuestionActivity implements DrawLineFragment.EventListener{
    MapView mMap;
    IMapController mMapController;

    public void setContent(){
        DrawLineFragment drawLineFragment = (DrawLineFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_map);
        drawLineFragment.paint.setMode(CustomPaintingSurface.Mode.Polygon);
        LockableScrollView lockableScrollView = findViewById(R.id.scroll);
        if (lockableScrollView != null){
            lockableScrollView.setScrollingEnabled(true);
        }

        mMap = (MapView) findViewById(R.id.mapview);
        mMap.setTileSource(TileSourceFactory.MAPNIK); // MAPNIK = Openstreemap

        mMap.setBuiltInZoomControls(true);

        // initialize controller
        mMapController = mMap.getController();
        mMapController.setZoom(17.0);

        // set zoom and start position
        if (question.getOptions().containsKey("zoom")){
            mMapController.setZoom(Double.valueOf(question.getOptions().get("zoom").toString()));
        }

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

        if(response != null){
            try {
                JSONArray array = response.getJSONArray("value");
                ArrayList<GeoPoint> points = new ArrayList<>();
                for (int i=0; i<array.length(); i++){
                    JSONObject obj = array.getJSONObject(i);
                    GeoPoint p = new GeoPoint(obj.getDouble("latitude"), obj.getDouble("longitude"));
                    //mPointsOverlay.addPoint(p, mMap);
                    points.add(p);
                }
                drawLineFragment.setPoints(points);
                mMap.invalidate();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public int getContentViewId(){
        return R.layout.activity_route;
    };

    public void cleanMap(View view){
        //this.mPointsOverlay.cleanPoints(mMap);
        DrawLineFragment drawLineFragment = (DrawLineFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_map);
        drawLineFragment.clearPoints();
    }

    public void saveResponse(View view){
        DrawLineFragment drawLineFragment = (DrawLineFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_map);

        Intent intent = new Intent();
        List<GeoPoint> points = drawLineFragment.getPoints();

        if (points == null || points.size() < 2){
            Toast.makeText(this, R.string.text_question_route_min_points, Toast.LENGTH_LONG).show();
            return;
        }

        JSONArray array = new JSONArray();

        for (GeoPoint p : points){
            JSONObject obj = new JSONObject();
            try {
                obj.put("latitude", p.getLatitude());
                obj.put("longitude", p.getLongitude());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            array.put(obj);
        }

        JSONObject response = new JSONObject();
        try {
            response.put("value", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        intent.setData(Uri.parse(response.toString()));
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


    @Override
    public void toggleScroll(boolean toggle) {
        LockableScrollView lockableScrollView = findViewById(R.id.scroll);
        if (lockableScrollView != null){
            lockableScrollView.setScrollingEnabled(toggle);
        }
    }

    class DrawLineOverlay extends MyLocationNewOverlay{
        Polyline figure;


        public DrawLineOverlay (MapView mapView) {
            super(mapView);
            figure = new Polyline();
            figure.getPaint().setARGB(200, 63,81,181);
            figure.getPaint().setStrokeCap(Paint.Cap.ROUND);
            mapView.getOverlayManager().add(figure);
        }

        public void cleanPoints(MapView map){
            figure.setPoints(new ArrayList<GeoPoint>());
            map.invalidate();
        }

        public void addPoint(GeoPoint pos, MapView map){
            figure.addPoint(pos);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e, MapView map) {
            Projection projection = map.getProjection();
            GeoPoint geoPoint = (GeoPoint) projection.fromPixels((int)e.getX(), (int)e.getY());
            figure.addPoint(geoPoint);
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
