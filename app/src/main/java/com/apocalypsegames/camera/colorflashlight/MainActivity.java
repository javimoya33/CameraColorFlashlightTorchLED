package com.apocalypsegames.camera.colorflashlight;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import android.hardware.camera2.CameraManager;

public class MainActivity extends AppCompatActivity {

    private static int CAMERA_REQUEST_CODE = 1;

    private CameraManager cameraManager;
    private int cameraFacingBack;
    private int cameraFacingFront;
    private boolean bolCameraFacing;
    private TextureView.SurfaceTextureListener surfaceTextureListener;

    private Camera camera;
    private Camera.Parameters parameters;

    private Size previewSize;
    private String cameraId;
    private CameraDevice.StateCallback stateCallBack;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private CameraDevice cameraDevice;
    private boolean flashLightStatus = false;
    private boolean frontCamera = false;

    private TextureView textureView;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest captureRequest;
    private File galleryFolder;

    private FloatingActionButton fabPhoto;
    private FloatingActionButton fabFrontPhoto;
    private FloatingActionButton fabTorchPhoto;

    public RelativeLayout relColorFilter1;
    public RelativeLayout relColorFilter2;
    public RelativeLayout relColorFilter3;
    public RelativeLayout relColorFilter4;
    public RelativeLayout relColorFilter5;
    public RelativeLayout relColorFilter6;

    public SwitchCompat switchCompat1;
    public SwitchCompat switchCompat2;
    public SwitchCompat switchCompat3;
    public SwitchCompat switchCompat4;
    public SwitchCompat switchCompat5;
    public SwitchCompat switchCompat6;

    public MediaPlayer sonidoCamara;

    private AdView mAdView = null;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this,
                "ca-app-pub-6925377246649300~6638983558");

        mAdView = findViewById(R.id.adView1);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        AdRequest adRequest1 = new AdRequest.Builder().build();
        mInterstitialAd.loadAd(adRequest1);

        mInterstitialAd.setAdListener(new AdListener(){

            @Override
            public void onAdLoaded() {

                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                }
            }

            @Override
            public void onAdOpened() {


            }

            @Override
            public void onAdFailedToLoad(int errorCode) {

            }
        });

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-6925377246649300/5694525409");

        sonidoCamara = MediaPlayer.create(this, R.raw.sonido_camara);

        switchCompat1 = findViewById(R.id.switch1);
        switchCompat2 = findViewById(R.id.switch2);
        switchCompat3 = findViewById(R.id.switch3);
        switchCompat4 = findViewById(R.id.switch4);
        switchCompat5 = findViewById(R.id.switch5);
        switchCompat6 = findViewById(R.id.switch6);

        final SwitchCompat[] switchCompats = new SwitchCompat[]{switchCompat1, switchCompat2,
                switchCompat3, switchCompat4, switchCompat5, switchCompat6};

        relColorFilter1 = findViewById(R.id.relColorFilter1);
        relColorFilter2 = findViewById(R.id.relColorFilter2);
        relColorFilter3 = findViewById(R.id.relColorFilter3);
        relColorFilter4 = findViewById(R.id.relColorFilter4);
        relColorFilter5 = findViewById(R.id.relColorFilter5);
        relColorFilter6 = findViewById(R.id.relColorFilter6);

        final RelativeLayout[] relativesColorFilters = new RelativeLayout[]{relColorFilter1, relColorFilter2, relColorFilter3,
                relColorFilter4, relColorFilter5, relColorFilter6};

        for (int i = 0; i < switchCompats.length; i++)
        {
            final int finalI = i;
            switchCompats[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                    if (isChecked)
                    {
                        relativesColorFilters[finalI].setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        relativesColorFilters[finalI].setVisibility(View.GONE);
                    }
                }
            });
        }

        textureView = findViewById(R.id.texture_view);

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_REQUEST_CODE);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraFacingBack = CameraCharacteristics.LENS_FACING_BACK;
        cameraFacingFront = CameraCharacteristics.LENS_FACING_FRONT;

        fabPhoto = findViewById(R.id.fab_take_photo);
        fabFrontPhoto = findViewById(R.id.fab_take_frontcamera);
        fabTorchPhoto = findViewById(R.id.fab_take_torchcamera);
        createImageGallery();

        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {

                setUpCamera(cameraFacingBack);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    openCameraManager();
                }
                else
                {
                    openCameraOld();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        };

        stateCallBack = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camDevice) {

                MainActivity.this.cameraDevice = camDevice;
                createPreviewSession();
            }

            @Override
            public void onDisconnected(CameraDevice camDevice) {

                camDevice.close();
                MainActivity.this.cameraDevice = null;
            }

            @Override
            public void onError(CameraDevice camDevice, int i) {

                camDevice.close();
                MainActivity.this.cameraDevice = null;
            }
        };

        fabPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                lock();
                FileOutputStream outputPhoto = null;

                try
                {
                    outputPhoto = new FileOutputStream(createImageFile(galleryFolder));
                    textureView.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, outputPhoto);

                    sonidoCamara.start();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    unlock();
                    try
                    {
                        if (outputPhoto != null)
                        {
                            outputPhoto.close();
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        });

        fabFrontPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                closeCamera();
                closeBackgroundThread();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    if (frontCamera)
                    {
                        setUpCamera(cameraFacingBack);
                        frontCamera = false;
                        fabFrontPhoto.setImageResource(R.drawable.change_camara_off);
                    }
                    else
                    {
                        setUpCamera(cameraFacingFront);
                        frontCamera = true;
                        fabFrontPhoto.setImageResource(R.drawable.change_camara_on);
                    }

                    openCameraManager();
                }
                else
                {
                    closeCamera();
                    closeBackgroundThread();

                    Camera.CameraInfo currentCamInfo = new Camera.CameraInfo();

                    if (currentCamInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                    {
                        camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                        Camera.Parameters parameters = camera.getParameters();
                        camera.setParameters(parameters);
                        camera.startPreview();

                        fabFrontPhoto.setImageResource(R.drawable.change_camara_off);
                    }
                    else
                    {
                        camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                        Camera.Parameters parameters = camera.getParameters();
                        camera.setParameters(parameters);
                        camera.startPreview();

                        fabFrontPhoto.setImageResource(R.drawable.change_camara_on);
                    }
                }
            }
        });

        fabTorchPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    if (flashLightStatus)
                    {
                        try
                        {
                            cameraManager.setTorchMode(cameraId, false);
                            flashLightStatus = false;
                            fabTorchPhoto.setImageResource(R.drawable.torch_camara_off);
                        }
                        catch (CameraAccessException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        if (frontCamera == true)
                        {
                            try
                            {
                                cameraManager.setTorchMode(cameraId, true);
                                flashLightStatus = true;
                                fabTorchPhoto.setImageResource(R.drawable.torch_camara_on);
                            }
                            catch (CameraAccessException e)
                            {
                                e.printStackTrace();
                            }
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(), R.string.mensaje_aviso_camara, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                else
                {
                    if (flashLightStatus)
                    {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        flashLightStatus = false;
                        fabTorchPhoto.setImageResource(R.drawable.torch_camara_off);
                    }
                    else
                    {
                        Camera.CameraInfo currentCamInfo = new Camera.CameraInfo();

                        if (currentCamInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                        {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            flashLightStatus = true;
                            fabTorchPhoto.setImageResource(R.drawable.torch_camara_on);
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(), R.string.mensaje_aviso_camara, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        openBackgroundThread();

        if (textureView.isAvailable())
        {
            setUpCamera(cameraFacingBack);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                openCameraManager();
            }
            else
            {
                openCameraOld();
            }
        }
        else
        {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        closeCamera();
        closeBackgroundThread();

        sonidoCamara.stop();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            try
            {
                cameraManager.setTorchMode(cameraId, false);
                flashLightStatus = false;
                fabFrontPhoto.setImageResource(R.drawable.change_camara_off);
            }
            catch (CameraAccessException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            flashLightStatus = false;
            fabFrontPhoto.setImageResource(R.drawable.change_camara_on);
        }
    }

    private void setUpCamera(int cameraFacing)
    {
        try
        {
            for (String cameraId : cameraManager.getCameraIdList())
            {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);

                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == cameraFacing)
                {
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                    previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
                    this.cameraId = cameraId;
                }
            }
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    private void openCameraManager()
    {
        try
        {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED)
            {
                cameraManager.openCamera(cameraId, stateCallBack, backgroundHandler);
                cameraId = cameraManager.getCameraIdList()[0];
            }
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    private void openCameraOld()
    {
        camera.open();
        Camera.Parameters parameters = camera.getParameters();
        camera.setParameters(parameters);
        camera.startPreview();
    }

    private void openBackgroundThread()
    {
        backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void closeCamera()
    {
        if (cameraCaptureSession != null)
        {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null)
        {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void closeBackgroundThread()
    {
        if (backgroundHandler != null)
        {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    private void createPreviewSession()
    {
        try
        {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {

                            if (cameraDevice == null)
                            {
                                return;
                            }

                            try{
                                captureRequest = captureRequestBuilder.build();
                                MainActivity.this.cameraCaptureSession = cameraCaptureSession;
                                MainActivity.this.cameraCaptureSession.setRepeatingRequest(captureRequest,
                                        null, backgroundHandler);
                            }
                            catch (CameraAccessException e)
                            {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {


                        }
                    }, backgroundHandler);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    private void createImageGallery()
    {
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        galleryFolder = new File(storageDirectory + "/CameraPrueba/");

        if (!galleryFolder.exists())
        {
            boolean wasCreated = galleryFolder.mkdirs();

            if (!wasCreated)
            {
                Log.e("CapturedImages", "Failed to create directory");
            }
        }
    }

    private File createImageFile(File galleryFolder) throws IOException
    {
        String timeStamp = new SimpleDateFormat("yyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFilename = "cp_" + timeStamp + "_";
        return File.createTempFile(imageFilename, ".jpg", galleryFolder);
    }

    private void lock()
    {
        try
        {
            cameraCaptureSession.capture(captureRequestBuilder.build(), null, backgroundHandler);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    private void unlock()
    {
        try
        {
            cameraCaptureSession.setRepeatingRequest(
                    captureRequestBuilder.build(), null, backgroundHandler);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }
}
