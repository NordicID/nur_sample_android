package com.nordicid.controllers;

import android.os.AsyncTask;
import android.util.Log;
import com.nordicid.helpers.UpdateContainer;
import com.nordicid.rfiddemo.Main;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class UpdateController {

    protected static String LOG_TAG = "Update Controller";
    protected String mAppUpdateSource;
    protected String mBldrUpdateSource;
    protected String hwType = null;
    protected String appVersion = null;
    protected String bldrVersion = null;
    protected String mFilePath = null;
    protected String mAppUpdateVersion = null;
    protected String mBldrUpdateVersion = null;

    public void setFilePath(String filePath){
        mFilePath = filePath;
    }

    public String getFilePath() { return mFilePath; }

    public boolean isFileSet(){
        return mFilePath != null;
    }

    public void setHWType(String module){
        hwType = module;
    }

    public void setAPPVersion(String version){
        appVersion = version;
    }

    public void setBldrVersion(String version){
        bldrVersion = version;
    }

    public void setAppSource(String source){
        mAppUpdateSource = source;
    }

    public void setBldrSource(String version){
        bldrVersion = version;
    }

    public String getAvailableAppUpdateVerion(){
        return mAppUpdateVersion;
    }

    public String getAvailableBldrUpdateVerion(){ return mBldrUpdateVersion; }

    private String Stream2String(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        is.close();
        return sb.toString();
    }

    /** Async task running GET request
     *  Should wait for completion before doing anything else
     *  Returns Json String
     **/
    private class ExecuteGetOperation extends AsyncTask<Boolean, Void, String>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Boolean... params) {
            try {
                URL srcURL = new URL((params[0]) ? mAppUpdateSource : mBldrUpdateSource);
                HttpURLConnection connection = (HttpURLConnection) srcURL.openConnection();
                connection.setRequestMethod("GET");
                InputStream in = new BufferedInputStream(connection.getInputStream());
                return Stream2String(in);
            } catch (Exception e){
                Log.e(LOG_TAG, e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }

    /**
     * Async task downloading the file to Cache
     * @return
     */
    private class ExecuteDownloadOperation extends AsyncTask<UpdateContainer, Void, String>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(UpdateContainer... params) {
            try{
                if(params[0] != null) {
                    URL fileUrl = new URL(params[0].url);
                    URLConnection connection = fileUrl.openConnection();

                    InputStream sin = connection.getInputStream();
                    BufferedInputStream bis = new BufferedInputStream(sin, 1024);

                    File file = new File(Main.getInstance().getFilesDir(), params[0].name);

                    if(file.exists())
                        file.delete();
                    file.createNewFile();
                    FileOutputStream fos = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];

                    int len;
                    while ((len = sin.read(buffer)) != -1)
                    {
                        fos.write(buffer, 0, len);
                    }

                    fos.flush();
                    fos.close();
                    sin.close();
                    return params[0].name;
                }
            } catch (Exception e){
                // this ide is retarded putting a comment makes the catch block non empty
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }

    private List<UpdateContainer> fetchAvailableUpdates(boolean target){

        List<UpdateContainer> availableUpdates = new ArrayList<>();
        if (hwType == null)
            return availableUpdates;

        try{
            /* invoke async task and wait for result */
            String data = new ExecuteGetOperation().execute(target).get();

            if(data != null){
                JSONArray firmwares = new JSONObject(data).getJSONArray("firmwares");

                for ( int it = 0; it < firmwares.length(); it++ ) {
                    JSONObject firmware = firmwares.getJSONObject(it);
                    UpdateContainer fwUpdate = new UpdateContainer();
                    fwUpdate.buildtime = new java.util.Date(Long.parseLong(firmware.getString("buildtime")) *1000).toString();
                    fwUpdate.md5 = firmware.getString("md5");
                    fwUpdate.url = firmware.getString("url");
                    fwUpdate.version = firmware.getString("version");
                    fwUpdate.name = firmware.getString("name");
                    /** Can be taken out
                     *  Only usefull for testing hardware match
                     **/
                    JSONArray hardware = firmware.getJSONArray("hw");
                    fwUpdate.hw = new String[hardware.length()];
                    for (int ite = 0; ite < hardware.length(); ite++)
                    {
                        fwUpdate.hw[ite] = hardware.getString(ite);
                    }
                    /* Only add to list if targets current device */
                    // TODO fix this when DFU hw is sorted out
                    if(Arrays.asList(fwUpdate.hw).contains(hwType))
                        availableUpdates.add(fwUpdate);
                }
            }
        } catch (Exception e){
            Log.e(LOG_TAG,e.getMessage());
        }
        //Log.e("fetchAvailableUpdates size=", Integer.toString(availableUpdates.size()));
        return availableUpdates;
    }

    public List<UpdateContainer> fetchBldrUpdates(){
        return  fetchAvailableUpdates(false);
    }

    public List<UpdateContainer> fetchApplicationUpdates(){
        return  fetchAvailableUpdates(true);
    }

    public UpdateContainer fetchLastApplicationUpdate(){
        List<UpdateContainer> updatesList = fetchAvailableUpdates(true);
        return (updatesList.isEmpty()) ? null : updatesList.get(0);
    }

    public UpdateContainer fetchLastBldrUpdate(){
        List<UpdateContainer> updatesList = fetchAvailableUpdates(false);
        if(updatesList.size() == 0) return null;
        return updatesList.get(0);
        //return (updatesList.isEmpty()) ? null : updatesList.get(0);
    }

    public String grabUpdateFile(UpdateContainer update){
        try {
            return new ExecuteDownloadOperation().execute(update).get();
        } catch (Exception e){
            return null;
        }
    }

    abstract public boolean startUpdate();
    abstract public void abortUpdate();
    abstract public void pauseUpdate();
    abstract public void resumeUpdate();

    /**
     * Check for updates using current version
     * @return true if update is available false if not
     */
    public boolean isAppUpdateAvailable(){
        try {
            UpdateContainer last = fetchLastApplicationUpdate();
            if(checkVersion(appVersion,last.version)){
                mAppUpdateVersion = last.version;
                return true;
            }
        } catch (Exception e){
            // TODO
        }
        return  false;
    }

    /**
     * Check for updates using current version
     * @return true if update is available false if not
     */
    public boolean isBldrUpdateAvailable(){
        try {
            UpdateContainer last = fetchLastBldrUpdate();
            if(checkVersion(bldrVersion,last.version)){
                mBldrUpdateVersion = last.version;
                return true;
            }
        } catch (Exception e){

        }
        return  false;
    }

    /**
     * Compares remote and current versions
     * @param currentVersion
     * @param remoteVersion
     * @return true if remote is newer false if not
     */
    abstract public boolean checkVersion(String currentVersion, String remoteVersion);


}
