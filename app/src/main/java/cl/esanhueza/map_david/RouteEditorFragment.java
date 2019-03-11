package cl.esanhueza.map_david;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Debug;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

public class RouteEditorFragment extends QuestionEditorFragment {
    MapView mMap;
    IMapController mMapController;
    ArrayList<OverlayItem> mItems;
    ItemizedIconOverlay<OverlayItem> mDrawingOverlay;
    DrawPointOverlay mPointsOverlay;

    @Override
    public boolean validate(){
        String error = null;
        if (mPointsOverlay.marker == null){
            error = "Debe seleccionar el punto en donde se centrará el mapa mientras se contesta la pregunta.";
        }

        if (error != null){
            new AlertDialog.Builder(getContext())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Error")
                    .setMessage(error)
                    .setPositiveButton("Aceptar", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
            return false;
        }
        return true;
    }


    public Map<String, Object> getOptions(){
        Map<String, Object> map = new HashMap<String, Object>();
        JSONObject centerAt = new JSONObject();
        if(mPointsOverlay.marker != null){
            try {
                centerAt.put("latitude", String.valueOf(mPointsOverlay.marker.getPosition().getLatitude()));
                centerAt.put("longitude", String.valueOf(mPointsOverlay.marker.getPosition().getLongitude()));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            map.put("center", centerAt);
        }
        map.put("zoom", String.valueOf(mMap.getZoomLevelDouble()));
        return map;
    }


    @Override
    public void updateQuestionContent(View view){
        super.updateQuestionContent(view);
        if (options.containsKey("center")){
            try {
                JSONObject center = new JSONObject(options.get("center").toString());
                GeoPoint point = new GeoPoint(center.getDouble("latitude"), center.getDouble("longitude"));
                mPointsOverlay.addMarker(mMap, point);
                mMapController.animateTo(point);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (options.containsKey("zoom")){
            mMapController.setZoom(Double.valueOf(options.get("zoom").toString()));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_route, null);

        mMap = (MapView) view.findViewById(R.id.map);
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

        mDrawingOverlay = new ItemizedOverlayWithFocus<OverlayItem>(getContext(), mItems,
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

        updateQuestionContent(view);
        return view;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*
        if (requestCode == PICKFILE_REQUEST_CODE){
            if (resultCode == Activity.RESULT_OK){
                TextView textView = getView().findViewById(R.id.text_image_attached);
                uriImage = data.getData().toString();
                textView.setText(uriImage);
                ImageView imageView = getView().findViewById(R.id.image_attached);
                imageView.setImageURI(Uri.parse(uriImage));
                imageView.setVisibility(View.VISIBLE);
            }
        }
        */
    }

    class DrawPointOverlay extends MyLocationNewOverlay {
        GeoPoint point;
        Marker marker;


        public DrawPointOverlay(MapView mapView) {
            super(mapView);
        }

        public void cleanPoints(MapView map){
            map.invalidate();
        }

        public void addMarker(MapView mapview, GeoPoint position){
            if (marker == null){
                marker = new Marker(mapview);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapview.getOverlayManager().add(marker);
            }
            marker.setPosition(position);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e, MapView mapview) {
            Projection projection = mapview.getProjection();
            point = (GeoPoint) projection.fromPixels((int)e.getX(), (int)e.getY());
            addMarker(mapview, point);

            mMapController.animateTo(point);
            mapview.invalidate();
            return true;
        }

        @Override
        public boolean onLongPress(MotionEvent e, MapView map) {
            return true;
        }
    }
}
