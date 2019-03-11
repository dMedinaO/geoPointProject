package cl.esanhueza.map_david;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;

import cl.esanhueza.map_david.models.Question;

public abstract class QuestionActivity extends CustomActivity {
    int contentView;
    Question question;
    JSONObject response = null;
    static final int PERMISSION_REQUEST = 100;
    public GeoPoint currentPosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(this.getContentViewId());

        Intent intent = getIntent();
        String message = intent.getStringExtra("QUESTION");
        if (intent.hasExtra("POSITION")){
            try {
                JSONObject positionObj = new JSONObject(intent.getStringExtra("POSITION"));
                currentPosition = new GeoPoint(positionObj.getDouble("latitude"), positionObj.getDouble("longitude"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            question = new Question(new JSONObject(message));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (question == null) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }

        if (intent.hasExtra("RESPONSE")){
            try {
                response = new JSONObject(intent.getStringExtra("RESPONSE"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        ((TextView) findViewById(R.id.questiontitle)).setText(question.getTitle());
        ((TextView) findViewById(R.id.questiondescription)).setText(question.getDescription());
        setContent();
    }

    public abstract int getContentViewId();

    public abstract void setContent();

    public abstract void saveResponse(View view);

}
