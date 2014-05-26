package helios.examples.MasterSlave;

import helios.api.*;
import helios.api.HeliosAPI.*;

import java.io.IOException;
import java.net.*;


public class MasterSlave {


    HeliosApplication local;
    HeliosAPI localHelios;

    HeliosApplication remote;
    HeliosAPI remoteHelios;

    AttitudeRad calculateOffset(AttitudeRad attitudeRad) {
        return attitudeRad;
    }

    void init() {
//        local = HeliosLocal.apply();
//        localHelios = local.Helios();

        try {
            startUdp();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void takeControl() {
        localHelios.takeControl();
//        remoteHelios.takeControl();
    }

    void arm() {
        localHelios.armMotors();
//        remoteHelios.armMotors();
    }

    void disarm() {
        localHelios.disarmMotors();
//        remoteHelios.disarmMotors();
    }
//        remote = HeliosRemote.apply("127.0.0.1", 12345);
//        remoteHelios = remote.Helios();

//        Observable<? extends AttitudeRad> commandStream =
//                local.Streams().attitudeCommandStream().asJavaObservable();
//
//        commandStream.subscribe(sendToSlave);

//    Action1<AttitudeRad> sendToSlave = new Action1<AttitudeRad>() {
//        @Override
//        public void call(AttitudeRad attitudeRad) {
//            remoteHelios.setAttitude(calculateOffset(attitudeRad), 0.5f);
//        }
//    };

    public void run() {
        init();
        try {
            startUdp();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void startUdp() throws Exception {
        startBroadcast();
        startReceive();
    }

    String receiveLoop(DatagramSocket socket, byte[] receive) throws Exception {
        DatagramPacket receivePacket = new DatagramPacket(receive, receive.length);
        socket.receive(receivePacket);
        return new String(receivePacket.getData());
    }

    private void startReceive() throws SocketException {
        final DatagramSocket serverSocket = new DatagramSocket(12345);
        final byte[] receiveData = new byte[8];

        class receiveRunnable implements Runnable {

            @Override
            public void run() {
                while (true) {
                    String res = null;
                    try {
                        res = receiveLoop(serverSocket, receiveData);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("Received: " + res);
                    for (char c : res != null ? res.toCharArray() : new char[0]) {
                        switch (c) {
                            case 'a':
                                takeControl();
                                break;

                            case 'b':
                                arm();
                                break;

                            case 'c':
                                disarm();
                                break;

                            default:
                                break;
                        }
                    }
                }

            }
        }

        new Thread(new receiveRunnable()).run();
    }

    private void startBroadcast() throws IOException {
        final MulticastSocket brdsock = new MulticastSocket();

        class broadcastRunnable implements Runnable {
            @Override
            public void run() {
                while (true) {
                    try {
                        broadcastLoop(brdsock);
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        new Thread(new broadcastRunnable()).start();
    }

    void broadcastLoop(MulticastSocket socket) throws Exception {
        InetAddress group = InetAddress.getByName("225.4.5.6");
        byte[] data = ("HELIOS").getBytes();
        DatagramPacket broadcastPacket = new DatagramPacket(data, data.length, group, 12345);
        socket.setTimeToLive(1);
        socket.send(broadcastPacket);

        System.out.print("Sent packet: " + broadcastPacket.getData());
        for(byte c : broadcastPacket.getData())
            System.out.print((char)c);
        System.out.print("\n");
    }
}
