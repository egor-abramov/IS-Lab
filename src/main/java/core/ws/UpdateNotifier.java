package core.ws;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

@ApplicationScoped
public class UpdateNotifier {

    public void broadcast(String topic) {
        for (Session s : UpdatesEndpoint.SESSIONS) {
            try {
                synchronized (s) {
                    if (s.isOpen()) {
                        s.getBasicRemote().sendText(topic);
                    }
                }
            } catch (Exception e) {
                UpdatesEndpoint.SESSIONS.remove(s);
            }
        }
    }
}
