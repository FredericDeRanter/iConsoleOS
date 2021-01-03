package eu.le_tian.iConsoleOS.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import eu.le_tian.iConsoleOS.R;
import eu.le_tian.iConsoleOS.databinding.FragmentHistoryBinding;

/**
 * This fragment is where you can see the done exercises.
 *
 *     //TODO: adjust per user selected
 *     //TODO: add list with all done exercises
 *     //TODO: show graph with history of total kcal/time per month of the last 12 months or so.
 *     //TODO: export 1/several/all none exported exercises and show progress of export
 *     //TODO: when exercise selected: show data/graphs of exercise
 *
 */

public class HistoryFragment extends Fragment {

    private IConsoleViewModel IConsoleViewModel;
    private FragmentHistoryBinding binding;
    private Resources res;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        IConsoleViewModel = new ViewModelProvider(this).get(IConsoleViewModel.class);
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        res = getResources();
        binding.exportExerciseBtn.setOnClickListener(v -> {
            writeExercisesToCSV();
        });

    }

    private void writeExercisesToCSV() {
        IConsoleViewModel.writeExercisesToCSV((long) 1);
    }


}




