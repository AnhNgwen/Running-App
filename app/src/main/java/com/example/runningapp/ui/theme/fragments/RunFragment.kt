package com.example.runningapp.ui.theme.fragments

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.runningapp.R
import com.example.runningapp.adapters.RunAdapter
import com.example.runningapp.databinding.FragmentRunBinding
import com.example.runningapp.db.Run
import com.example.runningapp.db.RunningDatabase
import com.example.runningapp.other.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.example.runningapp.other.SortType
import com.example.runningapp.other.TrackingUtility
import com.example.runningapp.repositories.MainRepository
import com.example.runningapp.ui.theme.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

@AndroidEntryPoint
class RunFragment : Fragment(R.layout.fragment_run), EasyPermissions.PermissionCallbacks {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var runAdapter: RunAdapter
    private lateinit var mainRepository: MainRepository
    private lateinit var bindingRun: FragmentRunBinding

    private var runs = MediatorLiveData<List<Run>>()
    private var sortType = SortType.DATE
    private lateinit var runsSortedByDate : LiveData<List<Run>>
    private lateinit var runsSortedByTimeInMillis : LiveData<List<Run>>
    private lateinit var runsSortedByDistance : LiveData<List<Run>>
    private lateinit var runsSortedByAvgSpeed : LiveData<List<Run>>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val database = RunningDatabase.getInstance(requireContext())
        val runDAO = database.getRunDao()
        mainRepository = MainRepository(runDAO)
        bindingRun = FragmentRunBinding.bind(view)

        requestPermissions()
        setupRecyclerView()

        runsSortedByDate = runDAO.getAllRunsSortedByDate()
        runsSortedByTimeInMillis = runDAO.getAllRunsSortedByTimeInMillis()
        runsSortedByDistance = runDAO.getAllRunsSortedByDistance()
        runsSortedByAvgSpeed = runDAO.getAllRunsSortedByAvgSpeed()

        runs.addSource(runsSortedByDate) { result ->
            if (sortType == SortType.DATE) {
                result?.let { runs.value = it }
            }
        }
        runs.addSource(runsSortedByTimeInMillis) { result ->
            if (sortType == SortType.RUNNING_TIME) {
                result?.let { runs.value = it }
            }
        }
        runs.addSource(runsSortedByDistance) { result ->
            if (sortType == SortType.DISTANCE) {
                result?.let { runs.value = it }
            }
        }
        runs.addSource(runsSortedByAvgSpeed) { result ->
            if (sortType == SortType.AVG_SPEED) {
                result?.let { runs.value = it }
            }
        }

        when(sortType) {
            SortType.DATE -> bindingRun.spFilter.setSelection(0)
            SortType.RUNNING_TIME -> bindingRun.spFilter.setSelection(1)
            SortType.DISTANCE -> bindingRun.spFilter.setSelection(2)
            SortType.AVG_SPEED -> bindingRun.spFilter.setSelection(3)
        }

        bindingRun.spFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                pos: Int,
                id: Long
            ) {
                when(pos) {
                    0 -> sortRuns(SortType.DATE)
                    1 -> sortRuns(SortType.RUNNING_TIME)
                    2 -> sortRuns(SortType.DISTANCE)
                    3 -> sortRuns(SortType.AVG_SPEED)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        runs.observe(viewLifecycleOwner, Observer {
            runAdapter.submitList(it)
        })

        bindingRun.fab.setOnClickListener {
            findNavController().navigate(R.id.action_runFragment_to_trackingFragment)
        }
    }

    private fun sortRuns(sortType: SortType) = when(sortType) {
        SortType.DATE -> runsSortedByDate.value?.let { runs.value = it }
        SortType.RUNNING_TIME -> runsSortedByTimeInMillis.value?.let { runs.value = it }
        SortType.DISTANCE -> runsSortedByDistance.value?.let { runs.value = it }
        SortType.AVG_SPEED -> runsSortedByAvgSpeed.value?.let { runs.value = it }
    }.also {
        this.sortType = sortType
    }

    private fun setupRecyclerView() = bindingRun.rvRuns.apply {
        runAdapter = RunAdapter()
        adapter = runAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun requestPermissions() {
        if(TrackingUtility.hasLocationPermissions(requireContext())) {
            return
        }
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept location permissions to use this app.",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept location permissions to use this app.",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    override fun onPermissionsGranted(
        requestCode: Int,
        perms: List<String?>
    ) {}

    override fun onPermissionsDenied(
        requestCode: Int,
        perms: List<String?>
    ) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermissions()
        }
    }

    @Deprecated("Support for Android 10 (API level 29) or lower.")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}