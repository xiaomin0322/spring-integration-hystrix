package org.springframework.integration.hystrix;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.netflix.hystrix.exception.HystrixRuntimeException;

import jodd.bean.BeanUtil;

@Aspect
@Component
public class HystrixCommandAspect {

	@Around("@annotation(com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand)")
	public Object circuitBreakerAround(final ProceedingJoinPoint joinPoint) throws Throwable {
		com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand cb = getAnnotation(joinPoint);
		try {
			return getHystrixCommand(joinPoint, cb).execute();
		} catch (HystrixRuntimeException e) {
			return handleException(e, joinPoint, cb);
		}
	}

	private Object handleException(HystrixRuntimeException e, ProceedingJoinPoint joinPoint,
			com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand cb) throws Throwable {
		if (cb.fallbackMethod().length() > 0) {
			return executeFallback(e, joinPoint, cb);
		}
		if (e.getCause() instanceof TimeoutException) {
			throw new CircuitBreakerTimeoutException();
		}
		if (e.getCause() != null) {
			throw e.getCause();
		}
		throw e;
	}

	private Object executeFallback(HystrixRuntimeException e, ProceedingJoinPoint joinPoint,
			com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand cb)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method method = getMethod(joinPoint);
		Class<?> clazz = method.getDeclaringClass();
		String name = cb.fallbackMethod();
		Class<?> params[] = method.getParameterTypes();
		Object[] args = joinPoint.getArgs();

		Method m = ReflectionUtils.findMethod(clazz, name, params);
		if (m == null) {
			Class<?>[] temp = params;
			params = new Class<?>[params.length + 1];
			System.arraycopy(temp, 0, params, 0, temp.length);
			params[params.length - 1] = Throwable.class;

			Object[] tempArgs = args;
			args = new Object[tempArgs.length + 1];
			System.arraycopy(tempArgs, 0, args, 0, tempArgs.length);
			args[args.length - 1] = e.getCause() == null ? e : e.getCause();

			m = ReflectionUtils.findMethod(clazz, name, params);
		}
		if (m == null) {
			throw new CircuitBreakerFallbackMethodMissing(clazz, name, params);
		}
		return m.invoke(joinPoint.getTarget(), args);
	}

	private HystrixCommand<?> getHystrixCommand(final ProceedingJoinPoint joinPoint,
			com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand cb)
			throws NoSuchMethodException, SecurityException {

		@SuppressWarnings("rawtypes")
		HystrixCommand<?> theCommand = new HystrixCommand(getCommandSetter(joinPoint, cb)) {
			@Override
			protected Object run() throws Exception {
				try {
					return joinPoint.proceed();
				} catch (Exception e) {
					throw e;
				} catch (Throwable e) {
					throw new Exception(e);
				}
			}
		};
		return theCommand;
	}

	private com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand getAnnotation(
			final ProceedingJoinPoint joinPoint) {
		Method method = getMethod(joinPoint);
		return method.getAnnotation(com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand.class);
	}

	private Method getMethod(final ProceedingJoinPoint joinPoint) {
		try {
			final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
			Method method = methodSignature.getMethod();
			if (method.getDeclaringClass().isInterface()) {
				final String methodName = joinPoint.getSignature().getName();
				method = joinPoint.getTarget().getClass().getDeclaredMethod(methodName, method.getParameterTypes());
			}
			return method;
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * ¶ÏÂ·Æ÷ÅäÖÃ ²Î¿¼ http://hot66hot.iteye.com/blog/2155036
	 * 
	 * @param joinPoint
	 * @param cb
	 * @return
	 */
	private HystrixCommand.Setter getCommandSetter(ProceedingJoinPoint joinPoint,
			com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand cb) {
		String name = getHystrixGroupName(joinPoint, cb);
		String groupKey = StringUtils.isEmpty(cb.groupKey()) ? name : cb.groupKey();
		String commandKey = StringUtils.isEmpty(cb.commandKey()) ? name : cb.commandKey();
		HystrixThreadPoolKey hystrixThreadPoolKey = StringUtils.isEmpty(cb.threadPoolKey()) ? null
				: HystrixThreadPoolKey.Factory.asKey(cb.threadPoolKey());

		HystrixCommandProperties.Setter commandPropertiesDefaults = getHystrixCommandPropertiesSetter(cb);

		HystrixThreadPoolProperties.Setter threadPoolPropertiesDefaults = getHystrixThreadPoolPropertiesSetter(cb);

		return HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
				.andCommandKey(HystrixCommandKey.Factory.asKey(commandKey)).andThreadPoolKey(hystrixThreadPoolKey)
				.andCommandPropertiesDefaults(commandPropertiesDefaults)
				.andThreadPoolPropertiesDefaults(threadPoolPropertiesDefaults);
	}

	private String getHystrixGroupName(final ProceedingJoinPoint joinPoint,
			com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand cb) {
		String name = cb.groupKey().length() == 0 ? cb.commandKey() : cb.groupKey();
		return name.length() == 0 ? joinPoint.getSignature().toShortString() : name;
	}

	private HystrixCommandProperties.Setter getHystrixCommandPropertiesSetter(
			com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand hystrixCommand) {
		HystrixCommandProperties.Setter commandPropertiesDefaults = HystrixCommandProperties.defaultSetter();

		if (hystrixCommand.commandProperties() == null || hystrixCommand.commandProperties().length == 0) {
			return commandPropertiesDefaults;
		}

		Map<String, Object> commandProperties = new HashMap<String, Object>();
		for (HystrixProperty commandProperty : hystrixCommand.commandProperties()) {
			commandProperties.put(commandProperty.name(), commandProperty.value());
			BeanUtil.setDeclaredProperty(commandPropertiesDefaults, commandProperty.name(),
					commandProperty.value());
		}
		return commandPropertiesDefaults;
	}

	private HystrixThreadPoolProperties.Setter getHystrixThreadPoolPropertiesSetter(
			com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand hystrixCommand) {
		HystrixThreadPoolProperties.Setter commandPropertiesDefaults = HystrixThreadPoolProperties.defaultSetter();

		if (hystrixCommand.threadPoolProperties() == null || hystrixCommand.threadPoolProperties().length == 0) {
			return commandPropertiesDefaults;
		}
		Map<String, Object> commandProperties = new HashMap<String, Object>();
		for (HystrixProperty commandProperty : hystrixCommand.threadPoolProperties()) {
			commandProperties.put(commandProperty.name(), commandProperty.value());
				BeanUtil.setDeclaredProperty(commandPropertiesDefaults, commandProperty.name(),
						commandProperty.value());
		}
		return commandPropertiesDefaults;
	}

}

