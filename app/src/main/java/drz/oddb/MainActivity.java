package drz.oddb;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.Serializable;

import drz.oddb.Memory.*;
import drz.oddb.Transaction.SystemTable.BiPointerTable;
import drz.oddb.Transaction.SystemTable.ClassTable;
import drz.oddb.Transaction.SystemTable.ClassTableItem;
import drz.oddb.Transaction.SystemTable.DeputyTable;
import drz.oddb.Transaction.SystemTable.ObjectTable;
import drz.oddb.Transaction.SystemTable.SwitchingTable;
import drz.oddb.Transaction.TransAction;

public class MainActivity extends AppCompatActivity {

    //查询输入框

    private EditText editText;

    private TextView text_view;
    TransAction trans = new TransAction(this);
    Intent music = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        music = new Intent(MainActivity.this,MusicServer.class);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //播放BGM
        startService(music);

        //查询按钮
        Button button = findViewById(R.id.button);
        editText = findViewById(R.id.edit_text);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //onStop();
                //trans.Test();
                trans.query(editText.getText().toString(), true);
            }
        });

        //退出按钮
        Button exit_button = findViewById(R.id.exit_button);
        exit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showexitdialog(v);
                stopService(music);
            }
        });

        //展示按钮
        Button show_button = findViewById(R.id.showbutton);
        show_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //onStop();
                trans.PrintTab();
            }
        });

        // 导入轨迹
        Button track_create_button = findViewById(R.id.trackcreatebutton);
        track_create_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trans.TrackCreate();

            }
        });
        // 合并轨迹
        Button track_merge_button = findViewById(R.id.trackmergebutton);
        track_merge_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trans.TrackMerge();

            }
        });
    }
    protected void onStop(){
        Intent intent = new Intent(MainActivity.this,MusicServer.class);
        stopService(intent);
        super.onStop();
        Log.e("main", "...onstop");
    }

    protected void onStart(){
        super.onStart();
        startService(this.music);
        Log.e("main","...onstart");
    }

    //点击exit_button退出程序
    public void showexitdialog(View v){
        //定义一个新对话框对象
        AlertDialog.Builder exit_dialog = new AlertDialog.Builder(this);
        //设置对话框提示内容
        exit_dialog.setMessage("Do you want to save it before exiting the program?");
        //定义对话框两个按钮及接受事件
        exit_dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //保存
                trans.SaveAll();
                //退出
                android.os.Process.killProcess(android.os.Process.myPid());

            }
        });
        exit_dialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //退出
                android.os.Process.killProcess(android.os.Process.myPid());

            }
        });
        //创建并显示对话框
        AlertDialog exit_dialog0 = exit_dialog.create();
        exit_dialog0.show();

    }

}
