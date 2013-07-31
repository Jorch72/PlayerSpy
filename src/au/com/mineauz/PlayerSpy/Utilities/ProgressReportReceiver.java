package au.com.mineauz.PlayerSpy.Utilities;

public interface ProgressReportReceiver<T>
{
	public void onProgressReport(T data);
}
