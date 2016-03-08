import java.util.*;

public interface ILoadBalancer {
    public boolean canAcceptNewConnections(Map<String, Object> statusVars); // pass generic argumens
}