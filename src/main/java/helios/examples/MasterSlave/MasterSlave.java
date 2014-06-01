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
        System.out.println("Take Control!");
        try {
            localHelios.takeControl();
//        remoteHelios.takeControl();
        } catch (Exception e) {

        }
    }

    void arm() {
        System.out.println("Arm!");
        try {

            localHelios.armMotors();
//        remoteHelios.armMotors();
        } catch (Exception e) {

        }
    }

    void disarm() {
        System.out.println("Disarm!");
        try {
            localHelios.disarmMotors();
//        remoteHelios.disarmMotors();
        } catch (Exception e) {

        }
    }

    void takeOff() {
        System.out.println("Take off!");
        try {
           localHelios.takeOff(0.5f);
        } catch (Exception e) {

        }
    }

    void land() {
        System.out.println("Land!");
        try {
            localHelios.land();
        } catch (Exception e) {

        }
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
                    //System.out.println("Received: " + res);
                    for (char c : res != null ? res.toCharArray() : new char[0]) {
                        switch (c) {
                            case 'a':
                                arm();
                                break;

                            case 'b':
                                disarm();
                                break;

                            case 'c':
                                takeOff();
                                break;

                            case 'd':
                                land();
                                break;

                            case 'e':
                                takeControl();
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
                        InetAddress group = InetAddress.getByName("225.4.5.6");
                        broadcastLoop(brdsock, group, 12345, "HELIOS");
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        new Thread(new broadcastRunnable()).start();
    }

    void broadcastLoop(MulticastSocket socket, InetAddress group, int port, String contents) throws Exception {
        byte[] data = contents.getBytes();
        DatagramPacket broadcastPacket = new DatagramPacket(data, data.length, group, port);
        socket.setTimeToLive(10);
        socket.send(broadcastPacket);

        System.out.print("Sent packet: ");
        for (byte c : broadcastPacket.getData())
            System.out.print((char) c);
        System.out.println(" on address: " + broadcastPacket.getAddress() + ":" + broadcastPacket.getPort());
    }
}
