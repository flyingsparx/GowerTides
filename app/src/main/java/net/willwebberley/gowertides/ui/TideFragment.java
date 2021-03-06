/*
Copyright 2013 Will Webberley.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

The full text of the License is available in the root of this
project repository.
*/

package net.willwebberley.gowertides.ui;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import net.willwebberley.gowertides.R;
import net.willwebberley.gowertides.classes.Day;
import net.willwebberley.gowertides.classes.Tide;

import java.util.Calendar;

/*
 Class to represent the tidal information views in the horizontal scroll bar
 */
public class TideFragment extends RelativeLayout {

    private View layoutView;
    private Tide tide;
    private Day day;

    public TideFragment(Context context, Tide t, Day d){
        super(context);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutView = inflater.inflate(R.layout.tide_table, null);

        tide = t;
        day = d;
        updateUI();
    }

    public View getView(){
        return layoutView;
    }

    private void updateUI(){
        ((TextView)layoutView.findViewById(R.id.time)).setTextColor(Color.rgb(50, 50, 50));
        ((TextView)layoutView.findViewById(R.id.time)).setText(tide.getTimeString());

        if(day.isToday()){
            ((TextView)layoutView.findViewById(R.id.time_diff)).setVisibility(View.VISIBLE);
            String timeDifference = tide.getTimeDifference(Calendar.getInstance());
            boolean negative = false;
            if (timeDifference.startsWith("-")){
                negative = true;
            }

            if (negative){
                ((TextView)layoutView.findViewById(R.id.time_diff)).setTextColor(Color.rgb(168,0,0));
            }
            if(!negative){
                ((TextView)layoutView.findViewById(R.id.time_diff)).setTextColor(Color.rgb(0,168,0));
            }
            ((TextView)layoutView.findViewById(R.id.time_diff)).setText("("+timeDifference+")");
        }
        else{
            ((TextView)layoutView.findViewById(R.id.time_diff)).setVisibility(View.GONE);
        }

        if(tide.getType() == tide.LOW){
            ((ImageView)layoutView.findViewById(R.id.tide_icon)).setImageResource(R.drawable.low);
            ((TextView)layoutView.findViewById(R.id.type)).setText("LOW");
        }
        if(tide.getType() == tide.HIGH){
            ((ImageView)layoutView.findViewById(R.id.tide_icon)).setImageResource(R.drawable.high);
            ((TextView)layoutView.findViewById(R.id.type)).setText("HIGH");
        }
    }
}
