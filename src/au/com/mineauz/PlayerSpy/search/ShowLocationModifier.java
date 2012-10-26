package au.com.mineauz.PlayerSpy.search;

import au.com.mineauz.PlayerSpy.Records.ILocationAware;
import au.com.mineauz.PlayerSpy.Records.Record;
import au.com.mineauz.PlayerSpy.Utilities.Utility;

public class ShowLocationModifier extends Modifier
{

	@Override
	public String getExtraData( Record record )
	{
		if(record instanceof ILocationAware && ((ILocationAware)record).getLocation() != null)
			return "At " + Utility.locationToStringShort(((ILocationAware)record).getLocation());
		return null;
	}

}
