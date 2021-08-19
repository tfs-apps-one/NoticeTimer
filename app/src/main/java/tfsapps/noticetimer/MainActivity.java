package tfsapps.noticetimer;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;

//タイマー関連
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Locale;
//アラーム関連
import android.media.MediaPlayer;
//ライト関連
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;

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

    }
    /*
        アプリスタート処理
     */
    @Override
    public void onStart() {
        super.onStart();

        //画面初期化
        DisplayScreen();
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
    }

    /*
        画面描画処理
     */
    public void DisplayScreen() {

        Button btn_start = (Button)findViewById(R.id.btn_start);
        Button btn_clear = (Button)findViewById(R.id.btn_clear);

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
        }
        else{
            btn_start.setBackgroundTintList(null);
            btn_start.setText(R.string.btn_START);
            btn_start.setTextColor(getColor(R.color.design_default_color_primary_variant));
            btn_start.setBackgroundResource(R.drawable.btn_round2);

            btn_clear.setBackgroundTintList(null);
            btn_clear.setTextColor(getColor(R.color.design_default_color_error));
            btn_clear.setBackgroundResource(R.drawable.btn_round2);

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

}