package org.springframework.integration.hystrix;

public class CircuitBreakerFallbackMethodMissing extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CircuitBreakerFallbackMethodMissing(Class<?> clazz, String name, Class<?>[] params) {
		super("Cannot find fallback method: looking for " + clazz.getName() + " " + name + "(" + format(params) + ")");
	}

	private static String format(Class<?>[] params) {
		StringBuilder sb = new StringBuilder();
		for (Class<?> clazz : params) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(clazz.getName());
		}
		return sb.toString();
	}

}
