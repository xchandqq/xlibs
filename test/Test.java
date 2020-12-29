
import java.net.InterfaceAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import net.NetworkInformation;
import net.NetworkListener;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Christian
 */
public class Test {
    private static final NetworkListener NETWORK_LISTENER = new NetworkListener() {
        @Override
        public void connected(InterfaceAddress ia, boolean reconnected) {
            System.out.println(reconnected?"Regained connection...":"You are connected to a network...");
            System.out.println("IP Address: "+ia.getAddress());
            try {
                System.out.println("Subnet Mask: "+NetworkInformation.getSubnetMask(ia.getNetworkPrefixLength()));
            } catch (UnknownHostException | SocketException | IllegalArgumentException ex) {
                Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Broadcast Address: "+ia.getBroadcast());
        }

        @Override
        public void disconnected() {
            System.out.println("Disconnected from the network");
        }
    };
    
    public static void main(String[] args) throws UnknownHostException, SocketException {
        NetworkInformation.startNetworkListener(NETWORK_LISTENER, 500, true);
        
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setVisible(true);
    }
}
