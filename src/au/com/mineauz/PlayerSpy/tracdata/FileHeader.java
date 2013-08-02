package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;

import au.com.mineauz.PlayerSpy.Utilities.Utility;
import au.com.mineauz.PlayerSpy.structurefile.IData;
import au.com.mineauz.PlayerSpy.structurefile.IndexEntry;

public class FileHeader implements IData<IndexEntry>
{
	public static final byte currentVersion = 4;
	public static final byte currentVersionMinor = 0;
	public byte VersionMajor = currentVersion;
	public byte VersionMinor = currentVersionMinor;
	public String PlayerName;
	public long IndexLocation;
	public long IndexSize;
	public int SessionCount;
	public long HolesIndexLocation;
	public long HolesIndexSize;
	public int HolesIndexCount;
	public short HolesIndexPadding;
	public boolean RequiresOwnerTags = false;
	public long OwnerMapLocation;
	public long OwnerMapSize;
	public int OwnerMapCount;
	public long RollbackIndexLocation;
	public long RollbackIndexSize;
	public int RollbackIndexCount;
	public long TagLocation;
	public long TagSize;
	public long ChunkIndexLocation;
	public long ChunkIndexSize;
	public int ChunkIndexCount;
	
	public byte[] Reserved = new byte[22];
	
	public void write(RandomAccessFile file) throws IOException
	{
		if(VersionMajor != currentVersion || VersionMinor != currentVersionMinor)
			throw new RuntimeException("Attempted to write an old version. Only the current version can be written");
		
		file.writeByte(VersionMajor);
		file.writeByte(VersionMinor);

		file.writeUTF(PlayerName);
		file.writeInt((int)IndexLocation);
		file.writeInt((int)IndexSize);
		
		file.writeShort((short)SessionCount);
		file.writeInt((int)HolesIndexLocation);
		file.writeInt((int)HolesIndexSize);
		file.writeShort((short)HolesIndexCount);
		file.writeShort(HolesIndexPadding);
		
		file.writeBoolean(RequiresOwnerTags);
		file.writeInt((int)OwnerMapLocation);
		file.writeInt((int)OwnerMapSize);
		file.writeShort((short)OwnerMapCount);
		
		file.writeInt((int)RollbackIndexLocation);
		file.writeInt((int)RollbackIndexSize);
		file.writeShort((int)RollbackIndexCount);
		
		file.writeInt((int)TagLocation);
		file.writeInt((int)TagSize);
		
		file.writeInt((int)ChunkIndexLocation);
		file.writeInt((int)ChunkIndexSize);
		file.writeShort((short)ChunkIndexCount);
		
		file.write(Reserved);
	}
	
	public void read(RandomAccessFile file) throws IOException
	{
		VersionMajor = file.readByte();
		VersionMinor = file.readByte();
		
		// Check the version
		// The minor version can be different since minor versions dont change what fields are present or the type but may change the contents of them
		if(VersionMajor != 1 && VersionMajor != 2 && VersionMajor != 3 && VersionMajor != 4)
			throw new RuntimeException("Unsupported file version!");
		
		PlayerName = file.readUTF();
			
		IndexLocation = Utility.getUnsignedInt(file.readInt());
		IndexSize = Utility.getUnsignedInt(file.readInt());
		SessionCount = (int)file.readShort();
		
		HolesIndexLocation = Utility.getUnsignedInt(file.readInt());
		HolesIndexSize = Utility.getUnsignedInt(file.readInt());
		HolesIndexCount = (int)file.readShort();
		HolesIndexPadding = file.readShort();
		
		if(VersionMajor >= 2)
		{
			RequiresOwnerTags = file.readBoolean();
			OwnerMapLocation = Utility.getUnsignedInt(file.readInt());
			OwnerMapSize = Utility.getUnsignedInt(file.readInt());
			OwnerMapCount = file.readShort();
		}
		
		if(VersionMajor >= 3)
		{
			RollbackIndexLocation = Utility.getUnsignedInt(file.readInt());
			RollbackIndexSize = Utility.getUnsignedInt(file.readInt());
			RollbackIndexCount = file.readShort();

			if(VersionMajor == 3)
			{
				// This data has been deprecated
				byte[] bytes = new byte[Utility.cBitSetSize/8];
				file.readFully(bytes);
			}
			
			TagLocation = Utility.getUnsignedInt(file.readInt());
			TagSize = Utility.getUnsignedInt(file.readInt());
			
			if(VersionMajor >= 4)
			{
				ChunkIndexLocation = Utility.getUnsignedInt(file.readInt());
				ChunkIndexSize = Utility.getUnsignedInt(file.readInt());
				ChunkIndexCount = file.readShort();
			}
			
			file.readFully(Reserved);
		}
		else if(VersionMajor == 2)
			file.readFully(new byte[14]);
	}
	
	public long getSize()
	{
		if(VersionMajor == 1)
			return 24 + Utility.getUTFLength(PlayerName);
		else if(VersionMajor == 2)
			return 49 +  + Utility.getUTFLength(PlayerName);
		else if(VersionMajor == 3)
			return 75 + Utility.getUTFLength(PlayerName) + (Utility.cBitSetSize/8);
		else if(VersionMajor == 4)
			return 85 + Utility.getUTFLength(PlayerName);
		else
			throw new IllegalArgumentException("Invalid Version " + VersionMajor);
	}
	
	@Override
	public long getLocation()
	{
		return 0;
	}
	
	@Override
	public IndexEntry getIndexEntry()
	{
		return null;
	}
	
	@Override
	public String toString()
	{
		return "TrackdataFileHeader";
	}
}