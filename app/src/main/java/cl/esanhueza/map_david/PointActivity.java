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
    ArrayList<OverlayItem> mItems;
    DrawPointOverlay mPointsOverlay;
    ItemizedOverlayWithFocus<OverlayItem> mDrawingOverlay;

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

        //mLineOverlay = new DrawLineOverlay(mMap);
        mPointsOverlay = new DrawPointOverlay(mMap);
        mMap.getOverlays().add(mPointsOverlay);

        if(response != null){
            try {
                JSONObject obj = response.getJSONObject("value");
                GeoPoint p = new GeoPoint(obj.getDouble("latitude"), obj.getDouble("longitude"));
                mPointsOverlay.figure.setPosition(p);
                mPointsOverlay.figure.setVisible(true);
                mPointsOverlay.figure.setEnabled(true);
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
        if (!mPointsOverlay.figure.isEnabled()){
            Toast.makeText(this, "Debe posicionar el marcador en un punto dentro del mapa.", Toast.LENGTH_LONG).show();
            return;
        }
        GeoPoint geoPoint= mPointsOverlay.figure.getPosition();
        if (geoPoint == null){
            Toast.makeText(this, "Debe posicionar el marcador en un punto dentro del mapa.", Toast.LENGTH_LONG).show();
            return;
        }

        String text = "";
        text += "{ \"latitude\":" + String.valueOf(geoPoint.getLatitude());
        text += ", \"longitude\":" + String.valueOf(geoPoint.getLongitude()) + "}";
        intent.setData(Uri.parse(text));
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
        Marker figure;

        public DrawPointOverlay (MapView mapView) {
            super(mapView);
            figure = new Marker(mapView);
            figure.setVisible(false);
            figure.setEnabled(false);
            figure.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            figure.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker, MapView mapView) {
                    return false;
                }
            });
            mapView.getOverlayManager().add(figure);
        }

        public void cleanPoints(MapView map){
            figure.setVisible(false);
            figure.setEnabled(false);
            map.invalidate();
        }

        @Override
        public boolean onDoubleTap(MotionEvent e, MapView map) {
            Projection projection = map.getProjection();
            GeoPoint geoPoint = (GeoPoint) projection.fromPixels((int)e.getX(), (int)e.getY());
            figure.setPosition(geoPoint);
            figure.setVisible(true);
            figure.setEnabled(true);
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
