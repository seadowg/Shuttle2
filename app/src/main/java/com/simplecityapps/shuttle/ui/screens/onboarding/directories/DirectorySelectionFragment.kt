package com.simplecityapps.shuttle.ui.screens.onboarding.directories

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.simplecityapps.adapter.RecyclerAdapter
import com.simplecityapps.shuttle.R
import com.simplecityapps.shuttle.dagger.Injectable
import com.simplecityapps.shuttle.ui.common.autoCleared
import com.simplecityapps.shuttle.ui.screens.onboarding.OnboardingChild
import com.simplecityapps.shuttle.ui.screens.onboarding.OnboardingPage
import com.simplecityapps.shuttle.ui.screens.onboarding.OnboardingParent
import timber.log.Timber
import javax.inject.Inject


class DirectorySelectionFragment : Fragment(),
    Injectable,
    DirectorySelectionContract.View,
    OnboardingChild {

    @Inject lateinit var presenter: DirectorySelectionPresenter

    lateinit var adapter: RecyclerAdapter

    private var recyclerView: RecyclerView by autoCleared()

    private val preAnimationConstraints = ConstraintSet()
    private val postAnimationConstraints = ConstraintSet()
    private val transition = ChangeBounds()


    // Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = RecyclerAdapter(lifecycle.coroutineScope)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_OPEN_DOCUMENT && resultCode == Activity.RESULT_OK) {
            data?.let { intent ->
                presenter.handleSafResult(requireContext().contentResolver, intent)
            } ?: Timber.e("onActivityResult failed to handle result: Intent data null")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_onboarding_directories, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { getParent()?.goToPrevious() ?: Timber.e("Failed to navigate, parent is null") }

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.adapter = adapter

        presenter.bindView(this)
        presenter.loadData(requireContext().contentResolver)

        val addDirectoryButton: Button = view.findViewById(R.id.addDirectoryButton)
        addDirectoryButton.setOnClickListener {
           presenter.presentDocumentProvider()
        }

        val helpButton: Button = view.findViewById(R.id.helpButton)
        helpButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Media Directory Chooser")
                .setMessage("In order to scan for music, Shuttle requires access to your media directories.\n\nWhen you press 'add directory', you'll be taken to your default 'file provider' app. Navigate to the folder(s) containing your music. You can add multiple directories. \n\nNote: You may need to enable 'show internal storage' in order to see your music folder(s).")
                .setNegativeButton("Close", null)
                .show()
        }

        preAnimationConstraints.clone(view as ConstraintLayout)
        postAnimationConstraints.clone(view)
        postAnimationConstraints.clear(R.id.addDirectoryButton, ConstraintSet.TOP)

        transition.interpolator = FastOutSlowInInterpolator()
        transition.duration = 300
    }

    override fun onResume() {
        super.onResume()

        getParent()?.let { parent ->
            parent.showBackButton("Back")
            parent.showNextButton("Scan")

            if (adapter.items.none { binder -> binder is DirectoryBinder } || adapter.items.any { binder -> binder is DirectoryBinder && !binder.directory.traversalComplete }) {
                parent.toggleNextButton(enabled = false)
            } else {
                parent.toggleNextButton(enabled = true)
            }
        } ?: Timber.e("Failed to update back/scan button - parent is null")
    }

    override fun onDestroyView() {
        presenter.unbindView()
        super.onDestroyView()
    }


    // MusicDirectoriesContract.View Implementation

    override fun setData(data: List<DirectorySelectionContract.Directory>) {
        adapter.update(data.map { DirectoryBinder(it, directoryBinderListener) }.toMutableList()) {
            getParent()?.let { parent ->
                if (adapter.items.none { binder -> binder is DirectoryBinder } || adapter.items.any { binder -> binder is DirectoryBinder && !binder.directory.traversalComplete }) {
                    parent.toggleNextButton(enabled = false)
                } else {
                    parent.toggleNextButton(enabled = true)
                }

                (view as? ConstraintLayout)?.let { constraintLayout ->
                    TransitionManager.beginDelayedTransition(constraintLayout, transition)
                    if (adapter.items.isEmpty()) {
                        preAnimationConstraints.applyTo(constraintLayout)
                    } else {
                        postAnimationConstraints.applyTo(constraintLayout)
                    }
                }

            } ?: Timber.e("Failed to update update buttons, getParent() returned null")
        }
    }

    override fun startActivity(intent: Intent, requestCode: Int) {
        startActivityForResult(intent, requestCode)
    }

    override fun showDocumentProviderNotAvailable() {
        AlertDialog.Builder(requireContext())
            .setTitle("Missing Document Provider")
            .setMessage("A 'Document Provider' (file manager) app can't be found on your device. You may have to install one, or revert to using the 'basic' media scanner.")
            .setNeutralButton("Close", null)
            .show()
    }

    // DirectoryBinder.Listener

    private val directoryBinderListener = object : DirectoryBinder.Listener {

        override fun onRemoveClicked(directory: DirectorySelectionContract.Directory) {
            presenter.removeItem(directory)
        }
    }


    // OnboardingChild Implementation

    override val page = OnboardingPage.MusicDirectories

    override fun getParent(): OnboardingParent? {
        return parentFragment as? OnboardingParent
    }

    override fun handleNextButtonClick() {
        getParent()?.goToNext() ?: Timber.e("Failed to goToNext() - getParent() returned null")
    }

    override fun handleBackButtonClick() {
        getParent()?.goToPrevious() ?: Timber.e("Failed to goToPrevious() - getParent() returned null")
    }


    // Static

    companion object {
        const val REQUEST_CODE_OPEN_DOCUMENT = 100
    }
}