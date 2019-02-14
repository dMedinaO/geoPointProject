package cl.esanhueza.map_david;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;

import cl.esanhueza.map_david.models.Question;

public abstract class QuestionActivity extends AppCompatActivity {
    int contentView;
    Question question;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(this.getContentViewId());

        Intent intent = getIntent();
        String message = intent.getStringExtra("QUESTION");

        try {
            question = new Question(new JSONObject(message));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (question == null){
            setResult(Activity.RESULT_CANCELED);
            finish();
        }

        ((TextView)findViewById(R.id.questiontitle)).setText(question.getTitle());
        ((TextView)findViewById(R.id.questiondescription)).setText(question.getDescription());
        setContent();
    }

    public abstract int getContentViewId();

    public abstract void setContent();

    public abstract void saveResponse(View view);
}
