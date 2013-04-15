package test.au.com.mineauz.PlayerSpy.runtime;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import au.com.mineauz.PlayerSpy.globalreference.GlobalReferenceFile;

@SuppressWarnings("unused")
public class TestGlobalReference
{
	private static Random mRand;
	private static GlobalReferenceFile mReference;
	
	public static void test() throws IOException
	{
		mRand = new Random(1234);
		
		File tempFile = File.createTempFile("tgr", ".tmp");
		
		mReference = GlobalReferenceFile.create(tempFile);
		
		tempFile.delete();
	}
}
