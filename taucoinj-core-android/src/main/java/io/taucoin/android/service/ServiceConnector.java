package io.taucoin.android.service;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;

import java.util.ArrayList;

public class ServiceConnector {

    /**
     * Incoming message handler. Calls to its binder are sequential!
     */
    protected final IncomingHandler handler;

    /**
     * Handler thread to avoid running on the main UI thread
     */
    protected final HandlerThread handlerThread;

    /** Context of the activity from which this connector was launched */
    protected Context context;

    /** The class of the service we want to connect to */
    protected Class serviceClass;

    /** Flag indicating if the service is bound. */
    protected boolean isBound;

    /** Sends messages to the service. */
    protected Messenger serviceMessenger = null;

    /** Receives messages from the service. */
    protected Messenger clientMessenger = null;

    protected ArrayList<ConnectorHandler> handlers = new ArrayList<>();

    Parcelable parcelableData;

    /** Handles incoming messages from service. */
    class IncomingHandler extends Handler {

        public IncomingHandler(HandlerThread thread) {

            super(thread.getLooper());
        }

        @Override
        public void handleMessage(Message message) {

            boolean isClaimed = false;
            if (message != null) {
//                String identifier = ((Bundle) message.obj).getString("identifier");
//                if (identifier != null) {

                    for (ConnectorHandler handler : handlers) {
//                        if (identifier.equals(handler.getID())) {
                            isClaimed = handler.handleMessage(message);
//                        }
                    }
//                }
            }
            if (!isClaimed) {
                super.handleMessage(message);
            }
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    protected ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {

            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service. We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            serviceMessenger = new Messenger(service);
            isBound = true;
            for (ConnectorHandler handler: handlers) {
                handler.onConnectorConnected();
            }
        }

        public void onServiceDisconnected(ComponentName className) {

            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            serviceMessenger = null;
            isBound = false;
            for (ConnectorHandler handler: handlers) {
                handler.onConnectorDisconnected();
            }
        }
    };

    public ServiceConnector(Context context, Class serviceClass) {

        this.context = context;
        this.serviceClass = serviceClass;
        handlerThread = new HandlerThread("HandlerThread");
        handlerThread.start();
        handler = new IncomingHandler(handlerThread);
        clientMessenger = new Messenger(handler);
    }

    /** Bind to the service */
    public boolean bindService() {

        if (serviceConnection != null) {
            Intent intent = new Intent(context, serviceClass);
            intent.putExtra("bean", parcelableData);
            try {
                context.getApplicationContext().startService(intent);
            } catch (IllegalStateException ex) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.getApplicationContext().startForegroundService(intent);
                } else {
                    context.getApplicationContext().startService(intent);
                }
            }
            return context.getApplicationContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            return false;
        }
    }

    /** Unbind from the service */
    public synchronized void unbindService() {

        if (isBound && serviceConnection != null) {
            context.getApplicationContext().unbindService(serviceConnection);
            isBound = false;
/*
            Intent intent = new Intent(context, serviceClass);
            context.getApplicationContext().stopService(intent);
*/
        }
    }

    public void registerHandler(ConnectorHandler handler) {

        if (!handlers.contains(handler)) {
            handlers.add(handler);
        }
    }

    public void removeHandler(ConnectorHandler handler) {

        handlers.remove(handler);
    }

}
