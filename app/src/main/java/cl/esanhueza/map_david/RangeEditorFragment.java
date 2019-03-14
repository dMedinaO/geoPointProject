package cl.esanhueza.map_david;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cl.esanhueza.map_david.models.Choice;
import cl.esanhueza.map_david.models.Item;

public class RangeEditorFragment extends QuestionEditorFragment {
    private RangeEditorFragment.ItemAdapter mAdapter;
    private ListView listView;
    @Override
    public boolean validate(){
        return true;
    }

    public Map<String, Object> getOptions(){
        mAdapter.saveEdit();
        Map<String, Object> map = new HashMap<String, Object>();
        TextView min = getView().findViewById(R.id.text_min);
        TextView max = getView().findViewById(R.id.text_max);
        map.put("min", min.getText().toString());
        map.put("max", max.getText().toString());

        JSONArray itemsArray = new JSONArray();
        for (int i=0; i<mAdapter.getCount(); i++){
            Item item = mAdapter.getItem(i);
            JSONObject jsonObject = new JSONObject();

            try {
                jsonObject.put("text", item.getLabel());
                itemsArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        map.put("items", itemsArray);
        return map;
    }

    @Override
    public void updateQuestionContent(View view){
        super.updateQuestionContent(view);
        if (options.containsKey("min")){
            TextView min = view.findViewById(R.id.text_min);
            min.setText(options.get("min").toString());
        }

        if (options.containsKey("max")){
            TextView max = view.findViewById(R.id.text_max);
            max.setText(options.get("max").toString());
        }

        JSONArray itemsArray = null;

        if (options.containsKey("items")){
            itemsArray = (JSONArray) options.get("items");
        }
        else{
            itemsArray = new JSONArray();
        }

        for (int i=0; i<itemsArray.length(); i++){
            JSONObject obj = null;
            try {
                obj = itemsArray.getJSONObject(i);
                mAdapter.add(new Item(obj.getString("text")));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        mAdapter.notifyDataSetChanged();
        setListViewHeightBasedOnChildren(listView);
    }

    public void addItem(){
        mAdapter.saveEdit();
        mAdapter.add(new Item("Texto " + String.valueOf(mAdapter.getCount() + 1)));
        setListViewHeightBasedOnChildren(listView);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new RangeEditorFragment.ItemAdapter(getContext(), new ArrayList<Item>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view =  inflater.inflate(R.layout.fragment_range, container, false);
        ImageButton btnMinusMin = view.findViewById(R.id.btn_minus_min);
        btnMinusMin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView min = view.findViewById(R.id.text_min);
                int value = Integer.valueOf(min.getText().toString()) - 1;
                min.setText(String.valueOf(value));
            }
        });
        ImageButton btnPlusMin = view.findViewById(R.id.btn_plus_min);
        btnPlusMin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView min = view.findViewById(R.id.text_min);
                TextView max = view.findViewById(R.id.text_max);
                int value = Integer.valueOf(min.getText().toString());
                int maxValue = Integer.valueOf(max.getText().toString());
                if (value + 1 < maxValue){
                    min.setText(String.valueOf(value + 1));
                }
            }
        });

        ImageButton btnMinuxMax = view.findViewById(R.id.btn_minus_max);
        btnMinuxMax.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView min = view.findViewById(R.id.text_min);
                TextView max = view.findViewById(R.id.text_max);
                int minValue = Integer.valueOf(min.getText().toString());
                int maxValue = Integer.valueOf(max.getText().toString());
                if (maxValue - 1 > minValue){
                    max.setText(String.valueOf(maxValue - 1));
                }
            }
        });
        ImageButton btnPlusMax = view.findViewById(R.id.btn_plus_max);
        btnPlusMax.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView max = view.findViewById(R.id.text_max);
                int value = Integer.valueOf(max.getText().toString()) + 1;
                max.setText(String.valueOf(value));
            }
        });

        listView = view.findViewById(R.id.item_list);
        listView.setAdapter(mAdapter);

        Button btn = view.findViewById(R.id.btn_add_item);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItem();
            }
        });
        updateQuestionContent(view);
        return view;
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


    public class ItemAdapter extends ArrayAdapter<Item> {
        private Context mContext;
        private ArrayList<Item> itemList;

        public ItemAdapter(@NonNull Context context, ArrayList<Item> list) {
            super(context, 0 );
            mContext = context;
            itemList = list;
        }

        @Override
        public void remove(Item item){
            saveEdit();
            super.remove(item);
            setListViewHeightBasedOnChildren(listView);
        }

        public void saveEdit(){
            for (int i=0; i<this.getCount(); i++){
                TextView label = listView.findViewWithTag("label" + String.valueOf(i));
                if (label != null){
                    this.getItem(i).setLabel(label.getText().toString());
                }
            }
        }

        @NonNull
        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            if(listItem == null)
                listItem = LayoutInflater.from(mContext).inflate(R.layout.listview_item, parent,false);

            final Item currentChoice = this.getItem(position);

            TextView number = (TextView) listItem.findViewById(R.id.item_index);
            number.setText(String.valueOf(position + 1));


            final EditText labelView = (EditText) listItem.findViewById(R.id.text);
            labelView.setText(currentChoice.getLabel());
            labelView.setTag("label" + String.valueOf(position));

            ImageButton removeBtn = (ImageButton) listItem.findViewById(R.id.btn_remove_item);
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
