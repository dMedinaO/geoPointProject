package cl.esanhueza.map_david;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cl.esanhueza.map_david.models.Choice;
import cl.esanhueza.map_david.storage.PollFileStorageHelper;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChoicesEditorFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ChoicesEditorFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChoicesEditorFragment extends QuestionEditorFragment {
    private ListView listView;
    private ChoiceAdapter mAdapter;
    private ArrayList<Choice> choicesList = new ArrayList<Choice>();
    private OnFragmentInteractionListener mListener;

    @Override
    public boolean validate(){
        TextView maxView = this.getView().findViewById(R.id.max_choices);
        int max = Integer.valueOf(maxView.getText().toString());
        String error = null;
        if (mAdapter.getCount() == 0){
            error = "Una pregunta con alternativas debe tener dos alternativas como minimo.\n";
        }
        if (mAdapter.getCount() < max){
            error = "El número de opciones seleccionables debe ser menor o igual al número de alternativas ingresadas.\n";
        }
        if (error != null){
            new AlertDialog.Builder(getContext())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Error")
                    .setMessage(error)
                    .setPositiveButton("Aceptar", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
            return false;
        }
        return true;
    }

    public Map<String, Object> getOptions(){
        mAdapter.notifyDataSetChanged();
        Map<String, Object> map = new HashMap<String, Object>();
        JSONArray alternativesArray = new JSONArray();

        for (int i=0; i<mAdapter.getCount(); i++){
            Choice choice = mAdapter.getItem(i);
            JSONObject jsonObject = new JSONObject();

            try {
                jsonObject.put("label", choice.getLabel());
                jsonObject.put("value", choice.getValue());
                alternativesArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        TextView maxView = this.getView().findViewById(R.id.max_choices);
        map.put("alternatives", alternativesArray);
        map.put("max", String.valueOf(maxView.getText()));
        return map;
    }

    public ChoicesEditorFragment() {
        // Required empty public constructor
    }

    public void addChoice(){
        choicesList.clear();
        for (int i=0; i<mAdapter.getCount(); i++){
            TextView valueView = listView.findViewWithTag("value" + String.valueOf(i));
            TextView labelView = listView.findViewWithTag("label" + String.valueOf(i));
            choicesList.add(new Choice(valueView.getText().toString(), labelView.getText().toString()));
        }
        mAdapter.clear();
        choicesList.add(new Choice("Valor", "Texto" + String.valueOf(choicesList.size())));
        mAdapter.addAll(choicesList);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ChoicesEditorFragment.
     */

    public static ChoicesEditorFragment newInstance() {
        ChoicesEditorFragment fragment = new ChoicesEditorFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new ChoiceAdapter(getContext(), new ArrayList<Choice>(choicesList));

        JSONArray alternativesArray = null;
        if (options.containsKey("alternatives")){
            alternativesArray = (JSONArray) options.get("alternatives");
        }
        else{
            alternativesArray = new JSONArray();
        }

        for (int i=0; i<alternativesArray.length(); i++){
            JSONObject obj = null;
            try {
                obj = alternativesArray.getJSONObject(i);
                mAdapter.add(new Choice(obj.getString("value"), obj.getString("label")));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_choices_list, container, false);

        listView = view.findViewById(R.id.choices_list);
        listView.setAdapter(mAdapter);

        Button btn = view.findViewById(R.id.btn_add_choice);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addChoice();
            }
        });
        return view;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public class ChoiceAdapter extends ArrayAdapter<Choice> {
        private Context mContext;
        private ArrayList<Choice> itemList;

        public ChoiceAdapter(@NonNull Context context, ArrayList<Choice> list) {
            super(context, 0 , list);
            mContext = context;
            itemList = list;
        }

        @NonNull
        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Log.d("TST ENCUESTAS: ", choicesList.get(position).toString());
            View listItem = convertView;
            if(listItem == null)
                listItem = LayoutInflater.from(mContext).inflate(R.layout.listview_choice_item, parent,false);

            final Choice currentChoice = itemList.get(position);

            TextView number = (TextView) listItem.findViewById(R.id.choice_number);
            number.setText(String.valueOf(position));

            final TextView valueView = (TextView) listItem.findViewById(R.id.value);
            valueView.setTag("value" + String.valueOf(position));
            valueView.setText(currentChoice.getValue());

            final TextView labelView = (TextView) listItem.findViewById(R.id.label);
            labelView.setTag("label" + String.valueOf(position));
            labelView.setText(currentChoice.getLabel());

            ImageButton removeBtn = (ImageButton) listItem.findViewById(R.id.btn_remove_choice);
            removeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("TST ENCUESTAS: ", "Position: " + String.valueOf(position));
                    Log.d("TST ENCUESTAS: ", "Eliminando: " + currentChoice.toString());
                    remove(currentChoice);
                    notifyDataSetChanged();
                }
            });
            return listItem;
        }
    }
}


