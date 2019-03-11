package cl.esanhueza.map_david;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cl.esanhueza.map_david.models.Question;

public class PointPlusEditorFragment extends QuestionEditorFragment {
    MapView mMap;
    IMapController mMapController;
    DrawPointOverlay mPointsOverlay;

    ArrayList<String> types = new ArrayList<>();
    ArrayList<String> keys = new ArrayList<>();
    QuestionEditorFragment currentFragment;
    Question secQuestion = new Question();


    @Override
    public boolean validate(){
        String error = "";
        currentFragment.validate();
        if (mPointsOverlay.markers.size() == 0){
            error = "Debe ingresar los puntos entre los cuales el encuestado seleccionara uno.";
        }
        if (error.length() > 0){
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
        JSONArray markersArray = new JSONArray();
        for (Marker marker : mPointsOverlay.markers){
            JSONObject point = new JSONObject();
            try {
                point.put("latitude", marker.getPosition().getLatitude());
                point.put("longitude", marker.getPosition().getLongitude());
                markersArray.put(point);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Map<String, Object> secOptions = currentFragment.getOptions();
        secQuestion.setOptions(secOptions);

        TextView secTitle = getView().findViewById(R.id.textview_secondary_question_title);
        TextView secDescription = getView().findViewById(R.id.textview_secondary_question_description);
        secQuestion.setTitle(secTitle.getText().toString());
        secQuestion.setDescription(secDescription.getText().toString());

        map.put("points", markersArray);
        try {
            map.put("question", new JSONObject(secQuestion.toJson()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return map;
    }


    @Override
    public void updateQuestionContent(View view){
        super.updateQuestionContent(view);
        if (options.containsKey("points")){
            try {
                JSONArray jsonArray = (JSONArray) options.get("points");
                for (int i=0; i<jsonArray.length(); i++){
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    GeoPoint point = new GeoPoint(jsonObject.getDouble("latitude"), jsonObject.getDouble("longitude"));
                    mPointsOverlay.addMarker(mMap, point);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (options.containsKey("question")){
            try {
                JSONObject obj = new JSONObject(options.get("question").toString());
                secQuestion = new Question(obj);

                TextView secTitle = view.findViewById(R.id.textview_secondary_question_title);
                TextView secDescription = view.findViewById(R.id.textview_secondary_question_description);

                secTitle.setText(secQuestion.getTitle());
                secDescription.setText(secQuestion.getDescription());

                Spinner typeSpinner = (Spinner) view.findViewById(R.id.spinner_question_type);
                typeSpinner.setSelection(keys.indexOf(secQuestion.getType()));

                setFragment(secQuestion.getType());
                currentFragment.setOptions(secQuestion.getOptions());
            } catch (JSONException e) {
                e.printStackTrace();
            }
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
        View view =  inflater.inflate(R.layout.fragment_point_plus, null);

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

        mPointsOverlay = new DrawPointOverlay(mMap);
        mMap.getOverlays().add(mPointsOverlay);


        final Set keysSet = PollEditorActivity.QUESTION_TYPE_LIST.keySet();
        Set entriesSet = PollEditorActivity.QUESTION_TYPE_LIST.entrySet();
        keys = new ArrayList<String>(keysSet);

        Object[] entries = entriesSet.toArray();

        ArrayList<String> supportedQuestionTypes = new ArrayList<>(Arrays.asList(new String[]{
                "text",
                "range",
                "choice"
        }));
        for (Object entry : entries){
            Map.Entry map = (Map.Entry<String,String>) entry;
            if (supportedQuestionTypes.contains(map.getKey())){
                types.add((String) map.getValue());
            }
            else{
                keys.remove(map.getKey().toString());
            }
        }

        Spinner typeSpinner = (Spinner) view.findViewById(R.id.spinner_question_type);
        typeSpinner.setAdapter(new ArrayAdapter<String>(
                getContext(),
                R.layout.spinner_question_type_item,
                types
        ));

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id){
                setFragment(keys.get(position));
                secQuestion.setType(keys.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent){}
        });

        updateQuestionContent(view);
        return view;
    }

    // cambia el fragmento del formulario, dependiendo del tipo de pregunta
    public void setFragment(String type){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (!fragmentManager.getFragments().isEmpty()) {
            for (Fragment f : fragmentManager.getFragments()) {
                fragmentTransaction.remove(f);
            }
        }
        Log.d("TST ENCUESTAS: ", "TIPO DE PREGUNTA: " + type);
        switch (type){
            case "choice":
                currentFragment = new ChoicesEditorFragment();
                break;
            case "text":
                currentFragment = new TextEditorFragment();
                break;
            case "range":
                currentFragment = new RangeEditorFragment();
                break;
            default:
                currentFragment = new BlankFragment();
                break;
        }

        currentFragment.setOptions(secQuestion.getOptions());

        fragmentTransaction.add(R.id.secondary_fragment_container, (Fragment) currentFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    class DrawPointOverlay extends MyLocationNewOverlay {
        ArrayList<Marker> markers;


        public DrawPointOverlay(MapView mapView) {
            super(mapView);
            markers = new ArrayList<>();
        }

        public void cleanPoints(MapView map){
            map.invalidate();
        }

        public void addMarker(MapView mapview, final GeoPoint position){
            Marker marker = new Marker(mapview);

            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setPosition(position);
            mapview.getOverlayManager().add(marker);
            marker.setPosition(position);
            marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(final Marker marker, final MapView mapView) {
                    new AlertDialog.Builder(getContext())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("¿Desea eliminar el punto seleccionado?")
                        .setMessage("Esta accion no puede revertirse.")
                        .setNegativeButton("Cancelar", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setPositiveButton("Eliminar", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            markers.remove(marker);
                            mapView.getOverlayManager().remove(marker);
                            mapView.invalidate();
                            }
                        })
                        .show();
                    return false;
                }
            });
            this.markers.add(marker);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e, MapView mapview){
            Projection projection = mapview.getProjection();
            GeoPoint position = (GeoPoint) projection.fromPixels((int)e.getX(), (int)e.getY());
            addMarker(mapview, position);
            mapview.invalidate();
            return true;
        }

        @Override
        public boolean onLongPress(MotionEvent e, MapView map) {
            return true;
        }
    }
}
