package tfsapps.noticetimer;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyOpenHelper extends SQLiteOpenHelper
{
    private static final String TABLE = "appinfo";
    public MyOpenHelper(Context context) {
        super(context, "AppDB", null, 1);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE + "("
                + "isopen integer,"             //DBオープン
                + "level integer,"              //ユーザーレベル（報酬の数）
                + "time_1 integer,"             //時間　履歴１
                + "sw_1 integer,"               //ＳＷ　履歴１
                + "time_2 integer,"             //時間　履歴２
                + "sw_2 integer,"               //ＳＷ　履歴２
                + "time_3 integer,"             //時間　履歴３
                + "sw_3 integer,"               //ＳＷ　履歴３
                + "time_4 integer,"             //時間　履歴４
                + "sw_4 integer,"               //ＳＷ　履歴４
                + "time_5 integer,"             //時間　履歴５
                + "sw_5 integer,"               //ＳＷ　履歴５
                + "data1 integer,"              //
                + "data2 integer,"              //
                + "data3 integer,"              //
                + "data4 integer,"              //
                + "data5 integer);");           //
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
