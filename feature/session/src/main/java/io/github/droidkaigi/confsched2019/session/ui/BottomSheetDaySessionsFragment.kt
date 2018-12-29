package io.github.droidkaigi.confsched2019.session.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.databinding.ViewHolder
import io.github.droidkaigi.confsched2019.ext.android.changed
import io.github.droidkaigi.confsched2019.model.Session
import io.github.droidkaigi.confsched2019.model.SessionPage
import io.github.droidkaigi.confsched2019.session.R
import io.github.droidkaigi.confsched2019.session.databinding.FragmentBottomSheetSessionsBinding
import io.github.droidkaigi.confsched2019.session.ui.actioncreator.SessionPageActionCreator
import io.github.droidkaigi.confsched2019.session.ui.actioncreator.SessionsActionCreator
import io.github.droidkaigi.confsched2019.session.ui.item.SessionItem
import io.github.droidkaigi.confsched2019.session.ui.item.SpecialSessionItem
import io.github.droidkaigi.confsched2019.session.ui.item.SpeechSessionItem
import io.github.droidkaigi.confsched2019.session.ui.store.SessionPageStore
import io.github.droidkaigi.confsched2019.session.ui.store.SessionsStore
import io.github.droidkaigi.confsched2019.session.ui.widget.DaggerFragment
import io.github.droidkaigi.confsched2019.session.ui.widget.SessionsItemDecoration
import io.github.droidkaigi.confsched2019.widget.BottomSheetBehavior
import me.tatarka.injectedvmprovider.ktx.injectedViewModelProvider
import javax.inject.Inject
import javax.inject.Provider

class BottomSheetDaySessionsFragment : DaggerFragment() {
    private lateinit var binding: FragmentBottomSheetSessionsBinding

    @Inject lateinit var sessionsActionCreator: SessionsActionCreator
    @Inject lateinit var sessionPageActionCreator: SessionPageActionCreator
    @Inject lateinit var sessionPageFragmentProvider: Provider<SessionPageFragment>
    @Inject lateinit var speechSessionItemFactory: SpeechSessionItem.Factory
    @Inject lateinit var sessionsStore: SessionsStore
    @Inject lateinit var sessionPageStoreFactory: SessionPageStore.Factory
    private val sessionPageStore: SessionPageStore by lazy {
        sessionPageFragmentProvider.get().injectedViewModelProvider
            .get(SessionPageStore::class.java.name) {
                sessionPageStoreFactory.create(SessionPage.pages[args.day])
            }
    }

    private val groupAdapter = GroupAdapter<ViewHolder<*>>()
    private val args: BottomSheetDaySessionsFragmentArgs by lazy {
        BottomSheetDaySessionsFragmentArgs.fromBundle(arguments)
    }

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
            sessionPageActionCreator.toggleFilterExpanded(SessionPage.pageOfDay(args.day))
        }
        binding.bottomSheetShowFilterButton.setOnClickListener(onFilterButtonClick)
        binding.bottomSheetHideFilterButton.setOnClickListener(onFilterButtonClick)

        sessionsStore.daySessions(args.day).changed(viewLifecycleOwner) { sessions ->
            val items = sessions
                .map<Session, Item<*>> { session ->
                    when (session) {
                        is Session.SpeechSession ->
                            speechSessionItemFactory.create(session, sessionsStore)
                        is Session.SpecialSession ->
                            SpecialSessionItem(session)
                    }
                }
            groupAdapter.update(items)

            val titleText = items
                .asSequence()
                .filterIsInstance<SessionItem>()
                .firstOrNull()
                ?.session
                ?.startDayText ?: return@changed
            binding.bottomSheetTitle.text = titleText
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

    companion object {
        fun newInstance(
            args: BottomSheetDaySessionsFragmentArgs
        ): BottomSheetDaySessionsFragment {
            return BottomSheetDaySessionsFragment().apply {
                arguments = args.toBundle()
            }
        }
    }
}
