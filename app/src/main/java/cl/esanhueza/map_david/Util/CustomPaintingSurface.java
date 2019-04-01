package cl.esanhueza.map_david.Util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.PointL;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;
import org.osmdroid.views.overlay.milestones.MilestoneBitmapDisplayer;
import org.osmdroid.views.overlay.milestones.MilestoneManager;
import org.osmdroid.views.overlay.milestones.MilestonePathDisplayer;
import org.osmdroid.views.overlay.milestones.MilestonePixelDistanceLister;

import java.util.ArrayList;
import java.util.List;

/**
 * A very simple borrowed from Android's "Finger Page" example, modified to generate polylines that
 * are geopoint bound after finger up.
 * created on 1/13/2017.
 *
 * @author Alex O'Ree
 */

public class CustomPaintingSurface extends View {
    public void setMode(CustomPaintingSurface.Mode mode) {
        this.drawingMode=mode;
    }
    private CustomPaintingSurface.Mode drawingMode= CustomPaintingSurface.Mode.Polyline;
    private Point firstPoint = null;
    public enum Mode{
        Polyline,
        Polygon,
        PolygonHole
    }
    protected boolean withArrows=false;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private MapView map;
    private List<Point> pts = new ArrayList<>();
    private Paint mPaint;
    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;
    private Overlay lastOverlay;


    transient Polygon lastPolygon=null;


    public CustomPaintingSurface(Context context, AttributeSet attrs) {
        super(context,attrs);
        mPath = new Path();
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }


    @Override
    protected void onDraw(Canvas canvas) {

        mCanvas = new Canvas(mBitmap);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFFFF0000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);

        canvas.drawPath(mPath, mPaint);
    }
    public void init(MapView mapView) {
        map=mapView;
    }

    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }
    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
        }
    }
    public void clear(){
        if (lastOverlay != null){
            map.getOverlayManager().remove(lastOverlay);

        }
        if (lastPolygon != null){
            map.getOverlayManager().remove(lastPolygon);
        }

        firstPoint = null;
        lastOverlay = null;
        lastPolygon = null;
        map.invalidate();
    }

    public ArrayList<GeoPoint> getPoints(){
        if (drawingMode == Mode.Polyline){
            Polyline polyline = (Polyline) lastOverlay;
            return polyline.getPoints();
        }
        else if (drawingMode == Mode.Polygon){
            Polygon p = lastPolygon;
            return new ArrayList<>(p.getPoints());
        }
        return null;
    }

    public void setPoints(ArrayList<GeoPoint> points){
        if (drawingMode == Mode.Polyline){
            Polyline polyline = new Polyline();
            polyline.setPoints(points);
            map.getOverlayManager().add(polyline);
            lastOverlay = polyline;
        }
        else if (drawingMode == Mode.Polygon){
            Polygon p = new Polygon(map);
            p.setPoints(points);
            map.getOverlayManager().add(p);
            p.setFillColor(Color.argb(75, 255,0,0));
            lastPolygon = p;
        }
        map.invalidate();
    }


    private void touch_up() {
        mPath.lineTo(mX, mY);
        // commit the path to our offscreen
        mCanvas.drawPath(mPath, mPaint);
        // kill this so we don't double draw
        mPath.reset();
        boolean forceLineDrawing = false;
        if (map!=null){
            Projection projection = map.getProjection();
            ArrayList<GeoPoint> geoPoints = new ArrayList<>();
            final Point unrotatedPoint = new Point();
            for (int i=0; i < pts.size(); i++) {
                projection.unrotateAndScalePoint(pts.get(i).x, pts.get(i).y, unrotatedPoint);
                GeoPoint iGeoPoint = (GeoPoint) projection.fromPixels(unrotatedPoint.x, unrotatedPoint.y);
                geoPoints.add(iGeoPoint);
            }

            //geoPoints = PointReducer.reduceWithTolerance(geoPoints, 1.0);
            //TODO run the douglas pucker algorithm to reduce the points for performance reasons
            if (geoPoints.size() > 2) {
                if (firstPoint == null){
                    firstPoint = pts.get(0);
                }
                //only plot a line unless there's at least one item
                switch (drawingMode) {
                    case Polyline:
                        final int color = Color.BLACK;
                        Polyline line = new Polyline(map);
                        line.setColor(color);
                        line.setPoints(geoPoints);
                        line.getPaint().setStrokeCap(Paint.Cap.ROUND);

                        if (lastOverlay != null){
                            map.getOverlayManager().remove(lastOverlay);
                        }
                        lastOverlay = line;
                        map.getOverlayManager().add(lastOverlay);
                        lastPolygon=null;
                        break;
                    case Polygon:
                        Log.d("TST ENCUSETAS: ", "lastPolygon != NULL : " + String.valueOf(lastPolygon != null));
                        if (lastPolygon == null){
                            Point last = projection.unrotateAndScalePoint(pts.get(pts.size()-1).x, pts.get(pts.size()-1).y, null);
                            Log.d("TST ESCUNESTAS: ", String.valueOf(Math.pow(firstPoint.x - last.x, 2.0) + Math.pow(firstPoint.y - last.y, 2) > Math.pow(100.0, 2)));
                            if (Math.pow(firstPoint.x - last.x, 2.0) + Math.pow(firstPoint.y - last.y, 2) > Math.pow(100.0, 2)){
                                forceLineDrawing = true;
                            }

                            if (forceLineDrawing){
                                int colorPolygon = Color.BLACK;
                                Polyline linePolygon;
                                if (lastOverlay != null){
                                    linePolygon = (Polyline) lastOverlay;
                                    for (GeoPoint p : geoPoints){
                                        linePolygon.addPoint(p);
                                    }
                                }
                                else{
                                    linePolygon = new Polyline(map);
                                    linePolygon.setPoints(geoPoints);
                                }
                                linePolygon.setColor(colorPolygon);
                                linePolygon.getPaint().setStrokeCap(Paint.Cap.ROUND);

                                if (lastOverlay != null){
                                    map.getOverlayManager().remove(lastOverlay);
                                }
                                lastOverlay = linePolygon;
                                map.getOverlayManager().add(lastOverlay);
                                lastPolygon=null;
                            }
                            else{
                                List<GeoPoint> p = geoPoints;
                                if (lastOverlay != null){
                                    p = ((Polyline) lastOverlay).getPoints();
                                    p.addAll(geoPoints);
                                }
                                else if (lastPolygon != null){
                                    p = lastPolygon.getPoints();
                                    p.addAll(geoPoints);
                                }
                                Polygon polygon = new Polygon(map);
                                polygon.setFillColor(Color.argb(75, 255,0,0));
                                polygon.setPoints(p);
                                map.getOverlayManager().remove(lastPolygon);
                                lastPolygon=polygon;
                                map.getOverlayManager().add(lastPolygon);
                            }
                        }

                        break;
                    case PolygonHole:
                        if (lastPolygon!=null) {
                            List<List<GeoPoint>> holes = new ArrayList<>();
                            holes.add(geoPoints);
                            lastPolygon.setHoles(holes);
                        }
                        break;
                }

                map.invalidate();
            }
        }

        pts.clear();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        pts.add(new Point((int)x,(int)y));
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }

    public void destroy(){
        map=null;
        this.lastPolygon=null;
    }

}