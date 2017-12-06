package org.jlab.coda.evio2et;

import org.jlab.coda.et.*;
import org.jlab.coda.et.enums.Mode;
import org.jlab.coda.et.exception.*;
import org.jlab.coda.jevio.EventWriter;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by gurjyan on 12/6/17.
 */
public class EvioToEt {
    private static EtSystem sys;
    private  static EtAttachment attach;

    private static String fileName;
    private static String etName;
    private static String host = "localhost";
    private static int port = 11111;

    public static void main(String[] args) {

        if(args.length == 0 || args[0].equals("-h") || args[0].equals("-help")){
            System.out.println("evio2et <fileName> <etName> <etHost (default = localhost)> <etPort (default = 11111)");
            return;
        } else if(args.length == 1) {
            System.out.println("File name and ET name are required arguments");
            return;
        } else if(args.length == 2){
            fileName = args[0];
            etName = args[1];
        } else if (args.length == 3){
            fileName = args[0];
            etName = args[1];
            host = args[2];
        }else if (args.length == 4){
            fileName = args[0];
            etName = args[1];
            host = args[2];
            port = Integer.parseInt(args[3]);
        } else {
            System.out.println("wrong set of arguments");
            return;
        }

        EvioEvent event;
        try {
            etConnect(etName, host, port);
        } catch (EtException | IOException | EtTooManyException | EtDeadException | EtClosedException e) {
            e.printStackTrace();
            return;
        }

        File fileIn = new File(fileName);
        System.out.println("read ev file: " + fileName + " size: " + fileIn.length());

        try {
            EvioReader fileReader = new EvioReader(fileName);
            System.out.println("\nnum ev = " + fileReader.getEventCount());
            System.out.println("dictionary = " + fileReader.getDictionaryXML() + "\n");

            event = fileReader.parseNextEvent();
            if (event == null) {
                System.out.println("We got a NULL event !!!");
                return;
            }
//            System.out.println("Event = \n" + event.toXML());
            int i=0;
            while ( (event = fileReader.parseNextEvent()) != null) {

                EtEvent[] etEventArray = new EtEvent[0];
                try {
                    etEventArray = sys.newEvents(attach, Mode.SLEEP, 0, 1, (int)sys.getEventSize());

//                    System.out.println("Event = " + event.toString());

                    ByteBuffer bf = etEventArray[0].getDataBuffer();
                    bf.order(ByteOrder.LITTLE_ENDIAN);
                        EventWriter evioWriter = new EventWriter(bf);
                        evioWriter.writeEvent(event);
                        sys.putEvents(attach, etEventArray);

                } catch (EtException | EtDeadException | EtClosedException | EtEmptyException
                        | EtBusyException | EtTimeoutException | EtWakeUpException  e) {
                    e.printStackTrace();
                }
            }
        }
        catch (IOException | EvioException e) {
            e.printStackTrace();
        }

    }

    private static void etConnect(String etName, String host, int port)
            throws EtException, IOException, EtTooManyException, EtDeadException, EtClosedException {

        EtSystemOpenConfig config = new EtSystemOpenConfig(etName, host, port);
        config.setConnectRemotely(true);

        sys = new EtSystem(config);
        sys.open();

        // get GRAND_CENTRAL station object
        EtStation gc = sys.stationNameToObject("GRAND_CENTRAL");

        // attach to GRAND_CENTRAL
        attach = sys.attach(gc);

    }

}
