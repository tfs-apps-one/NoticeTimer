package tfsapps.noticetimer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;

//タイマー関連
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Locale;
//アラーム関連
import android.media.MediaPlayer;
//ライト関連
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
//広告
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdRequest;
//DB
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    //カウントダウン
    private CountDown countDown;
    private TextView timerText;
    private SimpleDateFormat dataFormat = new SimpleDateFormat("mm:ss.SS", Locale.US);
    //バイブレーション
    static private Vibrator vibrator;
    //アラーム
    private MediaPlayer alarm;
    //ライト関連
    private CameraManager mCameraManager;
    private String mCameraId = null;
    static boolean islightON = false;
    static long countNumber = 0;
    private boolean isActive = false;           //タイマーカウント中か否か　true=動作中、false=停止中
    final long INTERVAL = 10;
    final long MIN_10 = 600000;
    final long MIN_1 = 60000;
    final long SEC_10 = 10000;
    final long SEC_1 = 1000;
    private boolean is_set_alarm = true;        //アラーム設定
    private boolean is_set_light = false;       //ライト設定
    private boolean is_set_vaib = false;        //バイブ設定

    private int now_volume;                     //現在の音量値
    private int init_volume;                    //アプリ起動時の音量値
    private SeekBar seek_volume;                //SeekBar
    private AudioManager am;
    private long now_countNumber = 0;           //現在のタイマー値（カウントダウン中の値）
    private boolean isPause = false;            //タイマー一時停止

    //  国設定
    private Locale _local;
    private String _language;
    private String _country;

    // 広告
    private AdView mAdview;

    // DB
    private MyOpenHelper helper;            //DBアクセス
    private int db_isopen = 0;              //DB使用したか
    private int db_level = 0;
    private long db_time_1 = 0;
    private int db_sw_1 = 0;
    private long db_time_2 = 0;
    private int db_sw_2 = 0;
    private long db_time_3 = 0;
    private int db_sw_3 = 0;
    private long db_time_4 = 0;
    private int db_sw_4 = 0;
    private long db_time_5 = 0;
    private int db_sw_5 = 0;
    private int db_index_wr = 0;

    // Spinner
    private String[] spinnerItems_EN = {"HISTORY", "1:", "2:", "3:","4:","5:"};
    private String[] spinnerItems_JP = {"操作履歴", "1:", "2:", "3:","4:","5:"};
    private String[] spinnerItems = {};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  国設定
        _local = Locale.getDefault();
        _language = _local.getLanguage();
        _country = _local.getCountry();

        /* 初期化処理 */
        timerText = findViewById(R.id.timer);
        timerText.setText(dataFormat.format(0));
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        alarm = MediaPlayer.create(this, R.raw.alarm);

        //カメラ初期化
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mCameraManager.registerTorchCallback(new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(String cameraId, boolean enabled) {
                super.onTorchModeChanged(cameraId, enabled);
                mCameraId = cameraId;
            }
        }, new android.os.Handler());

        /*音*/
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        now_volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);

        //広告
        mAdview = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdview.loadAd(adRequest);

        /* SeekBar */
        seek_volume = (SeekBar)findViewById(R.id.seekBar);
        seek_volume.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    //ツマミをドラッグした時
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        now_volume = seekBar.getProgress();
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, now_volume, 0);
                        DisplayScreen();
                    }
                    //ツマミに触れた時
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        if (is_set_alarm == false)  return;
                    }
                    //ツマミを離した時
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        if (is_set_alarm == false)  return;
                    }
                }
        );

        // Spiner
        // 選択肢
        Spinner spinner = findViewById(R.id.sp_history);

        if (_language.equals("ja")) {
            spinnerItems = spinnerItems_JP;
        }
        else{
            spinnerItems = spinnerItems_EN;
        }

        // ArrayAdapter
        ArrayAdapter<String> adapter
                = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, spinnerItems);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // spinner に adapter をセット
        spinner.setAdapter(adapter);

        // リスナーを登録
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            //　アイテムが選択された時
            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View view, int position, long id) {
                Spinner spinner = (Spinner)parent;
                String item = (String)spinner.getSelectedItem();
//                textView.setText(item);
            }

            //　アイテムが選択されなかった
            public void onNothingSelected(AdapterView<?> parent) {
                //
            }
        });
    }
    /*
        アプリスタート処理
     */
    @Override
    public void onStart() {
        super.onStart();

        //DBのロード
        /* データベース */
        helper = new MyOpenHelper(this);
        AppDBInitRoad();


        //画面初期化
        DisplayScreen();
    }
    @Override
    public void onPause(){
        super.onPause();
        //  DB更新
        AppDBUpdated();
    }
    @Override
    public void onStop(){
        super.onStop();
        //  DB更新
        AppDBUpdated();
    }
    /*
        アプリ終了処理
     */
    @Override
    public void onDestroy(){
        super.onDestroy();
        //カメラ
        if (mCameraManager != null)
        {
            mCameraManager = null;
        }
        /* 音量の戻しの処理 */
        if (am != null){
            am.setStreamVolume(AudioManager.STREAM_MUSIC, init_volume, 0);
            am = null;
        }
        //  DB更新
        AppDBUpdated();
    }

    /*
        画面描画処理
     */
    public void DisplayScreen() {

        Button btn_start = (Button)findViewById(R.id.btn_start);
        Button btn_clear = (Button)findViewById(R.id.btn_clear);
        Button btn_tips = (Button)findViewById(R.id.btn_tips);

        /* 音量 */
        TextView t_volume = (TextView)findViewById(R.id.text_volume);
        t_volume.setText(""+now_volume);
        if (is_set_alarm == true)   seek_volume.setBackgroundColor(0xFFBBDDFC);
        else                        seek_volume.setBackgroundColor(0xFFEDEDED);
        seek_volume.setProgress(now_volume);

        //以下は、タイマー動作中は表示更新しない
        if (isActive == true){
            if (isPause == true){
                btn_start.setText(R.string.btn_STOP);
            }else{
                btn_start.setText(R.string.btn_START);
            }
            btn_start.setBackgroundTintList(null);
            btn_start.setTextColor(getColor(R.color.material_on_background_disabled));
            btn_start.setBackgroundResource(R.drawable.btn_round2);

            btn_clear.setBackgroundTintList(null);
            btn_clear.setTextColor(getColor(R.color.design_default_color_error));
            btn_clear.setBackgroundResource(R.drawable.btn_round2);

            btn_tips.setBackgroundTintList(null);
            btn_tips.setTextColor(getColor(R.color.material_on_background_disabled));
            btn_tips.setBackgroundResource(R.drawable.btn_round2);
        }
        else{
            btn_start.setBackgroundTintList(null);
            btn_start.setText(R.string.btn_START);
            btn_start.setTextColor(getColor(R.color.design_default_color_primary_variant));
            btn_start.setBackgroundResource(R.drawable.btn_round2);

            btn_clear.setBackgroundTintList(null);
            btn_clear.setTextColor(getColor(R.color.design_default_color_error));
            btn_clear.setBackgroundResource(R.drawable.btn_round2);

            btn_tips.setBackgroundTintList(null);
            btn_tips.setTextColor(getColor(R.color.teal_700));
            btn_tips.setBackgroundResource(R.drawable.btn_round2);

            /* タイマー */
            timerText = findViewById(R.id.timer);
            timerText.setText(dataFormat.format(countNumber));

            /* イメージボタンの表示 */
            ImageButton i_btn1 = (ImageButton) findViewById(R.id.btn_img_alarm);
            if (is_set_alarm == true)   i_btn1.setImageResource(R.drawable.alarm_1);
            else                        i_btn1.setImageResource(R.drawable.alarm_0);

            ImageButton i_btn2 = (ImageButton) findViewById(R.id.btn_img_light);
            if (is_set_light == true)   i_btn2.setImageResource(R.drawable.light_1);
            else                        i_btn2.setImageResource(R.drawable.light_0);

            ImageButton i_btn3 = (ImageButton) findViewById(R.id.btn_img_vaib);
            if (is_set_vaib == true)    i_btn3.setImageResource(R.drawable.vaib_1);
            else                        i_btn3.setImageResource(R.drawable.vaib_0);
        }

        Button btn1 = (Button)findViewById(R.id.btn_10min);
        Button btn2 = (Button)findViewById(R.id.btn_1min);
        Button btn3 = (Button)findViewById(R.id.btn_10sec);
        Button btn4 = (Button)findViewById(R.id.btn_1sec);

        btn1.setBackgroundTintList(null);
        btn1.setBackgroundResource(R.drawable.btn_round);
        btn2.setBackgroundTintList(null);
        btn2.setBackgroundResource(R.drawable.btn_round);
        btn3.setBackgroundTintList(null);
        btn3.setBackgroundResource(R.drawable.btn_round);
        btn4.setBackgroundTintList(null);
        btn4.setBackgroundResource(R.drawable.btn_round);

        spinnerItems[1] = change_TimeSw_db_to_app(1,db_time_1, db_sw_1);
        spinnerItems[2] = change_TimeSw_db_to_app(2,db_time_2, db_sw_2);
        spinnerItems[3] = change_TimeSw_db_to_app(3,db_time_3, db_sw_3);
        spinnerItems[4] = change_TimeSw_db_to_app(4,db_time_4, db_sw_4);
        spinnerItems[5] = change_TimeSw_db_to_app(5,db_time_5, db_sw_5);
    }

    /*
        ボタン各処理
     */
    // 開始
    public void onStart(View view) throws InterruptedException {
        if (isActive == false ) {
            if (countNumber > 0) {
                if (countDown != null){
                    countDown.cancel();
                    countDown = null;
                }
                // タイマー関連（インスタンス生成）
                countDown = new CountDown(countNumber, INTERVAL);
                countDown.start();
                isActive = true;
                isPause = false;
                // 履歴にセット
                Set_App_to_Db();
            }
            DisplayScreen();
        }
        else{
            /* タイマー途中 */
            if (now_countNumber > 0){
                if (isPause == true) {
                    if (countDown != null){
                        countDown.cancel();
                        countDown = null;
                    }
                    countNumber = now_countNumber;
                    countDown = new CountDown(countNumber, INTERVAL);
                    countDown.start();
                    isActive = true;
                    isPause = false;
                }
                else{
                    if (countDown != null){
                        countDown.cancel();
                    }
                    isPause = true;
                }
                DisplayScreen();
            }
            //エラー表示が親切
        }
    }
    // クリア
    public void onClear(View view){
        if (isActive == true){
            DeviceOff();
        }
        countNumber = 0;
        if (countDown != null){
            countDown.cancel();
        }
        isActive = false;
        isPause = false;
        timerText.setText(dataFormat.format(0));
        DisplayScreen();
    }
    // +10min
    public void on10min(View view){
        if (isActive == false){
            countNumber += MIN_10;
            DisplayScreen();
        }
        else{
            //エラー表示が親切
        }
    }
    // +1min
    public void on1min(View view){
        if (isActive == false){
            countNumber += MIN_1;
            DisplayScreen();
        }
        else{
            //エラー表示が親切
        }

    }
    // +10sec
    public void on10sec(View view){
        if (isActive == false){
            countNumber += SEC_10;
            DisplayScreen();
        }
        else{
            //エラー表示が親切
        }
    }
    // +1sec
    public void on1sec(View view){
        if (isActive == false){
            countNumber += SEC_1;
            DisplayScreen();
        }
        else{
            //エラー表示が親切
        }
    }
    // alarm 設定
    public void onAlarm(View view){
        if (isActive == false) {
            is_set_alarm = (!is_set_alarm);
            DisplayScreen();
        }
        else{
            //エラー表示が親切
        }
    }
    // light 設定
    public void onLight(View view){
        if (isActive == false) {
            is_set_light = (!is_set_light);
            DisplayScreen();
        }
        else{
            //エラー表示が親切
        }
    }
    // Vaib 設定
    public void onVaib(View view){
        if (isActive == false) {
            is_set_vaib = (!is_set_vaib);
            DisplayScreen();
        }
        else{
            //エラー表示が親切
        }
    }
    // TIPS 設定
    public void onTips(View view){
        if (isActive == false) {
            DisplayScreen();
        }
        else{
            //エラー表示が親切
        }
    }

    /*
        SeekBar処理
     */



    /*
        カウントダウン処理
     */
    class CountDown extends CountDownTimer {
        private long nowcount;

        CountDown(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
            nowcount = millisInFuture;
        }
        @Override
        public void onFinish() {
            // 完了
            //テキスト
            timerText.setText(dataFormat.format(0));
            now_countNumber = 0;
            DeviceOn();
        }
        public void onPause1(){
            onPause();
        }
        public void onResume1(){
            onResume1();
        }
        public void onRestart(){
            onRestart();
        }
        // インターバルで呼ばれる
        @Override
        public void onTick(long millisUntilFinished) {
            now_countNumber = millisUntilFinished;
            // 残り時間を分、秒、ミリ秒に分割
            long mm = millisUntilFinished / 1000 / 60;
            long ss = millisUntilFinished / 1000 % 60;
            long ms = (millisUntilFinished - ss * 1000 - mm * 1000 * 60)/10;
            timerText.setText(String.format("%1$02d:%2$02d.%2$02d", mm, ss, ms));
            timerText.setText(dataFormat.format(millisUntilFinished));
        }
    }

    //ライトの制御
    public void lightControl(boolean isOn)
    {
        boolean _flag = false;
        //カメラ正常認識できていない場合
        if(mCameraId == null){
            return;
        }
        if (isOn == false){
            _flag = false;
        }else{
            _flag = true;
        }
        try {
            mCameraManager.setTorchMode(mCameraId, _flag);
        } catch (CameraAccessException e) {
            //エラー処理
            e.printStackTrace();
            return;
        }
        islightON = _flag;
    }
    //ON処理　ライト・バイブレーション・音
    public void DeviceOn()
    {
        //アラーム
        if (is_set_alarm == true) {
            alarm.setLooping(true);
            alarm.start();
        }
        //バイブレーション
        if (is_set_vaib) {
            long vibratePattern[] = {300, 1000, 300, 1000};/* OFF→ON→OFF→ON*/
            vibrator.vibrate(vibratePattern, 1);
        }
        //ライト
        if (is_set_light == true) {
            lightControl(true);
        }
    }
    //OFF処理　ライト・バイブレーション・音
    public void DeviceOff()
    {
        //アラーム
        alarm.pause();
        //バイブレーション
        vibrator.cancel();
        //ライト
        lightControl(false);
    }

    /*
        TIME 表示形式（文字列）　変換処理
        DBデータからアプリデータへ
        引数：db_time1〜5
    */
    public String change_TimeSw_db_to_app(int index, long time, int sw){
        String disp = "";
        long min = 0;
        long sec = 0;

        min = (time/MIN_1);
        sec = ((time % MIN_1) / SEC_1);

        switch (index){
            default:
            case 1: disp = "履歴① ";  break;
            case 2: disp = "履歴② ";  break;
            case 3: disp = "履歴③ ";  break;
            case 4: disp = "履歴④ ";  break;
            case 5: disp = "履歴⑤ ";  break;
        }

        disp += String.format("%02d:%02d:00",min,sec);
        disp += ", ";

        if ((sw/100) > 0)   disp += "■";
        else                disp += "□";

        if ((sw/10) > 0)    disp += "■";
        else                disp += "□";

        if ((sw%10) > 0)    disp += "■";
        else                disp += "□";

        return disp;
    }


    /*
        SW変換処理
        DBデータからアプリデータへ
    */
    public void change_sw_db_to_app(){
        int sw_temp = 111;

        if ((sw_temp/100) > 0){
            is_set_alarm = true;
        }

        sw_temp = sw_temp / 10;
        if ((sw_temp/10) > 0){
            is_set_light = true;
        }

        if ((sw_temp%10)> 0){
            is_set_vaib = true;
        }
    }
    /*
        SW変換処理
        アプリデータからDBデータへ
    */
    public int change_sw_app_to_db(){
        int sw_temp = 0;

        if (is_set_alarm){
            sw_temp = 100;
        }
        if (is_set_light){
            sw_temp += 10;
        }
        if (is_set_vaib){
            sw_temp += 1;
        }
        return sw_temp;
    }

    /***************************************************
         履歴データをDBにセット
     ****************************************************/
    public void Set_App_to_Db() {
        int wr_MAX = 0;

        //  DBセット処理をスキップ
        if (db_level == 0){
            return;
        }

        wr_MAX = db_level;
        if (db_level > 5){
            wr_MAX = 5;
        }

        //書き込み用インデックスセット
        db_index_wr++;
        if (db_index_wr > wr_MAX){
            db_index_wr = 1;
        }

        switch (db_index_wr){
            case 1: db_time_1 = countNumber;
                    db_sw_1 = change_sw_app_to_db();
                    break;
            case 2: db_time_2 = countNumber;
                    db_sw_2 = change_sw_app_to_db();
                    break;
            case 3: db_time_3 = countNumber;
                    db_sw_3 = change_sw_app_to_db();
                    break;
            case 4: db_time_4 = countNumber;
                    db_sw_4 = change_sw_app_to_db();
                    break;
            case 5: db_time_5 = countNumber;
                    db_sw_5 = change_sw_app_to_db();
                    break;
        }

    }

    /***************************************************
        DB初期ロードおよび設定
     ****************************************************/
    public void AppDBInitRoad() {
        SQLiteDatabase db = helper.getReadableDatabase();
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT");
        sql.append(" isopen");
        sql.append(" ,level");
        sql.append(" ,time_1");
        sql.append(" ,sw_1");
        sql.append(" ,time_2");
        sql.append(" ,sw_2");
        sql.append(" ,time_3");
        sql.append(" ,sw_3");
        sql.append(" ,time_4");
        sql.append(" ,sw_4");
        sql.append(" ,time_5");
        sql.append(" ,sw_5");
        sql.append(" ,index_wr");
        sql.append(" ,data1");
        sql.append(" ,data2");
        sql.append(" ,data3");
        sql.append(" ,data4");
        sql.append(" ,data5");
        sql.append(" FROM appinfo;");
        try {
            Cursor cursor = db.rawQuery(sql.toString(), null);
            //TextViewに表示
            StringBuilder text = new StringBuilder();
            if (cursor.moveToNext()) {
                db_isopen = cursor.getInt(0);
                db_level = cursor.getInt(1);
                db_time_1 = cursor.getInt(2);
                db_sw_1 = cursor.getInt(3);
                db_time_2 = cursor.getInt(4);
                db_sw_2 = cursor.getInt(5);
                db_time_3 = cursor.getInt(6);
                db_sw_3 = cursor.getInt(7);
                db_time_4 = cursor.getInt(8);
                db_sw_4 = cursor.getInt(9);
                db_time_5 = cursor.getInt(10);
                db_sw_5 = cursor.getInt(11);
                db_index_wr = cursor.getInt(12);
            }
        } finally {
            db.close();
        }

        db = helper.getWritableDatabase();
        if (db_isopen == 0) {
            long ret;
            /* 新規レコード追加 */
            ContentValues insertValues = new ContentValues();
            insertValues.put("isopen", 1);
            insertValues.put("level", 0);
            insertValues.put("time_1", 0);
            insertValues.put("sw_1", 0);
            insertValues.put("time_2", 0);
            insertValues.put("sw_2", 0);
            insertValues.put("time_3", 0);
            insertValues.put("sw_3", 0);
            insertValues.put("time_4", 0);
            insertValues.put("sw_4", 0);
            insertValues.put("time_5", 0);
            insertValues.put("sw_5", 0);
            insertValues.put("index_wr", 0);
            insertValues.put("data1", 0);
            insertValues.put("data2", 0);
            insertValues.put("data3", 0);
            insertValues.put("data4", 0);
            insertValues.put("data5", 0);
            try {
                ret = db.insert("appinfo", null, insertValues);
            } finally {
                db.close();
            }
            db_isopen = 1;
            /*
            if (ret == -1) {
                Toast.makeText(this, "DataBase Create.... ERROR", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "DataBase Create.... OK", Toast.LENGTH_SHORT).show();
            }
             */

        } else {
            /*
            Toast.makeText(this, "Data Loading...  isopen:" + db_isopen, Toast.LENGTH_SHORT).show();
             */

        }
    }

    /***************************************************
         DB更新
     ****************************************************/
    public void AppDBUpdated() {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues insertValues = new ContentValues();
        insertValues.put("isopen", db_isopen);
        insertValues.put("level", db_level);
        insertValues.put("time_1", db_time_1);
        insertValues.put("sw_1", db_sw_1);
        insertValues.put("time_2", db_time_2);
        insertValues.put("sw_2", db_sw_2);
        insertValues.put("time_3", db_time_3);
        insertValues.put("sw_3", db_sw_3);
        insertValues.put("time_4", db_time_4);
        insertValues.put("sw_4", db_sw_4);
        insertValues.put("time_5", db_time_5);
        insertValues.put("sw_5", db_sw_5);
        insertValues.put("index_wr", db_index_wr);
        int ret;
        try {
            ret = db.update("appinfo", insertValues, null, null);
        } finally {
            db.close();
        }

        if (ret == -1) {
            Toast.makeText(this, "Saving.... ERROR ", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Saving.... OK "+ "time1= "+db_time_1+", sw1= "+db_sw_1 + "time5= "+db_time_5+", sw5= "+db_sw_5,
                    Toast.LENGTH_SHORT).show();
        }

    }

}