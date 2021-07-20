package com.example.annotamobile.ui.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.annotamobile.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SearchResultAdapter extends ArrayAdapter<SearchResult>  {

    private Context mContext;
    int mResource;

    public SearchResultAdapter(@NonNull @NotNull Context context, int resource, @NonNull @NotNull ArrayList<SearchResult> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        //get SearchResult info
        String name = getItem(position).getName();
        String date_time = getItem(position).getDate_time();
        String cat1 = getItem(position).getCat1();
        String cat2 = getItem(position).getCat2();
        String cat3 = getItem(position).getCat3();
        Bitmap icon = getItem(position).getIcon();

        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource, parent, false);

        ImageView iconView = (ImageView) convertView.findViewById(R.id.icon);
        TextView nameView = (TextView) convertView.findViewById(R.id.item_name);
        TextView dateView = (TextView) convertView.findViewById(R.id.item_date);
        TextView catView = (TextView) convertView.findViewById(R.id.item_cat_list);

        iconView.setImageBitmap(icon);
        nameView.setText(name);
        dateView.setText(date_time);
        catView.setText(cat1 + "\n" + cat2 + "\n" + cat3);

        return convertView;
    }
}
