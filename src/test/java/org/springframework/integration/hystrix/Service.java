package org.springframework.integration.hystrix;

public interface Service {
	public String get(String str);
	public String throwException() throws MyException;
	public String withTimeout(String str);
	public int getThreadId();
	public int getNonThreadedThreadThreadId();
	String withZeroTimeout(String str);
	public String exceptionWithFallback(String testStr);
	public Throwable exceptionWithFallbackIncludingException(String testStr);
}
