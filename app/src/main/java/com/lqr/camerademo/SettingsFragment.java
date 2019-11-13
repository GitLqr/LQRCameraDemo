package com.lqr.camerademo;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;


/**
 * @创建者 LQR
 * @时间 19-11-11
 * @描述 相机配置界面
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String KEY_PREF_PREV_SIZE = "preview_size";
    public static final String KEY_PREF_PIC_SIZE = "picture_size";
    public static final String KEY_PREF_VIDEO_SIZE = "video_size";
    public static final String KEY_PREF_FLASH_MODE = "flash_mode";
    public static final String KEY_PREF_FOCUS_MODE = "focus_mode";
    public static final String KEY_PREF_WHITE_BALANCE = "white_balance";
    public static final String KEY_PREF_SCENE_MODE = "scene_mode";
    public static final String KEY_PREF_GPS_DATA = "gps_data";
    public static final String KEY_PREF_EXPOS_COMP = "exposoure_compensation";
    public static final String KEY_PREF_JPEG_QUALITY = "jpeg_quality";

    static Camera sCamera;
    static Camera.Parameters sParameters;

    public static void passCamera(Camera camera) {
        sCamera = camera;
        sParameters = camera.getParameters();
    }

    public static void setDefault(SharedPreferences sharedPrefs) {
        String valuePreviewSize = sharedPrefs.getString(KEY_PREF_PREV_SIZE, null);
        if (valuePreviewSize == null) {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(KEY_PREF_PREV_SIZE, getDefaultPreviewSize());
            editor.putString(KEY_PREF_PIC_SIZE, getDefaultPictureSize());
            editor.putString(KEY_PREF_VIDEO_SIZE, getDefaultVideoSize());
            editor.putString(KEY_PREF_FOCUS_MODE, getDefaultFocusMode());
            editor.apply();
        }
    }

    private static String getDefaultPreviewSize() {
        Camera.Size previewSize = sParameters.getPreviewSize();
        return previewSize.width + "x" + previewSize.height;
    }

    private static String getDefaultPictureSize() {
        Camera.Size pictureSize = sParameters.getPictureSize();
        return pictureSize.width + "x" + pictureSize.height;
    }

    private static String getDefaultVideoSize() {
        Camera.Size videoSize = sParameters.getPreferredPreviewSizeForVideo();
        return videoSize.width + "x" + videoSize.height;
    }

    private static String getDefaultFocusMode() {
        List<String> supportedFocusModes = sParameters.getSupportedFocusModes();
        if (supportedFocusModes.contains("continuous-picture")) {
            return "continuous-picture";
        }
        return "continuous-video";
    }

    public static void init(SharedPreferences sharedPref) {
        setPreviewSize(sharedPref.getString(KEY_PREF_PREV_SIZE, ""));
        setPictureSize(sharedPref.getString(KEY_PREF_PIC_SIZE, ""));
        setFlashMode(sharedPref.getString(KEY_PREF_FLASH_MODE, ""));
        setFocusMode(sharedPref.getString(KEY_PREF_FOCUS_MODE, ""));
        setWhiteBalance(sharedPref.getString(KEY_PREF_WHITE_BALANCE, ""));
        setSceneMode(sharedPref.getString(KEY_PREF_SCENE_MODE, ""));
        setExposComp(sharedPref.getString(KEY_PREF_EXPOS_COMP, ""));
        setJpegQuality(sharedPref.getString(KEY_PREF_JPEG_QUALITY, ""));
        setGpsData(sharedPref.getBoolean(KEY_PREF_GPS_DATA, false));
        sCamera.stopPreview();
        sCamera.setParameters(sParameters);
        sCamera.startPreview();
    }

    public static void setPreviewSize(String value) {
        String[] split = value.split("x");
        sParameters.setPreviewSize(Integer.valueOf(split[0]), Integer.valueOf(split[1]));
    }

    public static void setPictureSize(String value) {
        String[] split = value.split("x");
        sParameters.setPictureSize(Integer.valueOf(split[0]), Integer.valueOf(split[1]));
    }

    public static void setFocusMode(String value) {
        sParameters.setFocusMode(value);
    }

    public static void setFlashMode(String value) {
        sParameters.setFocusMode(value);
    }

    private static void setWhiteBalance(String value) {
        sParameters.setWhiteBalance(value);
    }

    private static void setSceneMode(String value) {
        sParameters.setSceneMode(value);
    }

    private static void setExposComp(String value) {
        if (value != null && !value.isEmpty()) {
            sParameters.setExposureCompensation(Integer.valueOf(value));
        }
    }

    private static void setJpegQuality(String value) {
        if (value != null && !value.isEmpty()) {
            sParameters.setJpegQuality(Integer.valueOf(value));
        }
    }

    private static void setGpsData(Boolean value) {
        if (value.equals(false)) {
            sParameters.removeGpsData();
        }
    }

    /**
     * 获取所有配置，显示当前值
     *
     * @param pref
     */
    private static void initSummary(Preference pref) {
        if (pref instanceof PreferenceGroup) {
            PreferenceGroup prefGroup = (PreferenceGroup) pref;
            for (int i = 0; i < prefGroup.getPreferenceCount(); i++) {
                initSummary(prefGroup.getPreference(i));
            }
        } else {
            updatePrefSummary(pref);
        }
    }

    /**
     * 显示ListPreference的当前值
     *
     * @param pref
     */
    private static void updatePrefSummary(Preference pref) {
        if (pref instanceof ListPreference) {
            pref.setSummary(((ListPreference) pref).getEntry());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        loadSupportedPreviewSize();
        loadSupportedPictureSize();
        loadSupportedVideoSize();
        loadSupportedFlashMode();
        loadSupportedFocusMode();
        loadSupportedWhiteBalance();
        loadSupportedSceneMode();
        loadSupportedExposeCompensation();
        initSummary(getPreferenceScreen());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ListView lv = view.findViewById(android.R.id.list);
        lv.setBackgroundColor(Color.WHITE);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePrefSummary(findPreference(key));
        switch (key) {
            case KEY_PREF_PREV_SIZE:
                setPreviewSize(sharedPreferences.getString(key, ""));
                break;
            case KEY_PREF_PIC_SIZE:
                setPictureSize(sharedPreferences.getString(key, ""));
                break;
            case KEY_PREF_FOCUS_MODE:
                setFocusMode(sharedPreferences.getString(key, ""));
                break;
            case KEY_PREF_FLASH_MODE:
                setFlashMode(sharedPreferences.getString(key, ""));
                break;
            case KEY_PREF_WHITE_BALANCE:
                setWhiteBalance(sharedPreferences.getString(key, ""));
                break;
            case KEY_PREF_SCENE_MODE:
                setSceneMode(sharedPreferences.getString(key, ""));
                break;
            case KEY_PREF_EXPOS_COMP:
                setExposComp(sharedPreferences.getString(key, ""));
                break;
            case KEY_PREF_JPEG_QUALITY:
                setJpegQuality(sharedPreferences.getString(key, ""));
                break;
            case KEY_PREF_GPS_DATA:
                setGpsData(sharedPreferences.getBoolean(key, false));
                break;
        }
        sCamera.stopPreview();
        sCamera.setParameters(sParameters);
        sCamera.startPreview();
    }

    private void loadSupportedPreviewSize() {
        cameraSizeListToListPreference(sParameters.getSupportedPreviewSizes(), KEY_PREF_PREV_SIZE);
    }

    private void loadSupportedPictureSize() {
        cameraSizeListToListPreference(sParameters.getSupportedPictureSizes(), KEY_PREF_PIC_SIZE);
    }

    private void loadSupportedVideoSize() {
        cameraSizeListToListPreference(sParameters.getSupportedVideoSizes(), KEY_PREF_VIDEO_SIZE);
    }

    private void loadSupportedFlashMode() {
        stringListToListPreference(sParameters.getSupportedFlashModes(), KEY_PREF_FLASH_MODE);
    }

    private void loadSupportedFocusMode() {
        stringListToListPreference(sParameters.getSupportedFocusModes(), KEY_PREF_FOCUS_MODE);
    }

    private void loadSupportedWhiteBalance() {
        stringListToListPreference(sParameters.getSupportedWhiteBalance(), KEY_PREF_WHITE_BALANCE);
    }

    private void loadSupportedSceneMode() {
        stringListToListPreference(sParameters.getSupportedSceneModes(), KEY_PREF_SCENE_MODE);
    }

    private void loadSupportedExposeCompensation() {
        int minExposComp = sParameters.getMinExposureCompensation();
        int maxExposComp = sParameters.getMaxExposureCompensation();
        List<String> exposComp = new ArrayList<>();
        for (int value = minExposComp; value <= maxExposComp; value++) {
            exposComp.add(Integer.toString(value));
        }
        stringListToListPreference(exposComp, KEY_PREF_EXPOS_COMP);
    }

    private void cameraSizeListToListPreference(List<Camera.Size> list, String key) {
        List<String> stringList = new ArrayList<>();
        for (Camera.Size size : list) {
            String stringSize = size.width + "x" + size.height;
            stringList.add(stringSize);
        }
        stringListToListPreference(stringList, key);
    }

    private void stringListToListPreference(List<String> list, String key) {
        if (list != null && list.size() > 0) {
            final CharSequence[] charSeq = list.toArray(new CharSequence[list.size()]);
            ListPreference listPref = (ListPreference) getPreferenceScreen().findPreference(key);
            if (listPref != null) {
                listPref.setEntries(charSeq);
                listPref.setEntryValues(charSeq);
            }
        }
    }

}
