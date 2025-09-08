package io.sci.citizen.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import io.sci.citizen.R;

public class SliderImageAdapter extends RecyclerView.Adapter<SliderImageAdapter.VH> {

    private final Context context;
    private final List<Uri> items = new ArrayList<>();

    public SliderImageAdapter(Context ctx) { this.context = ctx; }


    public ArrayList<Uri> getData() {
        return new ArrayList<>(items);
    }

    public void add(Uri uri){
        items.add(uri);
        notifyDataSetChanged();
    }

    public void removeAt(int position) {
        if (position < 0 || position >= items.size()) return;
        items.remove(position);
        notifyItemRemoved(position);
        // optional, helps pager rebind positions after removal
        notifyItemRangeChanged(position, items.size() - position);
    }

    public void removeAll(){
        items.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_slide_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Uri uri = items.get(position);
        Glide.with(context).load(uri).into(h.img);
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img);
        }
    }
}