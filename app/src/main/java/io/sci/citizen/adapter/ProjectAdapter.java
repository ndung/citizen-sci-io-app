package io.sci.citizen.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import io.sci.citizen.R;
import io.sci.citizen.model.Project;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(Project project);
    }

    private final ProjectAdapter.OnItemClickListener listener;

    Activity activity;
    private List<Project> list;

    public ProjectAdapter(Activity activity, ProjectAdapter.OnItemClickListener listener){
        this.list = new ArrayList<>();
        this.activity = activity;
        this.listener = listener;
    }

    public void clear(){
        list.clear();
        notifyDataSetChanged();
    }

    public void add(Project project, int position) {
        list.add(position, project);
        notifyItemInserted(position);
    }

    @NonNull
    @Override
    public ProjectAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_project, parent, false);
        return new ProjectAdapter.ViewHolder(view);
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Override
    public void onBindViewHolder(@NonNull ProjectAdapter.ViewHolder holder, int position) {
        final Project project = list.get(position);

        holder.line1.setText(project.getName());
        holder.line2.setText(project.getDescription());
        holder.line3.setText("Created at: "+sdf.format(project.getCreatedAt()));
        Glide.with(activity).load(project.getIconUrl()).into(holder.imageView);
        holder.cardView.setOnClickListener(view -> {
            listener.onClick(project);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView line1, line2, line3;
        ImageView imageView;
        public ViewHolder(View view) {
            super(view);
            cardView = view.findViewById(R.id.cv);
            imageView = view.findViewById(R.id.imageView);
            line1 = view.findViewById(R.id.line1);
            line2 = view.findViewById(R.id.line2);
            line3 = view.findViewById(R.id.line3);
        }
    }
}
