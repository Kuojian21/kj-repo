package com.kj.repo.infra.rpc;

import java.util.List;

public abstract class Registry {

    public abstract List<ServerInfo> getServerInfo();

    public abstract List<ClientInfo> getClientInfo();

    public abstract void register(ServerInfo info);

    public abstract void register(ClientInfo info);

}
