package com.arkconcepts.cameraserve;

import android.app.Activity;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by Teo on 12/14/2017.
 */

public class TcpServiceHandler implements Runnable {
    TCPListener _listener;
    private Activity _act;
    public TcpServiceHandler(TCPListener listener, Activity act){
        _listener = listener;
        _act = act;
    }

    public synchronized void run() {
        // TODO Auto-generated method stub
        //if(socket==null){
        try {
            //InetAddress serverAddr = InetAddress.getByName("192.168.178.25");
            Socket socket = new Socket("192.168.1.4", 8001, true);
            //
            while(true){
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    final String str = in.readLine();
                    Log.d("TCPSERVICEHANDLER", str);
                    this._act.runOnUiThread(new Runnable(){

                        public void run() {
                            _listener.callCompleted(str);
                        }
                    });
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}