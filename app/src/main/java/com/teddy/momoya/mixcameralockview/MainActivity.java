package com.teddy.momoya.mixcameralockview;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Camera camera;
    Boolean is_close = false;
    // 用來參照拍照存檔的 Uri 物件
    Uri imgUri, copyUri;
    // LockScreenView
    private LockScreenView lockScreenView;
    private boolean isFirst = true;
    //Toast display message
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(MainActivity.this,CheckScreenOff.class);
        startService(intent);
        setContentView(R.layout.activity_main);
        get_permission_and_executing();
    }

    //取得權限並執行初始化
    private void get_permission_and_executing() {
        int permission = ActivityCompat.checkSelfPermission(this, CAMERA);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // 無權限，向使用者請求
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{CAMERA ,WRITE_EXTERNAL_STORAGE ,READ_EXTERNAL_STORAGE},
                    0
            );
        } else {
            //已有權限，執行程式
            FindSurfaceView();
            FindLockView();
        }
    }

    private void FindSurfaceView() {
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(callback);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void FindLockView(){
        lockScreenView = (LockScreenView) findViewById(R.id.lockscreenview);
        lockScreenView.setOnUnLockScreenListener(new LockScreenView.OnUnLockScreenListener(){
            @Override
            public void onSuccess() {
                // TODO Auto-generated method stub
                camera.autoFocus(afcb);
                ShowToast("解鎖成功");
            }
            @Override
            public void onFinish(List<Integer> list) {
                // TODO Auto-generated method stub
                if (isFirst) {
                    lockScreenView.setLockedList(list);
                    isFirst = false;
                    Log.d("LockData", list + " " + isFirst);
                }
            }
            @Override
            public void onFail() {
                // TODO Auto-generated method stub
                ShowToast("解鎖失敗");
            }
        });
    }
    public static Toast toast;
    private void ShowToast(String message){
        if(toast == null)
            toast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT);
        else
            toast.setText(message);
        toast.show();
    }
    private void initCamera() {
        // 0後1前 預設為後鏡頭
        camera = Camera.open(1);
        try {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPictureFormat(PixelFormat.JPEG);
            // get Supported Preview Sizes
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            Camera.Size cs = sizes.get(0);
            parameters.setPreviewSize(cs.width, cs.height);
            // parameters.setPreviewSize(320, 220);
            camera.setParameters(parameters);
            // 設置參數
            camera.setPreviewDisplay(surfaceHolder);
            // 鏡頭的方向和手機相差90度，所以要轉向
            camera.setDisplayOrientation(90);
            // 攝影頭畫面顯示在Surface上
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // afcb = AutoFocusCallback
    Camera.AutoFocusCallback afcb = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                // 對焦成功才拍照
                camera.takePicture(null, null, jpeg);
            }
        }
    };

    Camera.PictureCallback jpeg = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            Bitmap bmp_test = bmp;
            // 定義一個矩陣圖
            Matrix m = new Matrix();
            // 取得圖片的寬度
            int width = bmp.getWidth();
            // 取得圖片的長度
            int height = bmp.getHeight();
            /**
             * 順時針旋轉90度
             * if (scene % 2 == 0)
             *    m.postRotate(90);
             * else
             *    m.postRotate(-90);
             */
            // 依照前後鏡頭旋轉照片
            m.postRotate(-90);
            // 產生新的旋轉後Bitmap檔
            Bitmap bmp_new = Bitmap.createBitmap(bmp_test, 0, 0, width, height,
                    m, true);
            FileOutputStream fop;
            try {
                // 取得系統的公用圖檔路徑
                String dir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString();
                String fname = "_EA_" + System.currentTimeMillis() + ".jpg";
                // 利用目前時間組合出一個不會重複的檔名
                imgUri = Uri.parse("file://" + dir + "/" + fname);
                // 依前面的路徑及檔名建立 Uri 物件
                copyUri = imgUri;
                //Log.e("momo", imgUri.toString());
                fop = new FileOutputStream(imgUri.getPath());
                bmp_new.compress(Bitmap.CompressFormat.JPEG, 100, fop);
                fop.close();
                // 設為系統共享媒體檔
                Intent it = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        imgUri);
                sendBroadcast(it);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "FileNotFoundException", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
            }
            camera.startPreview();
        }
    };

    SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            initCamera();
        }
        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {}
        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            if (!is_close) {
                // 停止預覽
                camera.stopPreview();
                // 執行
                camera.release();
                camera = null;
            }
        }
    };
}
