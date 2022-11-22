package me.one;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    AdbService service =null;
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnConnAdb:
                if (service==null){
                    service=new AdbService(this);
                }
                break;
            case R.id.btnCommandA:
                service.performAdbRequest("dumpsys activity|grep mResume",new Callback(){

                    @Override
                    public void onSuccess(String adbResponse) {
                        Log.i("sanbo","onSuccess. "+adbResponse);
                    }

                    @Override
                    public void onFail(String failString) {
                        Log.i("sanbo","onFail. "+failString);
                    }
                });
                break;
            case R.id.btnCommandB:
                break;
            case R.id.btnCommandC:
                break;
            default:
                break;
        }
    }
}