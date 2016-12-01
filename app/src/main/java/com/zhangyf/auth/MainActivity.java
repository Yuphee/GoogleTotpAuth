package com.zhangyf.auth;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zhangyf.auth.anim.RotateAnimation;
import com.zhangyf.auth.config.MTokenConstants;
import com.zhangyf.auth.utils.Base32String;
import com.zhangyf.auth.utils.CountUtils;
import com.zhangyf.auth.utils.OtpSourceException;
import com.zhangyf.auth.utils.PasscodeGenerator;
import com.zhangyf.auth.utils.PrefsUtil;
import com.zhangyf.auth.utils.TotpCountdownTask;
import com.zhangyf.auth.utils.Utilities;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    private static final int PIN_LENGTH = 6;
    private static final int REFLECTIVE_PIN_LENGTH = 9;
    private String SEED;
    private static final long TOTP_COUNTDOWN_REFRESH_PERIOD = 100;
    private TotpCountdownTask mTotpCountdownTask;
    private double mTotpCountdownPhase;
    // totp计算结果
    private String result;

    private TextView[] txtArray = new TextView[6];
    private Handler handler;
    private char[] totpChar = new char[6];

    private TextView rotate_text1;
    private TextView rotate_text2;
    private TextView rotate_text3;
    private TextView rotate_text4;
    private TextView rotate_text5;
    private TextView rotate_text6;
    private TextView serverTime;
    private ProgressBar key_bar;
    private TextView count_number;

    public PrefsUtil prefsUtil;

    private Date date;
    private SimpleDateFormat timeFromat;
    private String servertime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initWidget();
        initData();
    }

    private void initWidget() {
        prefsUtil = new PrefsUtil(this);
        rotate_text1 = (TextView) findViewById(R.id.rotate_text1);
        rotate_text2 = (TextView) findViewById(R.id.rotate_text2);
        rotate_text3 = (TextView) findViewById(R.id.rotate_text3);
        rotate_text4 = (TextView) findViewById(R.id.rotate_text4);
        rotate_text5 = (TextView) findViewById(R.id.rotate_text5);
        rotate_text6 = (TextView) findViewById(R.id.rotate_text6);
        serverTime = (TextView) findViewById(R.id.serverTime);
        count_number = (TextView) findViewById(R.id.count_number);
        serverTime = (TextView) findViewById(R.id.serverTime);
        key_bar = (ProgressBar) findViewById(R.id.key_bar);

        txtArray[0] = rotate_text1;
        txtArray[1] = rotate_text2;
        txtArray[2] = rotate_text3;
        txtArray[3] = rotate_text4;
        txtArray[4] = rotate_text5;
        txtArray[5] = rotate_text6;
    }

    private void initData() {
        SEED = prefsUtil.getString(MTokenConstants.PREFS_USER_KEY, "");
        if(SEED.equals("")){
            SEED = "FZ6S5VB64HVSYLJN";
        }

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


        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                date = new Date(System.currentTimeMillis() + prefsUtil.getLong(MTokenConstants.PREFS_SERVICE_TIME_REDUCE, 0l));
                timeFromat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                servertime = timeFromat.format(date);
                Message msg = new Message();
                msg.what = 100;
                handler.sendMessage(msg);
            }
        };
        timer.schedule(task, 0, 1000);

        Log.d("seed", SEED);

        // 第一次进来的时候计算
        updateDataAndExcuteAnim();

    }

    private void updateDataAndExcuteAnim(){
        try {
            // 加上与服务器的时间差，再计算结果
            Log.d("reduce time:", String.valueOf(prefsUtil.getLong(MTokenConstants.PREFS_SERVICE_TIME_REDUCE, 0l)));
            result = computePin(SEED, CountUtils.getValueAtTime(CountUtils
                    .millisToSeconds(CountUtils.currentTimeMillis(prefsUtil))), null);
        } catch (OtpSourceException e) {
            e.printStackTrace();
        }

        if(result != null){
            totpChar[0] = result.charAt(0);
            totpChar[1] = result.charAt(1);
            totpChar[2] = result.charAt(2);
            totpChar[3] = result.charAt(3);
            totpChar[4] = result.charAt(4);
            totpChar[5] = result.charAt(5);
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
     * Computes the one-time PIN given the secret key.
     *
     * @author zhangyf
     *
     * @param secret
     *            the secret key
     * @param otp_state
     *            current token state (counter or time-interval)
     * @param challenge
     *            optional challenge bytes to include when computing passcode.
     * @return the PIN
     */
    private String computePin(String secret, long otp_state, byte[] challenge)
            throws OtpSourceException {
        if (secret == null || secret.length() == 0) {
            throw new OtpSourceException("Null or empty secret");
        }

        try {
            PasscodeGenerator.Signer signer = getSigningOracle(secret);
            PasscodeGenerator pcg = new PasscodeGenerator(signer,
                    (challenge == null) ? PIN_LENGTH : REFLECTIVE_PIN_LENGTH);

            return (challenge == null) ? pcg.generateResponseCode(otp_state)
                    : pcg.generateResponseCode(otp_state, challenge);
        } catch (GeneralSecurityException e) {
            throw new OtpSourceException("Crypto failure", e);
        }
    }

    static PasscodeGenerator.Signer getSigningOracle(String secret) {
        try {
            byte[] keyBytes = decodeKey(secret);
            final Mac mac = Mac.getInstance("HMACSHA1");
            mac.init(new SecretKeySpec(keyBytes, ""));

            // Create a signer object out of the standard Java MAC
            // implementation.
            return new PasscodeGenerator.Signer() {
                @Override
                public byte[] sign(byte[] data) {
                    return mac.doFinal(data);
                }
            };
        } catch (Base32String.DecodingException error) {
            Log.e("Mlog", error.getMessage());
        } catch (NoSuchAlgorithmException error) {
            Log.e("Mlog", error.getMessage());
        } catch (InvalidKeyException error) {
            Log.e("Mlog", error.getMessage());
        }

        return null;
    }

    private static byte[] decodeKey(String secret) throws Base32String.DecodingException {
        return Base32String.decode(secret);
    }

    /**
     * 刷新任务
     * @author zhangyf
     */
    private void updateCodesAndStartTotpCountdownTask() {
        stopTotpCountdownTask();

        mTotpCountdownTask = new TotpCountdownTask(
                TOTP_COUNTDOWN_REFRESH_PERIOD,prefsUtil);
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
     * 计算新的phase
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
        ProgressBar totp_progress = (ProgressBar)findViewById(R.id.key_bar);
        if (totp_progress != null && count_number != null) {
            totp_progress.setProgress((int) (2000 - mTotpCountdownPhase * 2000));
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
        try {
            result = computePin(SEED, CountUtils.getValueAtTime(CountUtils
                    .millisToSeconds(CountUtils.currentTimeMillis(prefsUtil))), null);
//			pin_text.setText(result);
            updateDataAndExcuteAnim();
        } catch (OtpSourceException e) {
            e.printStackTrace();
        }
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


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        updateCodesAndStartTotpCountdownTask();
        super.onStart();
    }

    @Override
    public void onStop() {
        stopTotpCountdownTask();
        super.onStop();
    }
}
