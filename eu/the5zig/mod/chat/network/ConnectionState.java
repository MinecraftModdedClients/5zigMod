package eu.the5zig.mod.chat.network;

public enum ConnectionState
{
  HANDSHAKE,  LOGIN,  PLAY,  ALL,  DISCONNECT;
  
  private ConnectionState() {}
}
