package com.koto.sir.racoenpfib.pages;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.transition.Explode;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.koto.sir.racoenpfib.R;
import com.koto.sir.racoenpfib.databases.AvisosLab;
import com.koto.sir.racoenpfib.models.Avis;
import com.koto.sir.racoenpfib.services.AvisosWorker;

import java.util.List;
import java.util.UUID;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

public class AvisosFragment extends VisibleFragment {
    private static final String TAG = "AvisosFragment";
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private OnFragmentGeneralNeedTransaction mCallback;
    private List<String> mAssigs;

    public static AvisosFragment newInstance() {
        return new AvisosFragment();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mAssigs.isEmpty()) {
            tryDownloadingAgain();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.avisos_fragment, container, false);
        mRecyclerView = v.findViewById(R.id.avisos_recycler_view);
        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        mAssigs = AvisosLab.get(getActivity()).getNomAssigsAvisos();
        mRecyclerView.setAdapter(new AvisosAdapter());


        mSwipeRefreshLayout = v.findViewById(R.id.swipe_refresh_avis);
        Resources resources = getResources();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mSwipeRefreshLayout.setColorSchemeColors(resources.getColor(R.color.colorSecondary, null),
                    resources.getColor(R.color.colorSecondaryVariant, null));
        } else
            mSwipeRefreshLayout.setColorSchemeColors(resources.getColor(R.color.colorSecondary),
                    resources.getColor(R.color.colorSecondaryVariant));
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                tryDownloadingAgain();
            }
        });

        return v;
    }


    private void tryDownloadingAgain() {
//        getActivity().startService(AvisosService.newIntent(getActivity()));
        OneTimeWorkRequest reload = new OneTimeWorkRequest.Builder(AvisosWorker.class).build();
        WorkManager.getInstance().enqueue(reload);
        WorkManager.getInstance().getWorkInfoByIdLiveData(reload.getId())
                .observe(getViewLifecycleOwner(), new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(@Nullable WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    }
                });

        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }, 200);*/
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Fragment fragment = getParentFragment();
        if (fragment instanceof OnFragmentGeneralNeedTransaction) {
            mCallback = (OnFragmentGeneralNeedTransaction) fragment;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    @Override
    protected void OnNewDataFound() {
        mAssigs = AvisosLab.get(getActivity()).getNomAssigsAvisos();
        mRecyclerView.getAdapter().notifyDataSetChanged();
    }

    public interface OnFragmentGeneralNeedTransaction {
        void transactionToList(String title, int position, View itemView);

        void transactionToDetailFromAvisosFragment(UUID uuid);
    }

    private class AvisosHolder extends RecyclerView.ViewHolder {
        private AppCompatTextView mTitle, mInfo1, mInfo2, mInfo3;

        AvisosHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.avis_card, parent, false));

            mTitle = itemView.findViewById(R.id.title_card_avis);
            mInfo1 = itemView.findViewById(R.id.avis_info_1);
            mInfo2 = itemView.findViewById(R.id.avis_info_2);
            mInfo3 = itemView.findViewById(R.id.avis_info_3);

            mTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setExitTransition(new Slide(Gravity.BOTTOM).setDuration(300));
                        setReenterTransition(new Slide(Gravity.BOTTOM).setDuration(300).setStartDelay(300));
//                        setExitTransition(new AutoTransition());
                    }
                    mCallback.transactionToList((String) mTitle.getText(), getAdapterPosition(), itemView);
                }
            });
        }

        void bind(String assig, List<Avis> avisos) {
//            ViewCompat.setTransitionName(mTitle, assig);
            mTitle.setText(assig);
            int n = avisos.size();

            if (n > 0) {
                Avis avis = avisos.get(0);
                mInfo1.setText(avis.getTitol());
                mInfo1.setOnClickListener(new OnClickListenerFast(avis.getUid()));
            } else {
                mInfo1.setText("");
                mInfo1.setOnClickListener(null);
            }
            if (n > 1) {
                Avis avis = avisos.get(1);
                mInfo2.setText(avis.getTitol());
                mInfo2.setOnClickListener(new OnClickListenerFast(avis.getUid()));
            } else {
                mInfo2.setText("");
                mInfo2.setOnClickListener(null);
            }
            if (n > 2) {
                Avis avis = avisos.get(2);
                mInfo3.setText(avis.getTitol());
                mInfo3.setOnClickListener(new OnClickListenerFast(avis.getUid()));
            } else {
                mInfo3.setText("");
                mInfo3.setOnClickListener(null);
            }
        }
    }

    private class OnClickListenerFast implements View.OnClickListener {
        private UUID mUUID;

        public OnClickListenerFast(UUID UUID) {
            mUUID = UUID;
        }

        @Override
        public void onClick(View v) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setExitTransition(new Explode());
                setReenterTransition(new Explode().setStartDelay(200));
            }
            mCallback.transactionToDetailFromAvisosFragment(mUUID);
        }
    }

    private class AvisosAdapter extends RecyclerView.Adapter<AvisosHolder> {


        @NonNull
        @Override
        public AvisosHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new AvisosHolder(layoutInflater, viewGroup);
        }

        @Override
        public void onBindViewHolder(@NonNull AvisosHolder avisosHolder, int i) {
            String assig = mAssigs.get(i);
            Log.d(TAG, "onBind avisHolder: " + assig);
            List<Avis> avisos = AvisosLab.get(getActivity()).getAvisos(assig, 3);
            avisosHolder.bind(assig, avisos);
        }

        @Override
        public int getItemCount() {
            return mAssigs.size();
        }
    }
}
