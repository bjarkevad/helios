package helios.examples.MasterSlave;

import helios.api.*;
import helios.api.HeliosAPI.*;
import rx.Observable;
import rx.util.functions.Action1;

import java.net.DatagramPacket;
import java.net.DatagramSocket;


public class MasterSlave {

    public void run() {
        init();
        try {
            startUdp();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    HeliosApplication local;
    HeliosAPI localHelios;

    HeliosApplication remote;
    HeliosAPI remoteHelios;

    AttitudeRad calculateOffset(AttitudeRad attitudeRad) {
        return attitudeRad;
    }

    void init() {
        local = HeliosLocal.apply();
        localHelios = local.Helios();

        try {
            startUdp();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        remote = HeliosRemote.apply("127.0.0.1", 12345);
//        remoteHelios = remote.Helios();

//        Observable<? extends AttitudeRad> commandStream =
//                local.Streams().attitudeCommandStream().asJavaObservable();
//
//        commandStream.subscribe(sendToSlave);
    }

//    Action1<AttitudeRad> sendToSlave = new Action1<AttitudeRad>() {
//        @Override
//        public void call(AttitudeRad attitudeRad) {
//            remoteHelios.setAttitude(calculateOffset(attitudeRad), 0.5f);
//        }
//    };

    void startUdp() throws Exception {
        DatagramSocket serverSocket = new DatagramSocket(9876);
        byte[] receiveData = new byte[8];

        while (true) {
            String res = udpLoop(serverSocket, receiveData);
            System.out.println("Received: " + res);
//            for (char c : res.toCharArray()) {
//                switch (c) {
//                    case 'a':
//                        takeControl();
//                        break;
//
//                    case 'b':
//                        arm();
//                        break;
//
//                    case 'c':
//                        disarm();
//                        break;
//
//                    default:
//                        break;
//                }
//            }
        }
    }

    String udpLoop(DatagramSocket socket, byte[] receive) throws Exception {
        DatagramPacket receivePacket = new DatagramPacket(receive, receive.length);
        socket.receive(receivePacket);
        return new String(receivePacket.getData());
    }

    void takeControl() {
        localHelios.takeControl();
        remoteHelios.takeControl();
    }

    void arm() {
        localHelios.armMotors();
        remoteHelios.armMotors();
    }

    void disarm() {
        localHelios.disarmMotors();
        remoteHelios.disarmMotors();
    }
}
