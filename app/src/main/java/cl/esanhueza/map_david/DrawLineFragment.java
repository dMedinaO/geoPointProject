package cl.esanhueza.map_david;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.util.ArrayList;

import cl.esanhueza.map_david.Util.BaseSampleFragment;
import cl.esanhueza.map_david.Util.CustomPaintingSurface;


/**
 * created on 1/13/2017.
 *
 * @author Alex O'Ree
 */

public class DrawLineFragment extends BaseSampleFragment {
    CustomPaintingSurface paint;
    private EventListener listener;

    @Override
    public String getSampleTitle() {
        return "";
    }


    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        if(activity instanceof EventListener) {
            listener = (EventListener)activity;
        } else {
            // Throw an error!
        }
    }

    public void clearPoints(){
        paint.clear();
    }

    public ArrayList<GeoPoint> getPoints(){
        return paint.getPoints();
    }

    public void setPoints(ArrayList<GeoPoint> points){
        paint.setPoints(points);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.layout_drawlines, null);
        mMapView = v.findViewById(R.id.mapview);
        RotationGestureOverlay mRotationGestureOverlay = new RotationGestureOverlay(mMapView);
        mRotationGestureOverlay.setEnabled(true);
        mMapView.setMultiTouchControls(false);


        mMapView.getOverlayManager().add(mRotationGestureOverlay);
        paint = v.findViewById(R.id.paintingSurface);
        paint.init(mMapView);


        ToggleButton btn = v.findViewById(R.id.toggle_draw);
        btn.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked){
                    paint.setVisibility(View.GONE);
                    mMapView.setMultiTouchControls(true);
                    mMapView.setEnabled(true);
                    mMapView.setOnTouchListener(new View.OnTouchListener() {

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            return false;
                        }
                    });
                    mMapView.setFocusable(true);
                    listener.toggleScroll(true);
                }
                else{
                    paint.setVisibility(View.VISIBLE);
                    mMapView.setMultiTouchControls(false);
                    mMapView.setFocusable(false);
                    listener.toggleScroll(false);
                    mMapView.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            return true;
                        }
                    });
                }
            }
        });
        return v;
    }


    /**
     * Created by cblack on 8/15/13.
     */
    public interface EventListener {

        public void toggleScroll(boolean toggle);

    }
}









