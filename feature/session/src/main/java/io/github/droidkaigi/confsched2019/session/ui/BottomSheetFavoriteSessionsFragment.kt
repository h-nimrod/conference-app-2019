package io.github.droidkaigi.confsched2019.session.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.databinding.ViewHolder
import dagger.Module
import dagger.Provides
import io.github.droidkaigi.confsched2019.ext.android.changed
import io.github.droidkaigi.confsched2019.model.SessionPage
import io.github.droidkaigi.confsched2019.session.R
import io.github.droidkaigi.confsched2019.session.databinding.FragmentBottomSheetSessionsBinding
import io.github.droidkaigi.confsched2019.session.ui.actioncreator.SessionPageActionCreator
import io.github.droidkaigi.confsched2019.session.ui.actioncreator.SessionsActionCreator
import io.github.droidkaigi.confsched2019.session.ui.item.SpeechSessionItem
import io.github.droidkaigi.confsched2019.session.ui.store.SessionPageStore
import io.github.droidkaigi.confsched2019.session.ui.store.SessionsStore
import io.github.droidkaigi.confsched2019.session.ui.widget.DaggerFragment
import io.github.droidkaigi.confsched2019.session.ui.widget.SessionsItemDecoration
import io.github.droidkaigi.confsched2019.widget.BottomSheetBehavior
import me.tatarka.injectedvmprovider.ktx.injectedViewModelProvider
import javax.inject.Inject
import javax.inject.Provider

class BottomSheetFavoriteSessionsFragment : DaggerFragment() {
    private lateinit var binding: FragmentBottomSheetSessionsBinding

    @Inject lateinit var sessionsActionCreator: SessionsActionCreator
    @Inject lateinit var sessionPageActionCreator: SessionPageActionCreator
    @Inject lateinit var sessionPageFragmentProvider: Provider<SessionPageFragment>
    @Inject lateinit var speechSessionItemFactory: SpeechSessionItem.Factory

    @Inject lateinit var sessionsStore: SessionsStore
    @Inject lateinit var sessionDetailStoreFactory: SessionPageStore.Factory
    private val sessionPageStore: SessionPageStore by lazy {
        sessionPageFragmentProvider.get().injectedViewModelProvider
            .get(SessionPageStore::class.java.name) {
                sessionDetailStoreFactory.create(SessionPage.Favorite)
            }
    }

    private val groupAdapter = GroupAdapter<ViewHolder<*>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_bottom_sheet_sessions, container, false
        )
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.sessionsRecycler.adapter = groupAdapter
        binding.sessionsRecycler.addItemDecoration(
            SessionsItemDecoration(requireContext(), groupAdapter)
        )

        val onFilterButtonClick: (View) -> Unit = {
            sessionPageActionCreator.toggleFilterExpanded(SessionPage.Favorite)
        }
        binding.bottomSheetShowFilterButton.setOnClickListener(onFilterButtonClick)
        binding.bottomSheetHideFilterButton.setOnClickListener(onFilterButtonClick)

        binding.sessionsRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                applyTitleText()
            }
        })

        sessionsStore.favoriteSessions().changed(viewLifecycleOwner) { sessions ->
            val items = sessions
                .map { session ->
                    speechSessionItemFactory.create(session, sessionsStore)
                }

            groupAdapter.update(items)
            applyTitleText()
        }
        sessionPageStore.filterSheetState.changed(viewLifecycleOwner) { newState ->
            if (newState == BottomSheetBehavior.STATE_EXPANDED ||
                newState == BottomSheetBehavior.STATE_COLLAPSED) {
                TransitionManager.beginDelayedTransition(
                    binding.root as ViewGroup, AutoTransition().apply {
                    excludeChildren(binding.bottomSheetTitle, true)
                    excludeChildren(binding.sessionsRecycler, true)
                })
                val isCollapsed = newState == BottomSheetBehavior.STATE_COLLAPSED
                binding.bottomSheetShowFilterButton.isVisible = !isCollapsed
                binding.bottomSheetHideFilterButton.isVisible = isCollapsed
            }
        }
    }

    private fun applyTitleText() {
        val linearLayoutManager = binding.sessionsRecycler.layoutManager as LinearLayoutManager
        val firstPosition = linearLayoutManager.findFirstVisibleItemPosition()
        if (firstPosition == RecyclerView.NO_POSITION || firstPosition >= groupAdapter.itemCount) {
            return
        }
        binding.bottomSheetTitle.text = (groupAdapter
            .getItem(firstPosition) as SpeechSessionItem)
            .session
            .startDayText
    }

    companion object {
        fun newInstance(): BottomSheetFavoriteSessionsFragment {
            return BottomSheetFavoriteSessionsFragment()
        }
    }
}

@Module
object FavoriteSessionsFragmentModule {
    @JvmStatic @Provides fun providesLifecycle(
        sessionsFragmentBottomSheet: BottomSheetFavoriteSessionsFragment
    ): LifecycleOwner {
        return sessionsFragmentBottomSheet.viewLifecycleOwner
    }
}
