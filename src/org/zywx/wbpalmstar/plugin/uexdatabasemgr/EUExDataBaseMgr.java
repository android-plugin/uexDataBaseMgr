package org.zywx.wbpalmstar.plugin.uexdatabasemgr;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build.VERSION;
import android.text.TextUtils;

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
    private static final int m_DbVer = 1;
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
        if (parm.length != 2) {
            return EUExCallback.F_C_FAILED;
        }
        String inDBName = parm[0];
        String inOpCode = parm[1];

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
                    inDBName, m_DbVer);
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

    public DataBaseVO open(String[] params){
        DataBaseVO dataBaseVO =new DataBaseVO();
        dataBaseVO.id=generateId();
        dataBaseVO.name=params[0];
        int result=openDataBase(new String[]{
                dataBaseVO.name,
                dataBaseVO.id,
        });
        return result== F_C_SUCCESS? dataBaseVO :null;
    }

    private String generateId(){
        sCurrentId++;
        return String.valueOf(sCurrentId);
    }

    public boolean executeSql(String[] parm) {
        if (parm.length < 3) {
            return false;
        }
        String inDBName = parm[0], inOpCode = parm[1], inSql = parm[2],executeSqlFuncId=null;
        if (parm.length == 4) {
            executeSqlFuncId = parm[3];
        }
        if (!BUtility.isNumeric(inOpCode)) {
            inOpCode = "0";
        }
        try {
            SQLiteDatabase object = m_dbMap.get(getDBFlg(inDBName, inOpCode));
            if (object != null) {

                object.execSQL(inSql);
                jsCallback(F_EXECSQL_CALLBACK, Integer.parseInt(inOpCode),
                        EUExCallback.F_C_INT, F_C_SUCCESS);
                if (null != executeSqlFuncId) {
                    callbackToJs(Integer.parseInt(executeSqlFuncId), false, true);
                }

            } else {
                jsCallback(F_EXECSQL_CALLBACK, Integer.parseInt(inOpCode),
                        EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
                if (null != executeSqlFuncId) {
                    callbackToJs(Integer.parseInt(executeSqlFuncId), false, false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            jsCallback(F_EXECSQL_CALLBACK, Integer.parseInt(inOpCode),
                    EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
            if (null != executeSqlFuncId) {
                callbackToJs(Integer.parseInt(executeSqlFuncId), false, false);
            }
        }
        return true;

    }

    public void sql(String[] params){
        DataBaseVO dbVO=DataHelper.gson.fromJson(params[0],DataBaseVO.class);
        String[] inParams=new String[params.length+1];
        inParams[0]=dbVO.name;
        inParams[1]=dbVO.id;
        inParams[2]=params[1];
        if (params.length>2){
            inParams[3]=params[2];
        }
        executeSql(inParams);
    }


    public void selectSql(String[] parm) {
        if (parm.length < 3) {
            return;
        }
        String inDBName = parm[0], inOpCode = parm[1], inSql = parm[2],selectSqlFuncId=null;
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
                            int sysVersion = Integer.parseInt(VERSION.SDK);
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
                        callbackToJs(Integer.parseInt(selectSqlFuncId), false,true, jsonItems);
                    }
                } else {
                    jsCallback(F_SELECTSQL_CALLBACK,
                            Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
                            EUExCallback.F_C_FAILED);
                    if (null != selectSqlFuncId) {
                        callbackToJs(Integer.parseInt(selectSqlFuncId), false,false, new JSONArray());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                jsCallback(F_SELECTSQL_CALLBACK, Integer.parseInt(inOpCode),
                        EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
                if (null != selectSqlFuncId) {
                    callbackToJs(Integer.parseInt(selectSqlFuncId), false,false);
                }
            }
        } else {
            jsCallback(F_SELECTSQL_CALLBACK, Integer.parseInt(inOpCode),
                    EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
            if (null != selectSqlFuncId) {
                callbackToJs(Integer.parseInt(selectSqlFuncId), false,false, new JSONArray());
            }
        }
    }

    public void select(String[] params){
        DataBaseVO dbVO=DataHelper.gson.fromJson(params[0],DataBaseVO.class);
        selectSql(new String[]{
                dbVO.name,
                dbVO.id,
                params[1],
                params.length>2?params[2]:null
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

    private static boolean isJson(String value){
        if (TextUtils.isEmpty(value)){
            return false;
        }
        try {
            JSONObject jsonObject=new JSONObject(value);
            return true;
        } catch (JSONException e) {
            if (BDebug.DEBUG){
                e.printStackTrace();
            }
            return false;
        }
    }

    public boolean beginTransaction(String[] parm) {
        String inDBName = null, inOpCode = null;
        boolean isJson=isJson(parm[0]);
        if (isJson){
            DataBaseVO dbVO=DataHelper.gson.fromJson(parm[0],DataBaseVO.class);
            inDBName=dbVO.name;
            inOpCode=dbVO.id;
        }else{
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
        String inDBName = null, inOpCode = null,transactionFuncId = null;
        boolean isJson=isJson(parm[0]);
        if (isJson){
            DataBaseVO dbVO=DataHelper.gson.fromJson(parm[0],DataBaseVO.class);
            inDBName=dbVO.name;
            inOpCode=dbVO.id;
            if (parm.length>2){
                transactionFuncId=parm[2];
            }
        }else{
            inDBName = parm[0];
            inOpCode = parm[1];
            if (parm.length >3) {
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
                    callbackToJs(Integer.parseInt(transactionFuncId), false, true);
                }
            } catch (IllegalStateException e) {
                jsCallback(F_CBTRANSACTION_CALLBACK,
                        Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
                        EUExCallback.F_C_FAILED);
                if (null != transactionFuncId) {
                    callbackToJs(Integer.parseInt(transactionFuncId), false, false);
                }
            } finally {
                object.endTransaction();
            }
        } else {
            jsCallback(F_CBTRANSACTION_CALLBACK, Integer.parseInt(inOpCode),
                    EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
            if (null != transactionFuncId) {
                callbackToJs(Integer.parseInt(transactionFuncId), false, false);
            }
        }
    }

    public void transactionEx(String[] params){
        DataBaseVO dataBaseVO=DataHelper.gson.fromJson(params[0],DataBaseVO.class);
        String transFuncId=params[1];
        String callbackFuncId=null;
        if (params.length>2){
            callbackFuncId=params[2];
        }
        beginTransaction(new String[]{
                dataBaseVO.name,
                dataBaseVO.id,
                transFuncId,
                callbackFuncId
        });
        callbackToJs(Integer.parseInt(transFuncId),false);//
        StringBuilder endTransJS=new StringBuilder("javascript:");
        endTransJS.append("uexDataBaseMgr.endTransaction('")
                .append(dataBaseVO.name).append("','")
                .append(dataBaseVO.id).append("','")
                .append(transFuncId).append("','")
                .append(callbackFuncId)
                .append("');");
        mBrwView.addUriTask(endTransJS.toString());
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

    public boolean close(String[] params){
        DataBaseVO dbVO=DataHelper.gson.fromJson(params[0],DataBaseVO.class);
        int result=closeDataBase(new String[]{
                dbVO.name,
                dbVO.id
        });
        return result==0;
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
            m_context.deleteDatabase(m_dbName);

        }
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