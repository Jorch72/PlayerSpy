package au.com.mineauz.PlayerSpy.search.interfaces;

import au.com.mineauz.PlayerSpy.Records.Record;

public interface ExtraDataModifier extends Modifier
{
	public String getExtraData(Record record);
}
