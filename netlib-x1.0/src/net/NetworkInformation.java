/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;

/**
 *
 * @author Christian
 */
public class NetworkInformation {
    
    private static boolean hasNetwork = false;
    private static boolean newThreadCheck = false;
    private static InterfaceAddress previousInterfaceAddress = null;
    private static NetworkListener networkListener = null;
    private static Timer networkListenerTimer = null;    
    private static final ActionListener TIMER_ACTION_LISTENER = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(networkListener == null) return;
            
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        InetAddress address = InetAddress.getLocalHost();
                        InterfaceAddress ia;
                        if(address.isLoopbackAddress()) ia = null;
                        else ia = getCurrentInterfaceAddress(address, true, true);

                        boolean hasPreviousIA = previousInterfaceAddress!=null;
                        boolean hasCurrentIA = ia!=null;


                        if(hasPreviousIA != hasCurrentIA){
                            if(ia == null){
                                hasNetwork = false;
                                networkListener.disconnected();
                            }
                            else{
                                hasNetwork = true;
                                networkListener.connected(ia, previousInterfaceAddress!=null);
                            }
                            previousInterfaceAddress = ia;
                        }
                    } catch (UnknownHostException | SocketException | VerifyError ex) {
                        Logger.getLogger(NetworkInformation.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };
            
            if(newThreadCheck) new Thread(runnable).start();
            else runnable.run();
        }
    };
    
    /**
     * Determines if your machine is connected to a network and has an IPv4 Address and a broadcast address.
     * This is done in two ways; first is to check for the interface address if and only if there is no
     * network listener specified; otherwise, the checking is done together with the listener.
     * @return <code>true</code> if your machine has an IPv4 address and a broadcast address.
     * @throws UnknownHostException if the local host name could not be resolved into an address.
     * @throws SocketException if an I/O error occurs.
     * @see #startNetworkListener
     */
    public static boolean hasNetwork() throws UnknownHostException, SocketException{
        if(networkListener == null){
            InetAddress address = InetAddress.getLocalHost();
            InterfaceAddress ia;
            if(address.isLoopbackAddress()) ia = null;
            else ia = getCurrentInterfaceAddress(address, true, true);
            return ia!=null;
        }
        else return hasNetwork;
    }
    
    /**
     * Starts a timer that periodically looks for changes in network information every <code>updateInterval</code> in milliseconds.
     * The minimum interval is set to <code>200</code> milliseconds.
     * @param nl the interface that gets called once there are changes in the network
     * @param updateInterval the interval in milliseconds
     * @param newThread if <code>true</code>, the network checking is done in a separate thread
     */
    public static void startNetworkListener(NetworkListener nl, int updateInterval, boolean newThread){
        networkListener = nl;
        newThreadCheck = newThread;
        if(networkListenerTimer != null) networkListenerTimer.stop();
        networkListenerTimer = new Timer(Math.max(updateInterval, 200), TIMER_ACTION_LISTENER);
        networkListenerTimer.setInitialDelay(0);
        networkListenerTimer.start();
    }
    
    /**
     * Returns the interface addresses associated by the <code>address</code>.
     * @param address the address to look for interfaces
     * @return An <code>array</code> of interface addresses
     * @throws SocketException if an I/O error occurs.
     */
    public static InterfaceAddress[] getInterfaceAddresses(InetAddress address) throws SocketException{
        NetworkInterface ni = NetworkInterface.getByInetAddress(address);
        if(ni == null) return new InterfaceAddress[0];
        return ni.getInterfaceAddresses().toArray(new InterfaceAddress[ni.getInterfaceAddresses().size()]);
    }
    
    /**
     * Returns the interface addresses associated by the <code>localHost</code> address.
     * @return An <code>array</code> of interface addresses
     * @throws UnknownHostException if the local host name could not be resolved into an address.
     * @throws SocketException if an I/O error occurs.
     */
    public static InterfaceAddress[] getInterfaceAddresses() throws UnknownHostException, SocketException{
        return getInterfaceAddresses(InetAddress.getLocalHost());
    }
    
    private static InterfaceAddress getCurrentInterfaceAddress(InetAddress address, boolean ipv4, boolean hasBroadcast) throws SocketException, VerifyError{
        InterfaceAddress returnAddress = null;
        int count = 0;
        for(InterfaceAddress ia : getInterfaceAddresses(address)){
            if(ipv4 != (ia.getNetworkPrefixLength() <= 32)) continue;
            if(hasBroadcast != (ia.getBroadcast()!=null)) continue;    
            returnAddress = ia;
            count++;
        }
        if(count > 1) throw new VerifyError("Multple interface address found ("+count+")");
        return returnAddress;
    }
    
    /**
     * Returns an InetAddress for the broadcast address for this InterfaceAddress. 
     * Only IPv4 networks have broadcast address therefore, in the case of an IPv6 network, null will be returned.
     * @return the <code>InetAddress</code> representing the broadcast address or null if there is no broadcast address.
     * @throws UnknownHostException if the local host name could not be resolved into an address.
     * @throws SocketException if an I/O error occurs.
     */
    public static InetAddress getBroadcastAddress() throws UnknownHostException, SocketException{
        return getCurrentInterfaceAddress(InetAddress.getLocalHost(), true, true).getBroadcast();
    }
    
    /**
     *
     * Determines the subnet mask of your interface in <code>InetAddress</code> format using a predefined prefix.
     * @param pref the prefix to be converted
     * @return The subnet mask equivalent to the specified prefix
     * @throws UnknownHostException if the local host name could not be resolved into an address.
     * @throws SocketException if an I/O error occurs.
     * @throws IllegalArgumentException if the <code>pref</code> argument is outside the accepted range
     */
    public static InetAddress getSubnetMask(short pref) throws UnknownHostException, SocketException, IllegalArgumentException{
        if(pref < 1 || pref > 32) throw new IllegalArgumentException("Prefix cannot be less than one or greater than 32");
        short prefix = pref;//getNetworkPreffix();
        byte[] addr = new byte[4];
        for(int i = 0; prefix > 0; i++){
            addr[i] = (byte) (256 - 256/(Math.pow(2, Math.min(prefix, 8))));
            prefix -= 8;
        }
        return InetAddress.getByAddress(addr);
    }
    
    /**
     * Determines the subnet mask of your interface in <code>InetAddress</code> format.
     * @return The subnet mask of your interface
     * @throws UnknownHostException if the local host name could not be resolved into an address.
     * @throws SocketException if an I/O error occurs.
     */
    public static InetAddress getSubnetMask() throws UnknownHostException, SocketException{
        return getSubnetMask(getNetworkPreffix());
    }
    
    /**
     * Returns the network prefix of the interface your machine is currently using. Only returns
     * if the interface has an IPv4 address and a broadcast address.
     * @return The <code>network prefix</code> of your interface
     * @throws UnknownHostException if the local host name could not be resolved into an address.
     * @throws SocketException if an I/O error occurs.
     * @throws NullPointerException if your machine is not connected to a network
     */
    public static short getNetworkPreffix() throws UnknownHostException, SocketException, NullPointerException{
        return getCurrentInterfaceAddress(InetAddress.getLocalHost(), true, true).getNetworkPrefixLength();
    }
    
}
