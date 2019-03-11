package cl.esanhueza.map_david;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.osmdroid.config.Configuration;

import cl.esanhueza.map_david.storage.ResponseContract;
import cl.esanhueza.map_david.storage.ResponseDbHelper;

public class MainActivity extends CustomActivity {
    ResponseDbHelper mDbHelper;
    String POLL_ID = "0";
    int pollsCompleted = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_main);

        mDbHelper = new ResponseDbHelper(getApplicationContext());
        updateResponsesCount();

    }

    public void startPoll(View view){
        Intent intent = new Intent(this, PollActiveActivity.class);
        // aqui deberia pasarle la encuesta serializada.
        intent.putExtra("POLL_COUNT", pollsCompleted + 1);
        intent.putExtra("POLL_ID", POLL_ID);
        startActivity(intent);
    }

    public void createPoll(View view){
        Intent intent = new Intent(this, PollEditorActivity.class);
        startActivity(intent);
    }

    private int getResponsesCountInDb(){
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String[] projection = {
                BaseColumns._ID,
        };

        String selection = ResponseContract.ResponseEntry.COLUMN_NAME_POLL_ID+ " = ?";
        String[] selectionArgs = { POLL_ID };

        pollsCompleted = db.query(
                ResponseContract.ResponseEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                ResponseContract.ResponseEntry.COLUMN_NAME_PERSON_ID,
                null,
                null
        ).getCount();

        db.close();
        return pollsCompleted;
    }

    private void updateResponsesCount(){
        getResponsesCountInDb();
        TextView textView = findViewById(R.id.polls);
        textView.setText("Encuestados: " + String.valueOf(pollsCompleted));
    }

    public void clearResponsesInDb(View view){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(ResponseContract.ResponseEntry.TABLE_NAME, "1", null);
        db.close();
        updateResponsesCount();
    }

    public void onResume(){
        super.onResume();
        updateResponsesCount();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    }

    public void onPause(){
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
    }

    @Override
    protected void onDestroy() {
        mDbHelper.close();
        super.onDestroy();
    }
}
