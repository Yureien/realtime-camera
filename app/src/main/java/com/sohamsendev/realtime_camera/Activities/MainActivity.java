package com.sohamsendev.realtime_camera.Activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.sohamsendev.realtime_camera.R;
import com.sohamsendev.realtime_camera.Utils.ServerManager;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static Fragment controlPanel, cameraPreview, settings;
    public static ServerManager serverManager;

    static {
        try {
            System.loadLibrary("image-processor");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("ConstantConditions")
    // If the SuppressWarnings is not added, a wrong warning would be shown on some parts.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, 0);
            }
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        controlPanel = new ControlPanelFragment();
        cameraPreview = new CameraPreviewFragment();
        settings = new SettingsFragment();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, controlPanel)
                .commit();
        getSupportActionBar().setTitle(getResources().
                getString(R.string.control_panel_fragment_name));

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        drawer.bringToFront();
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        serverManager = new ServerManager(getSupportFragmentManager());
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) drawer.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
    }

    @SuppressWarnings("ConstantConditions")
    // If the SuppressWarnings is not added, a wrong warning would be shown on some parts.
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case R.id.nav_control_panel:
                if (controlPanel.isAdded())
                    getSupportFragmentManager().beginTransaction()
                            .show(controlPanel)
                            .hide(cameraPreview)
                            .remove(settings)
                            .commit();
                getSupportActionBar().setTitle(getResources().
                        getString(R.string.control_panel_fragment_name));
                break;
            case R.id.nav_camera_preview:
                getSupportFragmentManager().beginTransaction()
                        .remove(settings)
                        .hide(controlPanel)
                        .show(cameraPreview)
                        .commit();
                getSupportActionBar().setTitle(getResources().
                        getString(R.string.camera_fragment_name));
                break;
            case R.id.nav_settings:
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, settings)
                        .hide(cameraPreview)
                        .hide(controlPanel)
                        .show(settings)
                        .commit();
                getSupportActionBar().setTitle(getResources().
                        getString(R.string.settings_fragment_name));
                break;
            case R.id.nav_about_dev:
                Toast.makeText(this, "Do Something", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_donate:
                Toast.makeText(this, "Do Something", Toast.LENGTH_SHORT).show();
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}