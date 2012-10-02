package au.com.mineauz.PlayerSpy;

public class RecordId implements Cloneable
{
	public RecordId()
	{
		
	}
	public RecordId(int sessionId, int recordIndex)
	{
		SessionId = sessionId;
		RecordIndex = recordIndex;
	}
	public RecordId clone()
	{
		return new RecordId(SessionId,RecordIndex);
	}
	
	@Override
	public boolean equals(Object obj) 
	{
		if(obj instanceof RecordId)
			return ((RecordId)obj).RecordIndex == RecordIndex && ((RecordId)obj).SessionId == SessionId;
		return false;
	}
	@Override
	public int hashCode() 
	{
		int hash = 1;
		hash = hash * 17 + SessionId;
		hash = hash * 31 + RecordIndex;
		return hash;
	}
	@Override
	public String toString() 
	{
		return "RecordId {" + SessionId + ", " + RecordIndex + "}";
	}
	public int SessionId;
	public int RecordIndex;
}
