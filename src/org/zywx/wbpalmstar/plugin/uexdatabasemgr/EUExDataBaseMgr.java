package org.zywx.wbpalmstar.plugin.uexdatabasemgr;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build.VERSION;
import android.text.TextUtils;

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
	Context m_eContext;

	public EUExDataBaseMgr(Context context, EBrowserView inParent) {
		super(context, inParent);
		m_dbMap = new HashMap<String, SQLiteDatabase>();
		m_dbHMap = new HashMap<String, DatabaseHelper>();
		m_eContext = context;
	}

	public void openDataBase(String[] parm) {
		if (parm.length != 2) {
			return;
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
				return;
			}

			DatabaseHelper m_databaseHelper = new DatabaseHelper(m_eContext,
					inDBName, m_DbVer);
			m_dbMap.put(inDBName, m_databaseHelper.getWritableDatabase());
			m_dbHMap.put(inDBName, m_databaseHelper);
			opCodeList.add(inOpCode);
			jsCallback(F_OPENDATABASE_CALLBACK, Integer.parseInt(inOpCode),
					EUExCallback.F_C_INT, EUExCallback.F_C_SUCCESS);

		} catch (Exception e) {
			e.printStackTrace();
			jsCallback(F_OPENDATABASE_CALLBACK, Integer.parseInt(inOpCode),
					EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
		}

	}

	public void executeSql(String[] parm) {
		if (parm.length != 3) {
			return;
		}
		String inDBName = parm[0], inOpCode = parm[1], inSql = parm[2];
		if (!BUtility.isNumeric(inOpCode)) {
			inOpCode = "0";
		}
		try {
			SQLiteDatabase object = m_dbMap.get(inDBName);
			if (object != null) {

				object.execSQL(inSql);
				jsCallback(F_EXECSQL_CALLBACK, Integer.parseInt(inOpCode),
						EUExCallback.F_C_INT, EUExCallback.F_C_SUCCESS);
			} else {
				jsCallback(F_EXECSQL_CALLBACK, Integer.parseInt(inOpCode),
						EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
			}
		} catch (Exception e) {
			e.printStackTrace();
			jsCallback(F_EXECSQL_CALLBACK, Integer.parseInt(inOpCode),
					EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
		}

	}

	public void selectSql(String[] parm) {
		if (parm.length != 3) {
			return;
		}
		String inDBName = parm[0], inOpCode = parm[1], inSql = parm[2];
		if (!BUtility.isNumeric(inOpCode)) {
			inOpCode = "0";
		}
		SQLiteDatabase object = m_dbMap.get(inDBName);
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
							String value =null;
							int sysVersion = Integer.parseInt(VERSION.SDK);
							if(sysVersion < 11){
								value = cursor.getString(i);
							}else{
								switch (cursor.getType(i)) {
								case Cursor.FIELD_TYPE_NULL:
									value = cursor.getString(i);
									break;
								case Cursor.FIELD_TYPE_BLOB:
									value = new String(cursor.getBlob(i));
									break;
								case Cursor.FIELD_TYPE_FLOAT:
									double dl = cursor.getDouble(i);
									value =String.valueOf(formatNum(dl));
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
				} else {
					jsCallback(F_SELECTSQL_CALLBACK,
							Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
							EUExCallback.F_C_FAILED);
				}
			} catch (Exception e) {
				e.printStackTrace();
				jsCallback(F_SELECTSQL_CALLBACK, Integer.parseInt(inOpCode),
						EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
			}
		} else {
			jsCallback(F_SELECTSQL_CALLBACK, Integer.parseInt(inOpCode),
					EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
		}
	}
	public  String formatNum(double value)
    {
        String retValue = null;
        DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(0);
        df.setMaximumFractionDigits(2);
        retValue = df.format(value);
        retValue = retValue.replaceAll(",", "");
        return retValue;
    }
	public void beginTransaction(String[] parm) {
		if (parm.length < 2) {
			return;
		}
		String inDBName = parm[0], inOpCode = parm[1];
		if (!BUtility.isNumeric(inOpCode)) {
			inOpCode = "0";
		}
		try {
			SQLiteDatabase object = m_dbMap.get(inDBName);
			if (object != null) {
				object.beginTransaction();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void endTransaction(String[] parm) {
		if (parm.length < 2) {
			return;
		}
		String inDBName = parm[0], inOpCode = parm[1];
		if (!BUtility.isNumeric(inOpCode)) {
			inOpCode = "0";
		}
		SQLiteDatabase object = m_dbMap.get(inDBName);
		if (object != null) {
			try {
				object.setTransactionSuccessful();
				jsCallback(F_CBTRANSACTION_CALLBACK,
						Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
						EUExCallback.F_C_SUCCESS);
			} catch (IllegalStateException e) {
				jsCallback(F_CBTRANSACTION_CALLBACK,
						Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
						EUExCallback.F_C_FAILED);
			} finally {
				object.endTransaction();
			}
		} else {
			jsCallback(F_CBTRANSACTION_CALLBACK, Integer.parseInt(inOpCode),
					EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
		}
	}

	public void closeDataBase(String[] parm) {
		if (parm.length != 2) {
			return;
		}
		String inDBName = parm[0], inOpCode = parm[1];
		if (inOpCode == null || inOpCode.length() == 0) {
			inOpCode = "0";
		}
		if (!BUtility.isNumeric(inOpCode)) {
			inOpCode = "0";
		}
		DatabaseHelper dbh = m_dbHMap.remove(inDBName);
		if (dbh != null) {
			try {
				dbh.close();
				dbh = null;
				SQLiteDatabase object = m_dbMap.remove(inDBName);
				object.close();
				object = null;
				opCodeList.remove(inOpCode);
				jsCallback(F_CLOSEDATABASE_CALLBACK,
						Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
						EUExCallback.F_C_SUCCESS);
			} catch (Exception e) {
				jsCallback(F_CLOSEDATABASE_CALLBACK,
						Integer.parseInt(inOpCode), EUExCallback.F_C_INT,
						EUExCallback.F_C_FAILED);
			}

		} else {
			jsCallback(F_CLOSEDATABASE_CALLBACK, Integer.parseInt(inOpCode),
					EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
		}
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