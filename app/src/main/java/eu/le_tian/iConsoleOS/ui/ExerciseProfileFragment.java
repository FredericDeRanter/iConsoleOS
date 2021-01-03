package eu.le_tian.iConsoleOS.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StableIdKeyProvider;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import eu.le_tian.iConsoleOS.R;
import eu.le_tian.iConsoleOS.data.ExerciseProfile;
import eu.le_tian.iConsoleOS.data.ExerciseProfileData;
import eu.le_tian.iConsoleOS.databinding.FragmentExerciseprofileBinding;

/**
 * This fragment is where to create new exercise profiles.
 *
 *     //TODO: export/import profiles
 *     //TODO: adjust to user selected
 *
 */

public class ExerciseProfileFragment extends Fragment {

    //DONE: something wrong when keyboard comes up: has something to do with stableIDKeyProvider, solved by making layout fixed when soft keyboard comes up (setting in manifest)

    private static String TAG = "ExerciseProfileFragment";
    private IConsoleViewModel IConsoleViewModel;
    private FragmentExerciseprofileBinding binding;
    private SelectionTracker<Long> selectionTracker;
    private ExerciseProfileAdapter exerciseProfileAdapter;
    private ExerciseProfileDataAdapter exerciseProfileDataAdapter;
    private ItemTouchHelper itemTouchHelper;
    private ExerciseProfileDataAdapter.OnStartDragListener onStartDragListener = new ExerciseProfileDataAdapter.OnStartDragListener() {
        @Override
        public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
            itemTouchHelper.startDrag(viewHolder);
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentExerciseprofileBinding.inflate(inflater, container, false);
        IConsoleViewModel = new ViewModelProvider(this).get(IConsoleViewModel.class);
        View root = binding.getRoot();

        /** init recycler view for exercise profiles */
        RecyclerView profileRecyclerView = binding.exPrRV;
        exerciseProfileAdapter = new ExerciseProfileAdapter(getContext(), this);
        profileRecyclerView.setAdapter(exerciseProfileAdapter);
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        profileRecyclerView.addItemDecoration(itemDecoration);
        profileRecyclerView.setHasFixedSize(true);
        profileRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        selectionTracker = new SelectionTracker.Builder<Long>(
                "ExerciseProfileSelection",
                profileRecyclerView,
                new StableIdKeyProvider(profileRecyclerView),
                new ExerciseProfileAdapter.DetailsLookup(profileRecyclerView),
                StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectSingleAnything()).build();

        exerciseProfileAdapter.setSelectionTracker(selectionTracker);
        selectNothingInExerciseProfile();
        selectionTracker.addObserver(new SelectionTracker.SelectionObserver() {
            @Override
            public void onSelectionChanged() {
                if (selectionTracker.hasSelection()) {
                    Iterator<Long> profileSelectionIterator = selectionTracker.getSelection().iterator();
                    while (profileSelectionIterator.hasNext()) {
                        long fieldPosition = profileSelectionIterator.next();
                        Log.d(TAG, "selectionhasChanged: " + fieldPosition);
                        if (fieldPosition < 99999999) {
                            selectExerciseProfile(exerciseProfileAdapter.getExerciseProfileForPosition(fieldPosition));
                        } else {
                            selectExerciseProfile(null);
                        }
                    }
                } else {
                    selectNothingInExerciseProfile();
                }
            }
        });

        IConsoleViewModel.getAllExerciseProfiles().observe(getViewLifecycleOwner(), exerciseProfiles -> exerciseProfileAdapter.setExerciseProfiles(exerciseProfiles));

        /**init recyclerview for profile data, data is only shown when selection is made in exerciseprofile */
        RecyclerView profileDataRecyclerView = binding.rvExerciseProfileData;
        exerciseProfileDataAdapter = new ExerciseProfileDataAdapter(getContext(), this, onStartDragListener);
        profileDataRecyclerView.setAdapter(exerciseProfileDataAdapter);
        profileDataRecyclerView.setHasFixedSize(true);
        profileDataRecyclerView.addItemDecoration(itemDecoration);
        profileDataRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                final int removePos = viewHolder.getAdapterPosition();
                exerciseProfileDataAdapter.onItemDismiss(removePos);
                // remove item from adapter
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                //Never called since dragDirs set to 0;
                final int fromPos = viewHolder.getAdapterPosition();
                final int toPos = target.getAdapterPosition();
                Log.d(TAG, "Moving item");
                exerciseProfileDataAdapter.onItemMove(fromPos, toPos);
                // move item in `fromPos` to `toPos` in adapter.
                return true;// true if moved, false otherwise
            }

            @Override
            public boolean isLongPressDragEnabled() {
                //return true;
                return false;
            }

        };

        itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(profileDataRecyclerView);
        exerciseProfileDataAdapter.setItemTouchHelper(itemTouchHelper);

        return root;
    }

    private long ReadID(EditText levelText) {
        //returns 0 if something wrong
        if (TextUtils.isEmpty(levelText.getText().toString())) {
            return 0;
        }
        long ID = Long.parseLong(levelText.getText().toString());
        if (ID > 0) {
            return ID;
        }
        return 0;
    }

    public void selectExerciseProfile(ExerciseProfile exerciseProfile) {
        /**is called when a selection is made in the exerciseProfile recyclerview
         * first check if valid selection (exerciseProfile != null)
         * then fill in the edit fields and enable buttons to be enabled */
        if (exerciseProfile != null) {
            binding.exPrNameEdit.setText(exerciseProfile.getExerciseProfileName());
            binding.exPrDuration.setTotalSeconds(exerciseProfile.getDefaultDuration());
            binding.exPrStartLevel.setValue(exerciseProfile.getStartLevel());
            binding.epIDedit.setText(Long.toString(exerciseProfile.getExerciseProfileID()));
            binding.epSortOrder.setText(Long.toString(exerciseProfile.getSortOrder()));
            binding.exPrRepeatSwitch.setChecked(exerciseProfile.isRepeatToFill());

            binding.bDeleteExerciseProfile.setEnabled(true);
            binding.bUpdateExerciseProfile.setEnabled(true);
            binding.btExerciseProfileDown.setEnabled(true);
            binding.btExerciseProfileUp.setEnabled(true);
            /** add items to the adapter of the  exerciseProfileData*/
            Future<List<ExerciseProfileData>> futureList = IConsoleViewModel.getExerciseProfileDataList(exerciseProfile.getExerciseProfileID());
            exerciseProfileDataAdapter.clearExerciseProfileDataRemoved();

            try {
                exerciseProfileDataAdapter.setmExerciseProfileDataList(futureList.get());
            } catch (ExecutionException e) {
                Log.e(TAG, e.toString());
            } catch (InterruptedException e) {
                Log.e(TAG, e.toString());
            }


        } else { /**nothing selected, reset the fields*/
            binding.exPrNameEdit.setText("Name");
            binding.exPrDuration.setTotalSeconds(120);
            binding.exPrStartLevel.setValue(1);
            binding.epIDedit.setText("0");
            binding.epSortOrder.setText("0");
            binding.exPrRepeatSwitch.setChecked(false);

            //disable update button
            binding.bDeleteExerciseProfile.setEnabled(false);
            binding.bUpdateExerciseProfile.setEnabled(false);
            binding.btExerciseProfileDown.setEnabled(false);
            binding.btExerciseProfileUp.setEnabled(false);

            exerciseProfileDataAdapter.setmExerciseProfileDataList(null);
            exerciseProfileDataAdapter.clearExerciseProfileDataRemoved();
        }
    }

    public void selectNothingInExerciseProfile() {
        selectionTracker.select((long) 99999999);
    }


    private ExerciseProfile readExerciseProfile(boolean readID) {
        //returns null if something is not right

        ExerciseProfile localExerciseProfile;
        String epName = binding.exPrNameEdit.getText().toString();
        if (epName == "Name" || TextUtils.isEmpty(epName)) {
            return null;
        }
        int startLevel = binding.exPrStartLevel.getValue();
        int epDuration = binding.exPrDuration.getTotalSeconds();
        boolean epRepeat = binding.exPrRepeatSwitch.isChecked();
        long epID = 0;
        long epSortOrder = exerciseProfileAdapter.getItemCount() + 1;

        if (readID) {
            //same standard as duration
            epID = ReadID(binding.epIDedit);
            epSortOrder = ReadID(binding.epSortOrder);
        }

        localExerciseProfile = new ExerciseProfile(epID, epSortOrder, epName, startLevel, epDuration, epRepeat);
        return localExerciseProfile;
    }


    //to make sortOrder work:need a function to save the current positions in the recyclerview as sortorder IDs

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        binding.bAddExerciseProfile.setOnClickListener(v -> {
            //Add the exerciseProfile
            ExerciseProfile readProfile = readExerciseProfile(false);
            if (readProfile != null) {
                IConsoleViewModel.insertExerciseProfile(readProfile);
                selectNothingInExerciseProfile();
            } else {
                Snackbar.make(view, "Some values are wrong to save the new ExerciseProfile", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        binding.bUpdateExerciseProfile.setOnClickListener(v -> {
            ExerciseProfile readProfile = readExerciseProfile(true);
            if (readProfile != null) {
                IConsoleViewModel.updateExerciseProfile(readProfile);
            }
        });

        binding.bDeleteExerciseProfile.setOnClickListener(v -> {
            ExerciseProfile readProfile = readExerciseProfile(true);
            if (readProfile != null) {
                IConsoleViewModel.deleteExerciseProfile(readProfile);
                selectNothingInExerciseProfile();
            }
        });

        binding.btExerciseProfileDown.setOnClickListener(v -> {
            //first step is to read the sortOrderID of the selection:
            long epSortOrder = ReadID(binding.epSortOrder);
            //only do stuff if epSortOrder is larger than 0 and smaller than the last item in the list (can't move that one further down
            if (epSortOrder < exerciseProfileAdapter.getItemCount() && epSortOrder > 0) {
                ExerciseProfile readProfile = readExerciseProfile(true);
                ExerciseProfile nextProfile = exerciseProfileAdapter.getExerciseProfileForPosition(epSortOrder);
                Log.d(TAG, "going to move item down");
                //we need to increase the orderID of the selected item with 1 and decrease the item next to the selected with 1
                readProfile.setSortOrder(epSortOrder + 1);
                IConsoleViewModel.updateExerciseProfile(readProfile);
                nextProfile.setSortOrder(epSortOrder);
                IConsoleViewModel.updateExerciseProfile(nextProfile);
                selectionTracker.select(epSortOrder);
            }
        });

        binding.btExerciseProfileUp.setOnClickListener(v -> {
            long epSortOrder = ReadID(binding.epSortOrder);
            if (epSortOrder > 1) {
                ExerciseProfile readProfile = readExerciseProfile(true);
                ExerciseProfile nextProfile = exerciseProfileAdapter.getExerciseProfileForPosition(epSortOrder - 2);
                Log.d(TAG, "going to move item up");
                //we need to decrease the orderID of the selected item with 1 and increase the item up from to the selected with 1
                Log.d(TAG, "sortOrder: " + epSortOrder);
                Log.d(TAG, "sortOrder: " + readProfile.getSortOrder());
                Log.d(TAG, "sortOrder2: " + nextProfile.getSortOrder());

                readProfile.setSortOrder(epSortOrder - 1);
                IConsoleViewModel.updateExerciseProfile(readProfile);

                nextProfile.setSortOrder(epSortOrder);
                IConsoleViewModel.updateExerciseProfile(nextProfile);


                selectionTracker.select(epSortOrder - 2);
            }

        });

        binding.bAddExerciseProfileData.setOnClickListener(v -> {
            long epID = ReadID(binding.epIDedit);
            /**Needs to be larger than 0 else do nothing**/
            if (epID > 0) {
                ExerciseProfileData exerciseProfileData = new ExerciseProfileData(0, exerciseProfileDataAdapter.getItemCount() + 1, 1, 1, 60, epID);
                exerciseProfileDataAdapter.addToExerciseProfileDataList(exerciseProfileData);
            }
        });

        binding.bSaveExerciseProfileData.setOnClickListener(v -> {

            List<ExerciseProfileData> exerciseProfileDataList = exerciseProfileDataAdapter.getmExerciseProfileDataList();
            List<ExerciseProfileData> exerciseProfileDataListRemoved = exerciseProfileDataAdapter.getExerciseProfileDataRemovedListAndClear();

            //List<ExerciseProfileData> exerciseProfileDataListToUpdate = exerciseProfileDataList.stream().filter(exerciseProfileData -> (exerciseProfileData.getExerciseProfileDataID()>0)).collect(Collectors.toList());
            //List<ExerciseProfileData> exerciseProfileDataListToInsert = exerciseProfileDataList.stream().filter(exerciseProfileData -> (exerciseProfileData.getExerciseProfileDataID()==0)).collect(Collectors.toList());
            IConsoleViewModel.insertExerciseProfileDataList(exerciseProfileDataList);
            IConsoleViewModel.updateExerciseProfileDataList(exerciseProfileDataList);
            if (exerciseProfileDataListRemoved != null) {
                IConsoleViewModel.deleteExerciseProfileDataList(exerciseProfileDataListRemoved);
            }
        });


        //TODO: duration and startlevel for exercise profile are offsets. Extra level is default 0, time can be zero, then it will be shown as the complete time of the different profiledata sets.
        //TODO: delete a profile deletes also its data
        //TODO: completely empty the db and init with some default data


    }

}