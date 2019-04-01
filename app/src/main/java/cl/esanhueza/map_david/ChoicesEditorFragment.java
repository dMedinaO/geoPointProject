package cl.esanhueza.map_david;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cl.esanhueza.map_david.models.Choice;


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

    @Override
    public boolean validate(){
        CheckBox checkBox = this.getView().findViewById(R.id.checkbox_limit);
        TextView maxView = this.getView().findViewById(R.id.max_choices);
        int max = -1;
        if (checkBox.isChecked()){
            max = Integer.valueOf(maxView.getText().toString());
        }
        String error = null;

        if (mAdapter.getCount() == 0){
            error = getString(R.string.text_question_choice_editor_at_least_two_alternatives) + "\n";
        }
        if (mAdapter.getCount() < max){
            error = getString(R.string.text_question_choice_editor_over_max_choices) + "\n";
        }
        if (error != null){
            new AlertDialog.Builder(getContext())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Error")
                    .setMessage(error)
                    .setPositiveButton(R.string.label_button_accept, new DialogInterface.OnClickListener()
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
        mAdapter.saveEdit();
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

        CheckBox checkBox = this.getView().findViewById(R.id.checkbox_limit);
        if (checkBox.isChecked()){
            map.put("max", String.valueOf(maxView.getText()));
        }
        return map;
    }

    public ChoicesEditorFragment() {
        // Required empty public constructor
    }

    public void addChoice(){
        mAdapter.saveEdit();
        mAdapter.add(new Choice(getString(R.string.default_choice_value), getString(R.string.default_choice_label) + String.valueOf(mAdapter.getCount())));
        setListViewHeightBasedOnChildren(listView);
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
        mAdapter = new ChoiceAdapter(getContext(), new ArrayList<Choice>());
    }

    @Override
    public void updateQuestionContent(View view){
        super.updateQuestionContent(view);
        JSONArray alternativesArray = null;
        if (options.containsKey("max")){
            CheckBox checkBox = view.findViewById(R.id.checkbox_limit);
            checkBox.setChecked(true);
            TextView textView = view.findViewById(R.id.max_choices);
            textView.setText(String.valueOf(options.get("max")));
        }
        else{
            TextView textView = view.findViewById(R.id.max_choices);
            textView.setText(String.valueOf(1));
        }

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
        setListViewHeightBasedOnChildren(listView);
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

        final LinearLayout linearLayout = view.findViewById(R.id.choices_limit_group);
        CheckBox checkBox = view.findViewById(R.id.checkbox_limit);
        checkBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    linearLayout.setVisibility(View.VISIBLE);
                }
                else{

                    linearLayout.setVisibility(View.GONE);
                }
            }
        });

        updateQuestionContent(view);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }


    public class ChoiceAdapter extends ArrayAdapter<Choice> {
        private Context mContext;
        private ArrayList<Choice> itemList;

        public ChoiceAdapter(@NonNull Context context, ArrayList<Choice> list) {
            super(context, 0 , list);
            mContext = context;
            itemList = list;
        }

        @Override
        public void remove(Choice choice){
            saveEdit();
            super.remove(choice);
            setListViewHeightBasedOnChildren(listView);
        }

        public void saveEdit(){
            for (int i=0; i<this.getCount(); i++){
                TextView label = listView.findViewWithTag("label" + String.valueOf(i));
                if(label != null){
                    this.getItem(i).setLabel(label.getText().toString());
                }

                TextView value = listView.findViewWithTag("value" + String.valueOf(i));
                if(value != null){
                    this.getItem(i).setValue(value.getText().toString());
                }
            }
        }

        @NonNull
        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
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
                    remove(currentChoice);
                    notifyDataSetChanged();
                }
            });
            return listItem;
        }
    }
}


