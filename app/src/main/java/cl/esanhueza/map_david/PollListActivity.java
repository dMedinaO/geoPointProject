package cl.esanhueza.map_david;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cl.esanhueza.map_david.models.Poll;
import cl.esanhueza.map_david.models.Question;
import cl.esanhueza.map_david.storage.PollFileStorageHelper;

public class PollListActivity extends AppCompatActivity {
    static final int EDIT_POLL = 300;
    static final int ANSWER_POLL = 400;
    static final int CREATE_POLL = 500;

    PollListActivity.PollAdapter mAdapter;
    ListView listView;
    ArrayList<Poll> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle("Encuestas");

        listView = findViewById(R.id.poll_list);
        mAdapter = new PollAdapter(this, list);
        listView.setAdapter(mAdapter);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                editPoll(list.get(position));
                return true;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openPoll(list.get(position));
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createPoll();
            }
        });

        loadList();
    }

    public void openPoll(Poll poll){
        Intent intent = new Intent(this, PollActivity.class);
        intent.putExtra("POLL", poll.toJson());
        startActivityForResult(intent, ANSWER_POLL);
    }

    public void editPoll(Poll poll){
        Intent intent = new Intent(this, PollEditorActivity.class);
        Log.d("TST ENCUESTAS: ", poll.toJson());
        intent.putExtra("POLL", poll.toJson());
        startActivityForResult(intent, EDIT_POLL);
    }

    public void createPoll(){
        Intent intent = new Intent(this, PollEditorActivity.class);
        startActivityForResult(intent, CREATE_POLL);
    }

    public void loadList(){
        list.clear();
        list.addAll(PollFileStorageHelper.readPolls());
        mAdapter.notifyDataSetChanged();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK){
            switch (requestCode){
                case CREATE_POLL:
                    loadList();
                    break;
                case EDIT_POLL:
                    loadList();
                    break;
                default:
                    break;
            }
        }
        else{

        }
    }

    public class PollAdapter extends ArrayAdapter<Poll> {

        private Context mContext;
        private List<Poll> list = new ArrayList<>();

        public PollAdapter(@NonNull Context context, ArrayList<Poll> list) {
            super(context, 0 , list);
            mContext = context;
            this.list = list;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            if(listItem == null) {
                listItem = LayoutInflater.from(mContext).inflate(R.layout.listview_poll, parent, false);
            }

            final Poll current = list.get(position);
            TextView number = (TextView) listItem.findViewById(R.id.poll_number);
            number.setText(String.valueOf(position + 1));

            TextView titleView = (TextView) listItem.findViewById(R.id.poll_title);
            titleView.setText(current.getTitle());

            return listItem;
        }
    }

}
