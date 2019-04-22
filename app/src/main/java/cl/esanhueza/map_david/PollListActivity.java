package cl.esanhueza.map_david;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
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
import cl.esanhueza.map_david.storage.PersonContract;
import cl.esanhueza.map_david.storage.PollFileStorageHelper;
import cl.esanhueza.map_david.storage.ResponseContract;
import cl.esanhueza.map_david.storage.ResponseDbHelper;

public class PollListActivity extends CustomActivity implements PollListFragment.OnPollSelectedListener{
    ViewGroup mListLayout;
    ViewGroup mDetailsLayout;
    Menu mMenu;
    PollListFragment pollListFragment;
    PollDetailsFragment pollDetailsFragment;
    ResponseDbHelper mDbHelper;

    static final String TAG = "PollListActivity";

    static final int EDIT_POLL = 300;
    static final int CREATE_POLL = 302;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_poll_list);
        setTitle(R.string.app_name);
        mDbHelper = new ResponseDbHelper(this);


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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.mMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_poll_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete_responses:
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.text_poll_delete_responses)
                        .setMessage(R.string.text_poll_delete_responses_more)
                        .setNegativeButton(R.string.label_button_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setPositiveButton(R.string.label_button_accept, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                                Poll poll = pollDetailsFragment.poll;
                                db.delete(PersonContract.PersonEntry.TABLE_NAME,
                                        PersonContract.PersonEntry.COLUMN_NAME_POLL_ID+ "= ?",
                                        new String[]{poll.getId()}
                                );
                                db.delete(ResponseContract.ResponseEntry.TABLE_NAME,
                                        ResponseContract.ResponseEntry.COLUMN_NAME_POLL_ID + "= ?",
                                        new String[]{poll.getId()}
                                );
                                db.close();
                                onPollSelected(poll);
                            }
                        })
                        .show();
                return true;
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
                    pollListFragment.loadList();
                    break;
                case EDIT_POLL:
                    pollListFragment.loadList();
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

        pollDetailsFragment = PollDetailsFragment.newInstance(poll);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(target.getId(), pollDetailsFragment,
                PollDetailsFragment.class.getName());

        // Commit the transaction
        fragmentTransaction.commit();

        MenuItem item = mMenu.findItem(R.id.action_delete_responses);
        item.setVisible(true);
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
            pollDetailsFragment = null;
            MenuItem item = mMenu.findItem(R.id.action_delete_responses);
            item.setVisible(false);
        }
        else{
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        if (mDbHelper != null){
            mDbHelper.close();
        }

        super.onDestroy();
    }
}
