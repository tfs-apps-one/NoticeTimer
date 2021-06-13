package tfsapps.noticetimer;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.os.Bundle;

//タイマー関連
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Locale;
//アラーム関連
import android.media.MediaPlayer;
//ライト関連
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;

//スレッド
//タイマースレッド

/*
* □　UIの整備（ライト、バイブレーション、音）
* □　時間の設定（分、秒）
* □　一時停止停止の処理
* □　アイコン作成
* □　
* */

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
    private boolean isActive = false;
    final long INTERVAL = 10;
    final long MIN_10 = 600000;
    final long MIN_1 = 60000;
    final long SEC_10 = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /* 初期化処理 */
        Button startButton = findViewById(R.id.btn_start);
        Button stopButton = findViewById(R.id.btn_clear);
        timerText = findViewById(R.id.timer);
        timerText.setText(dataFormat.format(0));
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        alarm = MediaPlayer.create(this, R.raw.alarm);

        // タイマー関連（インスタンス生成）
        countDown = new CountDown(countNumber, INTERVAL);

        //カメラ初期化
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mCameraManager.registerTorchCallback(new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(String cameraId, boolean enabled) {
                super.onTorchModeChanged(cameraId, enabled);
                mCameraId = cameraId;
            }
        }, new android.os.Handler());
    }

    /*
        ボタン各処理
     */
    // 開始
    public void onStart(View view) throws InterruptedException {
        if (isActive == false ) {
            if (countNumber > 0) {
                countDown.start();
                isActive = true;
            }
        }else{
            isActive = false;
            countDown.onPause1();
        }
    }
    // クリア
    public void onClear(View view){
        countDown.cancel();
        isActive = false;
        timerText.setText(dataFormat.format(0));
        DeviceOff();
    }
    // +10min
    public void on10min(View view){
        if (isActive == false){
            countNumber += MIN_10;
        }
    }
    // +1min
    public void on1min(View view){
        if (isActive == false){
            countNumber += MIN_1;
        }
    }
    // +10sec
    public void on10sec(View view){
        if (isActive == false){
            countNumber += SEC_10;
        }
    }

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
            // 残り時間を分、秒、ミリ秒に分割
            long mm = millisUntilFinished / 1000 / 60;
            long ss = millisUntilFinished / 1000 % 60;
            long ms = (millisUntilFinished - ss * 1000 - mm * 1000 * 60)/10;
            timerText.setText(String.format("%1$02d:%2$02d.%2$02d", mm, ss, ms));
            timerText.setText(dataFormat.format(millisUntilFinished));
        }
    }

    //ライトの制御
    public void lightControl()
    {
        boolean _flag = false;
        //カメラ正常認識できていない場合
        if(mCameraId == null){
            return;
        }
        if (islightON == false){
            _flag = true;
        }else{
            _flag = false;
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
        alarm.setLooping(true);
        alarm.start();
        //バイブレーション
        long vibratePattern[] = {300,1000,300,1000};/* OFF→ON→OFF→ON*/
        vibrator.vibrate(vibratePattern,1);
        //ライト
        lightControl();
    }
    //OFF処理　ライト・バイブレーション・音
    public void DeviceOff()
    {
        //アラーム
        alarm.pause();
        //バイブレーション
        vibrator.cancel();
        //ライト
        lightControl();
    }

}