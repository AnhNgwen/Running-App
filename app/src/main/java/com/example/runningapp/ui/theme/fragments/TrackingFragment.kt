package com.example.runningapp.ui.theme.fragments

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.runningapp.R
import com.example.runningapp.databinding.FragmentTrackingBinding
import com.example.runningapp.db.Run
import com.example.runningapp.db.RunningDatabase
import com.example.runningapp.other.Constants.ACTION_PAUSE_SERVICE
import com.example.runningapp.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.runningapp.other.Constants.ACTION_STOP_SERVICE
import com.example.runningapp.other.Constants.MAP_ZOOM
import com.example.runningapp.other.Constants.POLYLINE_COLOR
import com.example.runningapp.other.Constants.POLYLINE_WIDTH
import com.example.runningapp.other.TrackingUtility
import com.example.runningapp.repositories.MainRepository
import com.example.runningapp.services.Polyline
import com.example.runningapp.services.TrackingService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.round

@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking){

    //private val viewModel: MainViewModel by viewModels()
    private lateinit var mainRepository: MainRepository

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        val contextThemeWrapper = ContextThemeWrapper(requireContext(), R.style.AppTheme)
        return inflater.cloneInContext(contextThemeWrapper)
    }

    private var curTimeInMillis = 0L

    private var menu: Menu? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private var map: GoogleMap? = null
    private lateinit var bindingTracking: FragmentTrackingBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val database = RunningDatabase.getInstance(requireContext())
        val runDAO = database.getRunDao()
        mainRepository = MainRepository(runDAO)
        bindingTracking = FragmentTrackingBinding.bind(view)

        bindingTracking.mapView.onCreate(savedInstanceState)


        bindingTracking.btnToggleRun.setOnClickListener {
            toggleRun()
        }

        bindingTracking.btnFinishRun.setOnClickListener {
            zoomToSeeWholeTrack()
            endRunAndSaveToDb()
        }

        bindingTracking.mapView.getMapAsync {
            map = it
            addAllPolylines()
        }
        setupMenu()
        subscribeToObservers()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.toolbar_tracking_menu, menu)
                this@TrackingFragment.menu = menu
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.miCancelTracking -> {
                        showCancelTrackingDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun subscribeToObservers() {
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })

        TrackingService.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()
        })

        TrackingService.timeRunInMillis.observe(viewLifecycleOwner, Observer {
            curTimeInMillis = it
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(curTimeInMillis, true)
            bindingTracking.tvTimer.text = formattedTime
            menu?.findItem(R.id.miCancelTracking)?.isVisible = curTimeInMillis > 0
        })
    }

    private fun toggleRun() {
        if (isTracking) {
            menu?.findItem(R.id.miCancelTracking)?.isVisible = true
            sendCommandToService(ACTION_PAUSE_SERVICE)
        } else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    private fun showCancelTrackingDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Cancel the Run?")
            .setMessage("Are you sure to cancel the current run and delete all the data?")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Yes") { _, _ ->
                stopRun()
            }
            .setNegativeButton("No") { dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
        dialog.show()
    }

    private fun stopRun() {
        sendCommandToService(ACTION_STOP_SERVICE)
        findNavController().navigate(R.id.action_trackingFragment_to_runFragment)
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if (!isTracking) {
            bindingTracking.btnToggleRun.text = "Start"
            bindingTracking.btnFinishRun.visibility = View.VISIBLE
        } else {
            bindingTracking.btnToggleRun.text = "Stop"
            menu?.findItem(R.id.miCancelTracking)?.isVisible = true
            bindingTracking.btnFinishRun.visibility = View.GONE
        }
    }

    private fun moveCameraToUser() {
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    MAP_ZOOM
                )
            )
        }
    }

    private fun zoomToSeeWholeTrack() {
        val bounds = LatLngBounds.Builder()
        for (polyline in pathPoints) {
            for (pos in polyline) {
                bounds.include(pos)
            }
        }

        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                bindingTracking.mapView.width,
                bindingTracking.mapView.height,
                (bindingTracking.mapView.height * 0.05f).toInt()
            )
        )
    }

    private fun insertRun(run: Run) {
        lifecycleScope.launch {
            mainRepository.insertRun(run)
        }
    }

    private fun endRunAndSaveToDb() {
        map?.snapshot { bmp ->
            var distanceInMeters = 0
            for (polyline in pathPoints) {
                distanceInMeters += TrackingUtility.calculatePolylineLength(polyline).toInt()
            }
            val avgSpeed = round((distanceInMeters / 1000f) / (curTimeInMillis / 1000f / 60 / 60) * 10) / 10f
            val dateTimestamp = Calendar.getInstance().timeInMillis
            val run = Run(bmp, dateTimestamp, avgSpeed, distanceInMeters, curTimeInMillis)
            //viewModel.insertRun(run)
            insertRun(run)
            Snackbar.make(
                requireActivity().findViewById(R.id.rootView),
                "Run saved successfully",
                Snackbar.LENGTH_LONG
            ).show()
            stopRun()
        }
    }

    private fun addAllPolylines() {
        for (polyline in pathPoints) {
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun addLatestPolyline() {
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(preLastLatLng)
                .add(lastLatLng)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    override fun onResume() {
        super.onResume()
        if (::bindingTracking.isInitialized) {
            bindingTracking.mapView.onResume()
        }
    }

    override fun onStart() {
        super.onStart()
        if (::bindingTracking.isInitialized) {
            bindingTracking.mapView.onStart()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::bindingTracking.isInitialized) {
            bindingTracking.mapView.onStop()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::bindingTracking.isInitialized) {
            bindingTracking.mapView.onPause()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (::bindingTracking.isInitialized) {
            bindingTracking.mapView.onLowMemory()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::bindingTracking.isInitialized) {
            bindingTracking.mapView.onSaveInstanceState(outState)
        }
    }
}