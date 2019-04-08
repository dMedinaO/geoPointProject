package cl.esanhueza.map_david;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cl.esanhueza.map_david.models.Poll;
import cl.esanhueza.map_david.storage.PollFileStorageHelper;

public class PollListActivity extends CustomActivity implements PollListFragment.OnPollSelectedListener{
    ViewGroup mListLayout;
    ViewGroup mDetailsLayout;

    PollListFragment pollListFragment;

    static final String TAG = "PollListActivity";

    static final int EDIT_POLL = 300;
    static final int ANSWER_POLL = 301;
    static final int CREATE_POLL = 302;

    static final int TAKE_POLL = 401;
    final static int PICKFOLDER_REQUEST_CODE = 402;
    final static int WRITE_REQUEST_CODE = 403;
    final static int WRITE_REQUEST_CODE_CSV = 404;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_poll_list);
        setTitle(R.string.app_name);


        // If our layout has a container for the image selector fragment,
        // create and add it
        mListLayout = (ViewGroup) findViewById(R.id.activity_poll_list_list);
        if (mListLayout != null) {
            Log.i(TAG, "onCreate: adding PollListFragment to PollListActivity");

            // Add image selector fragment to the activity's container layout
            pollListFragment = new PollListFragment();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(mListLayout.getId(), pollListFragment,
                    PollListFragment.class.getName());

            // Commit the transaction
            fragmentTransaction.commit();
        }

        // If our layout has a container for the image rotator fragment, create
        // it and add it
        mDetailsLayout = (ViewGroup) findViewById(R.id.activity_poll_list_details);
        if (mDetailsLayout != null) {
            Log.i(TAG, "onCreate: adding PollDetailsFragment to PollListActivity");

            // Add image rotator fragment to the activity's container layout
            PollDetailsFragment detailsFragment = new PollDetailsFragment();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(mDetailsLayout.getId(), detailsFragment,
                    PollDetailsFragment.class.getName());

            // Commit the transaction
            fragmentTransaction.commit();
        }

        return;

        /*
        listView = findViewById(R.id.poll_list);
        mAdapter = new PollAdapter(this, list);
        listView.setAdapter(mAdapter);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
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
        */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_poll_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public void editPoll(Poll poll){
        Intent intent = new Intent(this, PollEditorActivity.class);
        intent.putExtra("POLL", poll.getPath());
        //intent.putExtra("POLL", poll.toJson());
        startActivityForResult(intent, EDIT_POLL);
    }

    public void createPoll(){
        Intent intent = new Intent(this, PollEditorActivity.class);
        startActivityForResult(intent, CREATE_POLL);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK){
            switch (requestCode){
                case CREATE_POLL:
                    break;
                case EDIT_POLL:
                    break;
                default:
                    break;
            }
        }
        else{

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof PollListFragment) {
            PollListFragment headlinesFragment = (PollListFragment) fragment;
            headlinesFragment.setOnPollSelectedListener(this);
        }
    }

    @Override
    public void onPollSelected(Poll poll) {
        ViewGroup target = mDetailsLayout != null ? mDetailsLayout : mListLayout;

        PollDetailsFragment detailsFragment = PollDetailsFragment.newInstance(poll);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(target.getId(), detailsFragment,
                PollDetailsFragment.class.getName());

        // Commit the transaction
        fragmentTransaction.commit();
    }

    @Override
    public void onPollEdit(Poll poll) {
        editPoll(poll);
    }

    @Override
    public void onPollCreate() {
        createPoll();
    }

    @Override
    public void onBackPressed(){
        if (getSupportFragmentManager().getFragments().get(0) instanceof PollDetailsFragment){
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(mListLayout.getId(), pollListFragment,
                    PollListFragment.class.getName());
            fragmentTransaction.commit();
        }
        else{
            super.onBackPressed();
        }
    }
}
