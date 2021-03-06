package test.au.com.mineauz.PlayerSpy.mocks;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.BlockChangeDelegate;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Difficulty;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

@SuppressWarnings( "deprecation" )
public class MockWorld implements World
{
	private String mName;
	
	public MockWorld(String name)
	{
		mName = name;
	}

	@Override
	public void sendPluginMessage( Plugin source, String channel, byte[] message )
	{

	}

	@Override
	public Set<String> getListeningPluginChannels()
	{
		return null;
	}

	@Override
	public void setMetadata( String metadataKey, MetadataValue newMetadataValue )
	{
	}

	@Override
	public List<MetadataValue> getMetadata( String metadataKey )
	{
		return null;
	}

	@Override
	public boolean hasMetadata( String metadataKey )
	{
		return false;
	}

	@Override
	public void removeMetadata( String metadataKey, Plugin owningPlugin )
	{
		

	}

	@Override
	public Block getBlockAt( int x, int y, int z )
	{
		
		return null;
	}

	@Override
	public Block getBlockAt( Location location )
	{
		
		return null;
	}

	@Override
	public int getBlockTypeIdAt( int x, int y, int z )
	{
		
		return 0;
	}

	@Override
	public int getBlockTypeIdAt( Location location )
	{
		
		return 0;
	}

	@Override
	public int getHighestBlockYAt( int x, int z )
	{
		
		return 0;
	}

	@Override
	public int getHighestBlockYAt( Location location )
	{
		
		return 0;
	}

	@Override
	public Block getHighestBlockAt( int x, int z )
	{
		
		return null;
	}

	@Override
	public Block getHighestBlockAt( Location location )
	{
		
		return null;
	}

	@Override
	public Chunk getChunkAt( int x, int z )
	{
		
		return null;
	}

	@Override
	public Chunk getChunkAt( Location location )
	{
		
		return null;
	}

	@Override
	public Chunk getChunkAt( Block block )
	{
		
		return null;
	}

	@Override
	public boolean isChunkLoaded( Chunk chunk )
	{
		
		return false;
	}

	@Override
	public Chunk[] getLoadedChunks()
	{
		
		return null;
	}

	@Override
	public void loadChunk( Chunk chunk )
	{
		

	}

	@Override
	public boolean isChunkLoaded( int x, int z )
	{
		
		return false;
	}

	@Override
	public boolean isChunkInUse( int x, int z )
	{
		
		return false;
	}

	@Override
	public void loadChunk( int x, int z )
	{
		

	}

	@Override
	public boolean loadChunk( int x, int z, boolean generate )
	{
		
		return false;
	}

	@Override
	public boolean unloadChunk( Chunk chunk )
	{
		
		return false;
	}

	@Override
	public boolean unloadChunk( int x, int z )
	{
		
		return false;
	}

	@Override
	public boolean unloadChunk( int x, int z, boolean save )
	{
		
		return false;
	}

	@Override
	public boolean unloadChunk( int x, int z, boolean save, boolean safe )
	{
		
		return false;
	}

	@Override
	public boolean unloadChunkRequest( int x, int z )
	{
		
		return false;
	}

	@Override
	public boolean unloadChunkRequest( int x, int z, boolean safe )
	{
		
		return false;
	}

	@Override
	public boolean regenerateChunk( int x, int z )
	{
		
		return false;
	}

	@Override
	public boolean refreshChunk( int x, int z )
	{
		
		return false;
	}

	@Override
	public Item dropItem( Location location, ItemStack item )
	{
		
		return null;
	}

	@Override
	public Item dropItemNaturally( Location location, ItemStack item )
	{
		
		return null;
	}

	@Override
	public Arrow spawnArrow( Location location, Vector velocity, float speed,
			float spread )
	{
		
		return null;
	}

	@Override
	public boolean generateTree( Location location, TreeType type )
	{
		
		return false;
	}

	@Override
	public boolean generateTree( Location loc, TreeType type,
			BlockChangeDelegate delegate )
	{
		
		return false;
	}

	@Override
	public Entity spawnEntity( Location loc, EntityType type )
	{
		
		return null;
	}

	@Override
	@Deprecated
	public LivingEntity spawnCreature( Location loc, EntityType type )
	{
		
		return null;
	}

	@Override
	@Deprecated
	public LivingEntity spawnCreature( Location loc, CreatureType type )
	{
		
		return null;
	}

	@Override
	public LightningStrike strikeLightning( Location loc )
	{
		
		return null;
	}

	@Override
	public LightningStrike strikeLightningEffect( Location loc )
	{
		
		return null;
	}

	@Override
	public List<Entity> getEntities()
	{
		
		return null;
	}

	@Override
	public List<LivingEntity> getLivingEntities()
	{
		
		return null;
	}

	@Override
	@Deprecated
	public <T extends Entity> Collection<T> getEntitiesByClass( Class<T>... classes )
	{
		
		return null;
	}

	@Override
	public <T extends Entity> Collection<T> getEntitiesByClass( Class<T> cls )
	{
		
		return null;
	}

	@Override
	public Collection<Entity> getEntitiesByClasses( Class<?>... classes )
	{
		
		return null;
	}

	@Override
	public List<Player> getPlayers()
	{
		
		return null;
	}

	@Override
	public String getName()
	{
		return mName;
	}

	@Override
	public UUID getUID()
	{
		
		return null;
	}

	@Override
	public Location getSpawnLocation()
	{
		
		return null;
	}

	@Override
	public boolean setSpawnLocation( int x, int y, int z )
	{
		
		return false;
	}

	@Override
	public long getTime()
	{
		
		return 0;
	}

	@Override
	public void setTime( long time )
	{
		

	}

	@Override
	public long getFullTime()
	{
		
		return 0;
	}

	@Override
	public void setFullTime( long time )
	{
		

	}

	@Override
	public boolean hasStorm()
	{
		
		return false;
	}

	@Override
	public void setStorm( boolean hasStorm )
	{
		

	}

	@Override
	public int getWeatherDuration()
	{
		
		return 0;
	}

	@Override
	public void setWeatherDuration( int duration )
	{
		

	}

	@Override
	public boolean isThundering()
	{
		
		return false;
	}

	@Override
	public void setThundering( boolean thundering )
	{
		

	}

	@Override
	public int getThunderDuration()
	{
		
		return 0;
	}

	@Override
	public void setThunderDuration( int duration )
	{
		

	}

	@Override
	public boolean createExplosion( double x, double y, double z, float power )
	{
		
		return false;
	}

	@Override
	public boolean createExplosion( double x, double y, double z, float power,
			boolean setFire )
	{
		
		return false;
	}

	@Override
	public boolean createExplosion( double x, double y, double z, float power,
			boolean setFire, boolean breakBlocks )
	{
		
		return false;
	}

	@Override
	public boolean createExplosion( Location loc, float power )
	{
		
		return false;
	}

	@Override
	public boolean createExplosion( Location loc, float power, boolean setFire )
	{
		
		return false;
	}

	@Override
	public Environment getEnvironment()
	{
		
		return null;
	}

	@Override
	public long getSeed()
	{
		
		return 0;
	}

	@Override
	public boolean getPVP()
	{
		
		return false;
	}

	@Override
	public void setPVP( boolean pvp )
	{
		

	}

	@Override
	public ChunkGenerator getGenerator()
	{
		
		return null;
	}

	@Override
	public void save()
	{
		

	}

	@Override
	public List<BlockPopulator> getPopulators()
	{
		
		return null;
	}

	@Override
	public <T extends Entity> T spawn( Location location, Class<T> clazz )
			throws IllegalArgumentException
	{
		
		return null;
	}

	@Override
	public FallingBlock spawnFallingBlock( Location location,
			Material material, byte data ) throws IllegalArgumentException
	{
		
		return null;
	}

	@Override
	public FallingBlock spawnFallingBlock( Location location, int blockId,
			byte blockData ) throws IllegalArgumentException
	{
		
		return null;
	}

	@Override
	public void playEffect( Location location, Effect effect, int data )
	{
		

	}

	@Override
	public void playEffect( Location location, Effect effect, int data,
			int radius )
	{
		

	}

	@Override
	public <T> void playEffect( Location location, Effect effect, T data )
	{
		

	}

	@Override
	public <T> void playEffect( Location location, Effect effect, T data,
			int radius )
	{
		

	}

	@Override
	public ChunkSnapshot getEmptyChunkSnapshot( int x, int z,
			boolean includeBiome, boolean includeBiomeTempRain )
	{
		
		return null;
	}

	@Override
	public void setSpawnFlags( boolean allowMonsters, boolean allowAnimals )
	{
		

	}

	@Override
	public boolean getAllowAnimals()
	{
		
		return false;
	}

	@Override
	public boolean getAllowMonsters()
	{
		
		return false;
	}

	@Override
	public Biome getBiome( int x, int z )
	{
		
		return null;
	}

	@Override
	public void setBiome( int x, int z, Biome bio )
	{
		

	}

	@Override
	public double getTemperature( int x, int z )
	{
		
		return 0;
	}

	@Override
	public double getHumidity( int x, int z )
	{
		
		return 0;
	}

	@Override
	public int getMaxHeight()
	{
		
		return 0;
	}

	@Override
	public int getSeaLevel()
	{
		
		return 0;
	}

	@Override
	public boolean getKeepSpawnInMemory()
	{
		
		return false;
	}

	@Override
	public void setKeepSpawnInMemory( boolean keepLoaded )
	{
		

	}

	@Override
	public boolean isAutoSave()
	{
		
		return false;
	}

	@Override
	public void setAutoSave( boolean value )
	{
		

	}

	@Override
	public void setDifficulty( Difficulty difficulty )
	{
		

	}

	@Override
	public Difficulty getDifficulty()
	{
		
		return null;
	}

	@Override
	public File getWorldFolder()
	{
		
		return null;
	}

	@Override
	public WorldType getWorldType()
	{
		
		return null;
	}

	@Override
	public boolean canGenerateStructures()
	{
		
		return false;
	}

	@Override
	public long getTicksPerAnimalSpawns()
	{
		
		return 0;
	}

	@Override
	public void setTicksPerAnimalSpawns( int ticksPerAnimalSpawns )
	{
		

	}

	@Override
	public long getTicksPerMonsterSpawns()
	{
		
		return 0;
	}

	@Override
	public void setTicksPerMonsterSpawns( int ticksPerMonsterSpawns )
	{
		

	}

	@Override
	public int getMonsterSpawnLimit()
	{
		
		return 0;
	}

	@Override
	public void setMonsterSpawnLimit( int limit )
	{
		

	}

	@Override
	public int getAnimalSpawnLimit()
	{
		
		return 0;
	}

	@Override
	public void setAnimalSpawnLimit( int limit )
	{
		

	}

	@Override
	public int getWaterAnimalSpawnLimit()
	{
		
		return 0;
	}

	@Override
	public void setWaterAnimalSpawnLimit( int limit )
	{
		

	}

	@Override
	public int getAmbientSpawnLimit()
	{
		
		return 0;
	}

	@Override
	public void setAmbientSpawnLimit( int limit )
	{
		

	}

	@Override
	public void playSound( Location loc, Sound sound, float volume, float pitch )
	{
		

	}

	@Override
	public String[] getGameRules()
	{
		
		return null;
	}

	@Override
	public String getGameRuleValue( String rule )
	{
		
		return null;
	}

	@Override
	public boolean setGameRuleValue( String rule, String value )
	{
		
		return false;
	}

	@Override
	public boolean isGameRule( String rule )
	{
		return false;
	}
	
	@Override
	public String toString()
	{
		return mName;
	}

}
