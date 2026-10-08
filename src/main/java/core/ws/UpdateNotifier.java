package core.ws;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.websocket.Session;

@ApplicationScoped
public class UpdateNotifier {
    @Inject
    Event<Update> updates;

    public void broadcast(String topic) {
        updates.fire(new Update(topic));
    }

    void send(@Observes(during = TransactionPhase.AFTER_SUCCESS) Update update) {
        for (Session s : UpdatesEndpoint.SESSIONS) {
            try {
                synchronized (s) {
                    if (s.isOpen()) {
                        s.getBasicRemote().sendText(update.topic());
                    }
                }
            } catch (Exception e) {
                UpdatesEndpoint.SESSIONS.remove(s);
            }
        }
    }

    public record Update(String topic) {
    }
}
