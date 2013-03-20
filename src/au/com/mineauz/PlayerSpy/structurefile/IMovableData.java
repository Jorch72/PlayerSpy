package au.com.mineauz.PlayerSpy.structurefile;

import java.io.IOException;

public interface IMovableData<T extends IndexEntry> extends IData<T>
{
	public void setLocation(long newLocation);
	
	public void saveChanges() throws IOException;
}
