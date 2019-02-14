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

public class RouteActivity extends QuestionActivity {
    MapView mMap;
    IMapController mMapController;
    ArrayList<OverlayItem> mItems;
    DrawLineOverlay mPointsOverlay;
    ItemizedOverlayWithFocus<OverlayItem> mDrawingOverlay;

    public void setContent(){
        mMap = (MapView) findViewById(R.id.map);
        mMap.setTileSource(TileSourceFactory.MAPNIK); // MAPNIK = Openstreemap

        mMap.setBuiltInZoomControls(true);
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

        //mLineOverlay = new DrawLineOverlay(mMap);
        mPointsOverlay = new DrawLineOverlay(mMap);
        mMap.getOverlays().add(mPointsOverlay);
    }

    public int getContentViewId(){
        return R.layout.activity_route;
    };

    public void cleanMap(View view){
        this.mPointsOverlay.cleanPoints(mMap);
    }

    public void saveResponse(View view){
        Intent intent = new Intent();
        List<GeoPoint> points = mPointsOverlay.figure.getPoints();
        if (points.size() < 2){
            Toast.makeText(this, "Un poligono debe tener al menos 2 puntos.", Toast.LENGTH_LONG).show();
            return;
        }

        String text = "";
        for (GeoPoint p : points){
            text += "{ \"latitude\":" + String.valueOf(p.getLatitude());
            text += ", \"longitude\":" + String.valueOf(p.getLongitude()) + "},";
        }
        if (points.size() > 0){
            text = text.substring(0, text.length()-1);
        }
        text = "[" + text + "]";
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

    class DrawLineOverlay extends MyLocationNewOverlay{
        List<GeoPoint> points = new ArrayList<>();
        Polyline figure;


        public DrawLineOverlay (MapView mapView) {
            super(mapView);
            figure = new Polyline();
            figure.getPaint().setARGB(200, 63,81,181);
            figure.getPaint().setStrokeCap(Paint.Cap.ROUND);
            mapView.getOverlayManager().add(figure);
        }

        public void cleanPoints(MapView map){
            figure.getPoints().clear();
            map.invalidate();
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
