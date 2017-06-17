package pivtrum;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.CoinDefinition;
import org.furszy.client.IoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import pivtrum.exceptions.InvalidPeerVersion;
import pivtrum.listeners.PeerListener;
import pivtrum.messages.VersionMsg;
import store.AddressStore;
import wallet.WalletManager;

/**
 * Created by furszy on 6/12/17.
 *
 * Class in charge of manage the connection with pivtrum servers.
 */

public class PivtrumPeergroup implements PeerListener {

    private static final Logger log = LoggerFactory.getLogger(PivtrumPeergroup.class);

    /**
     * Default number of connections
     */
    public static final int DEFAULT_CONNECTIONS = 1;

    /** Network configurations */
    private NetworkConf networkConf;
    /** Connection manager */
    private IoManager ioManager;
    /** Trusted peer */
    private PivtrumPeer trustedPeer;
    // Currently active peers.
    private final CopyOnWriteArrayList<PivtrumPeer> peers;
    // Currently connecting peers.
    private final CopyOnWriteArrayList<PivtrumPeer> pendingPeers;
    // The version message to use for new connections
    private VersionMsg versionMsg;
    // How many connections we want to have open at the current time. If we lose connections, we'll try opening more
    // until we reach this count.
    private AtomicInteger maxConnections = new AtomicInteger(1);
    // Whether the peer group is currently running. Once shut down it cannot be restarted.
    private volatile boolean isRunning;
    /** Whether the peer group was active. */
    private volatile boolean isActive;
    /** How many milliseconds to wait after receiving a pong before sending another ping. */
    public static final long DEFAULT_PING_INTERVAL_MSEC = 2000;
    private long pingIntervalMsec = DEFAULT_PING_INTERVAL_MSEC;
    /** Wallet manager */
    private WalletManager walletManager;
    /** Address-status store */
    private AddressStore addressStore;
    /** Minumum amount of server in which the app is going to broadcast a tx */
    private int minBroadcastConnections = CoinDefinition.minBroadcastConnections;

    public PivtrumPeergroup(NetworkConf networkConf, WalletManager walletManager, AddressStore addressStore) throws IOException {
        this.peers = new CopyOnWriteArrayList<>();
        this.pendingPeers = new CopyOnWriteArrayList<>();
        this.networkConf = networkConf;
        this.walletManager = walletManager;
        this.addressStore = addressStore;
        this.ioManager = new IoManager(1,1);
        // create the version message that the manager will always use
        versionMsg = new VersionMsg(networkConf.getClientName(),networkConf.getMaxProtocolVersion(),networkConf.getMinProtocolVersion());
    }

    /**
     *
     * La conexión no deberia ser sincrona, lo único que necesito es agregar una variable de "isRunning", una de "isConnecting" y un listener de conexion.
     * todo: return future..
     */
    public synchronized void start(){
        try {
            log.info("Starting PivtrumPeergroup");
            isActive = true;
            // todo: first part discovery..
            /*
            * Connect to the trusted node and get servers from it.
            */
            trustedPeer = new PivtrumPeer(networkConf.getTrustedServer(), ioManager,versionMsg);
            trustedPeer.addPeerListener(this);
            trustedPeer.connect();
        }catch (Exception e){
            isRunning = false;
            isActive = false;
            e.printStackTrace();
            log.error("PivtrumPeerGroup start",e);
        }
    }


    public boolean isRunning(){
        return isRunning;
    }


    @Override
    public void onConnected(PivtrumPeer pivtrumPeer) {
        try {
            if (pivtrumPeer == trustedPeer) {
                // trusted peer connected.
                isRunning = true;
                // Get more peers from the trusted server to use it later
                trustedPeer.getPeers();
                // Suscribe watched addresses to the trusted server
                List<Address> addresses = walletManager.getWatchedAddresses();
                if (!addresses.isEmpty())
                    trustedPeer.subscribeAddresses(addresses);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDisconnected(PivtrumPeer pivtrumPeer) {

    }

    @Override
    public void onExceptionCaught(PivtrumPeer pivtrumPeer, Exception e) {
        if (e instanceof InvalidPeerVersion){
            if (pivtrumPeer == trustedPeer){
                // We are fuck. Invalid trusted peer version..
                isActive = false;
                isRunning = false;
                // notify error..

            }
        }
    }
}