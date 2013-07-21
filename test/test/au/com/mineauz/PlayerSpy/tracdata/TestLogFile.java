package test.au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import test.au.com.mineauz.PlayerSpy.mocks.MockServer;

import au.com.mineauz.PlayerSpy.RecordList;
import au.com.mineauz.PlayerSpy.Records.*;
import au.com.mineauz.PlayerSpy.Records.LogoffRecord.LogoffType;
import au.com.mineauz.PlayerSpy.Utilities.ACIDRandomAccessFile;
import au.com.mineauz.PlayerSpy.Utilities.ReflectionHelper;
import au.com.mineauz.PlayerSpy.structurefile.StructuredFile;
import au.com.mineauz.PlayerSpy.tracdata.LogFile;
import au.com.mineauz.PlayerSpy.tracdata.SessionEntry;
import au.com.mineauz.PlayerSpy.tracdata.SessionIndex;
import au.com.mineauz.PlayerSpy.tracdata.SessionIndex.SessionData;

import static org.junit.Assert.*;

public class TestLogFile
{
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	
	private static Random mRand;
	
	@Before
	public void init() throws Exception
	{
		mRand = new Random(123456);
		MockServer.installIntoBukkit();
	}
	
	private Location createRandomLocation()
	{
		return new Location(Bukkit.getWorlds().get(mRand.nextInt(Bukkit.getWorlds().size())), (mRand.nextDouble() - 0.5) * 10000, (mRand.nextDouble() - 0.5) * 10000, (mRand.nextDouble() - 0.5) * 10000);
	}
	
	private Record createRandomRecord()
	{
		switch(mRand.nextInt(14))
		{
		case 0:
			return new ArmSwingRecord();
		case 1:
			return new ChatCommandRecord("Test message", false);
		case 2:
			return new DamageRecord(null, 10);
		case 3:
			return new DeathRecord(createRandomLocation(), "Death by over testing!");
		case 4:
			return new SessionInfoRecord(mRand.nextBoolean());
		case 5:
			return new GameModeRecord(GameMode.values()[mRand.nextInt(3)].getValue());
		case 6:
			return new HeldItemChangeRecord(mRand.nextInt(9));
		case 7:
			return new LoginRecord(createRandomLocation());
		case 8:
			return new LogoffRecord(LogoffType.Kick, "Kicked for over testing");
		case 9:
			return new RespawnRecord(createRandomLocation());
		case 10:
			return new SleepRecord(mRand.nextBoolean(), createRandomLocation());
		case 11:
			return new SprintRecord(mRand.nextBoolean());
		case 12:
			return new SneakRecord(mRand.nextBoolean());
		case 13:
			return new TeleportRecord(createRandomLocation(), TeleportCause.UNKNOWN);
		default:
			return null;
		}
	}
	
	@Test
	public void testAppend() throws IOException, RecordFormatException
	{
		LogFile log = LogFile.create("testAppend", folder.newFile().getAbsolutePath());
		log.testOverride = true;
		
		SessionIndex index = (SessionIndex)ReflectionHelper.readField(log.getClass(), "mSessionIndex", log);
		ACIDRandomAccessFile file = (ACIDRandomAccessFile) ReflectionHelper.readField(StructuredFile.class, "mFile", log);
		
		HashMap<Integer, RecordList> stored = new HashMap<Integer, RecordList>();
		
		// Simulate heavy appends
		for(int section = 0; section < 500; ++section)
		{
			file.beginTransaction();
			
			SessionData data = index.addEmptySession();
			// Fill it with data
			RecordList total = new RecordList();
			while (true)
			{
				RecordList list = new RecordList();
				for (int i = 0; i < mRand.nextInt(100) + 30; ++i)
					list.add(createRandomRecord());
				
				RecordList result = data.append(list);
				total.addAll(list);
				
				if(result != null)
					break;
			}
			stored.put(data.getIndexEntry().Id, total);
			
			file.commit();
		}
		
		int count = 0;
		for(SessionEntry session : index)
		{
			// Now read it back to confirm its there
			RecordList loaded = index.getDataFor(session).read();
			RecordList store = stored.get(session.Id);
			
			if(loaded.size() != store.size())
				fail("Size Mismatch in session " + count + ". Loaded: " + loaded.size() + " Stored: " + store.size());
			
			for(int i = 0; i < loaded.size(); ++i)
			{
				if(loaded.get(i).getType() != store.get(i).getType())
					fail(String.format("Type Mismatch in %d:%d/%d - ", count, i, loaded.size()) + "Expected Record " + store.get(i).toString() + " got " + loaded.get(i).toString());
				
				if(loaded.get(i).getTimestamp() != store.get(i).getTimestamp())
					fail(String.format("Timestamp Mismatch in %d:%d/%d - ", count, i, loaded.size()) + "Expected timestamp " + store.get(i).getTimestamp() + " got " + loaded.get(i).getTimestamp() + " Record: " + store.get(i).toString());
				
				if(loaded.get(i).getSize(true) != store.get(i).getSize(true))
					fail(String.format("Size(ABS) Mismatch in %d:%d/%d - ", count, i, loaded.size()) + "Expected size " + store.get(i).getSize(true) + " got " + loaded.get(i).getSize(true) + " Record: " + store.get(i).toString());
				
				if(loaded.get(i).getSize(false) != store.get(i).getSize(false))
					fail(String.format("Size(REL) Mismatch in %d:%d/%d - ", count, i, loaded.size()) + "Expected size " + store.get(i).getSize(false) + " got " + loaded.get(i).getSize(false) + " Record: " + store.get(i).toString());
			}
			
			count++;
		}
		
		log.close(true);
		
	}

}
