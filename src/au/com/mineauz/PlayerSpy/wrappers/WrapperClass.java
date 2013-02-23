package au.com.mineauz.PlayerSpy.wrappers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WrapperClass
{
	/**
	 * This is the full class name. * is a wild character
	 * @return
	 */
	public String value();
}
