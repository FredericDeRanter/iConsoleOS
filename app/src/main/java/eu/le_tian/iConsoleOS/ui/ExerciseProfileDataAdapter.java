package eu.le_tian.iConsoleOS.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.michaelmuenzer.android.scrollablennumberpicker.ScrollableNumberPicker;
import com.michaelmuenzer.android.scrollablennumberpicker.ScrollableNumberPickerListener;

import java.util.ArrayList;
import java.util.List;

import eu.le_tian.iConsoleOS.R;
import eu.le_tian.iConsoleOS.customtimepicker.CustomTimePicker2;
import eu.le_tian.iConsoleOS.data.ExerciseProfileData;


public class ExerciseProfileDataAdapter extends RecyclerView.Adapter<ExerciseProfileDataAdapter.ExerciseProfileDataViewHolder> {
    String TAG = "ExerciseProfileDataAdapter";
    private SelectionTracker<Long> selectionTracker;
    private ExerciseProfileFragment mFragment;
    private Resources res;
    private final LayoutInflater mInflater;
    private List<ExerciseProfileData> mExerciseProfileDataList;
    private List<ExerciseProfileData> exerciseProfileDataRemovedList;
    private ItemTouchHelper itemTouchHelper;
    private final OnStartDragListener mDragStartListener;

    private onValueChangeListener valueChangeListener = new onValueChangeListener() {
        @Override
        public void onValueChange(int position, int value, int type) {
            /**In this listener we need to update the  mExerciseProfileDataList with the changed values**/
            ExerciseProfileData original = mExerciseProfileDataList.get(position);
            switch (type) {
                case 1:
                    /**type: 1 is level*/
                    original.setRelativeLevel(value);
                    break;
                case 2:
                    /**type: 2 is duration*/
                    original.setDuration(value);
                    break;
                case 3:
                    original.setDataType(value);
                    /**type: 3 is spinner*/
                    break;
                case 4:
                    /** type: 4 is button up (value 2) or down (value 1)
                     ** first check if it can go up or down
                     **/
                    int maxPosition = mExerciseProfileDataList.size() - 1;
                    long originalPos = original.getSortOrder();
                    int originalDataType = original.getDataType();
                    Log.e(TAG, "maxPosition: " + maxPosition + " | originalPos: " + originalPos + " | position: " + position + " | type: " + original.getDataType() + " | order in List : " + mExerciseProfileDataList.indexOf(original));
                    if (value == 1) { //Down
                        if (position < maxPosition) {
                            //can move down
                            ExerciseProfileData exerciseProfileDataDown = mExerciseProfileDataList.get(position + 1);
                            //check if next position has same datatype (then can move)
                            if (exerciseProfileDataDown.getDataType() == originalDataType) {
                                //move items
                                exerciseProfileDataDown.setSortOrder(position);
                                original.setSortOrder(position + 1);
                                mExerciseProfileDataList.remove(position);
                                mExerciseProfileDataList.add(position + 1, original);
                                Log.e(TAG, "moving item down");
                                notifyItemRangeChanged(position, 2);
                                //notifyItemMoved(position, position+1);
                                //notifyItemMoved(position+1, position);

                            } else {
                                //can't move set SortOrder back to what it was
                                exerciseProfileDataDown.setSortOrder(position + 1);
                                original.setSortOrder(position);
                            }
                        }
                    } else { //Up
                        if (position > 1) {
                            //can move up
                            ExerciseProfileData exerciseProfileDataUp = mExerciseProfileDataList.get(position - 1);
                            if (exerciseProfileDataUp.getDataType() == originalDataType) {
                                //move items
                                exerciseProfileDataUp.setSortOrder(position);
                                original.setSortOrder(position - 1);
                                mExerciseProfileDataList.remove(position);
                                mExerciseProfileDataList.add(position - 1, original);
                                Log.e(TAG, "moving item up");
                                notifyItemRangeChanged(position - 1, 2);
                                //notifyItemMoved(position-1, position);
                            } else {
                                //can't move set SortOrder back to what it was
                                exerciseProfileDataUp.setSortOrder(position - 1);
                                original.setSortOrder(position);
                            }
                        }
                    }
                    break;
            }
        }
    };

    public void setSelectionTracker(SelectionTracker<Long> selectionTracker) {
        this.selectionTracker = selectionTracker;
    }

    static class Details extends ItemDetailsLookup.ItemDetails<Long> {
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
            if (view != null) {
                RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(view);
                if (viewHolder instanceof ExerciseProfileDataViewHolder) {
                    final ExerciseProfileDataViewHolder exerciseProfileDataViewHolder = (ExerciseProfileDataViewHolder) viewHolder;
                    return exerciseProfileDataViewHolder.getItemDetails();
                }
            }
            return null;
        }
    }

    class ExerciseProfileDataViewHolder extends RecyclerView.ViewHolder {
        private final Spinner spProfileDataType;
        private final TextView tvProfileDataID;
        private final TextView tvProfileDataSortOrder;
        private final TextView tvProfileDataParentID;
        private final CustomTimePicker2 tpProfileDataDuration;
        private final ScrollableNumberPicker npProfileDataLevel;
        private final ImageButton btUp;
        private final ImageButton btDown;
        //public final ImageView handle;

        private Details details;

        private ExerciseProfileDataViewHolder(View itemView, final onValueChangeListener listener) {
            super(itemView);
            spProfileDataType = itemView.findViewById(R.id.exPrDataType);
            tvProfileDataID = itemView.findViewById(R.id.exPrDataID);
            tvProfileDataSortOrder = itemView.findViewById(R.id.exPrDataOrder);
            tpProfileDataDuration = itemView.findViewById(R.id.exPrDataDuration);
            npProfileDataLevel = itemView.findViewById(R.id.exPrDataLevel);
            tvProfileDataParentID = itemView.findViewById(R.id.exPrDataParentID);
            btUp = itemView.findViewById(R.id.exPrDataButtonUp);
            btDown = itemView.findViewById(R.id.exPrDataButtonDown);
            //handle = itemView.findViewById(R.id.handle);
            details = new Details();
            Log.v(TAG, "init adapter");
            npProfileDataLevel.setListener(value -> listener.onValueChange(getLayoutPosition(), value, 1));
            tpProfileDataDuration.setOnValueChangeListener(totalSeconds -> listener.onValueChange(getLayoutPosition(), totalSeconds, 2));
            spProfileDataType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    listener.onValueChange(getLayoutPosition(), position, 3);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    //nothing happens
                }
            });
            btUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onValueChange(getLayoutPosition(), 2, 4);
                }
            });
            btDown.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onValueChange(getLayoutPosition(), 1, 4);
                }
            });

//            holder.handle.setOnTouchListener(new View.OnTouchListener() {
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//                    if (event.getActionMasked() ==
//                            MotionEvent.ACTION_DOWN) {
//                        mDragStartListener.onStartDrag(holder);
//                    }
//                    return false;
//                }
//            });
        }

        void bind(final ExerciseProfileData exerciseProfileData, int position) {
            details.position = position;
            tvProfileDataID.setText(Long.toString(exerciseProfileData.getExerciseProfileDataID()));
            tvProfileDataParentID.setText(Long.toString(exerciseProfileData.getParentExerciseProfileID()));
            tvProfileDataSortOrder.setText(Long.toString(exerciseProfileData.getSortOrder()));
            tpProfileDataDuration.setTotalSeconds(exerciseProfileData.getDuration());
            npProfileDataLevel.setValue(exerciseProfileData.getRelativeLevel());
            spProfileDataType.setSelection(exerciseProfileData.getDataType());
        }

        Details getItemDetails() {
            return details;
        }
    }


    ExerciseProfileDataAdapter(Context context, ExerciseProfileFragment fragment, OnStartDragListener dragStartListener) {
        mInflater = LayoutInflater.from(context);
        this.mFragment = fragment;
        this.res = fragment.getResources();
        setHasStableIds(true);
        mDragStartListener = dragStartListener;
    }

    @Override
    public ExerciseProfileDataViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.recyclerview_epdataitem, parent, false);
        return new ExerciseProfileDataViewHolder(itemView, valueChangeListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseProfileDataViewHolder holder, int position) {
        if (mExerciseProfileDataList != null) {
            Log.v(TAG, "onBindViewHolder found data for ProfileList");
            holder.bind(mExerciseProfileDataList.get(position), position);
        } else {
            // Covers the case of data not being ready yet.
            Log.d(TAG, "found nothing in db");
            //holder.exerciseProfileItemView.setText("No Items found");
        }
    }

    List<ExerciseProfileData> getmExerciseProfileDataList() {

        return mExerciseProfileDataList;
    }

    List<ExerciseProfileData> getExerciseProfileDataRemovedListAndClear() {
        List<ExerciseProfileData> exerciseProfileDataListTemp = exerciseProfileDataRemovedList;
        if (exerciseProfileDataRemovedList != null) {
            exerciseProfileDataRemovedList.clear();
        }
        return exerciseProfileDataListTemp;
    }

    void clearExerciseProfileDataRemoved() {
        if (exerciseProfileDataRemovedList != null) {
            exerciseProfileDataRemovedList.clear();
        }
    }

    public void setItemTouchHelper(ItemTouchHelper itemTouchHelper) {
        this.itemTouchHelper = itemTouchHelper;
    }

    void setmExerciseProfileDataList(List<ExerciseProfileData> exerciseProfileDataList) {
        mExerciseProfileDataList = exerciseProfileDataList;
        notifyDataSetChanged();
    }

    void addToExerciseProfileDataList(ExerciseProfileData exerciseProfileData) {
        mExerciseProfileDataList.add(exerciseProfileData);
        notifyItemInserted(mExerciseProfileDataList.size());
    }

    public ExerciseProfileData getExerciseProfileDataForPosition(long mPosition) {
        ExerciseProfileData returnValue = null;
        returnValue = mExerciseProfileDataList.get((int) mPosition);
        return returnValue;
    }

    public void onItemDismiss(int position) {
        ExerciseProfileData exerciseProfileData = mExerciseProfileDataList.remove(position);
        if (exerciseProfileDataRemovedList == null) {
            exerciseProfileDataRemovedList = new ArrayList<ExerciseProfileData>();
        }
        exerciseProfileDataRemovedList.add(exerciseProfileData);
        notifyItemRemoved(position);
    }

    public void onItemMove(int fromPosition, int toPosition) {
        /**Moving items around**/
        ExerciseProfileData tmp = mExerciseProfileDataList.remove(fromPosition);
        Log.d(TAG, "Moving item: " + fromPosition + "to : " + toPosition);
        Log.d(TAG, "Moving item: " + tmp.getSortOrder() + "level : " + tmp.getRelativeLevel());
        //mExerciseProfileDataList.add(toPosition, tmp);
        mExerciseProfileDataList.add(toPosition > fromPosition ? toPosition - 1 : toPosition, tmp);

        //notifyDataSetChanged();
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        if (mExerciseProfileDataList != null) {
            return mExerciseProfileDataList.size();
        } else return 0;
    }

    interface onValueChangeListener {
        void onValueChange(int position, int value, int type);
        /** type: 1 is level
         ** type: 2 is duration
         ** type: 3 is spinner
         ** type: 4 is button up (value 2) or down (value 1)
         * */
    }

    interface OnStartDragListener {
        /**
         * Called when a view is requesting a start of a drag.
         *
         * @param viewHolder The holder of the view to drag.
         */
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }


}
