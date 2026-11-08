package core.ws;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.websocket.Session;
import jakarta.websocket.CloseReason;
import java.io.IOException;
import core.security.CurrentUser;
import core.service.AuthService;
import core.model.AppUser;
import core.model.Role;
import java.util.Objects;

@ApplicationScoped
public class UpdateNotifier {
    @Inject
    Event<Update> updates;
    @Inject
    CurrentUser currentUser;
    @Inject
    AuthService auth;

    public void broadcast(String topic) {
        updates.fire(new Update(topic, currentUser.id()));
    }

    void send(@Observes(during = TransactionPhase.AFTER_SUCCESS) Update update) {
        for (Session s : UpdatesEndpoint.SESSIONS.keySet()) {
            try {
                AppUser user = auth.authenticate(UpdatesEndpoint.SESSIONS.get(s));
                if (user.getRole() != Role.ADMIN && !Objects.equals(user.getId(), update.ownerId())) {
                    continue;
                }
                synchronized (s) {
                    if (s.isOpen()) {
                        s.getBasicRemote().sendText(update.topic());
                    }
                }
            } catch (Exception e) {
                UpdatesEndpoint.SESSIONS.remove(s);
                try {
                    s.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Session ended"));
                } catch (IOException ignored) {
                }
            }
        }
    }

    public record Update(String topic, Long ownerId) {
    }
}
