package eu.le_tian.iConsoleOS.ui;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import eu.le_tian.iConsoleOS.R;
import eu.le_tian.iConsoleOS.data.ExerciseProfile;


public class ExerciseProfileAdapter extends RecyclerView.Adapter<ExerciseProfileAdapter.ExerciseProfileViewHolder> {

    String TAG = "ExerciseProfileAdapter";
    private SelectionTracker<Long> selectionTracker;
    private ExerciseProfileFragment mFragment;
    private Resources res;
    private final LayoutInflater mInflater;
    private List<ExerciseProfile> mExerciseProfiles;

    public void setSelectionTracker(SelectionTracker<Long> selectionTracker) {
        this.selectionTracker = selectionTracker;
    }

    static class Details extends ItemDetailsLookup.ItemDetails<Long> {
        long ID;
        long position;

        Details() {
        }

        @Override
        public int getPosition() {
            Log.d("DetailsClass", "getPosition: " + position);
            return (int) position;
        }

        @Nullable
        @Override
        public Long getSelectionKey() {
            Log.d("DetailsClass", "getSelectionKey " + position);
            return position;
        }
    }

    static class DetailsLookup extends ItemDetailsLookup<Long> {

        private RecyclerView recyclerView;

        DetailsLookup(RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
        }

        @Nullable
        @Override
        public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
            View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
            Log.d("DetailsLookup", "first step");
            if (view != null) {
                Log.d("DetailsLookup", "second step");
                RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(view);
                if (viewHolder instanceof ExerciseProfileViewHolder) {
                    Log.d("DetailsLookup", "third step");
                    final ExerciseProfileViewHolder exerciseProfileViewHolder = (ExerciseProfileViewHolder) viewHolder;
                    return exerciseProfileViewHolder.getItemDetails();
                }
            }
            return null;
        }
    }

    class ExerciseProfileViewHolder extends RecyclerView.ViewHolder {
        private final TextView exerciseProfileItemView;
        private final TextView exerciseProfileItemID;
        private Details details;

        private ExerciseProfileViewHolder(View itemView) {
            super(itemView);
            exerciseProfileItemView = itemView.findViewById(R.id.textViewExerciseProfileName);
            exerciseProfileItemID = itemView.findViewById(R.id.textViewExerciseProfileID);
            details = new Details();
            Log.v(TAG, "init adapter");
        }

        void bind(final ExerciseProfile exerciseProfile, int position) {
            details.position = position;
            exerciseProfileItemView.setText(exerciseProfile.getExerciseProfileName());
            exerciseProfileItemID.setText(Long.toString(exerciseProfile.getExerciseProfileID()));
            exerciseProfileItemView.setActivated(ExerciseProfileAdapter.this.selectionTracker.isSelected(details.getSelectionKey()));
            Log.d(TAG, "exerciseProfile:" + exerciseProfile.getExerciseProfileName() + ", position " + position + "is activated : " + ExerciseProfileAdapter.this.selectionTracker.isSelected(details.getSelectionKey()));
        }

        Details getItemDetails() {
            Log.d("TAG", "getItemDetails " + details.position + " : " + ExerciseProfileAdapter.this.selectionTracker.isSelected(details.getSelectionKey()));
            return details;
        }
    }


    ExerciseProfileAdapter(Context context, ExerciseProfileFragment fragment) {
        mInflater = LayoutInflater.from(context);
        this.mFragment = fragment;
        this.res = fragment.getResources();
        setHasStableIds(true);
    }

    @Override
    public ExerciseProfileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.v(TAG, "before init viewholder");
        View itemView = mInflater.inflate(R.layout.recyclerview_exerciseprofileitem, parent, false);
        Log.v(TAG, "init viewholder");
        return new ExerciseProfileViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseProfileViewHolder holder, int position) {
        if (mExerciseProfiles != null) {
            Log.d(TAG, "found smtg in db");
            holder.bind(mExerciseProfiles.get(position), position);
        } else {
            // Covers the case of data not being ready yet.
            Log.d(TAG, "found nothing in db");
            holder.exerciseProfileItemView.setText("No Items found");
        }
    }

    void setExerciseProfiles(List<ExerciseProfile> exerciseProfiles) {
        mExerciseProfiles = exerciseProfiles;
        notifyDataSetChanged();
    }

    public ExerciseProfile getExerciseProfileForPosition(long mPosition) {
        ExerciseProfile returnValue = null;
        returnValue = mExerciseProfiles.get((int) mPosition);
        return returnValue;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        if (mExerciseProfiles != null) {
            return mExerciseProfiles.size();
        } else return 0;
    }
}
