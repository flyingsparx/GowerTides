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
import android.graphics.Matrix;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import net.willwebberley.gowertides.R;
import net.willwebberley.gowertides.classes.Day;
import net.willwebberley.gowertides.classes.Surf;

/*
 Class to represent the detailed surf information for the Surf Details Activity
 */
public class SurfDetailFragment extends RelativeLayout {

    private View layoutView;
    private Surf surf;

    public SurfDetailFragment(Context context, Surf s){
        super(context);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutView = inflater.inflate(R.layout.fragment_surfdetail, null);


        surf = s;
        updateUI();
    }

    public View getView(){
        return layoutView;
    }

    private void updateUI(){
        // Format the title textviews
        ((TextView)layoutView.findViewById(R.id.time)).setTextColor(Color.rgb(0, 150, 220));
        ((TextView)layoutView.findViewById(R.id.time)).setText(surf.hour+":00");

        // Update fields with information

        // Max and min surf heights
        if(surf.max_surf-surf.min_surf == 0){((TextView)layoutView.findViewById(R.id.surf_size)).setText(Html.fromHtml("<b>" + surf.max_surf + "</b> <i>ft</i>"));}
        else{((TextView)layoutView.findViewById(R.id.surf_size)).setText(Html.fromHtml("<b>"+surf.min_surf+"-"+surf.max_surf+"</b> <i>ft</i>"));}
        ((TextView)layoutView.findViewById(R.id.surf_size)).setTextColor(Color.rgb(70, 80, 70));

        ((TextView)layoutView.findViewById(R.id.abs_min_surf_size)).setText(Html.fromHtml(String.format("%.1f", surf.abs_min_surf) + "<i>ft</i>"));
        ((TextView)layoutView.findViewById(R.id.abs_max_surf_size)).setText(Html.fromHtml(String.format("%.1f", surf.abs_min_surf) + "<i>ft</i>"));


        // Swell direction
        ((TextView)layoutView.findViewById(R.id.swell_direction_text)).setText(Html.fromHtml("<b>"+surf.swell_direction+"</b>"));
        ((TextView)layoutView.findViewById(R.id.swell_direction_degrees)).setText(Html.fromHtml("("+surf.swell_angle+"&deg;)"));
        RotateAnimation rAnim = new RotateAnimation(0, 180+(float)surf.swell_angle, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rAnim.setDuration(0);
        rAnim.setFillEnabled(true);
        rAnim.setFillAfter(true);
        ((ImageView)layoutView.findViewById(R.id.swell_direction_arrow)).startAnimation(rAnim);

        // Swell period
        ((TextView)layoutView.findViewById(R.id.period_text)).setText(Html.fromHtml(surf.swell_period+"<i>s</i>"));

        // Swell height
        ((TextView)layoutView.findViewById(R.id.height_text)).setText(Html.fromHtml(surf.swell_height+"<i>ft</i>"));
    }
}