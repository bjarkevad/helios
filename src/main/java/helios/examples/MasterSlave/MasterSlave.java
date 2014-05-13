package helios.examples.MasterSlave;

import helios.api.HeliosAPI;
import helios.api.Streams;
import helios.api.Streams.StreamsImpl;
import helios.api.HeliosApplication;
import helios.api.HeliosLocal;
import helios.api.HeliosLocal.*;

public class MasterSlave {
    //public MasterSlave() {}

    public void run() {
        init();
    }

    HeliosApplication ha;
    HeliosAPI helios;
    void init() {
        ha = HeliosLocal.apply();
        helios = ha.Helios();

        while(true) {
            helios.takeControl();
            try { Thread.sleep(500);} catch (InterruptedException e) {
                e.printStackTrace();
            } {}
        }
    }
}
