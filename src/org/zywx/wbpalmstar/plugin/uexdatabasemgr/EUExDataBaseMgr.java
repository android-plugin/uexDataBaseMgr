package org.zywx.wbpalmstar.plugin.uexdatabasemgr;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.DataHelper;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;
import org.zywx.wbpalmstar.plugin.uexdatabasemgr.vo.DataBaseVO;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static org.zywx.wbpalmstar.engine.universalex.EUExCallback.F_C_SUCCESS;

public class EUExDataBaseMgr extends EUExBase {

    private static final String F_OPENDATABASE_CALLBACK = "uexDataBaseMgr.cbOpenDataBase";
    private static final String F_EXECSQL_CALLBACK = "uexDataBaseMgr.cbExecuteSql";
    private static final String F_SELECTSQL_CALLBACK = "uexDataBaseMgr.cbSelectSql";
    private static final String F_CBTRANSACTION_CALLBACK = "uexDataBaseMgr.cbTransaction";
    private static final String F_CLOSEDATABASE_CALLBACK = "uexDataBaseMgr.cbCloseDataBase";

    private HashMap<String, SQLiteDatabase> m_dbMap;
    private HashMap<String, DatabaseHelper> m_dbHMap;
    private List<String> opCodeList = new ArrayList<String>();
    private Context m_eContext;
    private static int sCurrentId;

    public EUExDataBaseMgr(Context context, EBrowserView inParent) {
        super(context, inParent);
        m_dbMap = new HashMap<String, SQLiteDatabase>();
        m_dbHMap = new HashMap<String, DatabaseHelper>();
        m_eContext = context;
    }

    private String getDBFlg(String dbName, String opCode) {
        return dbName + opCode;

    }

    public int openDataBase(String[] parm) {
        if (parm.length < 2) {
            return EUExCallback.F_C_FAILED;
        }
        String inDBName = parm[0];
        String inOpCode = parm[1];
        int dbVersion=1;
        if (parm.length>2){
            dbVersion= Integer.parseInt(parm[2]);
        }
        if (inOpCode == null || inOpCode.length() == 0) {
            inOpCode = "0";
        }
        if (!BUtility.isNumeric(inOpCode)) {
            inOpCode = "0";
        }
        try {
            if (opCodeList.contains(inOpCode)) {
                return -1;
            }

            DatabaseHelper m_databaseHelper = new DatabaseHelper(m_eContext,
                    inDBName, dbVersion);
            String dbFlg = getDBFlg(inDBName, inOpCode);
            m_dbMap.put(dbFlg, m_databaseHelper.getWritableDatabase());
            m_dbHMap.put(dbFlg, m_databaseHelper);
            opCodeList.add(inOpCode);
            jsCallback(F_OPENDATABASE_CALLBACK, Integer.parseInt(inOpCode),
                    EUExCallback.F_C_INT, F_C_SUCCESS);
            return F_C_SUCCESS;

        } catch (Exception e) {
            e.printStackTrace();
            jsCallback(F_OPENDATABASE_CALLBACK, Integer.parseInt(inOpCode),
                    EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
            return EUExCallback.F_C_FAILED;
        }

    }

    public DataBaseVO open(String[] params) {
        DataBaseVO dataBaseVO = new DataBaseVO();
        dataBaseVO.id = generateId();
        dataBaseVO.name = params[0];
        if (params.length>1){
            dataBaseVO.version= Integer.parseInt(params[1]);
        }
        int result = openDataBase(new String[]{
                dataBaseVO.name,
                dataBaseVO.id,
                String.valueOf(dataBaseVO.version)
        });
        return result == F_C_SUCCESS ? dataBaseVO : null;
    }

    private String generateId() {
        sCurrentId++;
        return String.valueOf(sCurrentId);
    }

    public boolean executeSql(String[] parm) {
        if (parm.length < 3) {
            return false;
        }
        final String inSql = parm[2];
        final String inDBName = parm[0];
        final String inOpCode = parm[1];
        String executeSqlFuncId = null;
        if (parm.length == 4) {
            executeSqlFuncId = parm[3];
        }
        final String finalExecuteSqlFuncId = executeSqlFuncId;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SQLiteDatabase object = m_dbMap.get(getDBFlg(inDBName, inOpCode));
                    if (object != null) {

                        object.execSQL(inSql);
                        jsCallback(F_EXECSQL_CALLBACK, Integer.parseInt(inOpCode),
                                EUExCallback.F_C_INT, F_C_SUCCESS);
                        if (null != finalExecuteSqlFuncId) {
                            callbackToJs(Integer.parseInt(finalExecuteSqlFuncId), false, 0);
                        }

                    } else {
                        jsCallback(F_EXECSQL_CALLBACK, Integer.parseInt(inOpCode),
                                EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
                        if (null != finalExecuteSqlFuncId) {
                            callbackToJs(Integer.parseInt(finalExecuteSqlFuncId), false, 1);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    jsCallback(F_EXECSQL_CALLBACK, Integer.parseInt(inOpCode),
                            EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
                    if (null != finalExecuteSqlFuncId) {
                        callbackToJs(Integer.parseInt(finalExecuteSqlFuncId), false, 1);
                    }
                }
            }
        }).start();
        return true;

    }

    public void sql(String[] params) {
        DataBaseVO dbVO = DataHelper.gson.fromJson(params[0], DataBaseVO.class);
        final String[] inParams = new String[params.length + 1];
        inParams[0] = dbVO.name;
        inParams[1] = dbVO.id;
        inParams[2] = params[1];
        if (params.length > 2) {
            inParams[3] = params[2];
        }
        executeSql(inParams);

    }

    public void selectSqlOnThread(String[] parm) {
        if (parm.length < 3) {
            return;
        }
        String inDBName = parm[0], inOpCode = parm[1], inSql = parm[2], selectSqlFuncId = null;
        if (!BUtility.isNumeric(inOpCode)) {
            inOpCode = "0";
        }
        if (parm.length == 4) {
            selectSqlFuncId = parm[3];
        }
        SQLiteDatabase object = m_dbMap.get(getDBFlg(inDBName, inOpCode));
        if (object != null) {
            try {
                Cursor cursor = object.rawQuery(inSql, null);
                if (cursor != null) {

                    JSONArray jsonItems = new JSONArray();
                    while (cursor.moveToNext()) {
                        int count = cursor.getColumnCount();
                        JSONObject jo = new JSONObject();
                        for (int i = 0; i < count; i++) {
                            String key = cursor.getColumnName(i);
                            String value = null;
                            int sysVersion = Integer.parseInt(Build.VERSION.SDK);
                            if (sysVersion < 11) {
                                value = cursor.getString(i);
                            } else {
                                switch (cursor.getType(i)) {
                                    case Cursor.FIELD_TYPE_NULL:
                                        value = cursor.getString(i);
                                        break;
                                    case Cursor.FIELD_TYPE_BLOB:
                                        value = new String(cursor.getBlob(i));
                                        break;
                                    case Cursor.FIELD_TYPE_FLOAT:
                                        double dl = cursor.getDouble(i);
                                        value = String.valueOf(formatNum(dl));
                                        break;
                                    case Cursor.FIELD_TYPE_INTEGER:
                                        value = String.valueOf(cursor.getInt(i));
                                        break;
                                    case Cursor.FIELD_TYPE_STRING:
                                        value = cursor.getString(i);
                                        break;
                                    default:
                                        break;
                                }
                            }

                            if (!TextUtils.isEmpty(value)) {
                                jo.put(key, value);
                            } else {
                                jo.put(key, "");
                            }
                        }
                        jsonItems.put(jo);
                    }
                    jsCallback(F_SELECTSQL_CALLBACK,
                            Integer.parseInt(inOpCode), EUExCallback.F_C_JSON,
                            BUtility.transcoding(jsonItems.toString()));
                    if (null != selectSqlFuncId) {
                        callbackToJs(Integer.parseInt(selectSqlFuncId), false, 0, jsonItems);
                    }
                } else {
                    jsCallback(F_SELECTSQL_CALLBACK,
                            Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
                            EUExCallback.F_C_FAILED);
                    if (null != selectSqlFuncId) {
                        callbackToJs(Integer.parseInt(selectSqlFuncId), false, 1, new JSONArray());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                jsCallback(F_SELECTSQL_CALLBACK, Integer.parseInt(inOpCode),
                        EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
                if (null != selectSqlFuncId) {
                    callbackToJs(Integer.parseInt(selectSqlFuncId), false, 1);
                }
            }
        } else {
            jsCallback(F_SELECTSQL_CALLBACK, Integer.parseInt(inOpCode),
                    EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
            if (null != selectSqlFuncId) {
                callbackToJs(Integer.parseInt(selectSqlFuncId), false, 1, new JSONArray());
            }
        }
    }

    public void selectSql(final String[] parm) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                selectSqlOnThread(parm);
            }
        }).start();
    }

    public void select(String[] params) {
        DataBaseVO dbVO = DataHelper.gson.fromJson(params[0], DataBaseVO.class);
        selectSql(new String[]{
                dbVO.name,
                dbVO.id,
                params[1],
                params.length > 2 ? params[2] : null
        });
    }

    private String formatNum(double value) {
        String retValue = null;
        DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(0);
        df.setMaximumFractionDigits(2);
        retValue = df.format(value);
        retValue = retValue.replaceAll(",", "");
        return retValue;
    }

    private static boolean isJson(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        try {
            JSONObject jsonObject = new JSONObject(value);
            return true;
        } catch (JSONException e) {
            if (BDebug.DEBUG) {
                e.printStackTrace();
            }
            return false;
        }
    }

    public boolean beginTransaction(String[] parm) {
        String inDBName = null, inOpCode = null;
        boolean isJson = isJson(parm[0]);
        if (isJson) {
            DataBaseVO dbVO = DataHelper.gson.fromJson(parm[0], DataBaseVO.class);
            inDBName = dbVO.name;
            inOpCode = dbVO.id;
        } else {
            inDBName = parm[0];
            inOpCode = parm[1];
        }


        if (!BUtility.isNumeric(inOpCode)) {
            inOpCode = "0";
        }
        try {
            SQLiteDatabase object = m_dbMap.get(getDBFlg(inDBName, inOpCode));
            if (object != null) {
                object.beginTransaction();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;

    }

    public void endTransaction(String[] parm) {
        String inDBName = null, inOpCode = null, transactionFuncId = null;
        boolean isJson = isJson(parm[0]);
        if (isJson) {
            DataBaseVO dbVO = DataHelper.gson.fromJson(parm[0], DataBaseVO.class);
            inDBName = dbVO.name;
            inOpCode = dbVO.id;
            if (parm.length > 2) {
                transactionFuncId = parm[2];
            }
        } else {
            inDBName = parm[0];
            inOpCode = parm[1];
            if (parm.length > 3) {
                transactionFuncId = parm[3];
            }
        }

        if (!BUtility.isNumeric(inOpCode)) {
            inOpCode = "0";
        }
        SQLiteDatabase object = m_dbMap.get(getDBFlg(inDBName, inOpCode));
        if (object != null) {
            try {
                object.setTransactionSuccessful();
                jsCallback(F_CBTRANSACTION_CALLBACK,
                        Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
                        F_C_SUCCESS);
                if (null != transactionFuncId) {
                    callbackToJs(Integer.parseInt(transactionFuncId), false, 0);
                }
            } catch (IllegalStateException e) {
                jsCallback(F_CBTRANSACTION_CALLBACK,
                        Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
                        EUExCallback.F_C_FAILED);
                if (null != transactionFuncId) {
                    callbackToJs(Integer.parseInt(transactionFuncId), false, 1);
                }
            } finally {
                object.endTransaction();
            }
        } else {
            jsCallback(F_CBTRANSACTION_CALLBACK, Integer.parseInt(inOpCode),
                    EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
            if (null != transactionFuncId) {
                callbackToJs(Integer.parseInt(transactionFuncId), false, 1);
            }
        }
    }

    public void transactionEx(final String[] params) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                transactionOnThread(params);
            }
        }).start();
    }

    private void transactionOnThread(String[] params){
        DataBaseVO dataBaseVO = DataHelper.gson.fromJson(params[0], DataBaseVO.class);
        String[] sqls = DataHelper.gson.fromJson(params[1], new TypeToken<String[]>() {
        }.getType());
        String callbackFuncId = null;
        if (params.length > 2) {
            callbackFuncId = params[2];
        }
        String inDBName = dataBaseVO.name, inOpCode = dataBaseVO.id;
        SQLiteDatabase object = m_dbMap.get(getDBFlg(inDBName, inOpCode));
        if (object == null) {
            return;
        }
        object.beginTransaction();
        boolean result=false;
        try {
            for (int i = 0; i < sqls.length; i++) {
                object.execSQL(sqls[i]);
            }
            object.setTransactionSuccessful();
            result=true;
        }catch (Exception e){
            if (BDebug.DEBUG){
                e.printStackTrace();
            }
            result=false;
        }finally {
            object.endTransaction();
        }
        if (!TextUtils.isEmpty(callbackFuncId)){
            callbackToJs(Integer.parseInt(callbackFuncId),false,result?0:1);
        }
    }

    public int closeDataBase(String[] parm) {
        if (parm.length != 2) {
            return EUExCallback.F_C_FAILED;
        }
        String inDBName = parm[0], inOpCode = parm[1];
        if (inOpCode == null || inOpCode.length() == 0) {
            inOpCode = "0";
        }
        if (!BUtility.isNumeric(inOpCode)) {
            inOpCode = "0";
        }
        DatabaseHelper dbh = m_dbHMap.remove(getDBFlg(inDBName, inOpCode));
        if (dbh != null) {
            try {
                dbh.close();
                dbh = null;
                SQLiteDatabase object = m_dbMap
                        .remove(getDBFlg(inDBName, inOpCode));
                object.close();
                object = null;
                opCodeList.remove(inOpCode);
                jsCallback(F_CLOSEDATABASE_CALLBACK,
                        Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
                        F_C_SUCCESS);
                return F_C_SUCCESS;
            } catch (Exception e) {
                jsCallback(F_CLOSEDATABASE_CALLBACK,
                        Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
                        EUExCallback.F_C_FAILED);
                return EUExCallback.F_C_FAILED;
            }

        } else {
            jsCallback(F_CLOSEDATABASE_CALLBACK, Integer.parseInt(inOpCode),
                    EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
            return EUExCallback.F_C_FAILED;
        }
    }

    public boolean close(String[] params) {
        DataBaseVO dbVO = DataHelper.gson.fromJson(params[0], DataBaseVO.class);
        int result = closeDataBase(new String[]{
                dbVO.name,
                dbVO.id
        });
        return result == 0;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        String m_dbName;
        Context m_context;

        DatabaseHelper(Context context, String dbName, int dbVer) {
            super(context, dbName, null, dbVer);
            m_dbName = dbName;
            m_context = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            BDebug.i("oldVersion",oldVersion,"newVersion",newVersion);
            m_context.deleteDatabase(m_dbName);

        }
    }

    public void copyDataBaseFile(String[] params){
        String inputUserPath=params[0];
        String dbName= getDBName(inputUserPath);
        new DatabaseHelper(mContext,dbName,1);
        int callbackId=-1;
        if (params.length>1){
            callbackId= Integer.parseInt(params[1]);
        }
        String realUserPath=BUtility.makeRealPath(inputUserPath,mBrwView);
        copyAndCallbackOnThread(realUserPath,inputUserPath,callbackId);

    }

    private void copyAndCallbackOnThread(final String realUserPath, final String inputUserPath, final int callbackId){
        final String targetPath=getTargetPath(getDBName(inputUserPath));
             new Thread(new Runnable() {
                @Override
                public void run() {
                    if (inputUserPath.startsWith("res://")){
                        boolean result=copyAssetsToFilesystem(realUserPath,targetPath);
                    if (callbackId!=-1){
                        callbackToJs(callbackId,false,result?0:1);
                    }
                    }else{
                        try {
                            FileUtils.copyFile(new File(realUserPath),new File(targetPath));
                            callbackToJs(callbackId,false,0);
                        } catch (IOException e) {
                            if (BDebug.DEBUG){
                                e.printStackTrace();
                            }
                            callbackToJs(callbackId,false,1);
                        }
                    }
                }
            }).start();
    }

    /**
     * 获取数据库的文件名
     * @param path
     * @return
     */
    private String getDBName(String path){
        return path.substring(path.lastIndexOf("/")+1);
    }

    /**
     * 获取数据库存放目标路径
     * @param dbName
     * @return
     */
    private String getTargetPath(String dbName){
        return String.format("/data/data/%s/databases/%s",mContext.getPackageName(),dbName);
    }

    private boolean copyAssetsToFilesystem(String assetsSrc, String des){
        InputStream istream = null;
        OutputStream ostream = null;
        try{
            AssetManager am = mContext.getAssets();
            istream = am.open(assetsSrc);
            ostream = new FileOutputStream(des);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = istream.read(buffer))>0){
                ostream.write(buffer, 0, length);
            }
            istream.close();
            ostream.close();
        }
        catch(Exception e){
            e.printStackTrace();
            try{
                if(istream!=null)
                    istream.close();
                if(ostream!=null)
                    ostream.close();
            }
            catch(Exception ee){
                ee.printStackTrace();
            }
            return false;
        }
        return true;
    }


    @Override
    protected boolean clean() {
        try {
            Iterator<String> iterator = m_dbHMap.keySet().iterator();
            while (iterator.hasNext()) {
                DatabaseHelper object = m_dbHMap.get(iterator.next());
                object.close();
                object = null;
            }
            m_dbHMap.clear();

            iterator = m_dbMap.keySet().iterator();
            while (iterator.hasNext()) {
                SQLiteDatabase object = m_dbMap.get(iterator.next());
                object.close();
                object = null;
            }
            m_dbMap.clear();
            opCodeList.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }
}