package com.example.networkmonitor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.MyViewHolder>{

    List<String> title;
    List<String> description;
    public Context context;


    public SettingsAdapter(Context ct,List<String> title,List<String> desc){
        context=ct;
        this.title=title;
        description=desc;
    }
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater=LayoutInflater.from(context);
        View view= inflater.inflate(R.layout.recycler_view_settings_row,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingsAdapter.MyViewHolder holder, int position) {
        holder.txtTitle.setText(title.get(position));
        holder.txtDesc.setText(description.get(position));
    }


    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle;
        TextView txtDesc;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle=itemView.findViewById(R.id.titleSettings);
            txtDesc=itemView.findViewById(R.id.settingsDesc);

        }
    }

    @Override
    public int getItemCount() {
        return title.size();
    }
}
