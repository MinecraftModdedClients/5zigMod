package eu.the5zig.mod.server.hypixel;

public enum HypixelGameType
{
  GENERAL("General", "General", 0),  QUAKECRAFT("Quakecraft", "Quake", 2),  WALLS("Walls", "Walls", 3),  PAINTBALL("Paintball", "Paintball", 4),  SURVIVAL_GAMES("Blitz Survival Games", "HungerGames", 5),  TNTGAMES("The TNT Games", "TNTGames", 6),  VAMPIREZ("VampireZ", "VampireZ", 7),  WALLS3("Mega Walls", "Walls3", 13),  ARCADE("Arcade", "Arcade", 14),  ARENA("Arena Brawl", "Arena", 17),  MCGO("Cops and Crims", "MCGO", 21),  UHC("UHC Champions", "UHC", 20),  BATTLEGROUND("Warlords", "Battleground", 23),  TURBO_KART_RACERS("Turbo Kart Racers", "GingerBread", 25),  SKYWARS("SkyWars", "SkyWars", 51);
  
  private final String name;
  private String databaseName;
  private final int id;
  
  private HypixelGameType(String name, String databaseName, int id)
  {
    this.name = name;
    this.databaseName = databaseName;
    this.id = id;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public String getDatabaseName()
  {
    return this.databaseName;
  }
  
  public int getId()
  {
    return this.id;
  }
  
  public static HypixelGameType fromName(String name)
  {
    for (HypixelGameType gameType : ) {
      if (gameType.getName().equals(name)) {
        return gameType;
      }
    }
    return null;
  }
  
  public static HypixelGameType fromId(int id)
  {
    for (HypixelGameType gameType : ) {
      if (gameType.id == id) {
        return gameType;
      }
    }
    return null;
  }
}
