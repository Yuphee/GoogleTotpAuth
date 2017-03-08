package com.zhangyf.auth;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zhangyf.auth.anim.RotateAnimation;
import com.zhangyf.library.config.MTokenConstants;
import com.zhangyf.library.utils.Base32String;
import com.zhangyf.library.utils.CountUtils;
import com.zhangyf.library.utils.OtpSourceException;
import com.zhangyf.library.utils.PasscodeGenerator;
import com.zhangyf.library.utils.SPUtils;
import com.zhangyf.library.utils.TotpCountdownTask;
import com.zhangyf.library.utils.TotpUtil;
import com.zhangyf.library.utils.Utilities;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MainActivity extends AppCompatActivity {

    private Unbinder mUnbinder = null;
    //进度条刷新周期
    private static final long TOTP_COUNTDOWN_REFRESH_PERIOD = 100;
    //倒计时任务
    private TotpCountdownTask mTotpCountdownTask;
    //当前相位
    private double mTotpCountdownPhase;
    // totp计算结果
    private String result;

    private TextView[] txtArray = new TextView[6];
    private Handler handler;
    private char[] totpChar = new char[6];

    @BindView(R.id.rotate_text1)
    TextView rotate_text1;
    @BindView(R.id.rotate_text2)
    TextView rotate_text2;
    @BindView(R.id.rotate_text3)
    TextView rotate_text3;
    @BindView(R.id.rotate_text4)
    TextView rotate_text4;
    @BindView(R.id.rotate_text5)
    TextView rotate_text5;
    @BindView(R.id.rotate_text6)
    TextView rotate_text6;
    @BindView(R.id.serverTime)
    TextView serverTime;
    @BindView(R.id.key_bar)
    ProgressBar key_bar;
    @BindView(R.id.count_number)
    TextView count_number;

    private Date date;
    private SimpleDateFormat timeFromat;
    private String servertime;
    private Timer timer;
    private TimerTask task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUnbinder = ButterKnife.bind(this);
        initWidget();
        initData();
    }

    @Override
    public void onStart() {
        startTotpCountdownTask();
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 首次进入刷新动画
        updateDataAndExcuteAnim();
    }

    @Override
    public void onStop() {
        stopTotpCountdownTask();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mUnbinder) {
            mUnbinder.unbind();
        }
        if (timer != null) {
            timer.cancel();
        }
    }

    private void initWidget() {
        txtArray[0] = rotate_text1;
        txtArray[1] = rotate_text2;
        txtArray[2] = rotate_text3;
        txtArray[3] = rotate_text4;
        txtArray[4] = rotate_text5;
        txtArray[5] = rotate_text6;
    }

    private void initData() {
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 100:
                        serverTime.setText(servertime);
                        break;
                    default:
                        TextRunnable textrun = new TextRunnable(txtArray[msg.what],totpChar[msg.what]);
                        textrun.run();
                        break;
                }

            }
        };

        // 服务器时间显示任务，得与服务器校准事件后再启动
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                date = new Date(System.currentTimeMillis() + SPUtils.getLong(MTokenConstants.PREFS_SERVICE_TIME_REDUCE, 0l));
                timeFromat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                servertime = timeFromat.format(date);
                Message msg = new Message();
                msg.what = 100;
                handler.sendMessage(msg);
            }
        };
        timer.schedule(task, 0, 1000);
    }

    /**
     * 刷新界面，启动动画
     */
    private void updateDataAndExcuteAnim(){

        Log.d("reduce time:", String.valueOf(SPUtils.getLong(MTokenConstants.PREFS_SERVICE_TIME_REDUCE, 0l)));

        result = TotpUtil.generate();

        if(result != null && result.length() == 6){
            totpChar = result.toCharArray();
        }

        int i = 0;
        while(true){
//          handler.postDelayed(new TextRunnable(txtArray[i]),300);
            Message msg = new Message();
            msg.what = i;
            handler.sendMessageDelayed(msg,50*i);
            i++;
            if(i == 6){
                break;
            }
        }

    }



    /**
     * 刷新任务
     * @author zhangyf
     */
    private void startTotpCountdownTask() {
        stopTotpCountdownTask();

        mTotpCountdownTask = new TotpCountdownTask(
                TOTP_COUNTDOWN_REFRESH_PERIOD);
        mTotpCountdownTask.setListener(new TotpCountdownTask.Listener() {
            @Override
            public void onTotpCountdown(long millisRemaining) {
                if (isFinishing()) {
                    return;
                }
                setTotpCountdownPhaseFromTimeTillNextValue(millisRemaining);
            }

            @Override
            public void onTotpCounterValueChanged() {
                if (isFinishing()) {
                    return;
                }
                refreshVerificationCodes();
            }
        });

        mTotpCountdownTask.startAndNotifyListener();
    }

    /**
     * 停止刷新
     * @author zhangyf
     */
    private void stopTotpCountdownTask() {
        if (mTotpCountdownTask != null) {
            mTotpCountdownTask.stop();
            mTotpCountdownTask = null;
        }
    }

    /**
     * 计算一个不到的步数时间/一个步数的时间（毫秒）
     * @author zhangyf
     */
    private void setTotpCountdownPhaseFromTimeTillNextValue(long millisRemaining) {
        setTotpCountdownPhase(((double) millisRemaining)
                / Utilities.secondsToMillis(CountUtils.mTimeStep));
    }

    /**
     * 设置新的phase
     * @author zhangyf
     */
    private void setTotpCountdownPhase(double phase) {
        mTotpCountdownPhase = phase;
        updateCountdownIndicators();
    }

    /**
     * 重新赋值相位值，刷新进度条界面
     * @author zhangyf
     */
    private void updateCountdownIndicators() {
        if (key_bar != null && count_number != null) {
            key_bar.setProgress((int) (2000 - mTotpCountdownPhase * 2000));
            String count = String.valueOf(Math.ceil(mTotpCountdownPhase*30));
            count_number.setText(count.substring(0, count.indexOf(".")));
        }
    }

    /**
     * 更新TextView
     * @author zhangyf
     *
     */
    private void refreshVerificationCodes() {
        updateDataAndExcuteAnim();
        setTotpCountdownPhase(1.0);
    }


    class TextRunnable implements Runnable, RotateAnimation.InterpolatedTimeListener {

        private TextView number;
        private float cX;
        private float cY;
        private char rotateText;
        /** TextNumber是否允许显示最新的数字。 */
        private boolean enableRefresh;
        private RotateAnimation rotateAnim = null;
        public TextRunnable(TextView textView,char rotateText) {
            this.number = textView;
            this.rotateText = rotateText;
            cX = textView.getWidth() / 2.0f;
            cY = textView.getHeight() / 2.0f;
            enableRefresh = true;
        }

        @Override
        public void run() {
            rotateAnim = new RotateAnimation(cX, cY, RotateAnimation.ROTATE_INCREASE);
            if (rotateAnim != null) {
                rotateAnim.setInterpolatedTimeListener(this);
                rotateAnim.setFillAfter(true);
                this.number.startAnimation(rotateAnim);
            }
        }

        @Override
        public void interpolatedTime(float interpolatedTime) {
            // 监听到翻转进度过半时，更新txtNumber显示内容。
            if (enableRefresh && interpolatedTime > 0.5f) {
//	                this.number.setText(String.valueOf(new Random().nextInt(10)));
                this.number.setText(String.valueOf(rotateText));
                enableRefresh = false;
            }
        }
    }

}
