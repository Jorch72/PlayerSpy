package au.com.mineauz.PlayerSpy.tracdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import au.com.mineauz.PlayerSpy.structurefile.IndexEntry;

class OwnerMapEntry extends IndexEntry
{
	public static final int cMaxOwnerLength = 16;
	public static final int cSize = 4 + cMaxOwnerLength;
	
	public int Id;
	public String Owner;
	
	public void write(RandomAccessFile file) throws IOException
	{
		file.writeInt(Id);
		byte[] ownerData = new byte[cMaxOwnerLength];
		Arrays.fill(ownerData, (byte)0);
		for(int i = 0; i < Owner.length() && i < cMaxOwnerLength; i++)
			ownerData[i] = (byte)Owner.charAt(i);
		
		file.write(ownerData);
	}
	
	public void read(RandomAccessFile file) throws IOException
	{
		Id = file.readInt();
		char[] ownerData = new char[cMaxOwnerLength];
		for(int i = 0; i < cMaxOwnerLength; i++)
			ownerData[i] = (char)file.readByte();
		
		Owner = String.valueOf(ownerData);
		if(Owner.indexOf(0) != -1)
			Owner = Owner.substring(0, Owner.indexOf(0));
	}
}