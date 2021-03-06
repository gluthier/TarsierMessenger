package ch.tarsier.tarsier.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ch.tarsier.tarsier.R;
import ch.tarsier.tarsier.domain.model.Peer;

/**
 * ChatroomPeersAdapter is the adapter for the ChatroomPeersActivity.
 *
 * @see ch.tarsier.tarsier.ui.activity.ChatroomPeersActivity
 * @author romac
 * @author gluthier
 */
public class ChatroomPeersAdapter extends ArrayAdapter<Peer> {
    private Context mContext;
    private int mLayoutResourceId;
    private List<Peer> mPeerList;

    public ChatroomPeersAdapter(Context context, int layoutResourceId) {
        super(context, layoutResourceId);
        mContext = context;
        mLayoutResourceId = layoutResourceId;
        mPeerList = new ArrayList<>();
    }

    @Override
    public long getItemId(int position) {
        if (mPeerList.get(position) != null) {
            return mPeerList.get(position).getId();
        }

        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        PeerHolder holder;

        if (row == null) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            row = inflater.inflate(mLayoutResourceId, parent, false);

            holder = new PeerHolder();
            holder.mAvatarSrc = (ImageView) row.findViewById(R.id.icon);
            holder.mName = (TextView) row.findViewById(R.id.name);
            holder.mStatus = (TextView) row.findViewById(R.id.status_message_profile_activity);

            row.setTag(holder);
        } else {
            holder = (PeerHolder) row.getTag();
        }

        Peer peer = getItem(position);

        holder.mAvatarSrc.setImageBitmap(peer.getPicture());
        holder.mName.setText(peer.getUserName());
        holder.mStatus.setText(peer.getStatusMessage());

        row.setContentDescription("Peer " + peer.getId());

        return row;
    }

    public void addAllPeers(List<Peer> peerList) {
        clear();
        addAll(peerList);
        mPeerList = peerList;
        setNotifyOnChange(true);
    }

    /**
     * PeerHolder is the class containing the peer's information
     */
    private class PeerHolder {
        private ImageView mAvatarSrc;
        private TextView mName;
        private TextView mStatus;
    }
}
