package cl.esanhueza.map_david.storage;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import cl.esanhueza.map_david.models.Poll;

public class PollFileStorageHelper {
    static final String LOG_TAG = "TEST ENCUESTAS: ";
    static final String FOLDER_NAME = "ENCUESTAS";
    static final String RESPONSES_FOLDER_NAME = "RESPUESTAS";
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public static File getPublicPollStorageDir() {
        // Get the directory for the user's public pictures directory.
        Log.d("TST ENCUESTAS: ", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath());
        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath() + File.separator + FOLDER_NAME);

        if (!folder.mkdirs()) {
            Log.i(LOG_TAG, "Directory not created");
        }
        return folder;
    }

    public static File getPublicResponsesStorageDir() {
        Log.d("TST ENCUESTAS: ", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath());
        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath() + File.separator + FOLDER_NAME + File.separator + RESPONSES_FOLDER_NAME);

        if (!folder.mkdirs()) {
            Log.i(LOG_TAG, "Directory not created");
        }
        return folder;
    }

    static final public boolean deletePoll(Poll poll){
        File folder = PollFileStorageHelper.getPublicPollStorageDir();
        String title = poll.getTitle().replaceAll(" ", "_");
        File file = new File(folder.getAbsolutePath(),  title + "_" + poll.getId() + ".json");
        boolean result = file.delete();
        if (!result){
            file = new File(folder.getAbsolutePath(),  poll.getTitle() + "_" + poll.getId() + ".json");
            result = file.delete();
        }
        return result;
    }

    static final private boolean writeResponses(File file, Poll poll, JSONArray array){
        FileWriter writer = null;
        try {

            writer = new FileWriter(file);
            writer.append("{\n");
            writer.append("\"idEncuesta\":  \"" + poll.getId() + "\",\n");
            writer.append("\"respuestas\": [\n" );
            for(int i=0; i<array.length(); i++){
                JSONObject obj = array.getJSONObject(i);
                writer.append(obj.toString() + ",\n");
            }
            writer.append("]}");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return true;
    }

    static final public boolean saveResponses(Context context, Uri uriDestination, Poll poll, JSONArray array){
        Log.d("TST ENCUESATAS: ", array.toString());
        try {
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uriDestination, "w");
            FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
            JSONObject obj = new JSONObject();

            obj.put("idEncuesta", poll.getId());
            obj.put("respuestas", array);

            fos.write(obj.toString(2).getBytes());
            fos.close();
            pfd.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
        /*
        File file = new File(destinationFilePath);
        Log.d("SAVE RESPONSES", destinationFilePath);
        try {
            boolean created = file.createNewFile();
            Log.d("CREATED: ", String.valueOf(created));

            FileOutputStream fos = new FileOutputStream(file);
            fos.write("hello world".getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //writeResponses(file, poll, array);
        return file.getAbsolutePath();
        */
    }

    static final public String savePoll(Poll poll){
        try {
            JSONObject json = new JSONObject(poll.toJson());
            File folder = PollFileStorageHelper.getPublicPollStorageDir();
            String title = poll.getTitle().replaceAll(" ", "_");

            File file = new File(folder.getAbsolutePath(),  title + "_" + poll.getId() + ".json");
            if (!file.exists()){
                file.createNewFile();
                FileWriter writer = new FileWriter(file);
                writer.append(json.toString(2));
                writer.close();
            }
            else{
                Log.e(LOG_TAG, "Archivo json ya existe.");
            }

            return file.getAbsolutePath();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    static final public ArrayList<Poll> readPolls(){
        ArrayList<Poll> polls = new ArrayList<>();
        try {
            File folder = PollFileStorageHelper.getPublicPollStorageDir();

            File[] files = folder.listFiles();

            for (int i=0; i<files.length; i++){
                if (files[i].isDirectory()){
                    continue;
                }
                StringBuilder stringBuilder = new StringBuilder();
                String line = null;
                BufferedReader br = new BufferedReader(new FileReader(files[i]));

                while ((line = br.readLine()) != null) {
                    stringBuilder.append(line);
                }

                JSONObject json = new JSONObject(stringBuilder.toString());

                Poll poll = new Poll(json);
                poll.setPath(files[i].getAbsolutePath());
                polls.add(poll);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return polls;
    }

    static final public Poll readPoll(String filePath){
        try {
            File file = new File(filePath);
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            BufferedReader br = new BufferedReader(new FileReader(file));

            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
            }

            JSONObject json = new JSONObject(stringBuilder.toString());
            Poll poll = new Poll(json);
            poll.setPath(filePath);
            return poll;
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
