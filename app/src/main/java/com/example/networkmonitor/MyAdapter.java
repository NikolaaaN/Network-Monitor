package com.example.networkmonitor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
     List<String> monthly_usage;
     List<String> daily_usage;
     List<String> percentage;
     List<Drawable> slike;
     Context context;

    public MyAdapter(Context ct,List<String> monthly_usage,List<String> daily_usage,List<String> percentage,List<Drawable> slike){
        context=ct;
        this.monthly_usage=monthly_usage;
        this.daily_usage=daily_usage;
        this.percentage=percentage;
        this.slike=slike;

    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater=LayoutInflater.from(context);
        View view= inflater.inflate(R.layout.recycler_view_row,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.txtMonth.setText(monthly_usage.get(position));
        holder.txtPercentage.setText(percentage.get(position));
        holder.icon.setImageDrawable(slike.get(position));
    }

    @Override
    public int getItemCount() {
        return monthly_usage.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView txtMonth;
        TextView txtDay;
        ImageView icon;
        TextView txtPercentage;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMonth=itemView.findViewById(R.id.txtMonthly);
            txtPercentage=itemView.findViewById(R.id.txtPercentage);
            icon=itemView.findViewById(R.id.rowIcon);

        }
    }
}