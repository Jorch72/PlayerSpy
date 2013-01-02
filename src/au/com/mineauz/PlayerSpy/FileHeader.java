package au.com.mineauz.PlayerSpy;

import java.io.IOException;
import java.io.RandomAccessFile;

import au.com.mineauz.PlayerSpy.Utilities.Utility;

public class FileHeader
{
	public byte VersionMajor = 3;
	public byte VersionMinor = 0;
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
	public byte[] Reserved = new byte[14];
	
	public void write(RandomAccessFile file) throws IOException
	{
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
		
		if(VersionMajor == 2 || VersionMajor == 3)
		{
			file.writeBoolean(RequiresOwnerTags);
			file.writeInt((int)OwnerMapLocation);
			file.writeInt((int)OwnerMapSize);
			file.writeShort((short)OwnerMapCount);
			file.write(Reserved);
		}
	}
	
	public void read(RandomAccessFile file) throws IOException
	{
		VersionMajor = file.readByte();
		VersionMinor = file.readByte();
		
		// Check the version
		// The minor version can be different since minor versions dont change what fields are present or the type but may change the contents of them
		if(VersionMajor != 1 && VersionMajor != 2 && VersionMajor != 3)
			throw new RuntimeException("Unsupported file version!");
		
		PlayerName = file.readUTF();
			
		IndexLocation = (long)file.readInt();
		IndexSize = (long)file.readInt();
		SessionCount = (int)file.readShort();
		
		HolesIndexLocation = (long)file.readInt();
		HolesIndexSize = (long)file.readInt();
		HolesIndexCount = (int)file.readShort();
		HolesIndexPadding = file.readShort();
		
		if(VersionMajor == 2 || VersionMajor == 3)
		{
			RequiresOwnerTags = file.readBoolean();
			OwnerMapLocation = file.readInt();
			OwnerMapSize = file.readInt();
			OwnerMapCount = file.readShort();
			file.readFully(Reserved);
		}
	}
	
	public int getSize()
	{
		if(VersionMajor == 1)
			return 24 + Utility.getUTFLength(PlayerName);
		else if(VersionMajor == 2 || VersionMajor == 3)
			return 49 +  + Utility.getUTFLength(PlayerName);
		else
			throw new IllegalArgumentException("Invalid Version " + VersionMajor);
	}
}