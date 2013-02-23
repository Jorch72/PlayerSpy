package au.com.mineauz.PlayerSpy.wrappers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WrapperMethod
{
	public String name();
	
	public Class<?> returnType();
	
	public Class<?>[] parameterTypes() default {};
}
