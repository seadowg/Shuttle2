package com.simplecityapps.shuttle.ui.screens.onboarding.storage

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.simplecityapps.shuttle.R
import com.simplecityapps.shuttle.ui.screens.onboarding.OnboardingChild
import com.simplecityapps.shuttle.ui.screens.onboarding.OnboardingPage
import com.simplecityapps.shuttle.ui.screens.onboarding.OnboardingParent
import com.simplecityapps.shuttle.ui.screens.onboarding.OnboardingParentFragment

class StoragePermissionFragment : Fragment(), OnboardingChild {

    // Lifecycle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_onboarding_permissions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mediaPermissionButton: Button = view.findViewById(R.id.grantPermissionButton)
        mediaPermissionButton.setOnClickListener {
            requestStoragePermission()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == OnboardingParentFragment.REQUEST_CODE_READ_STORAGE && grantResults.all { result -> result == PackageManager.PERMISSION_GRANTED }) {
            getParent().goToNext()
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Storage Permission Required")
                    .setMessage("Shuttle may not function correctly without permission to access your music")
                    .setPositiveButton("Retry") { _, _ -> requestStoragePermission() }
                    .setNegativeButton("Close", null)
                    .show()
            } else {
                getParent().goToNext()
            }
        }
    }


    // Private

    private fun requestStoragePermission() {
        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), OnboardingParentFragment.REQUEST_CODE_READ_STORAGE)
    }


    // OnboardingChild Implementation

    override val page = OnboardingPage.StoragePermission

    override fun getParent() = parentFragment as OnboardingParent

    override fun handleNextButtonClick() {
        getParent().goToNext()
    }
}