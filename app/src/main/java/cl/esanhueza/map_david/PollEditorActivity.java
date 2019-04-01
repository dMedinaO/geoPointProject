package cl.esanhueza.map_david;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;


import cl.esanhueza.map_david.models.Choice;
import cl.esanhueza.map_david.models.Poll;
import cl.esanhueza.map_david.models.Question;
import cl.esanhueza.map_david.storage.PersonContract;
import cl.esanhueza.map_david.storage.PollFileStorageHelper;
import cl.esanhueza.map_david.storage.ResponseContract;
import cl.esanhueza.map_david.storage.ResponseDbHelper;


public class PollEditorActivity extends CustomActivity {
    static final public int NEW_QUESTION_CODE = 200;
    static final public int EDIT_QUESTION_CODE = 100;
    static final public int PICKFILE_REQUEST_CODE = 300;
    QuestionAdapter mAdapter;
    ListView listView;
    Context mContext;
    Poll poll = new Poll();

    static final public HashMap QUESTION_TYPE_LIST = new HashMap<String, String>(){{
        put("choice", "Selección múltiple");
        put("text", "Ingresar texto");
        put("route", "Dibujar ruta");
        put("polygon", "Dibujar polígono");
        put("range", "Ingresar número entre rango");
        put("point", "Ingresar punto");
        put("point+", "Seleccionar punto y pregunta");

        // put("point", "Puntos en mapa");
    }};

    ArrayList<Question> questionsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll_editor);
        Intent intent = getIntent();

        if (intent.hasExtra("POLL")){
            try {
                poll = new Poll(new JSONObject(intent.getStringExtra("POLL")));
                TextView titleView = findViewById(R.id.polltitle);
                TextView descriptionView = findViewById(R.id.polldescription);
                titleView.setText(poll.getTitle());
                descriptionView.setText(poll.getDescription());
                questionsList.addAll(poll.getQuestions());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        mContext = getApplicationContext();

        mAdapter = new QuestionAdapter(this, questionsList);
        listView = findViewById(R.id.question_list);
        listView.setAdapter(mAdapter);

        // al mantener presionado una fila, solicitar confirmacion para eliminar fila
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(PollEditorActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.text_delete_question)
                        .setMessage(R.string.text_delete_question_more)
                        .setNegativeButton(R.string.label_button_cancel, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setPositiveButton(R.string.label_button_accept, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                questionsList.remove(position);
                                mAdapter.notifyDataSetChanged();
                            }
                        })
                        .show();
                return true;
            }
        });

        // al dar click sobre una fila, pasar pregunta al editor de preguntas
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(PollEditorActivity.this, QuestionEditorActivity.class);
                Question q = questionsList.get(position);
                intent.putExtra("QUESTION", q.toJson());
                startActivityForResult(intent, 0);
            }
        });

        Toolbar myToolbar = (Toolbar) findViewById(R.id.poll_toolbar);
        myToolbar.setTitle("Editor");
        setSupportActionBar(myToolbar);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_poll_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_import_poll_file:
                importPoll();
                return true;
            case R.id.action_delete_poll:
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.text_delete_poll)
                        .setMessage(R.string.text_delete_poll_more)
                        .setNegativeButton(R.string.label_button_cancel, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setPositiveButton(R.string.label_button_accept, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                PollFileStorageHelper.deletePoll(poll);
                                setResult(Activity.RESULT_OK);
                                finish();
                            }
                        })
                        .show();
                return true;

            case R.id.action_save_poll:
                // si tiene respuestas, se guarda como una nueva encuesta.
                if (pollHasAnswers(poll.getId())){
                    new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.text_save_poll)
                        .setMessage(R.string.text_save_poll_as_new_more)
                        .setNegativeButton(R.string.label_button_cancel, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setPositiveButton(R.string.label_button_accept, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                savePoll(true);
                            }
                        })
                        .show();
                }
                else{
                    new AlertDialog.Builder(this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(R.string.text_save_poll)
                            .setMessage(R.string.text_save_poll_update)
                            .setNegativeButton(R.string.label_button_cancel, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .setPositiveButton(R.string.label_button_accept, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    savePoll(false);
                                }
                            })
                            .show();
                }

                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public void savePoll(boolean newPoll){
        if (newPoll){
            String uuid = UUID.randomUUID().toString().replace("-", "");
            poll.setId(uuid);
        }
        else{
            PollFileStorageHelper.deletePoll(poll);
        }
        TextView titleView = findViewById(R.id.polltitle);
        TextView descriptionView = findViewById(R.id.polldescription);
        poll.setTitle(titleView.getText().toString());
        poll.setDescription(descriptionView.getText().toString());
        poll.getQuestions().clear();
        for (Question q : questionsList){
            poll.addQuestion(q);
        }

        String result = PollFileStorageHelper.savePoll(poll);
        if (result != null){
            refreshStorage(result);
        }
        setResult(Activity.RESULT_OK);
        finish();
    }

    private boolean pollHasAnswers(String pollId) {
        ResponseDbHelper mDbHelper = new ResponseDbHelper(getApplicationContext());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Insert the new row, returning the primary key value of the new row
        Cursor cursor = db.query(PersonContract.PersonEntry.TABLE_NAME,
                new String[]{
                        PersonContract.PersonEntry.COLUMN_NAME_PERSON_ID
                },PersonContract.PersonEntry.COLUMN_NAME_POLL_ID+ " = ?",
                new String[]{
                        poll.getId()
                }, null, null, null);
        int answers = cursor.getCount();
        db.close();
        return answers > 0;
    }

    private void importPoll(){
        Intent contentSelectionIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        contentSelectionIntent.setType("*/*");
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(contentSelectionIntent, PICKFILE_REQUEST_CODE);
    }

    private void loadImportedPoll(Uri uri){
        try {

            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();


            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            BufferedReader br = null;

            br = new BufferedReader(new FileReader(fileDescriptor));
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
            }

            JSONObject pollJson = new JSONObject(stringBuilder.toString());

            poll = new Poll(pollJson);

            TextView titleView = findViewById(R.id.polltitle);
            TextView descriptionView = findViewById(R.id.polldescription);
            titleView.setText(poll.getTitle());
            descriptionView.setText(poll.getDescription());

            questionsList.clear();
            questionsList.addAll(poll.getQuestions());
            mAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            Toast.makeText(this, "JSON no valido.", Toast.LENGTH_SHORT);
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Archivo no encontrado.", Toast.LENGTH_SHORT);
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void refreshStorage(String result){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            MediaScannerConnection.scanFile(this, new String[]{result}, null, new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                }
            });
        } else {
            this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://" + result)));
        }
    }

    public void handleNewQuestion(View view){
        Intent intent = new Intent(this, QuestionEditorActivity.class);
        startActivityForResult(intent, 0);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICKFILE_REQUEST_CODE){
            if (resultCode == Activity.RESULT_OK){
                loadImportedPoll(data.getData());
            }
            return;
        }
        if (resultCode == Activity.RESULT_OK) {
            String returnedResult = data.getData().toString();
            //Log.d("Result", "Pregunta contestada, respuesta: " + returnedResult);

            //Toast.makeText(this, returnedResult, Toast.LENGTH_LONG).show();

            JSONObject obj = new JSONObject();
            try {
                obj = new JSONObject(data.getData().toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Question q = new Question(obj);

            if (q.getNumber() >= 0){
                //Log.i("TST ENCUESTA: ", "Pregunta actualizada");
                questionsList.set(q.getNumber(), q);
            }
            else{
                //Log.i("TST ENCUESTA: ", "Pregunta agregada");
                questionsList.add(q);
            }
            mAdapter.notifyDataSetChanged();

        }
        else{
            //Log.d("Result", "Pregunta cerrada sin terminar de contestar.");
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.text_leave_poll_editor)
                .setMessage(R.string.text_leave_poll_editor_more)
                .setNegativeButton(R.string.label_button_cancel, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton(R.string.text_poll_leave_poll, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setResult(Activity.RESULT_CANCELED);
                        finish();
                    }
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public class QuestionAdapter extends ArrayAdapter<Question> {

        private Context mContext;
        private List<Question> questionList = new ArrayList<>();

        public QuestionAdapter(@NonNull Context context, ArrayList<Question> list) {
            super(context, 0 , list);
            mContext = context;
            questionList = list;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            if(listItem == null) {
                listItem = LayoutInflater.from(mContext).inflate(R.layout.listview_new_question, parent, false);
                Log.i("TST ENCUESTA: ", "INFLATE");
            }

            Log.i("TST ENCUESTA: ", String.valueOf(position));

            final Question currentQuestion = questionList.get(position);

            Log.i("TST ENCUESTA: ", currentQuestion.toJson());

            currentQuestion.setNumber(position);
            TextView number = (TextView) listItem.findViewById(R.id.question_number);
            number.setText(String.valueOf(position + 1));

            TextView titleView = (TextView) listItem.findViewById(R.id.question_title);
            titleView.setText(currentQuestion.getTitle());

            TextView typeView = (TextView) listItem.findViewById(R.id.question_type);
            typeView.setText(QUESTION_TYPE_LIST.get(currentQuestion.getType()).toString());

            Log.i("TST ENCUESTA: ", listView.toString());

            return listItem;
        }
    }
}
