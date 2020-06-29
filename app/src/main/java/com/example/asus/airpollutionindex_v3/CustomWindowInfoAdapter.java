package com.example.asus.airpollutionindex_v3;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class CustomWindowInfoAdapter implements GoogleMap.InfoWindowAdapter{

    private final View mWindow;
    private Context mContext;
    public CustomWindowInfoAdapter(Context context){
        mContext=context;
        mWindow= LayoutInflater.from(context).inflate(R.layout.custom_info_window,null);
    }

    private void rendorWindowText(Marker marker, View view){
        String title=marker.getTitle();
        TextView titleTV=view.findViewById(R.id.title);
        if(!title.equals("")){
            titleTV.setText(title);
        }
        String content =marker.getSnippet();
        TextView conTV=view.findViewById(R.id.content);
        if(content!=null){
                if(!content.equals("")){
            conTV.setText(content);
        }

        }

    }
    @Override
    public View getInfoWindow(Marker marker) {
        rendorWindowText(marker,mWindow);
        return mWindow;
    }

    @Override
    public View getInfoContents(Marker marker) {
        rendorWindowText(marker,mWindow);
        return mWindow;
    }
}
