package core.ws;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.OnMessage;
import jakarta.websocket.CloseReason;
import jakarta.inject.Inject;
import jakarta.enterprise.context.Dependent;
import core.service.AuthService;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/updates")
@Dependent
public class UpdatesEndpoint {
    static final Map<Session, String> SESSIONS = new ConcurrentHashMap<>();

    @Inject
    AuthService auth;

    @OnOpen
    public void onOpen(Session session) {
        session.setMaxIdleTimeout(10_000);
        session.setMaxTextMessageBufferSize(4096);
    }

    @OnMessage
    public void authenticate(String token, Session session) throws IOException {
        try {
            auth.authenticate(token);
            SESSIONS.put(session, token);
            session.setMaxIdleTimeout(3_600_000);
            session.getBasicRemote().sendText("ready");
        } catch (NotAuthorizedException e) {
            SESSIONS.remove(session);
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Authentication required"));
        }
    }

    @OnClose
    public void onClose(Session session) {
        SESSIONS.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable t) {
        SESSIONS.remove(session);
    }
}
