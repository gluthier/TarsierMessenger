package ch.tarsier.tarsier.network;

import com.google.protobuf.InvalidProtocolBufferException;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ch.tarsier.tarsier.Tarsier;
import ch.tarsier.tarsier.domain.model.Peer;
import ch.tarsier.tarsier.event.ConnectToDeviceEvent;
import ch.tarsier.tarsier.event.ConnectedEvent;
import ch.tarsier.tarsier.event.CreateGroupEvent;
import ch.tarsier.tarsier.event.ErrorConnectionEvent;
import ch.tarsier.tarsier.event.ReceivedChatroomPeersListEvent;
import ch.tarsier.tarsier.event.ReceivedMessageEvent;
import ch.tarsier.tarsier.event.ReceivedNearbyPeersListEvent;
import ch.tarsier.tarsier.event.RequestChatroomPeersListEvent;
import ch.tarsier.tarsier.event.RequestNearbyPeersListEvent;
import ch.tarsier.tarsier.event.SendMessageEvent;
import ch.tarsier.tarsier.exception.DomainException;
import ch.tarsier.tarsier.exception.PeerCipherException;
import ch.tarsier.tarsier.network.client.ClientConnection;
import ch.tarsier.tarsier.network.messages.TarsierWireProtos;
import ch.tarsier.tarsier.network.server.ServerConnection;
import ch.tarsier.tarsier.network.messages.MessageType;

import static ch.tarsier.tarsier.network.messages.TarsierWireProtos.TarsierPublicMessage;

/**
 * MessagingManager is the class that manage all the messaging, abstracting over the
 * type of connection we have (thanks to {@link ConnectionInterface}).
 *
 * @author FredericJacobs
 * @author amirezzaw
 */
public class MessagingManager extends BroadcastReceiver implements ConnectionInfoListener,
        Handler.Callback{

    private static final String NETWORK_LAYER_TAG = "TarsierMessagingManager";

    private static final String WIFI_DIRECT_TAG = "WiFiDirect";

    private static final int CONFIG_GROUP_OWNER_INTENT = 15;

    private WifiP2pManager mManager;

    private WifiP2pManager.Channel mChannel;

    private ConnectionInterface mConnection;

    private WifiP2pManager.PeerListListener peerListListener;

    private final ArrayList<WifiP2pDevice> mP2pDevices = new ArrayList<WifiP2pDevice>();

    private Handler mHandler = new Handler(this);

    private Bus mEventBus;

    private boolean mDisabled = false;

    public MessagingManager(WifiP2pManager wifiManager, WifiP2pManager.Channel channel) {
        mManager = wifiManager;
        mChannel = channel;

        createPeerListener();
        resetNetworkStackAndInitiatePeerDiscovery();

        mManager.requestPeers(mChannel, peerListListener);
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        // The group owner accepts connections using a server socket and then spawns a
        // client socket for every client.
        Log.d(WIFI_DIRECT_TAG, "onConnectionInfoAvailable");

        if (mConnection == null) {

            if (p2pInfo.isGroupOwner) {
                Log.d(WIFI_DIRECT_TAG, "Connected as group owner");

                try {
                    mConnection = new ServerConnection(getConnectionHandler());
                } catch (IOException e) {
                    Log.d(WIFI_DIRECT_TAG, "Failed to create a server thread - " + e.getMessage());
                    return;
                }

                mEventBus.post(new ConnectedEvent(true));

            } else {
                Log.d(WIFI_DIRECT_TAG, "Connected as peer");

                mConnection = new ClientConnection(
                        getConnectionHandler(),
                        p2pInfo.groupOwnerAddress);

                mEventBus.post(new ConnectedEvent(false));
            }
        }
    }

    public void connectToDevice(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = CONFIG_GROUP_OWNER_INTENT;
        Log.d(NETWORK_LAYER_TAG, "connectToDevice is called");
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(NETWORK_LAYER_TAG, "Connected to device");
            }

            @Override
            public void onFailure(int errorCode) {
                Log.d(NETWORK_LAYER_TAG, "Failed connecting to service");
            }
        });
    }

    public void createGroup() {
        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(NETWORK_LAYER_TAG, "Created a new group");
            }

            @Override
            public void onFailure(int reasonCode) {
                if (reasonCode == WifiP2pManager.P2P_UNSUPPORTED) {
                    Log.d(WIFI_DIRECT_TAG,
                            "Failed to create a group. P2P isn't supported on this device.");
                } else if (reasonCode == WifiP2pManager.BUSY) {
                    Log.d(WIFI_DIRECT_TAG,
                            "Failed to create a group. The system is too busy to process the request.");
                } else if (reasonCode == WifiP2pManager.ERROR) {
                    Log.d(WIFI_DIRECT_TAG,
                            "Failed to create a group. The operation failed due to an internal error.");
                }
            }
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(WIFI_DIRECT_TAG, action);

        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            Log.d(WIFI_DIRECT_TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");

            if (mManager == null) {
                Log.e(WIFI_DIRECT_TAG, "Fatal error! mManager does not exist");
                return;
            }

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                // we are connected with the other device, request connection
                // info to find group owner IP
                Log.d(WIFI_DIRECT_TAG,
                        "Connected to p2p network. Requesting network details");

                mManager.requestConnectionInfo(mChannel, this);
            } else {
                Log.d(WIFI_DIRECT_TAG, "Did receive an Intent action : " + action);
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

            Log.d(WIFI_DIRECT_TAG, "Device status -" + device.status);

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (mManager != null) {
                mManager.requestPeers(mChannel, peerListListener);
            }

            Log.d(WIFI_DIRECT_TAG, "P2P peers changed");
        }
    }

    private void createPeerListener() {
        peerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public synchronized void onPeersAvailable(WifiP2pDeviceList peersList) {
                mP2pDevices.clear();
                mP2pDevices.addAll(peersList.getDeviceList());

                // If an AdapterView is backed by this data, notify it
                // of the change.  For instance, if you have a ListView of available
                // peers, trigger an update.

                Log.d(WIFI_DIRECT_TAG, "Peer list updated: " + mP2pDevices.toString());

                mEventBus.post(new ReceivedNearbyPeersListEvent(mP2pDevices));

                if (mP2pDevices.size() == 0) {
                    Log.d(WIFI_DIRECT_TAG, "No devices found");
                }
            }
        };
    }

    private void resetNetworkStackAndInitiatePeerDiscovery() {
        Log.d(NETWORK_LAYER_TAG, "Resetting network stack");
        mConnection = null;
        mManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                removeGroupAndInitiateDiscover();
            }

            @Override
            public void onFailure(int reason) {
                removeGroupAndInitiateDiscover();
            }
        });
    }

    private void removeGroupAndInitiateDiscover() {
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                initiatePeerDiscovery();
            }

            @Override
            public void onFailure(int reason) {
                initiatePeerDiscovery();
            }
        });
    }

    private void initiatePeerDiscovery() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(WIFI_DIRECT_TAG, "Peer discovery initiation succeeded");
            }

            @Override
            public void onFailure(int reasonCode) {
                if (reasonCode == WifiP2pManager.P2P_UNSUPPORTED) {
                    Log.d(WIFI_DIRECT_TAG,
                            "Peer discovery initiation failed. P2P isn't supported on this device.");
                } else if (reasonCode == WifiP2pManager.BUSY) {
                    Log.d(WIFI_DIRECT_TAG,
                            "Peer discovery initiation failed. The system is too busy to process the request.");
                } else if (reasonCode == WifiP2pManager.ERROR) {
                    Log.d(WIFI_DIRECT_TAG,
                            "Peer discovery initiation failed. The operation failed due to an internal error.");
                }
            }
        });
    }

    public List<Peer> getChatroomPeersList() {
        if (mConnection != null) {
            return mConnection.getPeersList();
        } else {
            Log.d("Connection", "mConnection is null");
            return new ArrayList<>();
        }

    }

    private List<WifiP2pDevice> getNearbyPeersList() {
        return mP2pDevices;
    }

    public void broadcastMessage(String message) {
        mConnection.broadcastMessage(message.getBytes());
    }

    public void sendMessage(Peer peer, String message) throws PeerCipherException {
        mConnection.sendMessage(peer, message.getBytes());
    }

    public void setEventBus(Bus eventBus) {
        if (eventBus == null) {
            throw new IllegalArgumentException("Event bus cannot be null");
        }

        mEventBus = eventBus;
        mEventBus.register(this);
    }

    @Override
    public boolean handleMessage(Message message) {
        Log.d(NETWORK_LAYER_TAG, "handleMessage called");

        switch (message.what) {
            case MessageType.MESSAGE_TYPE_HELLO:
                Log.d(NETWORK_LAYER_TAG, "MESSAGE_TYPE_HELLO received.");
                break;

            case MessageType.MESSAGE_TYPE_PEER_LIST:
                Log.d(NETWORK_LAYER_TAG, "MESSAGE_TYPE_PEER_LIST received.");
                mEventBus.post(new ReceivedChatroomPeersListEvent(getChatroomPeersList()));
                break;

            case MessageType.MESSAGE_TYPE_PRIVATE:
                Log.d(NETWORK_LAYER_TAG, "MESSAGE_TYPE_PRIVATE received.");
                postReceivedMessageEvent(message);
                break;

            case MessageType.MESSAGE_TYPE_PUBLIC:
                Log.d(NETWORK_LAYER_TAG, "MESSAGE_TYPE_PUBLIC received.");
                postReceivedMessageEvent(message);
                break;

            default:
                Log.d(NETWORK_LAYER_TAG, "Unknown message type");
        }

        return true;
    }

    private void postReceivedMessageEvent(Message message) {
        try {
            boolean isPrivate = message.what == MessageType.MESSAGE_TYPE_PRIVATE;

            if (isPrivate) {
                TarsierWireProtos.TarsierPrivateMessage msg =
                        TarsierWireProtos.TarsierPrivateMessage.parseFrom((byte[]) message.obj);
                byte[] publicKey = msg.getSenderPublicKey().toByteArray();
                String contents = msg.getCipherText().toStringUtf8();
                Peer sender = Tarsier.app().getPeerRepository().findByPublicKey(publicKey);

                mEventBus.post(new ReceivedMessageEvent(contents, sender, true));
            } else {
                TarsierPublicMessage msg = TarsierPublicMessage.parseFrom((byte[]) message.obj);

                byte[] publicKey = msg.getSenderPublicKey().toByteArray();
                String contents = msg.getPlainText().toStringUtf8();
                Peer sender = Tarsier.app().getPeerRepository().findByPublicKey(publicKey);

                mEventBus.post(new ReceivedMessageEvent(contents, sender, false));
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        } catch (DomainException e) {
            Log.d(NETWORK_LAYER_TAG, "Could not find peer in database for received message.");
        }
    }

    @Subscribe
    public void onSendMessageEvent(SendMessageEvent event) {
        if (mDisabled) {
            return;
        }

        ch.tarsier.tarsier.domain.model.Message message = event.getMessage();
        if (mConnection != null) {
            if (event.isPublic()) {
                Log.d(NETWORK_LAYER_TAG, "Got SendMessageEvent for a public message.");
                broadcastMessage(message.getText());
            } else if (event.isPrivate()) {
                Log.d(NETWORK_LAYER_TAG, "Got SendMessageEvent for a private message.");
                try {
                    Peer host = event.getChat().getHost();
                    sendMessage(event.getChat().getHost(), message.getText());
                } catch (PeerCipherException e) {
                    Log.e(NETWORK_LAYER_TAG, "Cannot send message, encountered issue encrypting");
                }
            } else {
                Log.d(NETWORK_LAYER_TAG, "Cannot send message that is neither private nor public.");
            }
        } else {
            Log.d(NETWORK_LAYER_TAG, "mConnection is null. Cannot send message to nobody");
            mEventBus.post(new ErrorConnectionEvent(
                    "Error : There is no connected devices. The message was not send"));
        }
    }

    @Subscribe
    public void onConnectToDeviceEvent(ConnectToDeviceEvent event) {
        if (mDisabled) {
            return;
        }

        Log.d(NETWORK_LAYER_TAG, "Got ConnectToDeviceEvent");
        connectToDevice(event.getDevice());
    }

    @Subscribe
    public void onCreateGroupEvent(CreateGroupEvent event) {
        if (mDisabled) {
            return;
        }

        Log.d(NETWORK_LAYER_TAG, "Got CreateGroupEvent");
        createGroup();
    }

    @Subscribe
    public void onRequestNearbyPeersListEvent(RequestNearbyPeersListEvent event) {
        if (mDisabled) {
            return;
        }

        Log.d(NETWORK_LAYER_TAG, "Got RequestNearbyPeersListEvent");

        if (mEventBus != null) {
            Log.d(NETWORK_LAYER_TAG, "Sending ReceivedNearbyPeersListEvent");
            resetNetworkStackAndInitiatePeerDiscovery();
            mEventBus.post(new ReceivedNearbyPeersListEvent(getNearbyPeersList()));
        }
    }

    @Subscribe
    public void onRequestChatroomPeersListEvent(RequestChatroomPeersListEvent event) {
        if (mDisabled) {
            return;
        }

        Log.d(NETWORK_LAYER_TAG, "Got RequestChatroomPeersListEvent");

        if (mEventBus != null) {
            Log.d(NETWORK_LAYER_TAG, "Sending ReceivedChatroomPeersListEvent");
            mEventBus.post(new ReceivedChatroomPeersListEvent(getChatroomPeersList()));
        }
    }

    public Handler getConnectionHandler() {
        return mHandler;
    }

    public void setDisabled(boolean disabled) {
        mDisabled = disabled;
    }

    public boolean isDisabled() {
        return mDisabled;
    }
}
