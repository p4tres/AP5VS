package utb.fai;

import java.nio.channels.SocketChannel;
import java.util.*;

public class ActiveHandlers {
    private static final long serialVersionUID = 1L;
    private HashSet<SocketHandler> activeHandlersSet = new HashSet<SocketHandler>();

    /**
     * sendMessageToAll - Pole zprávu vem aktivním klientùm kromì sebe sama
     * 
     * @param sender  - reference odesílatele
     * @param message - øetìzec se zprávou
     */
    synchronized void sendMessageToAll(SocketHandler sender, String message) {
        for (SocketHandler handler : activeHandlersSet){
            if (handler == sender || handler.name == null) continue;
            if(sharesGroup(sender, handler)){
                if (!handler.messages.offer(message))
                    System.err.printf("Client %s message queue is full, dropping the message!\n", handler.clientID);
            }
        }
    }

    private boolean sharesGroup(SocketHandler a, SocketHandler b){
        synchronized(a.groups){
            for (String g : a.groups)
                if(b.groups.contains(g)) return true;
        }
        return false;
    }

    synchronized boolean sendPrivate(String targetName, String message){
        SocketHandler target = findByName(targetName);
        if (target == null) return false;
        if(!target.messages.offer(message))
            System.err.printf("Client %s message queue is full, dropping the message!\n", target.clientID);
        return true;
    }

    synchronized SocketHandler findByName(String name){
        for (SocketHandler handler : activeHandlersSet)
            if (name.equals(handler.name)) return handler;
        return null;
    }

    synchronized boolean setName(SocketHandler handler, String name){
        SocketHandler owner = findByName(name);
        if (owner != null && owner != handler) return false;
        handler.name = name;
        return true;
    }

    /**
     * add pøidá do mnoiny aktivních handlerù nový handler.
     * Metoda je sychronizovaná, protoe HashSet neumí multithreading.
     * 
     * @param handler - reference na handler, který se má pøidat.
     * @return true if the set did not already contain the specified element.
     */
    synchronized boolean add(SocketHandler handler) {
        return activeHandlersSet.add(handler);
    }

    /**
     * remove odebere z mnoiny aktivních handlerù nový handler.
     * Metoda je sychronizovaná, protoe HashSet neumí multithreading.
     * 
     * @param handler - reference na handler, který se má odstranit
     * @return true if the set did not already contain the specified element.
     */
    synchronized boolean remove(SocketHandler handler) {
        return activeHandlersSet.remove(handler);
    }
}
